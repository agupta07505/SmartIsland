/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.CutoutRect
import com.agupta07505.smartisland.data.IdleDotTap
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.RotationMorph
import com.agupta07505.smartisland.data.RotationMorphTracker
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.edgeCamera
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.CardStepState
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.LauncherClose
import com.agupta07505.smartisland.model.PILL_SPRING_DAMPING
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.RotationVeil
import com.agupta07505.smartisland.model.cutoutCardCount
import com.agupta07505.smartisland.model.hasSecondaryCutout
import com.agupta07505.smartisland.model.pillOnIsland
import com.agupta07505.smartisland.ui.expanded.CardStepsAccess
import com.agupta07505.smartisland.ui.expanded.LocalCardSteps
import com.agupta07505.smartisland.ui.expanded.LocalIslandPalette
import com.agupta07505.smartisland.ui.expanded.LocalMediaExpansion
import com.agupta07505.smartisland.ui.expanded.LocalPictureArrowSide
import com.agupta07505.smartisland.ui.expanded.LocalReaderScroll
import com.agupta07505.smartisland.ui.expanded.LocalReplyDrafts
import com.agupta07505.smartisland.ui.expanded.LocalStepDrag
import com.agupta07505.smartisland.ui.expanded.LocalTextSelection
import com.agupta07505.smartisland.ui.expanded.MediaExpansion
import com.agupta07505.smartisland.ui.expanded.SelectionBarHost
import com.agupta07505.smartisland.ui.expanded.SelectionBarLayer
import com.agupta07505.smartisland.ui.expanded.StepDragState
import com.agupta07505.smartisland.ui.expanded.TextSelectionAccess
import com.agupta07505.smartisland.ui.expanded.pictureArrowSide
import com.agupta07505.smartisland.ui.expanded.rememberIslandPalette
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.FaceIdController
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import com.agupta07505.smartisland.util.ReaderScrollTracker
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

