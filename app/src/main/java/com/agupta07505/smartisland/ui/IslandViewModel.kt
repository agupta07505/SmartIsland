/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.IdleWidgetOff
import com.agupta07505.smartisland.model.ExemptMode
import com.agupta07505.smartisland.model.IslandExemption
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.data.landscapeCameraSide
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandPriority
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.appGroupKey
import com.agupta07505.smartisland.model.landscapeWidgetPillWanted
import com.agupta07505.smartisland.model.pillOnIsland
import com.agupta07505.smartisland.model.nextAppGroupIndex
import com.agupta07505.smartisland.model.nextSiblingIndex
import com.agupta07505.smartisland.model.siblingIndices
import com.agupta07505.smartisland.util.AppEventRules
import com.agupta07505.smartisland.util.ExpandedSelection
import com.agupta07505.smartisland.util.IslandTimings
import kotlinx.coroutines.flow.collectLatest
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModel(
    private val settingsRepo: SmartIslandSettingsRepository,
    val notificationRepo: INotificationRepository
) : ViewModel() {

    /** Current display orientation; the overlay service keeps this up to date on rotation. */
    val orientation = MutableStateFlow(OrientationState())

    /** True while the phone is turning and the island is hidden until the new orientation has settled (model/RotationVeil.kt). */
    val rotationVeil = MutableStateFlow(false)

    val settings: StateFlow<SmartIslandSettings> = combine(
        settingsRepo.settings,
        orientation
    ) { raw, state -> raw.forOrientation(state) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SmartIslandSettings.Default
    )

    /**
     * Landscape sibling picker: true when it opens on the left, i.e. the camera is on the right (reverse landscape),
     * so the picker always sits on the side away from the camera.
     */
    val pickerOnLeft: StateFlow<Boolean> = combine(
        settingsRepo.settings,
        orientation
    ) { raw, state ->
        state.isLandscape &&
            raw.landscapeCameraSide(state) == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    fun settingsSnapshot(): SmartIslandSettings = rawSettings.value

    private val rawSettings: StateFlow<SmartIslandSettings> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SmartIslandSettings.Default
    )

    val notifications = notificationRepo.notifications
    val foregroundPackage = MutableStateFlow<String?>(null)

    /**
     * True while the phone is locked (set by the overlay service). Declared before the flows below that read it: they
     * are built in this order, and a property read earlier would still be null.
     */
    val isLocked = MutableStateFlow(false)

    private var selectionKeys: List<String> = emptyList()

    private var pendingSelectKey: String? = null

    private val baseVisible: StateFlow<List<IslandNotification>> = combine(
        notifications,
        foregroundPackage,
        settings,
        isLocked
    ) { list, fgPkg, s, locked ->
        filterVisible(list, fgPkg, s, locked).also { shown ->
            traceHidden(list, shown) { notif ->
                val event = com.agupta07505.smartisland.model.IslandEvent.forNotification(notif)
                if (event != null && !s.isEventEnabled(event.id)) "event-switch-off(${event.id})"
                else if (locked && (event?.let { s.isEventHiddenOnLockScreen(it.id) } == true ||
                        s.isAppHiddenOnLockScreen(notif.packageName, notif.mode))
                ) "hidden-on-lock-screen(${event?.id ?: notif.packageName})"
                else "live-card-hidden-foreground-app(${notif.mode}, $fgPkg)"
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val idleHidden = MutableStateFlow<Set<IdleToken>>(emptySet())

    /** True while the idle timeout has put cards away that a tap on the idle cutout can bring back. */
    val hasIdleStoredCards: StateFlow<Boolean> = combine(
        baseVisible,
        idleHidden
    ) { list, hidden ->
        hidden.isNotEmpty() && list.any { it.awayToken() in hidden }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    private val userRevealedLive = MutableStateFlow<Set<String>>(emptySet())

    /** Half-typed quick replies per notification; ended by a send or when the notification is gone. */
    val replyDrafts = com.agupta07505.smartisland.model.ReplyDrafts()

    private val callInterruptReplyKey = MutableStateFlow<String?>(null)

    /**
     * Tap on the idle cutout: the cards the idle timeout put away come back. Live cards brought back this way get NO
     * auto timer; the static ones idle out again by the normal countdown.
     */
    fun revealIdleCards(tapAnimation: Boolean = true) {
        if (tapAnimation && widgetIslandHolds()) return
        // A tap that brings the stored cards back is a tap to open: the widgets may show again (the automatic reveal is not a tap).
        if (tapAnimation) _idleClosedByTap.value = false
        // Portrait only animates a card's arrival on the idle cutout for a tap (the same slide-in as landscape). The flag
        // goes up before the cards come back, so the overlay sees it on the frame they arrive.
        if (tapAnimation && idleHidden.value.isNotEmpty()) {
            holdIdleTapActive(com.agupta07505.smartisland.model.IdleCloseSequence.openingMs(openingSpeed()).toLong())
        }
        val hidden = idleHidden.value
        val brought = baseVisible.value
            .filter { it.awayToken() in hidden && liveMayTimeOut(it) }
            .mapTo(HashSet()) { it.key }
        if (brought.isNotEmpty()) userRevealedLive.update { it + brought }
        idleHidden.value = emptySet()
    }

    /**
     * The idle dot as its own tap target while cards are on the island: landscape always, portrait while expanded.
     * Follows rotation, the landscape circle settings and the camera cutout.
     */
    val idleDot: StateFlow<com.agupta07505.smartisland.model.IdleDotGeometry?> = combine(
        settingsRepo.settings,
        orientation
    ) { raw, state -> com.agupta07505.smartisland.model.idleDot(raw, state) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private var idleTapJob: kotlinx.coroutines.Job? = null

    /**
     * True during step 1 of the close: the expanded island closes and the secondary cutout merges (the overlay forces
     * the merge), the pill stays.
     */
    val idleClosing = MutableStateFlow(false)

    private fun putAwayIdleCards() {
        val tokens = visibleNotifications.value.map { it.awayToken() }
        if (tokens.isNotEmpty()) idleHidden.update { it + tokens }
    }

    /**
     * True while a tap on the idle circle runs its open or close sequence
     * ([com.agupta07505.smartisland.model.IdleCloseSequence]).
     */
    val idleTapActive = MutableStateFlow(false)

    /** True during step 2 of the close: the pill's exit runs on the sequence's own spring (the expanded close is over). */
    val idleLeaving = MutableStateFlow(false)

    private var idleTapActiveJob: Job? = null

    private fun holdIdleTapActive(sequenceMs: Long) {
        idleTapActive.value = true
        idleTapActiveJob?.cancel()
        val wait = (sequenceMs * com.agupta07505.smartisland.util.SystemAnimScale.timeScale()).toLong() + IDLE_TAP_ACTIVE_MARGIN_MS
        idleTapActiveJob = viewModelScope.launch {
            delay(wait)
            idleTapActive.value = false
        }
    }

    private fun openingSpeed(): com.agupta07505.smartisland.util.AnimationSpeed =
        settings.value.animationSpeed(orientation.value.isLandscape)

    private fun putAwayAllWithLead() {
        val wasExpanded = expanded.value
        // Putting every card away with a tap on the cutout is a tap to close: "off after tap to close" holds the widgets off from here.
        _idleClosedByTap.value = true
        val plan = com.agupta07505.smartisland.model.IdleCloseSequence.plan(openingSpeed())
        // The waits are plain wall-clock time, so the system animator scale is applied here (once). Animations off: scale 0,
        // no waits, everything happens at once.
        val scale = com.agupta07505.smartisland.util.SystemAnimScale.timeScale()
        val step1Ms = if (wasExpanded) (plan.step1Ms * scale).toLong() else 0L
        val step2Ms = (plan.step2Ms * scale).toLong()
        holdIdleTapActive(plan.totalMs.toLong())
        idleTapJob?.cancel()
        // The flag goes up BEFORE the island collapses, so the overlay sees it on the very frame the collapse starts
        // and picks the idle-circle close spring for it.
        if (step1Ms > 0L) idleClosing.value = true
        if (wasExpanded) collapse()
        idleTapJob = viewModelScope.launch {
            val self = this.coroutineContext[Job]
            try {
                if (step1Ms > 0L) delay(step1Ms)
                // Opened again during step 1: the close is over, nothing is put away.
                if (expanded.value) {
                    com.agupta07505.smartisland.util.AppLogRecorder.i("IdleDot", "close aborted: the island expanded again during step 1 (a new card or a tap); nothing put away")
                    return@launch
                }
                // Step 2: the exit flag first, so the overlay knows which spring the pill leaves on when the cards go.
                idleLeaving.value = true
                putAwayIdleCards()
                // After a tap put-away the next live events (not calls) run the auto timer like any new event.
                userRevealedLive.value = emptySet()
                idleClosing.value = false
                delay(step2Ms + IDLE_TAP_ACTIVE_MARGIN_MS)
            } finally {
                // A newer tap has replaced this job: its flags are not ours to drop.
                if (idleTapJob === self || idleTapJob == null) {
                    idleClosing.value = false
                    idleLeaving.value = false
                }
            }
        }
    }

    fun onIdleDotTapped(): com.agupta07505.smartisland.model.IdleDotTap {
        val shown = visibleNotifications.value
        val raw = settings.value
        val action = com.agupta07505.smartisland.model.idleDotTapAction(
            hasCards = shown.isNotEmpty(),
            hasIdleStoredCards = hasIdleStoredCards.value,
            emptyPillAvailable = orientation.value.isLandscape && raw.enableAppShortcuts && !raw.enableNotchMode &&
                !landscapeWidgetPill.value && !shadeOpen.value,
            emptyPillShown = _emptyPillShown.value
        )
        when (action) {
            com.agupta07505.smartisland.model.IdleDotTap.RevealStored -> revealIdleCards()
            com.agupta07505.smartisland.model.IdleDotTap.PutAwayNow -> putAwayAllWithLead()
            com.agupta07505.smartisland.model.IdleDotTap.ShowEmptyPill -> showEmptyPill()
            com.agupta07505.smartisland.model.IdleDotTap.HideEmptyPill -> hideEmptyPill()
            com.agupta07505.smartisland.model.IdleDotTap.None -> Unit
        }
        com.agupta07505.smartisland.util.AppLogRecorder.i(
            "IdleDot",
            "tap action=$action cards=${shown.size} idleable=${shown.count { IslandPriority.canIdleOut(it) }} " +
                "storedAway=${hasIdleStoredCards.value}"
        )
        return action
    }

    /**
     * True while the notification shade (or quick settings) is pulled down. Every event is hidden then and only the
     * idle cutout stays; the overlay service sets it from the accessibility windows.
     */
    val shadeOpen = MutableStateFlow(false)

    fun setShadeOpen(open: Boolean) {
        if (shadeOpen.value == open) return
        shadeOpen.value = open
        // An open card must not stay up under the shade, and neither must the widget island.
        if (open) {
            collapse()
            closeWidgetIsland(snap = true)
        }
    }

    private val _pillHidden = MutableStateFlow(false)
    val pillHidden: StateFlow<Boolean> = _pillHidden

    fun setPillHidden(hidden: Boolean) {
        if (_pillHidden.value == hidden) return
        _pillHidden.value = hidden
        // Hiding while a non-call card is open would leave an expanded island with nothing in it.
        if (hidden && expanded.value && visibleNotifications.value.none { it.mode == IslandMode.IncomingCall }) {
            collapse()
        }
    }

    fun togglePillHidden() = setPillHidden(!_pillHidden.value)

    private val _emptyPillShown = MutableStateFlow(false)
    private var emptyPillJob: Job? = null

    private fun showEmptyPill() {
        _emptyPillShown.value = true
        emptyPillJob?.cancel()
        // "Never" keeps it until the next tap.
        if (settings.value.idleTimeoutMs >= SmartIslandSettings.IDLE_TIMEOUT_NEVER_MS) return
        emptyPillJob = viewModelScope.launch {
            delay(settings.value.idleTimeoutMs.toLong() + IDLE_TIMER_EXTRA_MS)
            _emptyPillShown.value = false
        }
    }

    private fun keepEmptyPillAfterLauncher() {
        if (appsLauncherOpen.value && _emptyPillShown.value) showEmptyPill()
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

    /**
     * The idle widgets are held off the pill right now: the chosen orientation's "off after tap to close" / "off
     * after long press to hide" switches, together with what just happened ([IdleWidgetOff]).
     */
    val idleWidgetsSuppressed: StateFlow<Boolean> = combine(
        _idleClosedByTap,
        _pillHidden,
        settingsRepo.settings,
        orientation,
        baseVisible
    ) { closedByTap, pillHidden, raw, state, stored ->
        IdleWidgetOff.suppressed(
            closedByTap = closedByTap,
            pillHidden = pillHidden,
            offAfterTapClose = if (state.isLandscape) raw.idleOffAfterTapCloseLandscape else raw.idleOffAfterTapClose,
            offAfterLongPress = if (state.isLandscape) raw.idleOffAfterLongPressLandscape else raw.idleOffAfterLongPress,
            hasStoredNotifications = stored.isNotEmpty(),
            offWhenStored = if (state.isLandscape) raw.idleOffWhenStoredLandscape else raw.idleOffWhenStored
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    /**
     * The app in the foreground is exempt in the current orientation (Settings > Exempt apps): the island is totally
     * off while it stays there.
     */
    val islandExemption: StateFlow<ExemptMode> = combine(
        foregroundPackage,
        settingsRepo.settings,
        orientation
    ) { fg, raw, state ->
        IslandExemption.modeFor(
            fg, raw.islandExemptApps, raw.islandExemptAppsStatic, raw.islandExemptAppsLandscape, state.isLandscape
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ExemptMode.None
    )

    /** The island is totally off for the app in the foreground (window hidden, touches go to the app). */
    val islandOffForApp: StateFlow<Boolean> = islandExemption
        .map { it == ExemptMode.Off }
        .distinctUntilChanged()
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = false)

    /** Portrait: the app in the foreground keeps the collapsed pill on screen, but dead (nothing on it, no touches). */
    val islandStaticForApp: StateFlow<Boolean> = islandExemption
        .map { it == ExemptMode.Static }
        .distinctUntilChanged()
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = false)

    /** The exemption right now, computed from the current values instead of the flow above. */
    fun exemptionNow(): ExemptMode {
        val raw = rawSettings.value
        return IslandExemption.modeFor(
            foregroundPackage.value, raw.islandExemptApps, raw.islandExemptAppsStatic, raw.islandExemptAppsLandscape,
            orientation.value.isLandscape
        )
    }

    private val visibleUnordered: StateFlow<List<IslandNotification>> = combine(
        baseVisible,
        idleHidden,
        _pillHidden,
        shadeOpen
    ) { list, hidden, pillHidden, shade ->
        // The notification shade is open: no event is shown, only the idle cutout stays (cards wait in the repository).
        if (shade) {
            traceHidden(list, emptyList()) { "notification-shade-open" }
            return@combine emptyList<IslandNotification>()
        }
        val shown = if (hidden.isEmpty()) list
        else list.filterNot { it.awayToken() in hidden }
        traceHidden(list, shown) { "idle-timeout-put-away" }
        applyPillHidden(shown, pillHidden).also { visible ->
            traceHidden(shown, visible) { "pill-hidden(landscape or long press: only calls stay)" }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private fun traceHidden(
        before: List<IslandNotification>,
        after: List<IslandNotification>,
        reason: (IslandNotification) -> String
    ) {
        if (!com.agupta07505.smartisland.util.AppLogRecorder.isRecording || before.size == after.size) return
        runCatching {
            val keptKeys = after.mapTo(HashSet()) { it.key }
            before.filter { it.key !in keptKeys }.forEach { notif ->
                val why = reason(notif)
                if (com.agupta07505.smartisland.util.NotificationTrace.shouldLog("vm|${notif.key}", why, false)) {
                    com.agupta07505.smartisland.util.AppLogRecorder.i(
                        "NotifTrace",
                        "HIDDEN-IN-VIEWMODEL pkg=${notif.packageName} key=${notif.key} mode=${notif.mode} reason=$why"
                    )
                }
            }
        }
    }

    private val popupKey = MutableStateFlow<String?>(null)

    /**
     * What the island shows, in priority order: index 0 owns the primary cutout, index 1 the secondary round cutout.
     */
    val visibleNotifications: StateFlow<List<IslandNotification>> = combine(
        visibleUnordered,
        popupKey
    ) { list, popup -> IslandPriority.order(list, popup) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * Landscape: the collapsed pill stays on screen to carry the idle widgets while no card is on the island (the
     * idle circle remains at its own spot as the toggle). See model/LandscapeWidgetPill.kt.
     */
    val landscapeWidgetPill: StateFlow<Boolean> = combine(
        settingsRepo.settings,
        orientation,
        _pillHidden,
        shadeOpen,
        combine(idleWidgetsSuppressed, _tickHold, _emptyPillShown) { suppressed, hold, empty -> Triple(suppressed, hold, empty) }
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
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    /**
     * [settings] plus the content-dependent collapsed geometry (with cards on the island the landscape cutout is the vertical
     * corner pill). The overlay window, its touch region and the island view read this one; everything in this class keeps
     * reading [settings] (the cards depend on it, so using this flow there would be circular).
     */
    val displaySettings: StateFlow<SmartIslandSettings> = combine(
        settingsRepo.settings,
        orientation,
        visibleNotifications,
        landscapeWidgetPill
    ) { raw, state, cards, widgetPill ->
        // The landscape widget pill keeps the collapsed pill's geometry while no card is on the island.
        raw.forOrientation(state, hasContent = pillOnIsland(cards.isNotEmpty(), widgetPill), hasCards = cards.isNotEmpty())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SmartIslandSettings.Default
    )

    val expanded = MutableStateFlow(false)
    /**
     * The semi-expanded idle widget island is open (a tap on the idle widget's glyph). The overlay draws it; the
     * service gives the window the whole frame while it is open, so a tap outside can close it.
     */
    val widgetIslandOpen = MutableStateFlow(false)
    /**
     * Counts the closes that skip the closing animation (shade, screen off, lock, rotation); the overlay snaps the
     * island shut when it changes.
     */
    val widgetIslandSnap = MutableStateFlow(0)
    @Volatile var widgetIslandSnapPending = false
        private set
    private var widgetIslandOpenedAt = 0L

    /** The widget island is drawn at all (growing, open or shrinking back into the cutout); set by the overlay. */
    val widgetIslandShown = MutableStateFlow(false)
    private var deferredExpandKey: String? = null

    private fun widgetIslandHolds(): Boolean =
        com.agupta07505.smartisland.model.widgetIslandHoldsExpand(widgetIslandOpen.value, widgetIslandShown.value)

    fun setWidgetIslandOpen(open: Boolean) {
        // An exempt app is in front: the island stays shut.
        if (open && exemptionNow() != ExemptMode.None) return
        if (open) {
            widgetIslandSnapPending = false
            widgetIslandOpenedAt = android.os.SystemClock.elapsedRealtime()
        }
        if (widgetIslandOpen.value != open) widgetIslandOpen.value = open
    }

    /**
     * Closes the widget island for a reason outside it: the shade, the screen turning off, the lock, a rotation
     * ([snap] = no closing animation), or another window coming up ([snap] false, with [graceMs] after the open so
     * the tap that opened it is never mistaken for one).
     */
    fun closeWidgetIsland(snap: Boolean, graceMs: Long = 0L) {
        if (!widgetIslandOpen.value) return
        if (graceMs > 0L && android.os.SystemClock.elapsedRealtime() - widgetIslandOpenedAt < graceMs) return
        if (snap) {
            widgetIslandSnapPending = true
            widgetIslandSnap.value = widgetIslandSnap.value + 1
        }
        widgetIslandOpen.value = false
    }
    val selectedIndex = MutableStateFlow(0)
    /**
     * True while the expanded island shows the list of the current app's cards (the sibling picker) instead of the
     * card pager. Always false while collapsed.
     */
    val siblingPickerOpen = MutableStateFlow(false)

    /**
     * The notification picture or sticker open at full size in the expanded card: the card's key and the picture's
     * position. Closed first by a tap outside the island.
     */
    val expandedMedia = MutableStateFlow<Pair<String, Int>?>(null)

    fun setExpandedMedia(key: String?, index: Int) {
        // A picture opened directly (a thumbnail tap) is at full size, never left at a stale half-size step.
        clearPictureHalf()
        expandedMedia.value = key?.let { it to index }
    }

    fun closeExpandedMedia() {
        clearPictureHalf()
        expandedMedia.value = null
    }

    private fun clearPictureHalf() {
        val old = cardStepState.value
        if (old.pictureHalf) cardStepState.value = old.copy(pictureHalf = false)
    }

    private fun stepMediaCount(card: IslandNotification): Int =
        if (card.media.any { it.isSticker }) 0 else card.media.size

    /** The open step (preview / reader) of the shown card; the open picture is [expandedMedia]. See model/CardDepth.kt. */
    val cardStepState = MutableStateFlow(com.agupta07505.smartisland.model.CardStepState())

    private fun shownCard(): IslandNotification? = visibleNotifications.value.getOrNull(selectedIndex.value)

    fun currentShownNotification(): IslandNotification? = shownCard()

    private fun currentSteps(card: IslandNotification): com.agupta07505.smartisland.model.CardSteps {
        val picture = expandedMedia.value
        return cardStepState.value.steps(card.key, picture?.first, picture?.second ?: 0)
    }

    val cardDepth: StateFlow<com.agupta07505.smartisland.model.CardDepth> = combine(
        cardStepState, expandedMedia, visibleNotifications, selectedIndex
    ) { step, picture, list, index ->
        val card = list.getOrNull(index)
        step.steps(card?.key, picture?.first, picture?.second ?: 0).depth
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = com.agupta07505.smartisland.model.CardDepth.First
    )

    private fun cardDepthNow(): com.agupta07505.smartisland.model.CardDepth {
        val picture = expandedMedia.value
        val card = visibleNotifications.value.getOrNull(selectedIndex.value)
        return cardStepState.value.steps(card?.key, picture?.first, picture?.second ?: 0).depth
    }

    fun setCardSteps(key: String, steps: com.agupta07505.smartisland.model.CardSteps) {
        val old = cardStepState.value
        cardStepState.value = com.agupta07505.smartisland.model.CardStepState(
            key = key,
            preview = steps.preview,
            reader = steps.reader,
            pictureHalf = steps.pictureHalf,
            selector = steps.selector,
            textExpandable = old.takeIf { it.key == key }?.textExpandable ?: false
        )
        expandedMedia.value = steps.pictureIndex?.let { key to it }
    }

    /** The card says whether its text (no pictures) has more to read. Only the shown card counts. */
    fun reportCardTextExpandable(key: String, expandable: Boolean) {
        if (shownCard()?.key != key) return
        val old = cardStepState.value
        cardStepState.value = if (old.key == key) old.copy(textExpandable = expandable)
        else com.agupta07505.smartisland.model.CardStepState(key = key, textExpandable = expandable)
    }

    /** Swipe down inside an expanded card: one step deeper. False when there is none (the caller then runs its normal action). */
    fun cardGoDeeper(): Boolean {
        val card = shownCard() ?: return false
        if (card.mode != IslandMode.Notification) return false
        val steps = currentSteps(card)
        val expandable = cardStepState.value.takeIf { it.key == card.key }?.textExpandable ?: false
        val next = com.agupta07505.smartisland.model.CardSteps.deeper(steps, stepMediaCount(card), expandable) ?: return false
        setCardSteps(card.key, next)
        return true
    }

    /** Swipe up inside an expanded card: one step back. False at the first state. */
    fun cardGoBack(): Boolean {
        val card = shownCard() ?: return false
        val next = com.agupta07505.smartisland.model.CardSteps.back(currentSteps(card), stepMediaCount(card)) ?: return false
        setCardSteps(card.key, next)
        return true
    }

    /**
     * Where the single expanded island is on the overlay window, in window pixels (null while
     * collapsed or while two separate islands are shown). The service uses it for the touch region.
     */
    val expandedSurfaceBounds = MutableStateFlow<android.graphics.Rect?>(null)

    val isInputActive = MutableStateFlow(false)

    /** True while the island is open because an event opened it by itself and the user has not touched it yet. */
    val autoExpanded = MutableStateFlow(false)

    fun markUserEngaged() {
        if (autoExpanded.value) autoExpanded.value = false
    }

    private fun navigationOpenedByUser(): Boolean =
        !autoExpanded.value && visibleNotifications.value.getOrNull(selectedIndex.value)?.mode == IslandMode.Navigation

    val mode: StateFlow<IslandMode> = combine(visibleNotifications, selectedIndex) { list, idx ->
        list.getOrNull(idx)?.mode ?: IslandMode.Empty
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = IslandMode.Empty
    )

    private var autoCollapseJob: Job? = null
    private var popupJob: Job? = null
    private var switchJob: Job? = null

    private val autoCollapsePaused = MutableStateFlow(false)

    /** True while the expanded notification's text is open for reading (it scrolls). */
    val readingExpandedText: StateFlow<Boolean> get() = autoCollapsePaused

    fun setAutoCollapsePaused(paused: Boolean) {
        autoCollapsePaused.value = paused
    }

    private val autoCollapseHeld = MutableStateFlow(false)

    fun setAutoCollapseHeld(held: Boolean) {
        autoCollapseHeld.value = held
    }

    private var popupSuppressedUntilMs = 0L

    private var autoExpandSuppressedUntilMs = 0L
    private var lastShadeImportMs = 0L

    private fun startPopup(key: String) {
        popupJob?.cancel()
        popupKey.value = key
        popupJob = viewModelScope.launch {
            delay(settings.value.popupMs.toLong())
            if (popupKey.value == key) popupKey.value = null
        }
    }

    private fun endPopup() {
        popupJob?.cancel()
        popupJob = null
        popupKey.value = null
    }

    init {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Notifications collector failed") {
                var previousKeys: List<String> = emptyList()
                var previousList: List<IslandNotification> = emptyList()
                visibleNotifications.collect { list ->
                    val keys = list.map { it.key }
                    val orderChanged = keys != previousKeys
                    val pendingIndex = pendingSelectKey?.let { keys.indexOf(it) }?.takeIf { it >= 0 }
                    if (pendingIndex != null) pendingSelectKey = null
                    // Open island and the card it shows has gone (a system event ended, the app cancelled it): the island
                    // must not slide onto another card as if that one had just arrived. See ExpandedSelection.
                    val shownKeyBefore = previousList.getOrNull(selectedIndex.value)?.key
                    // A ringing call that takes over the open island is the only arrival that may keep it open.
                    val pendingCard = pendingIndex?.let { list.getOrNull(it) }
                    val ringingTakesOver = pendingCard != null && pendingCard.mode == IslandMode.IncomingCall && pendingCard.isCallRinging
                    // The screen recording ended while its card was open: close at once (a call is the one exception).
                    // Nothing waiting in the list may take its place, and nothing may auto-expand over it for a few seconds.
                    val recorderEnded = expanded.value && !ringingTakesOver &&
                        ExpandedSelection.recorderEnded(previousList, selectedIndex.value, list)
                    if (recorderEnded) {
                        com.agupta07505.smartisland.util.AppLogRecorder.i(
                            TAG,
                            "Open island closed: the screen recording ended ($shownKeyBefore)"
                        )
                        autoExpandSuppressedUntilMs = System.currentTimeMillis() + RECORDER_END_NO_EXPAND_MS
                        pendingSelectKey = null
                        collapse()
                    }
                    val goneOutcome = if (orderChanged && expanded.value && !ringingTakesOver) {
                        ExpandedSelection.afterSelectedGone(previousList, selectedIndex.value, list)
                    } else {
                        ExpandedSelection.Outcome.Keep
                    }
                    selectedIndex.update { currentSelected ->
                        when {
                            // Collapsed: the primary cutout always shows the top-priority card.
                            (orderChanged || recorderEnded) && !expanded.value -> 0
                            // Auto-expand picked a card of this very list: keep it.
                            pendingIndex != null -> pendingIndex
                            // The shown card was replaced by the same app's new card: show that one.
                            goneOutcome is ExpandedSelection.Outcome.Select -> goneOutcome.index
                            // Expanded: keep the card the user is looking at: the card at the selected index of the
                            // PREVIOUS list.
                            orderChanged -> previousKeys.getOrNull(currentSelected)
                                ?.let { keys.indexOf(it) }
                                ?.takeIf { it >= 0 }
                                ?: currentSelected
                            else -> currentSelected
                        }.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                    }
                    // Diagnostic: an open island whose shown card is still the same card must keep it; if the card on screen
                    // changed without a swipe, a tap or a collapse, the log says what the list looked like.
                    if (expanded.value && shownKeyBefore != null && goneOutcome == ExpandedSelection.Outcome.Keep) {
                        val shownKeyAfter = list.getOrNull(selectedIndex.value)?.key
                        if (shownKeyAfter != shownKeyBefore && !recorderEnded) {
                            com.agupta07505.smartisland.util.AppLogRecorder.i(
                                TAG,
                                "Open island moved to another card: $shownKeyBefore -> $shownKeyAfter " +
                                    "before=${previousList.map { it.key + ':' + it.mode }} after=${list.map { it.key + ':' + it.mode }} " +
                                    "ringingTakesOver=$ringingTakesOver pendingIndex=$pendingIndex"
                            )
                        }
                    }
                    if (goneOutcome is ExpandedSelection.Outcome.Collapse) {
                        com.agupta07505.smartisland.util.AppLogRecorder.i(
                            TAG,
                            "Open island closed: the card it showed is gone ($shownKeyBefore), no replacement"
                        )
                        // Whatever arrives next must not open the island again at once (see the auto-expand collector).
                        autoExpandSuppressedUntilMs = System.currentTimeMillis() + GONE_CARD_NO_EXPAND_MS
                        pendingSelectKey = null
                        collapse()
                    }
                    previousKeys = keys
                    previousList = list
                    selectionKeys = keys
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Popup collector failed") {
                // Tokens of short events already seen.
                val seen = HashSet<PopupToken>()
                visibleUnordered.collect { list ->
                    val liveKeys = notifications.value.mapTo(HashSet()) { it.key }
                    seen.retainAll { it.key in liveKeys }
                    var fresh: IslandNotification? = null
                    for (card in list) {
                        if (!IslandPriority.popsUp(card, list)) continue
                        if (seen.add(card.popupToken())) fresh = card
                    }
                    val card = fresh ?: return@collect
                    if (expanded.value || isInputActive.value) return@collect
                    if (System.currentTimeMillis() < popupSuppressedUntilMs) return@collect
                    startPopup(card.key)
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Idle-token cleanup failed") {
                notifications.collect { raw ->
                    // A card keeps its token only while it is still the same card in the same state: a resumed
                    // card loses it (so pausing it again starts a fresh countdown), a card that is gone loses it.
                    val live = raw.map { it.awayToken() }.toSet()
                    idleHidden.update { hidden ->
                        val kept = hidden.filterTo(HashSet()) { it in live }
                        if (kept.size == hidden.size) hidden else kept
                    }
                    // A live card that is gone is forgotten, so the same app's next one is "automatic" again.
                    val rawKeys = raw.mapTo(HashSet()) { it.key }
                    replyDrafts.retainOnly(rawKeys)
                    userRevealedLive.update { revealed ->
                        if (revealed.all { it in rawKeys }) revealed else revealed.filterTo(HashSet()) { it in rawKeys }
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Static idle timeout failed") {
                // Island collapsed, not being typed in, and every card is idle-able (static notification, paused
                // media, paused live card): after the timeout the island returns to the idle cutout.
                combine(visibleNotifications, expanded, isInputActive) { list, isExpanded, inputActive ->
                    if (isExpanded || inputActive || list.isEmpty()) null
                    else if (list.any { !IslandPriority.canIdleOut(it) }) null
                    else list.map { it.key }
                }.distinctUntilChanged().collectLatest { keys ->
                    if (keys == null) return@collectLatest
                    // Timeout set to "never": cards stay on the island until the app or the user removes them.
                    if (settings.value.idleTimeoutMs >= SmartIslandSettings.IDLE_TIMEOUT_NEVER_MS) return@collectLatest
                    delay(settings.value.idleTimeoutMs.toLong() + IDLE_TIMER_EXTRA_MS)
                    val tokens = visibleNotifications.value
                        .filter { IslandPriority.canIdleOut(it) }
                        .map { it.idleToken() }
                    idleHidden.update { it + tokens }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Live idle timeout failed") {
                // Live idle timeout: a live card (music, navigation, timer, recorder, ...) that came onto the island
                // by itself goes back to the idle cutout after the collapsed-pill timer, like a static one.
                combine(visibleNotifications, expanded, isInputActive, userRevealedLive, orientation) { list, isExpanded, inputActive, revealed, _ ->
                    if (isExpanded || inputActive) null
                    else list.filter { liveMayTimeOut(it) && it.key !in revealed }
                        .mapTo(HashSet()) { it.key }
                        .takeIf { it.isNotEmpty() }
                }.distinctUntilChanged().collectLatest { keys ->
                    if (keys == null) return@collectLatest
                    // Timeout set to "never": cards stay on the island until the app or the user removes them.
                    if (settings.value.idleTimeoutMs >= SmartIslandSettings.IDLE_TIMEOUT_NEVER_MS) return@collectLatest
                    delay(settings.value.idleTimeoutMs.toLong() + IDLE_TIMER_EXTRA_MS)
                    val tokens = visibleNotifications.value
                        .filter { it.key in keys && liveMayTimeOut(it) }
                        .map { it.awayToken() }
                    if (tokens.isNotEmpty()) idleHidden.update { it + tokens }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Auto-expand collector failed") {
                notificationRepo.autoExpandEvent.collect { key ->
                    // Locked phone and the lock-screen switch is off: no event opens the island by itself, except calls.
                    // Checked before endPopup() so the event keeps its normal popup in the pill.
                    if (isLocked.value && !settings.value.autoExpandOnLockScreen &&
                        notifications.value.firstOrNull { it.key == key }?.mode != IslandMode.IncomingCall
                    ) return@collect
                    // The notification shade (or quick settings) is open: every event is hidden, so there is no card
                    // to open.
                    if (shadeOpen.value) return@collect
                    // The island has just closed because the live event it showed ended: a new event or notification
                    // does not open it again right away.
                    if (System.currentTimeMillis() < autoExpandSuppressedUntilMs) {
                        val arrivingNow = notifications.value.firstOrNull { it.key == key }
                        if (!(arrivingNow?.mode == IslandMode.IncomingCall && arrivingNow.isCallRinging)) return@collect
                    }
                    // An island that is already open is never interrupted: the card on screen stays, whatever
                    // arrives. Only a ringing call takes it over.
                    if (expanded.value) {
                        val arriving = notifications.value.firstOrNull { it.key == key }
                        if (!(arriving?.mode == IslandMode.IncomingCall && arriving.isCallRinging)) return@collect
                    }
                    // Look the card up in the list the UI actually shows. The raw list contains cards hidden by event
                    // switches or the foreground app, which would pick the wrong index or open an empty island.
                    endPopup()
                    val visible = IslandPriority.order(
                        applyPillHidden(
                            filterVisible(notifications.value, foregroundPackage.value, settings.value, isLocked.value),
                            _pillHidden.value
                        )
                    )
                    val index = visible.indexOfFirst { it.key == key }
                    // Typing a reply: a ringing call (the only event that gets this far on an open island) takes the card;
                    // the reply resumes after it (callInterruptReplyKey).
                    val typingKey = if (isInputActive.value) visibleNotifications.value.getOrNull(selectedIndex.value)?.key else null
                    if (typingKey != null && index >= 0 && callInterruptReplyKey.value == null) {
                        callInterruptReplyKey.value = typingKey
                    }
                    if (index >= 0) {
                        selectedIndex.value = index
                        // The selection collector may not have seen this list yet: tell it which card was chosen.
                        if (selectionKeys != visible.map { it.key }) pendingSelectKey = key
                        val card = visible[index]
                        val cfg = settings.value
                        val event = IslandEvent.forNotification(card)
                        val eventMayAutoExpand = event == null || !event.canSwitchAutoExpand || cfg.isEventAutoExpand(event.id)
                        if (eventMayAutoExpand &&
                            AppEventRules.mayAutoExpand(
                                globalAutoExpand = cfg.autoExpandOnNotification,
                                noAutoExpandPackages = cfg.noAutoExpandPackages,
                                autoExpandPackages = cfg.autoExpandPackages,
                                noExpandPackages = cfg.noExpandPackages,
                                packageName = card.packageName,
                                mode = card.mode,
                                isLoud = card.isLoud
                            )
                        ) {
                            if (widgetIslandHolds()) {
                                if (com.agupta07505.smartisland.model.cardClosesWidgetIsland(card.mode)) deferredExpandKey = key
                            } else {
                                val wasExpanded = expanded.value
                                expand()
                                if (!wasExpanded && expanded.value) autoExpanded.value = true
                            }
                        }
                        // The island did not open for this card (its event or app may not auto-expand, the shade is
                        // open, ...): the collapsed primary cutout keeps showing the top-priority card.
                        if (!expanded.value) {
                            pendingSelectKey = null
                            selectedIndex.value = 0
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Exempt-app collector failed") {
                // An exempt app came to the front: whatever was open closes (an off island's window is hidden by the
                // service, a static one stops taking touches).
                combine(islandExemption, expanded) { mode, open -> Pair(mode != ExemptMode.None, open) }
                    .distinctUntilChanged()
                    .collect { (exempt, open) ->
                        if (exempt) {
                            if (open) collapse()
                            closeWidgetIsland(snap = true)
                        }
                    }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Widget-island release collector failed") {
                combine(widgetIslandOpen, widgetIslandShown) { open, shown -> open || shown }
                    .distinctUntilChanged()
                    .collect { holds ->
                        val key = deferredExpandKey
                        if (holds || key == null) return@collect
                        deferredExpandKey = null
                        val index = visibleNotifications.value.indexOfFirst { it.key == key }
                        if (index < 0 || expanded.value) return@collect
                        selectedIndex.value = index
                        expand()
                        if (expanded.value) autoExpanded.value = true
                    }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Timer-reset collector failed") {
                notificationRepo.resetTimerEvent.collect {
                    resetAutoCollapseTimer()
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Empty-island guard failed") {
                // Safety net: an expanded island with nothing to show closes. The short wait lets a card that was
                // just posted reach the visible list first (the auto-expand path sets `expanded` a moment before it).
                combine(expanded, visibleNotifications, appsLauncherOpen) { open, list, apps -> open && list.isEmpty() && !apps }
                    .distinctUntilChanged()
                    .collectLatest { emptyOpen ->
                        if (!emptyOpen) return@collectLatest
                        kotlinx.coroutines.delay(EMPTY_ISLAND_GRACE_MS)
                        if (expanded.value && visibleNotifications.value.isEmpty() && !appsLauncherOpen.value) {
                            com.agupta07505.smartisland.util.AppLogRecorder.i(TAG, "Empty expanded island closed: no card to show")
                            collapse()
                        }
                    }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Expanded-state collector failed") {
                expanded.collect { isExpanded ->
                    if (isExpanded) {
                        endPopup()
                        // Every way of expanding (tap, auto-expand, swipe) ends up here. Look at the
                        // shade for missed notifications and for the current app's other cards.
                        requestShadeImport()
                        requestSameAppCards()
                        if (!isInputActive.value) {
                            startAutoCollapseTimer()
                        }
                    } else {
                        stopAutoCollapseTimer()
                        autoCollapsePaused.value = false
                        autoCollapseHeld.value = false
                        isInputActive.value = false
                        siblingPickerOpen.value = false
                        expandedMedia.value = null
                        replyDrafts.resume.value = null
                        pendingSelectKey = null
                        selectedIndex.value = 0
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Sibling picker collector failed") {
                // The picker belongs to one card's app. It closes when the selected card changes
                // (swipe, event switch, a pick) or when that card no longer has any sibling.
                var pickerCardKey: String? = null
                combine(siblingPickerOpen, visibleNotifications, selectedIndex) { open, list, index ->
                    Triple(open, list, index)
                }.collect { (open, list, index) ->
                    if (!open) {
                        pickerCardKey = null
                        return@collect
                    }
                    val key = list.getOrNull(index)?.key
                    if (pickerCardKey == null) pickerCardKey = key
                    if (key != pickerCardKey || siblingIndices(list, index).isEmpty()) {
                        siblingPickerOpen.value = false
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Card step collector failed") {
                // The open step belongs to one card: another card, or a collapsed island, starts at the first state.
                combine(expanded, visibleNotifications, selectedIndex) { open, list, index ->
                    if (open) list.getOrNull(index)?.key else null
                }.distinctUntilChanged().collect { shownKey ->
                    val state = cardStepState.value
                    if (state.key != null && state.key != shownKey) {
                        cardStepState.value = com.agupta07505.smartisland.model.CardStepState()
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Expanded media collector failed") {
                // An open picture belongs to one card: it closes when that card is no longer the shown
                // one (swipe, switch, removal) or no longer has that picture.
                combine(expandedMedia, visibleNotifications, selectedIndex) { open, list, index ->
                    Triple(open, list, index)
                }.collect { (open, list, index) ->
                    if (open == null) return@collect
                    val card = list.getOrNull(index)
                    if (card == null || card.key != open.first || open.second !in card.media.indices) {
                        expandedMedia.value = null
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Auto-collapse pause collector failed") {
                // The quick-launch window (Open Apps shortcut) is in use, not a card to be put away: no countdown while it is open.
                combine(
                    combine(autoCollapsePaused, siblingPickerOpen, autoCollapseHeld, appsLauncherOpen, cardDepth) { reading, picking, held, apps, depth ->
                        // The card's own open step (reader, preview, picture selector, picture) also holds the
                        // countdown.
                        reading || picking || held || apps || depth != com.agupta07505.smartisland.model.CardDepth.First
                    },
                    // Map navigation opened by the user stays open until the user puts it away.
                    combine(visibleNotifications, selectedIndex, autoExpanded) { list, index, auto ->
                        !auto && list.getOrNull(index)?.mode == IslandMode.Navigation
                    }
                ) { stepHold, navigationHold -> stepHold || navigationHold }
                    .distinctUntilChanged()
                    .collect { paused ->
                    if (paused) {
                        stopAutoCollapseTimer()
                    } else if (expanded.value && !isInputActive.value) {
                        // Reading is over: the usual countdown starts again from zero.
                        startAutoCollapseTimer()
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Selection collector failed") {
                // An expanded message belongs to the card it was opened on.
                selectedIndex.collect { autoCollapsePaused.value = false }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Auto-expanded reset collector failed") {
                // However the island closes (timer, tap, shade, swipe), the next opening starts as a normal one.
                expanded.collect { open -> if (!open) autoExpanded.value = false }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Input-active collector failed") {
                isInputActive.collect { active ->
                    if (active) {
                        autoExpanded.value = false
                        stopAutoCollapseTimer()
                        // Typing is active again: the resume request is used up.
                        replyDrafts.resume.value = null
                    } else if (expanded.value) {
                        startAutoCollapseTimer()
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Typing resume failed") {
                // A call took the island while a reply was being typed.
                combine(notifications, callInterruptReplyKey, shadeOpen) { raw, replyKey, shade ->
                    when {
                        replyKey == null -> ResumeState.NONE
                        raw.none { it.key == replyKey } -> ResumeState.GONE
                        shade || raw.any { it.mode == IslandMode.IncomingCall && it.isCallRinging } -> ResumeState.WAIT
                        else -> ResumeState.READY
                    }
                }.distinctUntilChanged().collectLatest { state ->
                    val replyKey = callInterruptReplyKey.value
                    when (state) {
                        ResumeState.GONE -> callInterruptReplyKey.value = null
                        ResumeState.READY -> {
                            // Lets the answer / hang-up button's own collapse and the call's card changes settle first.
                            delay(RESUME_SETTLE_MS)
                            val index = visibleNotifications.value.indexOfFirst { it.key == replyKey }
                            if (replyKey != null && index >= 0) {
                                selectedIndex.value = index
                                expand()
                                // The card is on screen now, so its reply box can open and take the keyboard.
                                delay(RESUME_CARD_SHOWN_MS)
                                replyDrafts.resume.value = replyKey
                            }
                            // Cleared only now: clearing it earlier made this collector restart (state NONE) and
                            // cancel the wait above, so the box never opened.
                            callInterruptReplyKey.value = null
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private enum class ResumeState { NONE, GONE, WAIT, READY }

    /** True while the expanded island shows the quick-launch apps grid instead of its cards (the Open Apps shortcut). */
    val appsLauncherOpen = MutableStateFlow(false)

    /** Open Apps shortcut (long press on the collapsed cutout, portrait): expands the island onto the quick-launch grid. */
    fun openAppsLauncher() {
        if (!settings.value.enableAppShortcuts || shadeOpen.value || expanded.value) return
        if (exemptionNow() != ExemptMode.None || widgetIslandHolds()) return
        // Landscape, the empty collapsed pill the long press was made on: it stays while the shortcuts are open, so
        // the island grows out of that pill (size, position, opacity) and closes back into it; hiding it swapped it
        // for the small see-through idle circle mid-opening.
        emptyPillJob?.cancel()
        appsLauncherOpen.value = true
        expanded.value = true
    }

    fun expand() {
        // Never open under the shade: the visible list is empty there, so the island would be empty.
        if (shadeOpen.value) return
        if (!currentCardMayExpand()) return
        // An exempt app is in front (island off or static): nothing opens, a popup included.
        if (exemptionNow() != ExemptMode.None || widgetIslandHolds()) return
        expanded.value = true
    }

    private fun currentCardMayExpand(): Boolean {
        val card = visibleNotifications.value.getOrNull(selectedIndex.value) ?: return true
        return AppEventRules.mayExpandCard(settings.value.noExpandPackages, card.packageName, card.mode)
    }

    private fun requestShadeImport() {
        val now = System.currentTimeMillis()
        if (now - lastExpandImportMs < EXPAND_IMPORT_INTERVAL_MS) return
        lastExpandImportMs = now
        // Cards the import brings in are old ones from the shade, not new alerts: no popup for them, even when the
        // island has closed again by the time the service delivers them.
        popupSuppressedUntilMs = now + IMPORT_POPUP_GRACE_MS
        notificationRepo.sendCommand(SmartIslandCommand.ImportShadeNotifications)
    }

    private var lastExpandImportMs = 0L

    private fun requestSameAppCards() {
        val current = visibleNotifications.value.getOrNull(selectedIndex.value) ?: return
        if (current.mode != IslandMode.Notification && current.mode != IslandMode.DownloadUpload) return
        val now = System.currentTimeMillis()
        if (current.packageName == lastSameAppRequestPkg && now - lastSameAppRequestMs < 3_000L) return
        lastSameAppRequestPkg = current.packageName
        lastSameAppRequestMs = now
        popupSuppressedUntilMs = maxOf(popupSuppressedUntilMs, now + IMPORT_POPUP_GRACE_MS)
        notificationRepo.sendCommand(SmartIslandCommand.ImportSameAppNotifications(current.packageName))
    }

    private var lastSameAppRequestPkg: String? = null
    private var lastSameAppRequestMs = 0L

    private var outsideCloseJob: Job? = null

    /**
     * A touch landed outside an island that opened by itself. The touch already went to the app below; the island
     * stays [OUTSIDE_CLOSE_DELAY_MS] longer so the event can still be seen.
     */
    fun collapseAfterOutsideTouch() {
        if (!autoExpanded.value || outsideCloseJob?.isActive == true) return
        outsideCloseJob = viewModelScope.launch {
            delay(OUTSIDE_CLOSE_DELAY_MS)
            outsideCloseJob = null
            if (autoExpanded.value) collapse()
        }
    }

    fun collapse() {
        keepEmptyPillAfterLauncher()
        appsLauncherOpen.value = false
        outsideCloseJob?.cancel()
        outsideCloseJob = null
        isInputActive.value = false
        autoExpanded.value = false
        expanded.value = false
    }

    fun setInputActive(active: Boolean) {
        if (isInputActive.value != active) {
            isInputActive.value = active
        }
    }

    private var lastToggleTimeMs = 0L

    fun toggleExpanded() {
        val now = System.currentTimeMillis()
        if (now - lastToggleTimeMs < 350L) return
        lastToggleTimeMs = now
        // Opening is refused for apps that never expand; closing is always allowed.
        if (!expanded.value && !currentCardMayExpand()) return
        if (!expanded.value && (exemptionNow() != ExemptMode.None || widgetIslandHolds())) return
        // Opening needs a card to show, and no shade: otherwise the island would open empty.
        if (!expanded.value && (shadeOpen.value || visibleNotifications.value.isEmpty())) return
        if (expanded.value) {
            keepEmptyPillAfterLauncher()
            appsLauncherOpen.value = false
        }
        // A tap that closes the open island is what "off after tap to close" waits for; opening it again brings the widgets back.
        _idleClosedByTap.value = expanded.value
        expanded.update { !it }
    }

    private fun startAutoCollapseTimer() {
        autoCollapseJob?.cancel()
        if (autoCollapseBlocked()) return
        autoCollapseJob = viewModelScope.launch {
            delay(AUTO_COLLAPSE_DELAY_MS)
            // Checked again against the live state: a step opened while the countdown ran must hold it, even if the pause
            // collector has not cancelled this job yet.
            if (autoCollapseBlocked()) return@launch
            com.agupta07505.smartisland.util.AppLogRecorder.i(TAG, "Open island closed: auto-collapse timer (depth=${cardDepthNow()})")
            collapse()
        }
    }

    private fun autoCollapseBlocked(): Boolean =
        isInputActive.value || autoCollapsePaused.value || siblingPickerOpen.value || autoCollapseHeld.value || appsLauncherOpen.value ||
            navigationOpenedByUser() ||
            cardDepthNow() != com.agupta07505.smartisland.model.CardDepth.First ||
            cardDepth.value != com.agupta07505.smartisland.model.CardDepth.First

    private fun stopAutoCollapseTimer() {
        autoCollapseJob?.cancel()
        autoCollapseJob = null
    }

    fun resetAutoCollapseTimer() {
        if (expanded.value) {
            startAutoCollapseTimer()
        }
    }

    fun setSelectedNotificationIndex(index: Int) {
        val list = visibleNotifications.value
        if (index in list.indices) {
            selectedIndex.value = index
            resetAutoCollapseTimer()
        }
    }

    /**
     * Swipe action "next / previous event": moves to the next stored event in priority order.
     *
     * The cycle covers every card the island holds (including ones the idle timeout put away) and
     * the loud notifications that are still in the system shade. Standard notifications the system
     * lists as silent are skipped; live cards (music, timers, calls ...) are always part of the cycle.
     */
    fun switchEvent(forward: Boolean) {
        switchJob?.cancel()
        switchJob = viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Event switch failed") {
                endPopup()
                val now = System.currentTimeMillis()
                // Notifications posted after the island started are already stored, so the shade
                // only has to be looked at now and then (it also costs a short wait).
                if (now - lastShadeImportMs >= SHADE_IMPORT_INTERVAL_MS) {
                    lastShadeImportMs = now
                    popupSuppressedUntilMs = now + SHADE_IMPORT_POPUP_GRACE_MS
                    val before = notifications.value
                    notificationRepo.sendCommand(SmartIslandCommand.ImportShadeNotifications)
                    withTimeoutOrNull(SHADE_IMPORT_WAIT_MS) { notifications.first { it != before } }
                    delay(40)
                }

                val full = IslandPriority.order(
                    applyPillHidden(
                        filterVisible(notifications.value, foregroundPackage.value, settings.value, isLocked.value),
                        _pillHidden.value
                    )
                )
                val currentKey = visibleNotifications.value.getOrNull(selectedIndex.value)?.key
                val target = nextEventTarget(full, currentKey, forward) ?: return@runSuspendCatchingLogged

                if (visibleNotifications.value.none { it.key == target.key }) {
                    // Put away by the idle timeout: bring the stored cards back first.
                    revealIdleCards(tapAnimation = false)
                    withTimeoutOrNull(SHADE_IMPORT_WAIT_MS) {
                        visibleNotifications.first { list -> list.any { it.key == target.key } }
                    }
                    delay(40)
                }
                val index = visibleNotifications.value.indexOfFirst { it.key == target.key }
                if (index >= 0) {
                    selectedIndex.value = index
                    resetAutoCollapseTimer()
                }
            }
        }
    }

    /**
     * Swipe action "next / previous notification": moves to the next (or previous) app entry in the cards on screen.
     */
    fun switchNotification(forward: Boolean): Boolean {
        val list = visibleNotifications.value
        val index = nextAppGroupIndex(list, selectedIndex.value, forward)
        if (index < 0) return false
        selectedIndex.value = index
        resetAutoCollapseTimer()
        return true
    }

    /**
     * Expanded-island gesture "next / previous sibling": moves between the cards of the current card's own app only
     * (wrapping). Returns false when the current card has no sibling.
     */
    fun switchSibling(forward: Boolean): Boolean {
        val list = visibleNotifications.value
        val index = nextSiblingIndex(list, selectedIndex.value, forward)
        if (index < 0) return false
        selectedIndex.value = index
        resetAutoCollapseTimer()
        return true
    }

    /**
     * Opens the sibling picker: the list of every card of the current card's app. Only while expanded, not while a
     * reply is being typed, and only when the current card has siblings; returns false otherwise.
     */
    fun openSiblingPicker(): Boolean {
        if (!expanded.value) return false
        // Opening it would take the reply field away and lose what is being typed.
        if (isInputActive.value) return false
        if (siblingIndices(visibleNotifications.value, selectedIndex.value).isEmpty()) return false
        siblingPickerOpen.value = true
        return true
    }

    fun closeSiblingPicker() {
        if (!siblingPickerOpen.value) return
        siblingPickerOpen.value = false
    }

    fun selectSibling(key: String): Boolean {
        val index = visibleNotifications.value.indexOfFirst { it.key == key }
        if (index < 0) return false
        selectedIndex.value = index
        siblingPickerOpen.value = false
        return true
    }

    fun dismissCurrentNotification() {
        val list = visibleNotifications.value
        val index = selectedIndex.value
        if (index in list.indices) {
            val notification = list[index]
            notificationRepo.removeNotification(notification.key)
            notificationRepo.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
        }
        collapse()
    }

    fun dismissAllNotifications() {
        val list = visibleNotifications.value
        for (notification in list) {
            notificationRepo.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
        }
        notificationRepo.removeAllNotifications()
        collapse()
    }

    private data class IdleToken(val key: String, val timeMillis: Long, val contentHash: Int)

    private data class PopupToken(val key: String, val timeMillis: Long, val contentHash: Int)

    private fun IslandNotification.popupToken() = PopupToken(key, timeMillis, (title to text).hashCode())

    private fun IslandNotification.idleToken() = IdleToken(key, timeMillis, (title to text).hashCode())

    private fun IslandNotification.awayToken() =
        if (IslandPriority.canIdleOut(this)) idleToken() else IdleToken(key, 0L, liveAwayHash(this))

    private fun liveMayTimeOut(n: IslandNotification) =
        !IslandPriority.canIdleOut(n) && n.mode != IslandMode.IncomingCall &&
            // Navigation stays until the trip ends or the user puts it away; it never times out by itself.
            n.mode != IslandMode.Navigation &&
            // Portrait: a live activity stays until it ends or the user puts it away (tap on the idle cutout).
            !(n.mode == IslandMode.LiveActivity && !orientation.value.isLandscape)

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        private const val TAG = "IslandViewModel"

        /** Added to the collapsed pill's auto-close timer (the idle timeout setting) before it closes by itself. */
        const val IDLE_TIMER_EXTRA_MS = 1_000L

        /** Slack added to an idle-tap sequence's flags, so the overlay's state machine finishes before they drop. */
        const val IDLE_TAP_ACTIVE_MARGIN_MS = 400L

        /**
         * What the island may show: cards whose event switch is on, minus the live cards (music, navigation, timers,
         * recordings, downloads, live activities; not calls) of the app that is already in the foreground, unless
         * [SmartIslandSettings.showLiveEventsInForeground] is on.
         */
        internal fun filterVisible(
            list: List<IslandNotification>,
            foregroundPackage: String?,
            settings: SmartIslandSettings,
            locked: Boolean = false
        ): List<IslandNotification> = list.filterNot { notif ->
            val event = com.agupta07505.smartisland.model.IslandEvent.forNotification(notif)
            (event != null && !settings.isEventEnabled(event.id)) ||
                // On the lock screen the events with a lock-screen switch stay out of the pill (calls never do).
                (locked && event != null && settings.isEventHiddenOnLockScreen(event.id)) ||
                // The per-app lock-screen rule (calls excepted).
                (locked && settings.isAppHiddenOnLockScreen(notif.packageName, notif.mode)) ||
                (!settings.showLiveEventsInForeground &&
                    com.agupta07505.smartisland.util.ForegroundApp.hidesCard(notif.mode, notif.packageName, foregroundPackage))
        }
        /**
         * While the pill is hidden only calls (incoming, ringing or ongoing) stay on the island; everything else
         * waits in the repository and comes back when the pill is shown again.
         */
        internal fun applyPillHidden(list: List<IslandNotification>, pillHidden: Boolean): List<IslandNotification> =
            if (pillHidden) list.filter { it.mode == IslandMode.IncomingCall } else list

        /** The part of a live card's put-away token that is not its key. */
        internal fun liveAwayHash(n: IslandNotification): Int =
            if (n.mode == IslandMode.Music) (n.title to n.text).hashCode() else 0

        private const val AUTO_COLLAPSE_DELAY_MS = 5000L
        private const val OUTSIDE_CLOSE_DELAY_MS = 800L
        private const val RESUME_SETTLE_MS = 300L
        private const val EMPTY_ISLAND_GRACE_MS = 400L
        private const val GONE_CARD_NO_EXPAND_MS = 1_500L

        private const val RECORDER_END_NO_EXPAND_MS = 1_800L
        private const val RESUME_CARD_SHOWN_MS = 150L
        private const val SHADE_IMPORT_INTERVAL_MS = 30_000L
        private const val EXPAND_IMPORT_INTERVAL_MS = 5_000L
        private const val SHADE_IMPORT_WAIT_MS = 250L
        private const val SHADE_IMPORT_POPUP_GRACE_MS = 1_500L
        private const val IMPORT_POPUP_GRACE_MS = 3_000L

        /**
         * The card a swipe moves to: the first card of the next (or previous) app entry after the one holding
         * [currentKey] in [ordered], wrapping around.
         */
        internal fun nextEventTarget(
            ordered: List<IslandNotification>,
            currentKey: String?,
            forward: Boolean
        ): IslandNotification? {
            if (ordered.isEmpty()) return null
            val groups = ordered.map { appGroupKey(it) }.distinct()
            val current = ordered.firstOrNull { it.key == currentKey }
            val currentGroup = current?.let { appGroupKey(it) }
            val currentPosition = groups.indexOf(currentGroup)
            // With no current card the walk starts just before the first (or just after the last).
            val start = if (currentPosition >= 0) currentPosition else if (forward) -1 else 0
            val step = if (forward) 1 else -1
            for (offset in 1..groups.size) {
                val group = groups[Math.floorMod(start + step * offset, groups.size)]
                if (group == currentGroup) continue
                val target = ordered.firstOrNull {
                    appGroupKey(it) == group && !(it.mode == IslandMode.Notification && it.isSilent)
                }
                if (target != null) return target
            }
            return null
        }

        fun provideFactory(
            settingsRepo: SmartIslandSettingsRepository,
            notificationRepo: INotificationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IslandViewModel(settingsRepo, notificationRepo) as T
            }
        }
    }
}
