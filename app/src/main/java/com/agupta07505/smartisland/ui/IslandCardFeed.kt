/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.IdleDotGeometry
import com.agupta07505.smartisland.data.IdleDotTap
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SettingsSource
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.collapsedPulses
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.data.idleDot
import com.agupta07505.smartisland.data.idleDotTapAction
import com.agupta07505.smartisland.data.landscapeCameraSide
import com.agupta07505.smartisland.model.ExemptMode
import com.agupta07505.smartisland.model.FoldPosture
import com.agupta07505.smartisland.model.FullscreenRules
import com.agupta07505.smartisland.model.IdleWidgetOff
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandExemption
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandPriority
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.landscapeWidgetPillWanted
import com.agupta07505.smartisland.model.pillOnIsland
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.ForegroundApp
import com.agupta07505.smartisland.util.IdleCloseSequence
import com.agupta07505.smartisland.util.NotificationTrace
import com.agupta07505.smartisland.util.SystemAnimScale
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the island shows: the posted cards (and the quick ones the pulse still needs), the settings and exemptions that
 * hide some of them, and the cards the idle timeout or a tap on the cutout put away. The view model passes in the
 * state it owns and keeps the public members as one-line delegates
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class IslandCardFeed(
    private val scope: CoroutineScope,
    private val settingsRepo: SettingsSource,
    private val notificationRepo: INotificationRepository,
    private val orientation: StateFlow<OrientationState>,
    private val foregroundPackage: StateFlow<String?>,
    private val isLocked: StateFlow<Boolean>,
    private val shadeOpen: StateFlow<Boolean>,
    private val expanded: StateFlow<Boolean>,
    private val isInputActive: StateFlow<Boolean>,
    private val secondaryApart: StateFlow<Boolean>,
    private val collapse: () -> Unit,
    private val widgetIslandHolds: () -> Boolean,
    private val elapsedMs: () -> Long = { SystemClock.elapsedRealtime() }
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeSettings: SharedFlow<SmartIslandSettings> = orientation
        .map { it.posture }
        .distinctUntilChanged()
        .flatMapLatest { posture ->
            // a plain phone reports posture Standard: the normal settings
            if (posture == FoldPosture.Standard) settingsRepo.settings else settingsRepo.forPosture(posture).settings
        }
        .shareIn(scope = scope, started = SharingStarted.Eagerly, replay = 1)

    val settings: StateFlow<SmartIslandSettings> = combine(
        activeSettings,
        orientation
    ) { raw, state -> raw.forOrientation(state) }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = SmartIslandSettings.Default
    )

    /** Landscape sibling picker: true when it opens on the left. */
    val pickerOnLeft: StateFlow<Boolean> = combine(
        activeSettings,
        orientation
    ) { raw, state ->
        state.isLandscape &&
            raw.landscapeCameraSide(state) == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val rawSettings: StateFlow<SmartIslandSettings> = activeSettings.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = SmartIslandSettings.Default
    )

    val pulse = CollapsedPulseDriver(
        scope = scope,
        nowMs = elapsedMs,
        holdPercent = { rawSettings.value.pulseHoldPercent }
    )

    val quickHold = QuickCardHolder(
        scope = scope,
        nowMs = elapsedMs
    )

    /** The posted cards, plus the quick ones the pulse still needs after their monitor removed them. */
    val notifications: StateFlow<List<IslandNotification>> = combine(
        notificationRepo.notifications,
        pulse.state,
        quickHold.changes,
        orientation.map { it.isLandscape }.distinctUntilChanged()
    ) { raw, pulseState, _, _ -> quickHold.merge(raw, pulseState) }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = notificationRepo.notifications.value
    )

    private val baseVisible: StateFlow<List<IslandNotification>> = combine(
        notifications,
        foregroundPackage,
        settings,
        isLocked
    ) { list, fgPkg, s, locked ->
        filterVisible(list, fgPkg, s, locked).also { shown ->
            traceHidden(list, shown) { notif ->
                val event = IslandEvent.forNotification(notif)
                if (event != null && !s.isEventEnabled(event.id)) "event-switch-off(${event.id})"
                else if (locked && (event?.let { s.isEventHiddenOnLockScreen(it.id) } == true ||
                        s.isAppHiddenOnLockScreen(notif.packageName, notif.mode))
                ) "hidden-on-lock-screen(${event?.id ?: notif.packageName})"
                else "live-card-hidden-foreground-app(${notif.mode}, $fgPkg)"
            }
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val idleHidden = MutableStateFlow<Set<IdleToken>>(emptySet())

    private val tapAwayCards = MutableStateFlow<Set<String>>(emptySet())

    val hasIdleStoredCards: StateFlow<Boolean> = combine(
        baseVisible,
        idleHidden,
        tapAwayCards
    ) { list, hidden, tapAway ->
        (hidden.isNotEmpty() && list.any { it.awayToken() in hidden }) ||
            list.any { IslandPriority.staysAwayAfterTap(it) && it.key in tapAway }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    private val userRevealedLive = MutableStateFlow<Set<String>>(emptySet())

    fun revealIdleCards(tapAnimation: Boolean = true) {
        if (tapAnimation && widgetIslandHolds()) return
        // a tap that brings the stored cards back is a tap to open
        if (tapAnimation) {
            _idleClosedByTap.value = false
            allowAutoExpandAgain()
        }
        if (tapAnimation && idleHidden.value.isNotEmpty()) {
            holdIdleTapActive(IdleCloseSequence.openingMs(openingSpeed()).toLong())
        }
        val hidden = idleHidden.value
        val brought = baseVisible.value
            .filter { it.awayToken() in hidden && liveMayTimeOut(it) }
            .mapTo(HashSet()) { it.key }
        if (brought.isNotEmpty()) userRevealedLive.update { it + brought }
        idleHidden.value = emptySet()
        tapAwayCards.value = emptySet()
    }

    /** Idle dot as its own tap target while cards are on the island. */
    val idleDot: StateFlow<IdleDotGeometry?> = combine(
        activeSettings,
        orientation
    ) { raw, state -> idleDot(raw, state) }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private var idleTapJob: Job? = null

    /** Island is shrinking, before the cards are put away. */
    val idleClosing = MutableStateFlow(false)

    private fun putAwayIdleCards() {
        val tokens = visibleNotifications.value.map { it.awayToken() }
        if (tokens.isNotEmpty()) idleHidden.update { it + tokens }
        val awayKeys = visibleNotifications.value.filter { IslandPriority.staysAwayAfterTap(it) }.map { it.key }
        if (awayKeys.isNotEmpty()) tapAwayCards.update { it + awayKeys }
    }

    val idleTapActive = MutableStateFlow(false)

    val idleLeaving = MutableStateFlow(false)

    private var idleTapActiveJob: Job? = null

    private fun holdIdleTapActive(sequenceMs: Long) {
        idleTapActive.value = true
        idleTapActiveJob?.cancel()
        val wait = (sequenceMs * SystemAnimScale.timeScale()).toLong() + IDLE_TAP_ACTIVE_MARGIN_MS
        idleTapActiveJob = scope.launch {
            delay(wait)
            idleTapActive.value = false
        }
    }

    private fun openingSpeed(): AnimationSpeed =
        settings.value.animationSpeed(orientation.value.isLandscape)

    private fun putAwayAllWithLead() {
        val wasExpanded = expanded.value
        _idleClosedByTap.value = true
        autoExpandBlockedByTap = true
        val edgeClose = orientation.value.isLandscape &&
            LandscapeLayoutStyle.isEdge(settings.value.landscapeLayoutStyle)
        val plan = idleClosePlan(openingSpeed(), edgeClose)
        val scale = SystemAnimScale.timeScale()
        val mergeLead = !wasExpanded && secondaryApart.value
        val step1Ms = if (wasExpanded || mergeLead) (plan.step1Ms * scale).toLong() else 0L
        val step2Ms = (plan.step2Ms * scale).toLong()
        holdIdleTapActive(plan.totalMs.toLong())
        idleTapJob?.cancel()
        // The flag goes up BEFORE the island collapses
        if (step1Ms > 0L) idleClosing.value = true
        if (wasExpanded) collapse()
        idleTapJob = scope.launch {
            val self = this.coroutineContext[Job]
            try {
                if (step1Ms > 0L) delay(step1Ms)
                // Opened again while shrinking: nothing is put away
                if (expanded.value) {
                    return@launch
                }
                idleLeaving.value = true
                putAwayIdleCards()
                userRevealedLive.value = emptySet()
                idleClosing.value = false
                delay(step2Ms + IDLE_TAP_ACTIVE_MARGIN_MS)
            } finally {
                // a newer tap has replaced this job: its flags are not ours to drop
                if (idleTapJob === self || idleTapJob == null) {
                    idleClosing.value = false
                    idleLeaving.value = false
                }
            }
        }
    }

    fun onIdleDotTapped(): IdleDotTap {
        val shown = visibleNotifications.value
        val raw = settings.value
        val action = idleDotTapAction(
            hasCards = shown.isNotEmpty(),
            hasIdleStoredCards = hasIdleStoredCards.value,
            emptyPillAvailable = orientation.value.isLandscape && raw.enableAppShortcuts && !raw.enableNotchMode &&
                !landscapeWidgetPill.value && !shadeOpen.value,
            emptyPillShown = _emptyPillShown.value
        )
        when (action) {
            IdleDotTap.RevealStored -> revealIdleCards()
            IdleDotTap.PutAwayNow -> putAwayAllWithLead()
            IdleDotTap.ShowEmptyPill -> showEmptyPill()
            IdleDotTap.HideEmptyPill -> hideEmptyPill()
            IdleDotTap.None -> Unit
        }
        return action
    }

    private val _pillHidden = MutableStateFlow(false)

    val pillHidden: StateFlow<Boolean> = _pillHidden

    private val hiddenByOrientation = BooleanArray(2)

    init {
        scope.launch {
            runSuspendCatchingLogged(TAG, "Orientation pill collector failed") {
                orientation.map { it.isLandscape }.distinctUntilChanged().collect { landscape ->
                    _pillHidden.value = hiddenByOrientation[if (landscape) 1 else 0]
                }
            }
        }
    }

    fun setPillHidden(hidden: Boolean) {
        hiddenByOrientation[if (orientation.value.isLandscape) 1 else 0] = hidden
        if (_pillHidden.value == hidden) return
        _pillHidden.value = hidden
        if (hidden && expanded.value && visibleNotifications.value.none { it.survivesPillHidden() }) {
            collapse()
        }
    }

    private val _emptyPillShown = MutableStateFlow(false)

    private var emptyPillJob: Job? = null

    private fun showEmptyPill() {
        _emptyPillShown.value = true
        emptyPillJob?.cancel()
        // "Never" keeps it until the next tap
        if (settings.value.idleTimeoutNever) return
        emptyPillJob = scope.launch {
            delay(settings.value.idleTimeoutMs.toLong() + IDLE_TIMER_EXTRA_MS)
            _emptyPillShown.value = false
        }
    }

    private fun hideEmptyPill() {
        emptyPillJob?.cancel()
        _emptyPillShown.value = false
    }

    private val _tickHold = MutableStateFlow(false)

    val tickHold: StateFlow<Boolean> = _tickHold

    fun setTickHold(hold: Boolean) {
        _tickHold.value = hold
    }

    private val _idleClosedByTap = MutableStateFlow(false)

    val idleWidgetsSuppressed: StateFlow<Boolean> = combine(
        _idleClosedByTap,
        _pillHidden,
        activeSettings,
        orientation,
        baseVisible
    ) { closedByTap, pillHidden, raw, state, stored ->
        IdleWidgetOff.suppressed(
            closedByTap = closedByTap,
            pillHidden = pillHidden,
            offAfterTapClose = if (state.isLandscape) raw.idleOffAfterTapCloseLandscape else raw.idleOffAfterTapClose,
            offAfterLongPress =
                if (state.isLandscape) raw.idleOffAfterLongPressLandscape else raw.idleOffAfterLongPress,
            hasStoredNotifications = stored.isNotEmpty(),
            offWhenStored = if (state.isLandscape) raw.idleOffWhenStoredLandscape else raw.idleOffWhenStored
        )
    }.distinctUntilChanged().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val islandExemption: StateFlow<ExemptMode> = combine(
        foregroundPackage,
        activeSettings,
        orientation
    ) { fg, raw, state ->
        IslandExemption.modeFor(
            fg, raw.islandExemptApps, raw.islandExemptAppsStatic, raw.islandExemptAppsLandscape, state.isLandscape
        )
    }.distinctUntilChanged().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = ExemptMode.None
    )

    val islandOffForApp: StateFlow<Boolean> = islandExemption
        .map { it == ExemptMode.Off }
        .distinctUntilChanged()
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = false)

    /** Portrait: the app in the foreground keeps the collapsed pill on screen. */
    val islandStaticForApp: StateFlow<Boolean> = islandExemption
        .map { it == ExemptMode.Static }
        .distinctUntilChanged()
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = false)

    fun exemptionNow(): ExemptMode {
        val raw = rawSettings.value
        return IslandExemption.modeFor(
            foregroundPackage.value, raw.islandExemptApps, raw.islandExemptAppsStatic, raw.islandExemptAppsLandscape,
            orientation.value.isLandscape
        )
    }

    val visibleUnordered: StateFlow<List<IslandNotification>> = combine(
        baseVisible,
        idleHidden,
        _pillHidden,
        shadeOpen,
        tapAwayCards
    ) { list, hidden, pillHidden, shade, tapAway ->
        if (shade) {
            traceHidden(list, emptyList()) { "notification-shade-open" }
            return@combine emptyList<IslandNotification>()
        }
        val shown = if (hidden.isEmpty() && tapAway.isEmpty()) list
        else list.filterNot {
            (hidden.isNotEmpty() && it.awayToken() in hidden)
                || (IslandPriority.staysAwayAfterTap(it) && it.key in tapAway)
        }
        traceHidden(list, shown) { "idle-timeout-put-away" }
        applyPillHidden(shown, pillHidden).also { visible ->
            traceHidden(shown, visible) { "pill-hidden(landscape or long press: only calls stay)" }
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private fun traceHidden(
        before: List<IslandNotification>,
        after: List<IslandNotification>,
        reason: (IslandNotification) -> String
    ) {
        if (!AppLogRecorder.isRecording || before.size == after.size) return
        runCatchingLogged(TAG, "Could not log the removed cards") {
            val keptKeys = after.mapTo(HashSet()) { it.key }
            before.filter { it.key !in keptKeys }.forEach { notif ->
                val why = reason(notif)
                if (NotificationTrace.shouldLog("vm|${notif.key}", why, false)) {
                    AppLogRecorder.i(
                        "NotifTrace",
                        "HIDDEN-IN-VIEWMODEL pkg=${notif.packageName} key=${notif.key} mode=${notif.mode} reason=$why"
                    )
                }
            }
        }
    }

    /** What the island shows, in priority order. */
    val visibleNotifications: StateFlow<List<IslandNotification>> = visibleUnordered
        .map { IslandPriority.order(it) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // ---- Fullscreen hide: the island is off while a fullscreen app has no status bar (see FullscreenRules) ----

    private val _fullscreen = MutableStateFlow(false)

    private val _fullscreenPeek = MutableStateFlow(false)

    private var fullscreenPeekJob: Job? = null

    private var knownCardKeys: Set<String>? = null

    /** A call that rings or connects needs the user, so it is never hidden. */
    private val callNeedsUser: StateFlow<Boolean> = baseVisible
        .map { list -> list.any { it.survivesPillHidden() } }
        .distinctUntilChanged()
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = false)

    /** The island draws nothing and only the camera cutout takes a touch. */
    /** The "hide in shade" switch is on and the shade or quick settings is open: nothing is drawn, no peek, no exception. */
    private val shadeHides: Flow<Boolean> = combine(
        activeSettings.map { it.hideInShade }.distinctUntilChanged(),
        shadeOpen
    ) { on, open -> on && open }

    val fullscreenHidden: StateFlow<Boolean> = combine(
        _fullscreen,
        _fullscreenPeek,
        expanded,
        callNeedsUser,
        shadeHides
    ) { fullscreen, peeking, open, call, shade ->
        FullscreenRules.hiddenWithShade(fullscreen, peeking, open, call, shade)
    }.distinctUntilChanged().stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = false)

    init {
        scope.launch {
            runSuspendCatchingLogged(TAG, "Fullscreen event collector failed") {
                baseVisible.collect { list ->
                    val keys = list.mapTo(HashSet()) { it.key }
                    val before = knownCardKeys
                    knownCardKeys = keys
                    // a new event pulses for a moment, then the island is hidden again
                    if (before != null && _fullscreen.value && keys.any { it !in before }) {
                        peekFromFullscreen(FullscreenRules.EVENT_PEEK_MS)
                    }
                }
            }
        }
    }

    fun setFullscreen(on: Boolean) {
        if (_fullscreen.value == on) return
        _fullscreen.value = on
        // leaving fullscreen shows the island as usual, entering it starts hidden
        fullscreenPeekJob?.cancel()
        _fullscreenPeek.value = false
    }

    /** Shows the island for [ms], then hides it again once no card is open. */
    fun peekFromFullscreen(ms: Long) {
        if (!_fullscreen.value) return
        _fullscreenPeek.value = true
        fullscreenPeekJob?.cancel()
        fullscreenPeekJob = scope.launch {
            delay(ms)
            while (!FullscreenRules.peekCanEnd(expanded.value, pulse.state.value.busy)) {
                if (expanded.value) {
                    // an open card holds the peek, the wait starts again when it closes
                    expanded.first { !it }
                    delay(ms)
                } else {
                    // a running pulse holds the peek, the island closes when the pulses are over
                    pulse.state.first { !it.busy }
                }
            }
            _fullscreenPeek.value = false
        }
    }

    val landscapeWidgetPill: StateFlow<Boolean> = combine(
        activeSettings,
        orientation,
        _pillHidden,
        shadeOpen,
        combine(idleWidgetsSuppressed, _tickHold, _emptyPillShown) { suppressed, hold, empty ->
            Triple(suppressed, hold, empty) }
    ) { raw, state, pillHidden, shade, (suppressed, hold, empty) ->
        landscapeWidgetPillWanted(
            isLandscape = state.isLandscape,
            widgetsEnabled = if (state.isLandscape) raw.idleWidgetsEnabledLandscape else raw.idleWidgetsEnabled,
            widgetIds = raw.idleWidgetIds,
            pillHidden = pillHidden,
            shadeOpen = shade,
            widgetsSuppressed = suppressed,
            tickHold = hold,
            emptyPillShown = empty
        )
    }.distinctUntilChanged().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val displaySettings: StateFlow<SmartIslandSettings> = combine(
        activeSettings,
        orientation,
        visibleNotifications,
        landscapeWidgetPill
    ) { raw, state, cards, widgetPill ->
        raw.forOrientation(state, hasContent = pillOnIsland(cards.isNotEmpty(), widgetPill),
            hasCards = cards.isNotEmpty())
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = SmartIslandSettings.Default
    )

    /** The collapsed pill pulses ([collapsedPulses]) as a flow for the overlay, which draws the wide pulse only then. */
    val pulseDriven: StateFlow<Boolean> = combine(rawSettings, orientation) { raw, state -> raw.collapsedPulses(state) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = rawSettings.value.collapsedPulses(orientation.value)
        )

    private val idleTimeoutRestart = MutableStateFlow(0)

    /** Swipe is on its way or has fired. */
    fun restartIdleTimeout() {
        idleTimeoutRestart.update { it + 1 }
        if (_emptyPillShown.value) showEmptyPill()
    }

    @Volatile var autoExpandBlockedByTap = false
        private set

    fun allowAutoExpandAgain() {
        autoExpandBlockedByTap = false
    }

    /** Every card the island may show, the ones put away included. */
    fun allCards(): List<IslandNotification> = IslandPriority.order(
        applyPillHidden(
            visibleWith(foregroundPackage.value),
            _pillHidden.value
        )
    )

    /** The posted cards the island may show with [foreground] as the app in front. */
    fun visibleWith(foreground: String?): List<IslandNotification> =
        filterVisible(notifications.value, foreground, settings.value, isLocked.value)

    fun setIdleClosedByTap(closed: Boolean) {
        _idleClosedByTap.value = closed
    }

    fun refreshEmptyPill() {
        if (_emptyPillShown.value) showEmptyPill()
    }

    fun holdEmptyPill() {
        emptyPillJob?.cancel()
    }

    private data class IdleToken(val key: String, val timeMillis: Long, val contentHash: Int)

    private fun IslandNotification.idleToken() = IdleToken(key, timeMillis, (title to text).hashCode())

    private fun IslandNotification.awayToken() =
        if (IslandPriority.canIdleOut(this)) idleToken() else IdleToken(key, 0L, liveAwayHash(this))

    private fun liveMayTimeOut(n: IslandNotification) =
        !IslandPriority.canIdleOut(n) && n.mode != IslandMode.IncomingCall &&
            n.mode != IslandMode.Navigation &&
            !(n.mode == IslandMode.LiveActivity && !orientation.value.isLandscape) &&
            // A running timer, stopwatch, recording or transfer stays on the pill: only the idle timeout is skipped,
            // auto-collapse and the cutout tap that puts cards away still apply
            !keepsPillVisible(listOf(n))

    fun start() {
        scope.launch {
            runSuspendCatchingLogged(TAG, "Idle-token cleanup failed") {
                notifications.collect { raw ->
                    // a card keeps its token only while it is still the same card in the same state
                    val live = raw.map { it.awayToken() }.toSet()
                    idleHidden.update { hidden ->
                        val kept = hidden.filterTo(HashSet()) { it in live }
                        if (kept.size == hidden.size) hidden else kept
                    }
                    val rawKeys = raw.mapTo(HashSet()) { it.key }
                    tapAwayCards.update { away ->
                        if (away.all { it in rawKeys }) away else away.filterTo(HashSet()) { it in rawKeys }
                    }
                    userRevealedLive.update { revealed ->
                        if (revealed.all { it in rawKeys }) revealed else revealed.filterTo(HashSet()) { it in rawKeys }
                    }
                }
            }
        }
        scope.launch {
            runSuspendCatchingLogged(TAG, "Static idle timeout failed") {
                // Hiding during a pulse cuts it off. The idle timer starts over once the queue clears.
                combine(visibleNotifications, expanded, isInputActive,
                    idleTimeoutRestart, pulse.state.map { it.busy }.distinctUntilChanged()
                ) { list, isExpanded, inputActive, restart, pulsing ->
                    if (isExpanded || inputActive || pulsing || list.isEmpty()) null
                    else if (list.any { !IslandPriority.canIdleOut(it) }) null
                    else list.map { it.key } to restart
                }.distinctUntilChanged().collectLatest { keys ->
                    if (keys == null) return@collectLatest
                    if (settings.value.idleTimeoutNever) return@collectLatest
                    delay(settings.value.idleTimeoutMs.toLong() + IDLE_TIMER_EXTRA_MS)
                    val tokens = visibleNotifications.value
                        .filter { IslandPriority.canIdleOut(it) }
                        .map { it.idleToken() }
                    idleHidden.update { it + tokens }
                }
            }
        }
        scope.launch {
            runSuspendCatchingLogged(TAG, "Live idle timeout failed") {
                // Live idle timeout: a live card
                combine(
                    visibleNotifications, expanded, isInputActive, userRevealedLive,
                    // a pulse running (or waiting in the gap) holds the timeout, it counts again after the pulses
                    combine(orientation, idleTimeoutRestart, pulse.state.map { it.busy }.distinctUntilChanged()
                    ) { _, restart, pulsing -> restart to pulsing }
                ) { list, isExpanded, inputActive, revealed, (restart, pulsing) ->
                    if (isExpanded || inputActive || pulsing) null
                    else list.filter { liveMayTimeOut(it) && it.key !in revealed }
                        .mapTo(HashSet()) { it.key }
                        .takeIf { it.isNotEmpty() }
                        ?.let { it to restart }
                }.distinctUntilChanged().collectLatest { pending ->
                    if (pending == null) return@collectLatest
                    val keys = pending.first
                    if (settings.value.idleTimeoutNever) return@collectLatest
                    delay(settings.value.idleTimeoutMs.toLong() + IDLE_TIMER_EXTRA_MS)
                    val tokens = visibleNotifications.value
                        .filter { it.key in keys && liveMayTimeOut(it) }
                        .map { it.awayToken() }
                    if (tokens.isNotEmpty()) idleHidden.update { it + tokens }
                }
            }
        }
    }

    companion object {
        private const val TAG = "IslandCardFeed"

        // A little breathing room after the pulse queue clears
        const val IDLE_TIMER_EXTRA_MS = 1_000L

        /** Slack added to an idle-tap sequence's flags. */
        const val IDLE_TAP_ACTIVE_MARGIN_MS = 400L

        internal fun filterVisible(
            list: List<IslandNotification>,
            foregroundPackage: String?,
            settings: SmartIslandSettings,
            locked: Boolean = false
        ): List<IslandNotification> = list.filterNot { notif ->
            val event = IslandEvent.forNotification(notif)
            (event != null && !settings.isEventEnabled(event.id)) ||
                (locked && event != null && settings.isEventHiddenOnLockScreen(event.id)) ||
                // the per-app lock-screen rule (calls excepted)
                (locked && settings.isAppHiddenOnLockScreen(notif.packageName, notif.mode)) ||
                (ForegroundApp.hidesCard(notif, foregroundPackage) && !settings.keepsCardInForeground(notif))
        }
        /**
         * While the pill is hidden only a call that needs the user shows (ringing or dialing), an ongoing call is a
         * live card so the hold hides it too
         */
        internal fun applyPillHidden(list: List<IslandNotification>, pillHidden: Boolean): List<IslandNotification> =
            if (pillHidden) list.filter { it.survivesPillHidden() } else list

        internal fun IslandNotification.survivesPillHidden(): Boolean =
            mode == IslandMode.IncomingCall && (isCallRinging || isCallConnecting)

        /** Part of a live card's put-away token that is not its key. */
        @Suppress("UNUSED_PARAMETER")
        internal fun liveAwayHash(n: IslandNotification): Int = 0
    }
}