@Composable
fun OverlayIsland(
    viewModel: IslandViewModel,
    statusBarHeight: Float,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit = { _, _ -> },
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit = {},
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = true
) {
    val shadeOpen by viewModel.shadeOpen.collectAsState()
    val idleSuppressed by viewModel.idleWidgetsSuppressed.collectAsState()
    val settings by viewModel.displaySettings.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val widgetIslandOpen by viewModel.widgetIsland.open.collectAsState()
    val widgetIslandSnap by viewModel.widgetIsland.snap.collectAsState()
    val appsLauncherOpen by viewModel.appsLauncherOpen.collectAsState()
    val launcherClosing by viewModel.launcherClosing.collectAsState()
    val launcherShown = LauncherClose.shown(appsLauncherOpen, expanded, launcherClosing)
    val notifications by viewModel.visibleNotifications.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isInputActive by viewModel.isInputActive.collectAsState()
    val autoExpanded by viewModel.autoExpanded.collectAsState()
    val autoCollapsePinned by viewModel.autoCollapsePinnedState.collectAsState()
    val textSelecting by viewModel.textSelecting.collectAsState()
    val selectionClearTick by viewModel.textSelectionClearTick.collectAsState()
    val hasIdleStoredCards by viewModel.hasIdleStoredCards.collectAsState()
    val pillHidden by viewModel.pillHidden.collectAsState()
    val fullscreenHidden by viewModel.fullscreenHidden.collectAsState()
    val orientationState by viewModel.orientation.collectAsState()
    val siblingPickerOpen by viewModel.siblingPickerOpen.collectAsState()
    val pickerOnLeft by viewModel.pickerOnLeft.collectAsState()
    val readingExpandedText by viewModel.readingExpandedText.collectAsState()
    val expandedMedia by viewModel.expandedMedia.collectAsState()
    val cardStepState by viewModel.cardStepState.collectAsState()
    val cardDepth by viewModel.cardDepth.collectAsState()
    val landscapeDot by viewModel.idleDot.collectAsState()
    val widgetPill by viewModel.landscapeWidgetPill.collectAsState()
    val tickHold by viewModel.tickHold.collectAsState()
    val collapsedPulse by viewModel.collapsedPulse.collectAsState()
    val pulseDriven by viewModel.pulseDriven.collectAsState()
    val idleClosing by viewModel.idleClosing.collectAsState()
    val idleLeaving by viewModel.idleLeaving.collectAsState()
    val rotationHidden by viewModel.rotationVeil.collectAsState()
    // portrait, static exempt app: the collapsed pill stays but is dead
    val staticPill by viewModel.islandStaticForApp.collectAsState()
    val faceIdState by FaceIdController.state.collectAsState()
    val quiet = staticPill || faceIdState.active

    val veilSpeed = settings.animationSpeed(
        LocalConfiguration.current.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
    )
    val veilVisible = rememberRotationVeil(rotationHidden, veilSpeed)
    val cardsOnIsland = notifications.isNotEmpty()
    val morph = rememberOverlayMorph(
        viewModel = viewModel,
        orientationState = orientationState,
        notifications = notifications,
        expanded = expanded,
        selectedIndex = selectedIndex,
        widgetPill = widgetPill,
        pillHidden = pillHidden,
        hasIdleStoredCards = hasIdleStoredCards,
        speed = veilSpeed
    )

    val isContentRedacted = isLocked && settings.lockScreenPrivacy == "AppIconOnly"
    val processedNotifications = remember(notifications, isContentRedacted) {
        if (isContentRedacted) redactContents(notifications) else notifications
    }
    val launcherApps by rememberLauncherApps(settings)

    val shownCardKey = notifications.getOrNull(selectedIndex)?.key
    val selectorOpen = cardStepState.key != null && cardStepState.key == shownCardKey && cardStepState.selector
    val stepTapHaptic = rememberStepTapHaptic(settings.hapticStrength)
    val mediaExpansion = rememberMediaExpansion(viewModel, expandedMedia, selectorOpen, stepTapHaptic)
    val cardStepsAccess = rememberCardStepsAccess(viewModel, cardStepState, expandedMedia, stepTapHaptic)
    val readerScrollTracker = remember { ReaderScrollTracker() }
    val stepDragState = remember { StepDragState() }
    val selectionBarHost = remember { SelectionBarHost() }
    val textSelectionAccess = remember(viewModel, selectionClearTick, autoCollapsePinned) {
        TextSelectionAccess(
            clearTick = selectionClearTick,
            heldByPill = autoCollapsePinned,
            setSelecting = { viewModel.setTextSelecting(it) },
            // Share or Search opened another app: the island has done its part
            onActionDone = {
                viewModel.clearTextSelection()
                viewModel.setExpanded(false)
            },
            clear = { viewModel.clearTextSelection() },
            barHost = selectionBarHost
        )
    }
    val onPulseScrollNeed = remember(viewModel) { { ms: Long -> viewModel.pulseScrollNeed(ms) } }
    CompositionLocalProvider(
        LocalPulseScrollNeed provides onPulseScrollNeed,
        LocalMediaExpansion provides mediaExpansion,
        LocalCardSteps provides cardStepsAccess,
        LocalReaderScroll provides readerScrollTracker,
        LocalIslandPalette provides rememberIslandPalette(settings),
        LocalStepDrag provides stepDragState,
        LocalTextSelection provides textSelectionAccess,
        LocalReplyDrafts provides viewModel.replyDrafts,
        LocalPictureArrowSide provides
            pictureArrowSide(
                isLandscape = LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE,
                pickerOnLeft = pickerOnLeft
            )
    ) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
    IslandOverlayView(
        settings = settings,
        expanded = expanded && !quiet,
        // The Open Apps shortcut shows the quick-launch grid
        notifications = if (quiet || launcherShown) emptyList() else processedNotifications,
        selectedIndex = selectedIndex,
        launcherApps = launcherApps,
        onPageSelected = { index -> viewModel.setSelectedNotificationIndex(index) },
        onOpenNotification = onOpenNotification,
        onLaunchApp = onLaunchApp,
        onAppAction = onAppAction,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onSetExpanded = { target -> viewModel.setExpanded(target) },
        onDragExpand = { open -> viewModel.dragExpand(open) },
        onPulseTouch = { held -> viewModel.pulseTouch(held) },
        onPulseCardOpen = { id -> viewModel.pulseCardOpened(id) },
        onRepulseCurrent = { viewModel.repulseCurrent() },
        onDismissNotification = { viewModel.dismissCurrentNotification() },
        onDismissAllNotifications = { viewModel.dismissAllNotifications() },
        onOpenFloatingWindow = onOpenFloatingWindow,
        onOpenNotificationShade = onOpenNotificationShade,
        statusBarHeight = statusBarHeight,
        isInputActive = isInputActive,
        onReplyStateChanged = { viewModel.setInputActive(it) },
        onTextExpandedChanged = { viewModel.setAutoCollapsePaused(it) },
        cardDepth = cardDepth,
        onCardDeeper = { viewModel.cardGoDeeper() },
        onCardBack = { viewModel.cardGoBack() },
        siblingPickerOpen = siblingPickerOpen && !quiet,
        readingExpandedText = readingExpandedText,
        onSiblingSelected = { key -> viewModel.selectSibling(key) },
        onSiblingPickerClose = { viewModel.closeSiblingPicker() },
        onOpenSiblingPicker = { viewModel.openSiblingPicker() },
        isFullWidth = isFullWidth,
        hasIdleStoredCards = hasIdleStoredCards && !quiet,
        idleWidgetsSuppressed = idleSuppressed || quiet,
        tickPillHold = tickHold,
        onTickPillHold = { viewModel.setTickHold(it) },
        textSelecting = textSelecting,
        onClearTextSelection = { viewModel.clearTextSelection() },
        onRevealIdleCards = { viewModel.revealIdleCards() },
        onSwitchEvent = { forward -> viewModel.switchEvent(forward) },
        onSwitchNotification = { forward -> viewModel.switchNotification(forward) },
        onSwitchSibling = { forward -> viewModel.switchSibling(forward) },
        onTogglePillHidden = { viewModel.togglePillHidden() },
        // the island reports its rect on every frame of a size animation
        onExpandedSurfaceBounds = { next ->
            val last = viewModel.expandedSurfaceBounds.value
            if (next == null || last == null ||
                abs(last.left - next.left) > 2 || abs(last.top - next.top) > 2 ||
                abs(last.right - next.right) > 2 || abs(last.bottom - next.bottom) > 2
            ) {
                viewModel.expandedSurfaceBounds.value = next
            }
        },
        onUserEngaged = { viewModel.markUserEngaged() },
        onSwapBusyChange = { busy -> viewModel.swapOwnsSelection.value = busy },
        onSwipeActive = { viewModel.restartIdleTimeout() },
        onHandleHeld = { viewModel.setAutoCollapseHeld(it) },
        onDragActive = { viewModel.resetAutoCollapseTimer() },
        onHandleTap = { viewModel.toggleAutoCollapsePin() },
        autoExpanded = autoExpanded,
        autoCollapsePinned = autoCollapsePinned,
        pickerOnLeft = pickerOnLeft,
        landscapeDot = if (quiet) null else landscapeDot,
        pillHidden = pillHidden,
        fullscreenHidden = fullscreenHidden,
        edgeCamera = if (quiet) null else orientationState.edgeCamera(settings.edgePill, withContent = cardsOnIsland),
        widgetPill = widgetPill && !quiet,
        onOpenAppsLauncher = { viewModel.openAppsLauncher() },
        onIdleDotTap = {
            if (viewModel.onIdleDotTapped() != IdleDotTap.None) stepTapHaptic()
        },
        idleClosing = idleClosing,
        appsLauncherOpen = launcherShown,
        idleLeaving = idleLeaving,
        widgetIslandOpen = widgetIslandOpen && !quiet,
        onWidgetIslandChange = { viewModel.widgetIsland.setOpen(it) },
        onWidgetIslandShownChange = { viewModel.widgetIsland.shown.value = it },
        onSecondaryApartChange = { viewModel.secondaryApart.value = it },
        collapsedPulse = collapsedPulse,
        pulseDriven = pulseDriven,
        pulseRedacted = isContentRedacted,
        widgetIslandSnap = widgetIslandSnap,
        modifier = modifier.graphicsLayer {
            alpha = morph.islandAlpha(veilVisible.value)
        }
    )
    if (morph.morphing.value) {
        RotationMorphSurface(morph, settings, orientationState)
    }
    SelectionBarLayer(selectionBarHost) { viewModel.expandedSurfaceBounds.value }
    }
    }
}

