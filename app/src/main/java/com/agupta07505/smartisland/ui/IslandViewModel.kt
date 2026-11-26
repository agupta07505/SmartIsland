/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.graphics.Rect
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.IdleDotGeometry
import com.agupta07505.smartisland.data.IdleDotTap
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SettingsSource
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.idleDot
import com.agupta07505.smartisland.model.AutoCollapsePause
import com.agupta07505.smartisland.model.BackStep
import com.agupta07505.smartisland.model.BackStepRules
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.CardStepState
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.model.CollapsedPulse
import com.agupta07505.smartisland.model.ExemptMode
import com.agupta07505.smartisland.model.FullscreenRules
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandPriority
import com.agupta07505.smartisland.model.LauncherClose
import com.agupta07505.smartisland.model.ReplyDrafts
import com.agupta07505.smartisland.model.TorchCard
import com.agupta07505.smartisland.model.TorchTimeout
import com.agupta07505.smartisland.model.appGroupKey
import com.agupta07505.smartisland.model.cardClosesWidgetIsland
import com.agupta07505.smartisland.model.nextAppGroupIndex
import com.agupta07505.smartisland.model.nextSiblingIndex
import com.agupta07505.smartisland.util.AppEventRules
import com.agupta07505.smartisland.util.ExpandedSelection
import com.agupta07505.smartisland.util.GoneCardGuard
import com.agupta07505.smartisland.util.TorchController
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModel(
    private val settingsRepo: SettingsSource,
    val notificationRepo: INotificationRepository,
    /** Wall clock, for deadlines compared with a card's post time. */
    private val wallClockMs: () -> Long = { System.currentTimeMillis() },
    /** Time since boot, for the short guard windows after a state change. */
    private val elapsedMs: () -> Long = { SystemClock.elapsedRealtime() }
) : ViewModel() {

    val orientation = MutableStateFlow(OrientationState())

    val rotationVeil = MutableStateFlow(false)

    val rotationMorphActive = MutableStateFlow(false)

    val rotationQuarterHold = MutableStateFlow(false)

    /** False from a turn until a read the rotation morph can aim at. */
    val rotationTargetReady = MutableStateFlow(true)

    val foregroundPackage = MutableStateFlow<String?>(null)

    /** True while the phone is locked (set by the overlay service). */
    val isLocked = MutableStateFlow(false)

    val shadeOpen = MutableStateFlow(false)

    /** Secondary bubble is out beside the collapsed pill (set by the overlay). */
    val secondaryApart = MutableStateFlow(false)

    val expanded = MutableStateFlow(false)

    val isInputActive = MutableStateFlow(false)

    private val feed = IslandCardFeed(
        scope = viewModelScope,
        settingsRepo = settingsRepo,
        notificationRepo = notificationRepo,
        orientation = orientation,
        foregroundPackage = foregroundPackage,
        isLocked = isLocked,
        shadeOpen = shadeOpen,
        expanded = expanded,
        isInputActive = isInputActive,
        secondaryApart = secondaryApart,
        collapse = { collapse() },
        widgetIslandHolds = { widgetIsland.holds() },
        elapsedMs = elapsedMs
    )

    // The feed owns the card list and idle state.
    val settings: StateFlow<SmartIslandSettings> get() = feed.settings

    val pickerOnLeft: StateFlow<Boolean> get() = feed.pickerOnLeft

    fun settingsSnapshot(): SmartIslandSettings = feed.rawSettings.value

    val notifications: StateFlow<List<IslandNotification>> get() = feed.notifications

    val visibleNotifications: StateFlow<List<IslandNotification>> get() = feed.visibleNotifications

    val hasIdleStoredCards: StateFlow<Boolean> get() = feed.hasIdleStoredCards

    val idleDot: StateFlow<IdleDotGeometry?> get() = feed.idleDot

    val idleClosing: StateFlow<Boolean> get() = feed.idleClosing

    val idleTapActive: StateFlow<Boolean> get() = feed.idleTapActive

    val idleLeaving: StateFlow<Boolean> get() = feed.idleLeaving

    val pillHidden: StateFlow<Boolean> get() = feed.pillHidden

    /** A fullscreen app is in front: the island is off until the cutout is touched or a new event comes. */
    val fullscreenHidden: StateFlow<Boolean> get() = feed.fullscreenHidden

    fun setFullscreen(on: Boolean) = feed.setFullscreen(on)

    /** The camera cutout was touched while the island is hidden by a fullscreen app. */
    fun peekFromFullscreenTap() = feed.peekFromFullscreen(FullscreenRules.TAP_PEEK_MS)

    val tickHold: StateFlow<Boolean> get() = feed.tickHold

    val idleWidgetsSuppressed: StateFlow<Boolean> get() = feed.idleWidgetsSuppressed

    val islandExemption: StateFlow<ExemptMode> get() = feed.islandExemption

    val islandOffForApp: StateFlow<Boolean> get() = feed.islandOffForApp

    val islandStaticForApp: StateFlow<Boolean> get() = feed.islandStaticForApp

    val landscapeWidgetPill: StateFlow<Boolean> get() = feed.landscapeWidgetPill

    val displaySettings: StateFlow<SmartIslandSettings> get() = feed.displaySettings

    val pulseDriven: StateFlow<Boolean> get() = feed.pulseDriven

    private var selectionKeys: List<String> = emptyList()

    private var pendingSelectKey: String? = null

    /** Half-typed quick replies per notification */
    val replyDrafts = ReplyDrafts()

    private val callInterruptReplyKey = MutableStateFlow<String?>(null)

    fun revealIdleCards(tapAnimation: Boolean = true) = feed.revealIdleCards(tapAnimation)

    fun onIdleDotTapped(): IdleDotTap = feed.onIdleDotTapped()

    fun setShadeOpen(open: Boolean) {
        if (shadeOpen.value == open) return
        shadeOpen.value = open
        if (open) {
            collapse()
            widgetIsland.close(snap = true)
        }
    }

    fun setPillHidden(hidden: Boolean) = feed.setPillHidden(hidden)

    fun togglePillHidden() = setPillHidden(!pillHidden.value)

    private fun keepEmptyPillAfterLauncher() {
        if (appsLauncherOpen.value) feed.refreshEmptyPill()
    }

    fun setTickHold(hold: Boolean) = feed.setTickHold(hold)

    fun exemptionNow(): ExemptMode = feed.exemptionNow()

    internal val widgetIsland = WidgetIslandState(
        exempt = { exemptionNow() != ExemptMode.None },
        elapsedMs = elapsedMs
    )

    val selectedIndex = MutableStateFlow(0)
    /** A tap swap is running, it changes the selected card on its way (the picker waits for it to settle). */
    val swapOwnsSelection = MutableStateFlow(false)

    private val siblingPicker = IslandSiblingPicker(
        expanded = expanded,
        isInputActive = isInputActive,
        visibleNotifications = visibleNotifications,
        selectedIndex = selectedIndex,
        swapOwnsSelection = swapOwnsSelection,
        selectByUser = { selectByUser(it) }
    )
    val siblingPickerOpen: StateFlow<Boolean> get() = siblingPicker.open

    private val cardSteps = IslandCardSteps(viewModelScope, visibleNotifications, selectedIndex)

    val expandedMedia get() = cardSteps.expandedMedia

    fun setExpandedMedia(key: String?, index: Int) = cardSteps.setExpandedMedia(key, index)

    fun closeExpandedMedia() = cardSteps.closeExpandedMedia()

    val cardStepState get() = cardSteps.cardStepState

    fun currentShownNotification(): IslandNotification? = cardSteps.shownCard()

    val cardDepth: StateFlow<CardDepth> get() = cardSteps.cardDepth

    fun setCardSteps(key: String, steps: CardSteps) = cardSteps.setCardSteps(key, steps)

    fun reportCardTextExpandable(key: String, expandable: Boolean) =
        cardSteps.reportCardTextExpandable(key, expandable)

    fun cardGoDeeper(): Boolean = cardSteps.cardGoDeeper()

    fun cardGoBack(): Boolean = cardSteps.cardGoBack()

    /** Where the single expanded island is on the overlay window. */
    val expandedSurfaceBounds = MutableStateFlow<Rect?>(null)

    val autoExpanded = MutableStateFlow(false)

    fun markUserEngaged() {
        if (TorchCard.owns(selectedMode())) return
        if (autoExpanded.value) autoExpanded.value = false
    }

    fun restartIdleTimeout() = feed.restartIdleTimeout()

    private fun torchCardOpen(): Boolean = TorchCard.open(selectedMode(), expanded.value)

    fun torchCardOpenNow(): Boolean = torchCardOpen()

    private fun selectedMode(): IslandMode? = visibleNotifications.value.getOrNull(selectedIndex.value)?.mode

    private fun torchTimeoutNow(): TorchTimeout =
        TorchCard.timeout(selectedMode(), autoExpanded.value, TorchController.holding.value)

    private fun navigationOpenedByUser(): Boolean =
        !autoExpanded.value && visibleNotifications.value.getOrNull(selectedIndex.value)?.mode == IslandMode.Navigation

    val mode: StateFlow<IslandMode> = combine(visibleNotifications, selectedIndex) { list, idx ->
        list.getOrNull(idx)?.mode ?: IslandMode.Empty
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = IslandMode.Empty
    )

    private val pulseGate = IslandPulseGate(
        scope = viewModelScope,
        feed = feed,
        notificationRepo = notificationRepo,
        orientation = orientation,
        foregroundPackage = foregroundPackage,
        expanded = expanded,
        isInputActive = isInputActive,
        selectedIndex = selectedIndex,
        mode = mode,
        torchCardOpen = { torchCardOpen() },
        mayAutoExpandOnArrival = { card, cfg -> mayAutoExpandOnArrival(card, cfg) }
    )

    private val imports = ShadeImports(
        notificationRepo = notificationRepo,
        notifications = notifications,
        visibleNotifications = visibleNotifications,
        selectedIndex = selectedIndex,
        pulseGate = pulseGate,
        wallClockMs = wallClockMs
    )

    /** The collapsed pill's pulse queue (portrait and landscape classic near the centre). */
    val collapsedPulse: StateFlow<CollapsedPulse.State> get() = pulseGate.pulse.state

    private val autoCollapse = AutoCollapseTimer(
        scope = viewModelScope,
        wallClockMs = wallClockMs,
        blocked = { autoCollapseBlocked() },
        onExpire = { collapse() }
    )
    private var switchJob: Job? = null

    val readingExpandedText: StateFlow<Boolean> get() = autoCollapse.paused

    fun setAutoCollapsePaused(paused: Boolean) {
        autoCollapse.paused.value = paused
    }

    /** A text on the card is selected (long press in the reader or on a card the white pill holds). */
    val textSelecting = MutableStateFlow(false)
    private val textSelectionClear = MutableStateFlow(0)

    /** Counts up each time the selection must go, the card text rebuilds itself without it. */
    val textSelectionClearTick: StateFlow<Int> get() = textSelectionClear

    fun setTextSelecting(active: Boolean) {
        if (textSelecting.value != active) textSelecting.value = active
    }

    /** Drops the selection, false when there was none. */
    fun clearTextSelection(): Boolean {
        if (!textSelecting.value) return false
        textSelectionClear.update { it + 1 }
        textSelecting.value = false
        return true
    }

    /**
     * Back key or back gesture: 1 layer of the island at a time, innermost first. False when the island is closed
     * and leaves it to the app below
     */
    fun handleBack(): Boolean {
        if (!BackStepRules.windowFocusable(isInputActive.value, expanded.value, autoExpanded.value)) return false
        return when (BackStepRules.next(expanded.value, isInputActive.value, textSelecting.value,
            siblingPickerOpen.value, appsLauncherOpen.value, cardDepth.value)) {
            BackStep.None -> false
            // Only the reply box goes: the card shows its buttons again and the island stays open
            BackStep.CloseReply -> { replyDrafts.closeTick.update { it + 1 }; setInputActive(false); true }
            BackStep.ClearSelection -> clearTextSelection()
            BackStep.CloseSiblingPicker -> { closeSiblingPicker(); true }
            BackStep.CloseLauncher -> { closeLauncher(); setExpanded(false); true }
            BackStep.CardBack -> {
                if (!cardGoBack()) {
                    closeExpandedMedia()
                    setExpanded(false)
                }
                true
            }
            BackStep.Collapse -> { setExpanded(false); true }
        }
    }

    /** White pill was tapped: the countdown is held until it is tapped again. */
    val autoCollapsePinnedState: StateFlow<Boolean> get() = autoCollapse.pinned

    /**
     * Set just before the user (or a ringing call) moves the selection, only such a move releases a held countdown,
     * never a new event or a reshuffle
     */
    private var selectionReleasesPin = false

    private fun selectByUser(index: Int) {
        if (index != selectedIndex.value) selectionReleasesPin = true
        selectedIndex.value = index
    }

    fun toggleAutoCollapsePin() {
        if (!expanded.value) return
        if (autoCollapse.pinned.value) {
            autoCollapse.pinned.value = false
            autoCollapse.start(autoCollapse.resumeDelayMs())
        } else {
            autoCollapse.stop()
            autoCollapse.pinned.value = true
        }
    }

    // Declared before the init block: its collectors read it at once
    val appsLauncherOpen = MutableStateFlow(false)

    fun setAutoCollapseHeld(held: Boolean) {
        autoCollapse.held.value = held
    }

    /** The card's event and app both allow it to open the island by itself. */
    private fun mayAutoExpandOnArrival(card: IslandNotification, cfg: SmartIslandSettings): Boolean {
        val event = IslandEvent.forNotification(card)
        val eventMayAutoExpand = event == null || !event.canSwitchAutoExpand || cfg.isEventAutoExpand(event.id)
        return eventMayAutoExpand && AppEventRules.mayAutoExpand(
            globalAutoExpand = cfg.autoExpandOnNotification,
            noAutoExpandPackages = cfg.noAutoExpandPackages,
            autoExpandPackages = cfg.autoExpandPackages,
            noExpandPackages = cfg.noExpandPackages,
            packageName = card.packageName,
            mode = card.mode,
            isLoud = card.isLoud
        )
    }

    fun pulseTouch(held: Boolean) = pulseGate.touch(held)

    fun pulseScrollNeed(ms: Long) = pulseGate.scrollNeed(ms)

    fun pulseCardOpened(id: String) = pulseGate.cardOpened(id)

    /** The pill tap opens the island on [index]: that card is kept, everything that pulses or waits settles. */
    private fun commitPulseTap(index: Int) {
        if (pulseGate.commitTap(index)) selectedIndex.value = index
    }

    private var autoExpandSuppressedUntilMs = 0L
    private var recentGoneCards: List<GoneCardGuard.Gone> = emptyList()

    init {
        observeNotificationList()
        pulseGate.start()
        observeTorchSwap()
        pausePulseAutoCollapse()
        cleanReplyDrafts()
        feed.start()
        observeAutoExpandEvents()
        observeExemptApp()
        observeWidgetIslandRelease()
        observeTimerReset()
        closeEmptyExpandedIsland()
        observeExpandedState()
        observeSiblingPicker()
        observeCardSteps()
        observeExpandedMedia()
        observeAutoCollapsePause()
        observeSelection()
        resetAutoExpandedOnClose()
        observeInputActive()
        resumeTyping()
    }

    private fun observeNotificationList() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Notifications collector failed") {
                var previousKeys: List<String> = emptyList()
                var previousList: List<IslandNotification> = emptyList()
                visibleNotifications.collect { list ->
                    val keys = list.map { it.key }
                    val orderChanged = keys != previousKeys
                    val pendingIndex = pendingSelectKey?.let { keys.indexOf(it) }?.takeIf { it >= 0 }
                    if (pendingIndex != null) pendingSelectKey = null
                    val pendingCard = pendingIndex?.let { list.getOrNull(it) }
                    val ringingTakesOver = pendingCard != null && pendingCard.mode == IslandMode.IncomingCall
                        && pendingCard.isCallRinging
                    // The screen recording ended while its card was open
                    val recorderEnded = expanded.value && !ringingTakesOver &&
                        ExpandedSelection.recorderEnded(previousList, selectedIndex.value, list)
                    if (recorderEnded) {
                        autoExpandSuppressedUntilMs = wallClockMs() + RECORDER_END_NO_EXPAND_MS
                        pendingSelectKey = null
                        collapse()
                    }
                    // Live cards (a download, a route) that left the list, open island or not: their app's older
                    // notifications must not open the island if the app re-posts one a moment later
                    if (orderChanged) {
                        val nowMs = wallClockMs()
                        recentGoneCards = GoneCardGuard.prune(recentGoneCards, nowMs) +
                            GoneCardGuard.vanished(previousList, list, nowMs)
                    }
                    // selectedIndex already points into the new list, so the gone check (it reads the old list)
                    // could judge an unrelated card and close the island that just opened
                    val goneOutcome = if (orderChanged && expanded.value && !ringingTakesOver && pendingIndex == null) {
                        ExpandedSelection.afterSelectedGone(previousList, selectedIndex.value, list)
                    } else {
                        ExpandedSelection.Outcome.Keep
                    }
                    selectedIndex.update { currentSelected ->
                        when {
                            // collapsed: the primary cutout always shows the top-priority card
                            (orderChanged || recorderEnded) && !expanded.value -> 0
                            // auto-expand picked a card of this very list: keep it
                            pendingIndex != null -> pendingIndex
                            // the shown card was replaced by the same app's new card: show that one
                            goneOutcome is ExpandedSelection.Outcome.Select -> goneOutcome.index
                            orderChanged -> previousKeys.getOrNull(currentSelected)
                                ?.let { keys.indexOf(it) }
                                ?.takeIf { it >= 0 }
                                ?: currentSelected
                            else -> currentSelected
                        }.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                    }
                    if (goneOutcome is ExpandedSelection.Outcome.Collapse) {
                        autoExpandSuppressedUntilMs = wallClockMs() + GONE_CARD_NO_EXPAND_MS
                        pendingSelectKey = null
                        collapse()
                    }
                    previousKeys = keys
                    previousList = list
                    selectionKeys = keys
                }
            }
        }
    }

    private fun observeTorchSwap() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Torch swap collector failed") {
                var wasOpen = false
                combine(expanded, visibleNotifications, selectedIndex) { open, list, index ->
                    TorchSwapView(open, list.getOrNull(index)?.mode, list.any { TorchCard.owns(it.mode) },
                        TorchCard.ringingCallIndex(list) >= 0)
                }.collect { v ->
                    if (TorchCard.settlesPatternOnSwap(wasOpen, v.open, v.selected, v.torchShown) ||
                        TorchCard.settlesPatternOnCollapse(wasOpen, v.open, v.callRinging, v.torchShown)
                    ) TorchController.leaveCard()
                    wasOpen = TorchCard.open(v.selected, v.open)
                }
            }
        }
    }

    private fun pausePulseAutoCollapse() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Pulse auto-collapse pause failed") {
                // The countdown of an open island waits for the pulses, then goes on with what was left
                var wasBusy = false
                pulseGate.pulse.state.map { it.busy }.distinctUntilChanged().collect { busy ->
                    if (busy) {
                        autoCollapse.stop()
                    } else if (wasBusy && expanded.value && !isInputActive.value && !autoCollapse.running) {
                        autoCollapse.start(
                            autoCollapse.resumeDelayMs()
                        )
                    }
                    wasBusy = busy
                }
            }
        }
    }

    private fun cleanReplyDrafts() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Reply-draft cleanup failed") {
                notifications.collect { raw ->
                    replyDrafts.retainOnly(raw.mapTo(HashSet()) { it.key })
                }
            }
        }
    }

    private fun observeAutoExpandEvents() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Auto-expand collector failed") {
                notificationRepo.autoExpandEvent.collect { key ->
                    try {
                        awaitCardSynced(key)
                        val ringing = notifications.value.firstOrNull { it.key == key }
                            ?.let { it.mode == IslandMode.IncomingCall && it.isCallRinging } == true
                        // a ringing call does not take the open torch card: it pulses and waits as the cutout
                        if (ringing && torchCardOpen()) return@collect
                        // the island does not open behind a Face ID scan, a ringing call excepted
                        if (!ringing) pulseGate.awaitFaceIdIdle()
                        // old notification popsup instead of actual one
                        // Stored cards can shift the index. Wait for the incoming key in the displayed list.
                        val latest = notifications.value.firstOrNull { it.key == key }
                        fun shownLatest(cards: List<IslandNotification>): Boolean {
                            val shown = cards.firstOrNull { it.key == key } ?: return false
                            return latest == null || shown == latest
                        }
                        if (!shownLatest(visibleNotifications.value)) {
                            withTimeoutOrNull(CARD_SYNC_TIMEOUT_MS) {
                                visibleNotifications.first { cards -> shownLatest(cards) }
                            }
                        }
                        if (autoExpandBlocked(key, ringing)) return@collect
                        val visible = visibleNotifications.value
                        val index = visible.indexOfFirst { it.key == key }
                        // Some downloads disappear as soon as they finish, and an older notification can resurface.
                        // The same can happen when a route or delivery ends.
                        // It keeps its place without taking over the island.
                        if (index >= 0 && GoneCardGuard.suppressesArrival(
                                recentGoneCards, visible[index], wallClockMs())
                        ) return@collect
                        val typingKey = if (isInputActive.value) visibleNotifications.value.getOrNull(selectedIndex.value)
                            ?.key else null
                        if (typingKey != null && index >= 0 && callInterruptReplyKey.value == null) {
                            callInterruptReplyKey.value = typingKey
                        }
                        if (index >= 0) {
                            // A ringing call takes the island and lets go of a held countdown, any other new event does not
                            val arrivingCard = visible[index]
                            if (arrivingCard.mode == IslandMode.IncomingCall
                                && arrivingCard.isCallRinging) selectByUser(index)
                            else selectedIndex.value = index
                            // the selection collector may not have seen this list yet
                            if (selectionKeys != visible.map { it.key }) pendingSelectKey = key
                            val card = visible[index]
                            val cfg = settings.value
                            if (mayAutoExpandOnArrival(card, cfg)
                            ) {
                                if (widgetIsland.holds()) {
                                    if (cardClosesWidgetIsland(card.mode) && pulseGate.claimExpand(card)) {
                                        widgetIsland.deferredExpandKey = key
                                    }
                                } else if (pulseGate.claimExpand(card)) {
                                    expandIsland(requireCard = false, automatic = true)
                                    // the island refused to open: the card was settled for it, so it pulses after all
                                    if (!expanded.value) pulseGate.repulse(card)
                                }
                            }
                            if (!expanded.value) {
                                pendingSelectKey = null
                                selectedIndex.value = 0
                            }
                        }
                    } finally {
                        // however the island decided, a pulse held back for it is let go
                        notificationRepo.settleAutoExpand(key)
                        pulseGate.recheck()
                    }
                }
            }
        }
    }

    /**
     * The checks after the last suspending wait: true when this arrival must not open the island or take the
     * selection. Order matters, a ringing call is let through by several of them
     */
    private fun autoExpandBlocked(key: String, ringing: Boolean): Boolean {
        if (ringing && !settings.value.isEventAutoExpand(IslandEvent.CALL.id)) return true
        // an exempt app is in front: the island stays out of it, the card pulses once the app is left
        if (!ringing && exemptionNow() != ExemptMode.None) return true
        if (feed.autoExpandBlockedByTap) {
            val arriving = notifications.value.firstOrNull { it.key == key }
            if (!(arriving?.mode == IslandMode.IncomingCall && arriving.isCallRinging)) return true
        }
        if (isLocked.value && !settings.value.autoExpandOnLockScreen &&
            notifications.value.firstOrNull { it.key == key }?.mode != IslandMode.IncomingCall
        ) return true
        if (shadeOpen.value) return true
        // The island has just closed because the live event it showed ended
        if (wallClockMs() < autoExpandSuppressedUntilMs) {
            val arrivingNow = notifications.value.firstOrNull { it.key == key }
            if (!(arrivingNow?.mode == IslandMode.IncomingCall && arrivingNow.isCallRinging)) return true
        }
        if (expanded.value) {
            val arriving = notifications.value.firstOrNull { it.key == key }
            if (!(arriving?.mode == IslandMode.IncomingCall && arriving.isCallRinging)) return true
        }
        return false
    }

    /**
     * `notifications` is derived from the repository through a combine and stateIn, so it can still be a step behind
     * the event. Wait until it holds the card as the repository has it now (a repost of a shown key, such as a timer
     * finishing, is already there but still the old version), else the card is looked up in a stale list and neither
     * expands the island nor takes the selection
     */
    private suspend fun awaitCardSynced(key: String) {
        fun synced(list: List<IslandNotification>): Boolean {
            val latest = notificationRepo.notifications.value.firstOrNull { it.key == key } ?: return true
            return list.firstOrNull { it.key == key } == latest
        }
        if (!synced(notifications.value)) {
            withTimeoutOrNull(CARD_SYNC_TIMEOUT_MS) {
                notifications.first { list -> synced(list) }
            }
        }
    }

    private fun observeExemptApp() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Exempt-app collector failed") {
                combine(islandExemption, expanded) { mode, open -> Pair(mode != ExemptMode.None, open) }
                    .distinctUntilChanged()
                    .collect { (exempt, open) ->
                        if (exempt) {
                            if (open) collapse()
                            widgetIsland.close(snap = true)
                        }
                    }
            }
        }
    }

    private fun observeWidgetIslandRelease() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Widget-island release collector failed") {
                combine(widgetIsland.open, widgetIsland.shown) { open, shown -> open || shown }
                    .distinctUntilChanged()
                    .collect { holds ->
                        val key = widgetIsland.deferredExpandKey
                        if (holds || key == null) return@collect
                        widgetIsland.deferredExpandKey = null
                        if (feed.autoExpandBlockedByTap) return@collect
                        val index = visibleNotifications.value.indexOfFirst { it.key == key }
                        if (index < 0 || expanded.value) return@collect
                        selectedIndex.value = index
                        expandIsland(requireCard = false, automatic = true)
                    }
            }
        }
    }

    private fun observeTimerReset() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Timer-reset collector failed") {
                notificationRepo.resetTimerEvent.collect {
                    resetAutoCollapseTimer()
                }
            }
        }
    }

    private fun closeEmptyExpandedIsland() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Empty-island guard failed") {
                // Safety net: an expanded island with nothing to show closes
                combine(expanded, visibleNotifications, appsLauncherOpen) { open, list, apps -> open && list.isEmpty()
                    && !apps }
                    .distinctUntilChanged()
                    .collectLatest { emptyOpen ->
                        if (!emptyOpen) return@collectLatest
                        delay(EMPTY_ISLAND_GRACE_MS)
                        if (expanded.value && visibleNotifications.value.isEmpty() && !appsLauncherOpen.value) {
                            collapse()
                        }
                    }
            }
        }
    }

    private fun observeExpandedState() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Expanded-state collector failed") {
                expanded.collect { isExpanded ->
                    if (isExpanded) {
                        // Every way of expanding (tap, auto-expand, swipe) ends up here
                        imports.requestShadeImport()
                        imports.requestSameAppCards()
                        if (!isInputActive.value) {
                            autoCollapse.start()
                        }
                    } else {
                        autoCollapse.stop()
                        autoCollapse.paused.value = false
                        textSelecting.value = false
                        autoCollapse.held.value = false
                        autoCollapse.pinned.value = false
                        autoCollapse.resetLeft()
                        isInputActive.value = false
                        siblingPicker.reset()
                        expandedMedia.value = null
                        replyDrafts.resume.value = null
                        pendingSelectKey = null
                        selectedIndex.value = 0
                    }
                }
            }
        }
    }

    private fun observeSiblingPicker() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Sibling picker collector failed") { siblingPicker.collect() }
        }
    }

    private fun observeCardSteps() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Card step collector failed") {
                // the open step belongs to 1 card
                combine(expanded, visibleNotifications, selectedIndex) { open, list, index ->
                    if (open) list.getOrNull(index)?.key else null
                }.distinctUntilChanged().collect { shownKey ->
                    val state = cardStepState.value
                    if (state.key != null && state.key != shownKey) {
                        cardStepState.value = CardStepState()
                    }
                }
            }
        }
    }

    private fun observeExpandedMedia() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Expanded media collector failed") {
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
    }

    private fun observeAutoCollapsePause() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Auto-collapse pause collector failed") {
                // The quick-launch window (Open Apps shortcut) is in use
                combine(
                    combine(
                        // Reading an expanded text or holding a selection
                        combine(autoCollapse.paused, textSelecting) { reading, selecting -> reading || selecting },
                        siblingPickerOpen, autoCollapse.held, appsLauncherOpen, cardDepth
                    ) { reading, picking, held, apps, depth ->
                        reading || picking || held || apps || depth != CardDepth.First
                    },
                    // Map navigation opened by the user stays open until the user puts it away
                    combine(visibleNotifications, selectedIndex, autoExpanded) { list, index, auto ->
                        !auto && list.getOrNull(index)?.mode == IslandMode.Navigation
                    },
                    // The torch card: none when the user opened it, held while it is touched
                    combine(visibleNotifications, selectedIndex, autoExpanded, TorchController.holding) {
                        list, index, auto, touched ->
                        TorchCard.timeout(list.getOrNull(index)?.mode, auto, touched) != TorchTimeout.Normal
                    }
                ) { stepHold, navigationHold, torchHold -> stepHold || navigationHold || torchHold }
                    .distinctUntilChanged()
                    .collect { paused ->
                    if (paused) {
                        autoCollapse.stop()
                    } else if (expanded.value && !isInputActive.value && !autoCollapse.running) {
                        if (TorchCard.owns(selectedMode())) {
                            autoCollapse.start(autoCollapse.resumeDelayMs())
                        } else {
                            // Reading is over: the usual countdown starts again from 0
                            autoCollapse.start()
                        }
                    }
                }
            }
        }
    }

    private fun observeSelection() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Selection collector failed") {
                // An expanded message belongs to the card it was opened on
                selectedIndex.collect {
                    autoCollapse.paused.value = false
                    // The selection belonged to the card that was shown
                    clearTextSelection()
                    val releasePin = selectionReleasesPin
                    selectionReleasesPin = false
                    // A card the user (or a ringing call) moved to starts with a fresh countdown, a new event that only
                    // shifted the selection leaves the held one alone
                    if (releasePin && autoCollapse.pinned.value) {
                        autoCollapse.pinned.value = false
                        if (expanded.value) autoCollapse.start()
                    }
                }
            }
        }
    }

    private fun resetAutoExpandedOnClose() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Auto-expanded reset collector failed") {
                // However the island closes (timer, tap, shade, swipe)
                expanded.collect { open ->
                    if (!open) {
                        autoExpanded.value = false
                        pulseGate.releaseKept()
                    }
                }
            }
        }
    }

    private fun observeInputActive() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Input-active collector failed") {
                isInputActive.collect { active ->
                    if (active) {
                        autoExpanded.value = false
                        autoCollapse.stop()
                        // Typing is active again: the resume request is used up
                        replyDrafts.resume.value = null
                    } else if (expanded.value) {
                        autoCollapse.start()
                    }
                }
            }
        }
    }

    private fun resumeTyping() {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Typing resume failed") {
                // a call took the island while a reply was being typed
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
                            delay(RESUME_SETTLE_MS)
                            pulseGate.pulse.state.first { !it.busy }
                            val index = visibleNotifications.value.indexOfFirst { it.key == replyKey }
                            if (replyKey != null && index >= 0 && !feed.autoExpandBlockedByTap) {
                                selectedIndex.value = index
                                expandIsland(requireCard = false)
                                // The card is on screen now, so its reply box can open and take the keyboard
                                delay(RESUME_CARD_SHOWN_MS)
                                replyDrafts.resume.value = replyKey
                            }
                            callInterruptReplyKey.value = null
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private enum class ResumeState { NONE, GONE, WAIT, READY }

    /** Open Apps shortcut (long press on the collapsed cutout, portrait). */
    fun openAppsLauncher() {
        if (!settings.value.enableAppShortcuts || shadeOpen.value || expanded.value) return
        if (exemptionNow() != ExemptMode.None || widgetIsland.holds()) return
        // landscape, the empty collapsed pill the long press was made on
        feed.holdEmptyPill()
        launcherClosingJob?.cancel()
        launcherClosing.value = false
        appsLauncherOpen.value = true
        expanded.value = true
    }

    val launcherClosing = MutableStateFlow(false)
    private var launcherClosingJob: Job? = null

    private fun closeLauncher() {
        if (!appsLauncherOpen.value) return
        val until = LauncherClose.holdUntil(wallClockMs())
        autoExpandSuppressedUntilMs = maxOf(autoExpandSuppressedUntilMs, until)
        pulseGate.suppressAtLeastUntil(until)
        launcherClosing.value = true
        appsLauncherOpen.value = false
        launcherClosingJob?.cancel()
        launcherClosingJob = viewModelScope.launch {
            delay(LauncherClose.HOLD_MS)
            launcherClosing.value = false
        }
    }

    /** A tap or call from the UI: with no card there is nothing to open. */
    fun expand() = expandIsland(requireCard = true)

    /**
     * [requireCard] false is for the callers that already hold the card they are opening (auto-expand, deferred
     * expand, typing resume): `visibleNotifications` is derived through several stateIn steps and can still be a
     * step behind them, so an empty list there must not stop the island opening
     */
    private fun expandIsland(requireCard: Boolean, automatic: Boolean = false) {
        if (shadeOpen.value) return
        val tapIndex = if (automatic) null else pulseGate.tapIndex()
        if (visibleNotifications.value.isEmpty() && (requireCard || tapIndex != null)) return
        if (!currentCardMayExpand(tapIndex)) return
        // An exempt app is in front (island off or static)
        if (exemptionNow() != ExemptMode.None || widgetIsland.holds()) return
        tapIndex?.let { commitPulseTap(it) }
        if (automatic && !expanded.value) autoExpanded.value = true
        expanded.value = true
    }

    /** [index] is the card a pulse tap would open, null: the selected one. */
    private fun currentCardMayExpand(index: Int? = null): Boolean {
        val card = visibleNotifications.value.getOrNull(index ?: selectedIndex.value) ?: return true
        return AppEventRules.mayExpandCard(settings.value.noExpandPackages, card.packageName, card.mode)
    }

    fun outsideTouchPassesThrough(): Boolean =
        expanded.value && AutoCollapsePause.outsideTouchPassesThrough(autoExpanded.value, autoCollapse.pinned.value) &&
            !isInputActive.value &&
            expandedMedia.value == null && !siblingPickerOpen.value && !appsLauncherOpen.value

    fun collapse() {
        keepEmptyPillAfterLauncher()
        closeLauncher()
        isInputActive.value = false
        expanded.value = false
        autoExpanded.value = false
    }

    fun setInputActive(active: Boolean) {
        if (isInputActive.value != active) {
            isInputActive.value = active
        }
    }

    fun toggleExpanded() = setExpanded(!expanded.value)

    /** Aims the island at [target], a tap asking for where it already is does nothing so a burst cannot flip-flop it. */
    fun setExpanded(target: Boolean) {
        if (expanded.value == target) return
        val tapIndex = if (target) pulseGate.tapIndex() else null
        pulseGate.clearCompanionPick()
        if (target) {
            openFromGesture(tapIndex)
        } else {
            keepEmptyPillAfterLauncher()
            closeLauncher()
            feed.setIdleClosedByTap(true)
            expanded.value = false
        }
    }

    private fun revealStoredBesideLive() {
        if (IslandPriority.revealsStoredOnOpen(visibleNotifications.value, hasIdleStoredCards.value)) {
            revealIdleCards(tapAnimation = false)
        }
    }

    fun dragExpand(open: Boolean): Boolean {
        if (!open) {
            if (expanded.value) {
                feed.setIdleClosedByTap(false)
                collapse()
            }
            return true
        }
        if (expanded.value) return false
        return openFromGesture(pulseGate.tapIndex())
    }

    private fun openFromGesture(tapIndex: Int?): Boolean {
        if (!currentCardMayExpand(tapIndex)) return false
        if (exemptionNow() != ExemptMode.None || widgetIsland.holds()) return false
        if (shadeOpen.value || visibleNotifications.value.isEmpty()) return false
        if (tapIndex != null) commitPulseTap(tapIndex)
        feed.setIdleClosedByTap(false)
        feed.allowAutoExpandAgain()
        revealStoredBesideLive()
        expanded.value = true
        return true
    }

    private fun autoCollapseBlocked(): Boolean =
        isInputActive.value || autoCollapse.paused.value || textSelecting.value || autoCollapse.pinned.value ||
            siblingPickerOpen.value || autoCollapse.held.value || appsLauncherOpen.value || pulseGate.blocksExpand() ||
            navigationOpenedByUser() || torchTimeoutNow() != TorchTimeout.Normal ||
            cardSteps.cardDepthNow() != CardDepth.First ||
            cardDepth.value != CardDepth.First

    fun resetAutoCollapseTimer() {
        if (!expanded.value) return
        // a touch during a pulse over the card starts the countdown over once the pulses are done
        if (pulseGate.blocksExpand()) autoCollapse.resetLeft()
        autoCollapse.start()
    }

    fun setSelectedNotificationIndex(index: Int) {
        val list = visibleNotifications.value
        if (index in list.indices) {
            if (!expanded.value) pulseGate.noteCompanionPick(index)
            selectByUser(index)
            resetAutoCollapseTimer()
        }
    }

    fun switchEvent(forward: Boolean) {
        switchJob?.cancel()
        switchJob = viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Event switch failed") { selectEventCard(forward) }
        }
    }

    fun repulseCurrent() = pulseGate.repulseCurrent()

    /** Picks the previous or next card (the shade's loud ones included) and selects it, returns it. */
    private suspend fun selectEventCard(forward: Boolean): IslandNotification? {
        imports.importBeforeSwipe()

        val full = feed.allCards()
        val currentKey = visibleNotifications.value.getOrNull(selectedIndex.value)?.key
        val target = nextEventTarget(full, currentKey, forward) ?: return null
        return target.takeIf { selectCard(target) }
    }

    /** Selects [target], bringing the cards that were put away back first when it is one of them. */
    private suspend fun selectCard(target: IslandNotification): Boolean {
        if (visibleNotifications.value.none { it.key == target.key }) {
            revealIdleCards(tapAnimation = false)
            withTimeoutOrNull(SHADE_IMPORT_WAIT_MS) {
                visibleNotifications.first { list -> list.any { it.key == target.key } }
            }
            delay(40)
        }
        val index = visibleNotifications.value.indexOfFirst { it.key == target.key }
        if (index < 0) return false
        selectByUser(index)
        resetAutoCollapseTimer()
        return true
    }

    /**
     * Steps to the next or previous entry, one per app. Entries put away after their pulse count too, so a card that
     * pulsed and settled is not out of reach of the swipe. Returns false when there is only 1 entry
     */
    fun switchNotification(forward: Boolean): Boolean {
        val full = feed.allCards()
        val shown = visibleNotifications.value
        val currentKey = shown.getOrNull(selectedIndex.value)?.key
        val fullIndex = nextAppGroupIndex(full, full.indexOfFirst { it.key == currentKey }, forward)
        if (fullIndex < 0) return false
        val group = appGroupKey(full[fullIndex])
        val index = shown.indexOfFirst { appGroupKey(it) == group }
        if (index >= 0) {
            selectByUser(index)
            resetAutoCollapseTimer()
            return true
        }
        val target = full[fullIndex]
        switchJob?.cancel()
        switchJob = viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Notification switch failed") { selectCard(target) }
        }
        return true
    }

    fun switchSibling(forward: Boolean): Boolean {
        val list = visibleNotifications.value
        val index = nextSiblingIndex(list, selectedIndex.value, forward)
        if (index < 0) return false
        selectByUser(index)
        resetAutoCollapseTimer()
        return true
    }

    fun openSiblingPicker(): Boolean = siblingPicker.openPicker()

    fun closeSiblingPicker() = siblingPicker.closePicker()

    fun selectSibling(key: String): Boolean = siblingPicker.selectSibling(key)

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

    companion object {
        private const val TAG = "IslandViewModel"

        /** Longest the auto-expand collector waits for `notifications` to catch up with a just-posted card. */
        private const val CARD_SYNC_TIMEOUT_MS = 250L

        private const val RESUME_SETTLE_MS = 300L
        private const val EMPTY_ISLAND_GRACE_MS = 400L
        private const val GONE_CARD_NO_EXPAND_MS = 1_500L

        private const val RECORDER_END_NO_EXPAND_MS = 1_800L
        private const val RESUME_CARD_SHOWN_MS = 150L
        private const val SHADE_IMPORT_WAIT_MS = 250L

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
            val start = if (currentPosition >= 0) currentPosition else if (forward) -1 else 0
            val step = if (forward) 1 else -1
            for (offset in 1..groups.size) {
                val group = groups[Math.floorMod(start + step * offset, groups.size)]
                if (group == currentGroup) continue
                val members = ordered.filter { appGroupKey(it) == group }
                return members.firstOrNull { !it.isSilent } ?: members.first()
            }
            return null
        }

        fun provideFactory(
            settingsRepo: SettingsSource,
            notificationRepo: INotificationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IslandViewModel(settingsRepo, notificationRepo) as T
            }
        }
    }
}

private data class TorchSwapView(val open: Boolean, val selected: IslandMode?, val torchShown: Boolean, val callRinging: Boolean)