/** Rotation veil: 1 while the island shows, eased down while the window turns and back up after. */
@Composable
private fun rememberRotationVeil(hidden: Boolean, speed: AnimationSpeed): Animatable<Float, AnimationVector1D> {
    val visible = remember { Animatable(1f) }
    LaunchedEffect(hidden) {
        if (hidden) {
            if (SystemAnimScale.animationsOff()) {
                visible.snapTo(0f)
            } else {
                visible.animateTo(
                    0f,
                    tween(
                        speed.durationMs(RotationVeil.HIDE_MS)
                    )
                )
            }
        } else if (SystemAnimScale.animationsOff()) {
            visible.snapTo(1f)
        } else {
            visible.animateTo(
                1f,
                spring(
                    dampingRatio = PILL_SPRING_DAMPING,
                    stiffness = speed.stiffness(PILL_SPRING_STIFFNESS)
                )
            )
        }
    }
    return visible
}

private class OverlayMorph(
    val tracker: RotationMorphTracker,
    val token: Int,
    val rect: State<CutoutRect?>,
    val shownToken: State<Int>,
    val progress: Animatable<Float, AnimationVector1D>,
    val handOver: Animatable<Float, AnimationVector1D>,
    val morphing: State<Boolean>,
    val active: State<Boolean>,
    val quarterHold: State<Boolean>
) {
    /** Island's own alpha: hidden until the morph owns the cutout, back during the hand-over. */
    fun islandAlpha(veil: Float): Float {
        val awaitingMorph = quarterHold.value && active.value && !morphing.value
        val morphHidden = if (awaitingMorph || (token != 0 && token != shownToken.value)) 0f else handOver.value
        return RotationVeil.alpha(veil) * morphHidden
    }
}

@Composable
private fun rememberOverlayMorph(
    viewModel: IslandViewModel,
    orientationState: OrientationState,
    notifications: List<IslandNotification>,
    expanded: Boolean,
    selectedIndex: Int,
    widgetPill: Boolean,
    pillHidden: Boolean,
    hasIdleStoredCards: Boolean,
    speed: AnimationSpeed
): OverlayMorph {
    val rawForMorph = viewModel.settingsSnapshot()
    val morphOn = LandscapeLayoutStyle.isEdge(rawForMorph.landscapeLayoutStyle) &&
        !SystemAnimScale.animationsOff()
    val morphTracker = remember { RotationMorphTracker() }
    val cardsOnIsland = notifications.isNotEmpty()
    val morphCameraArea = rememberCameraCutoutArea(rawForMorph, orientationState.isLandscape)
    val morphRectNow = RotationMorph.collapsedRect(
        raw = rawForMorph,
        state = orientationState,
        hasContent = pillOnIsland(cardsOnIsland, widgetPill),
        hasCards = cardsOnIsland,
        edgeShown = orientationState.isLandscape && (cardsOnIsland || hasIdleStoredCards),
        drawn = !pillHidden && !(rawForMorph.hideWhenIdle && !cardsOnIsland),
        cameraArea = morphCameraArea,
        hasCompanion = hasSecondaryCutout(
            cutoutCardCount(notifications, expanded, selectedIndex)
        )
    )
    morphTracker.observe(orientationState, morphRectNow, morphOn, System.currentTimeMillis())
    val morphToken = morphTracker.token
    val morphSkips = morphTracker.skips
    LaunchedEffect(morphSkips) {
        if (morphSkips > 0) viewModel.rotationMorphActive.value = false
    }
    val morphActive = viewModel.rotationMorphActive.collectAsState()
    val quarterHold = viewModel.rotationQuarterHold.collectAsState()
    LaunchedEffect(morphActive.value) {
        if (!morphActive.value) viewModel.rotationQuarterHold.value = false
    }
    val morphRectState = rememberUpdatedState(morphRectNow)
    val targetReady = viewModel.rotationTargetReady.collectAsState()
    val morphShownToken = remember { mutableIntStateOf(0) }
    val morphProgress = remember { Animatable(1f) }
    val morphHandOver = remember { Animatable(1f) }
    LaunchedEffect(morphToken) {
        if (morphToken == 0) return@LaunchedEffect
        val startedAt = SystemClock.elapsedRealtime()
        morphProgress.snapTo(0f)
        morphHandOver.snapTo(0f)
        morphShownToken.intValue = morphToken
        if (viewModel.expanded.value) viewModel.collapse()
        try {
            // the cutout stays where the old one was until the target is a real read, then moves or morphs from there
            withTimeoutOrNull(RotationVeil.SETTLED_READ_AT_MS + RotationVeil.MARGIN_MS) {
                snapshotFlow { targetReady.value }.first { it }
            }
            val start = morphTracker.from
            val end = morphRectState.value
            if (start != null && end != null && RotationMorph.nearlySame(start, end)) morphProgress.snapTo(1f)
            coroutineScope {
                launch {
                    morphProgress.animateTo(
                        1f,
                        spring(
                            dampingRatio = PILL_SPRING_DAMPING,
                            stiffness = speed.stiffness(PILL_SPRING_STIFFNESS)
                        )
                    )
                }
                launch {
                    snapshotFlow { morphProgress.value }
                        .first { it >= RotationMorph.HAND_OVER_FROM }
                    val settledAt = RotationVeil.SETTLED_READ_AT_MS +
                        RotationVeil.MARGIN_MS
                    val wait = settledAt - (SystemClock.elapsedRealtime() - startedAt)
                    if (wait > 0L) delay(wait)
                    morphHandOver.animateTo(
                        1f,
                        tween(
                            speed.durationMs(RotationMorph.HAND_OVER_MS)
                        )
                    )
                }
            }
        } finally {
            // Cancelled by a newer morph: that one owns the flag now
            if (morphToken == morphTracker.token) viewModel.rotationMorphActive.value = false
        }
    }
    // derived, so the overlay recomposes only when the morph starts or ends
    val morphing = remember(morphToken) {
        derivedStateOf {
            morphToken != 0 &&
                (morphToken != morphShownToken.intValue || morphProgress.value < 1f || morphHandOver.value < 1f)
        }
    }
    return OverlayMorph(
        tracker = morphTracker,
        token = morphToken,
        rect = morphRectState,
        shownToken = morphShownToken,
        progress = morphProgress,
        handOver = morphHandOver,
        morphing = morphing,
        active = morphActive,
        quarterHold = quarterHold
    )
}

/** Cutout drawn while the rotation morph runs, from the old orientation's rect to the new one's. */
@Composable
@NonRestartableComposable
private fun RotationMorphSurface(
    morph: OverlayMorph,
    settings: SmartIslandSettings,
    orientationState: OrientationState
) {
    val from = morph.tracker.from
    val pillColor = Color(settings.islandSurfaceColor)
    val windowTopDp = if (orientationState.isLandscape || settings.enableNotchMode) 0f else settings.yOffset
    Canvas(modifier = Modifier.fillMaxSize()) {
        val to = morph.rect.value
        if (from == null || to == null) return@Canvas
        val started = morph.token == morph.shownToken.value
        val progress = if (started) morph.progress.value else 0f
        val handOver = if (started) morph.handOver.value else 0f
        val r = RotationMorph.lerpInPlace(from, to, progress)
        val surfaceAlpha = RotationMorph.surfaceAlpha(r.opacity, handOver)
        if (surfaceAlpha <= 0f) return@Canvas
        val d = density
        drawPath(
            morphPillPath(r, (r.top - windowTopDp) * d, d),
            color = pillColor.copy(alpha = surfaceAlpha)
        )
    }
}

private fun morphPillPath(r: CutoutRect, top: Float, d: Float): Path = Path().apply {
    addRoundRect(
        RoundRect(
            left = r.left * d,
            top = top,
            right = r.right * d,
            bottom = top + r.height * d,
            topLeftCornerRadius = CornerRadius(r.radiusTopLeft * d),
            topRightCornerRadius = CornerRadius(r.radiusTopRight * d),
            bottomRightCornerRadius = CornerRadius(r.radiusBottomRight * d),
            bottomLeftCornerRadius = CornerRadius(r.radiusBottomLeft * d)
        )
    )
}

/** Lock screen privacy "app icon only": a notification card keeps its app name and loses its contents. */
private fun redactContents(notifications: List<IslandNotification>): List<IslandNotification> =
    notifications.map { notif ->
        if (notif.mode == IslandMode.Notification) {
            notif.copy(
                title = notif.appName,
                text = "Contents hidden",
                actionIntents = emptyList(),
                media = emptyList()
            )
        } else {
            notif
        }
    }

/** Apps of the quick-launch grid: null until the first load finishes, then the loaded list. */
@Composable
private fun rememberLauncherApps(settings: SmartIslandSettings): State<List<LaunchableApp>?> {
    val context = LocalContext.current
    val selectedApps = remember(settings.shortcutPackages, settings.enableAppShortcuts) {
        if (settings.enableAppShortcuts) {
            AppShortcutProvider.selectedApps(context, settings.shortcutPackages)
        } else {
            emptyList()
        }
    }
    return produceState<List<LaunchableApp>?>(
        initialValue = when {
            !settings.enableAppShortcuts -> emptyList()
            selectedApps.isNotEmpty() -> selectedApps
            settings.shortcutPackages.isEmpty() && !settings.showRecentApps -> emptyList()
            !AppShortcutProvider.hasUsageAccess(context) -> emptyList()
            else -> null
        },
        settings.enableAppShortcuts,
        settings.shortcutPackages,
        settings.showRecentApps
    ) {
        value = if (!settings.enableAppShortcuts) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                AppShortcutProvider.shortcuts(
                    context = context,
                    selectedPackages = settings.shortcutPackages,
                    includeRecent = settings.showRecentApps
                )
            }
        }
    }
}

@Composable
private fun rememberStepTapHaptic(hapticStrength: String): () -> Unit {
    val context = LocalContext.current
    val hapticStrengthId by rememberUpdatedState(hapticStrength)
    return remember(context) {
        {
            IslandHaptics.play(
                context,
                HapticStrength.fromId(hapticStrengthId),
                HapticKind.Swipe
            )
        }
    }
}

@Composable
private fun rememberMediaExpansion(
    viewModel: IslandViewModel,
    expandedMedia: Pair<String, Int>?,
    selectorOpen: Boolean,
    stepTapHaptic: () -> Unit
): MediaExpansion = remember(expandedMedia, selectorOpen, viewModel) {
    MediaExpansion(
        key = expandedMedia?.first,
        index = expandedMedia?.second ?: 0,
        selectorOpen = selectorOpen,
        onChange = { key, index ->
            stepTapHaptic()
            viewModel.setExpandedMedia(key, index)
        }
    )
}

@Composable
private fun rememberCardStepsAccess(
    viewModel: IslandViewModel,
    cardStepState: CardStepState,
    expandedMedia: Pair<String, Int>?,
    stepTapHaptic: () -> Unit
): CardStepsAccess = remember(cardStepState, expandedMedia, viewModel) {
    CardStepsAccess(
        state = cardStepState,
        pictureKey = expandedMedia?.first,
        pictureIndex = expandedMedia?.second ?: 0,
        set = { key, steps ->
            stepTapHaptic()
            viewModel.setCardSteps(key, steps)
        },
        reportExpandable = { key, expandable -> viewModel.reportCardTextExpandable(key, expandable) }
    )
}
