/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.res.Configuration
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

/**
 * The island overlay. This function only wires the pieces together; each behaviour lives in its own file.
 */
@Composable
fun IslandOverlayView(
    settings: SmartIslandSettings,
    expanded: Boolean,
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    launcherApps: List<LaunchableApp>?,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, com.agupta07505.smartisland.model.AppShortcutAction) -> Unit = { _, _ -> },
    onToggleExpanded: () -> Unit,
    onDismissNotification: () -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit = {},
    statusBarHeight: Float,
    /** The idle widgets are held off the pill (Settings > Idle widgets: off after tap to close / long press to hide). */
    idleWidgetsSuppressed: Boolean = false,
    /**
     * Landscape: a tick has brought the pill in although it was hidden (ui/TickPill.kt): the idle widgets stay off
     * it, only the tick shows.
     */
    tickPillHold: Boolean = false,
    onTickPillHold: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    isInputActive: Boolean = false,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onTextExpandedChanged: (Boolean) -> Unit = {},
    siblingPickerOpen: Boolean = false,
    readingExpandedText: Boolean = false,
    /** How deep the shown card is (preview / reader / picture), and the swipe-down / swipe-up step functions (false = no step). */
    cardDepth: com.agupta07505.smartisland.model.CardDepth = com.agupta07505.smartisland.model.CardDepth.First,
    onCardDeeper: () -> Boolean = { false },
    onCardBack: () -> Boolean = { false },
    onSiblingSelected: (String) -> Unit = {},
    onSiblingPickerClose: () -> Unit = {},
    onOpenSiblingPicker: () -> Boolean = { false },
    onDismissAllNotifications: () -> Unit = {},
    isFullWidth: Boolean = true,
    hasIdleStoredCards: Boolean = false,
    onRevealIdleCards: () -> Unit = {},
    onSwitchEvent: (forward: Boolean) -> Unit = {},
    onSwitchNotification: ((forward: Boolean) -> Boolean)? = null,
    onSwitchSibling: (forward: Boolean) -> Boolean = { false },
    onTogglePillHidden: () -> Unit = {},
    onExpandedSurfaceBounds: (android.graphics.Rect?) -> Unit = {},
    onUserEngaged: () -> Unit = {},
    onHandleHeld: (Boolean) -> Unit = {},
    pickerOnLeft: Boolean = false,
    landscapeDot: com.agupta07505.smartisland.model.IdleDotGeometry? = null,
    widgetPill: Boolean = false,
    onIdleDotTap: () -> Unit = {},
    onOpenAppsLauncher: () -> Unit = {},
    idleClosing: Boolean = false,
    /** The expanded island shows the quick-launch grid (Open Apps shortcut) with no card: not a "last card gone" close. */
    appsLauncherOpen: Boolean = false,
    idleLeaving: Boolean = false,
    /** The semi-expanded idle widget island is open (see [WidgetIslandLayer]) and the call that opens or closes it. */
    widgetIslandOpen: Boolean = false,
    onWidgetIslandChange: (Boolean) -> Unit = {},
    onWidgetIslandShownChange: (Boolean) -> Unit = {},
    widgetIslandSnap: Int = 0
) {
    val safeIndex = selectedIndex.coerceIn(0, (notifications.size - 1).coerceAtLeast(0))
    // Gesture code (pointerInput(Unit) never restarts its coroutine) reads these instead of capturing the parameters.
    val cb = rememberIslandCallbacks(
        onToggleExpanded = onToggleExpanded,
        hasIdleStoredCards = hasIdleStoredCards,
        onExpandedSurfaceBounds = onExpandedSurfaceBounds,
        onRevealIdleCards = onRevealIdleCards,
        onIdleDotTap = onIdleDotTap,
        onOpenAppsLauncher = onOpenAppsLauncher,
        onTogglePillHidden = onTogglePillHidden,
        onDismissNotification = onDismissNotification,
        onDismissAllNotifications = onDismissAllNotifications,
        onOpenFloatingWindow = onOpenFloatingWindow,
        onOpenNotificationShade = onOpenNotificationShade,
        onOpenNotification = onOpenNotification,
        onPageSelected = onPageSelected,
        onSwitchEvent = onSwitchEvent,
        onSwitchNotification = onSwitchNotification,
        onSwitchSibling = onSwitchSibling,
        onOpenSiblingPicker = onOpenSiblingPicker,
        onSiblingPickerClose = onSiblingPickerClose,
        siblingPickerOpen = siblingPickerOpen,
        mediaExpansion = com.agupta07505.smartisland.ui.expanded.LocalMediaExpansion.current,
        readingExpandedText = readingExpandedText,
        cardDepth = cardDepth,
        onCardDeeper = onCardDeeper,
        onCardBack = onCardBack,
        expanded = expanded,
        settings = settings,
        notifications = notifications,
        safeIndex = safeIndex
    )
    ExpandCollapseHapticEffect(expanded, cb, launcherOpen = appsLauncherOpen)

    val scope = rememberCoroutineScope()
    val ui = remember { IslandInteractionState() }
    val onUserEngagedState = rememberUpdatedState(onUserEngaged)
    ui.onUserTouch = { onUserEngagedState.value() }
    val onHandleHeldState = rememberUpdatedState(onHandleHeld)
    ui.onHandleHeld = { held -> onHandleHeldState.value(held) }
    val onTickPillHoldState = rememberUpdatedState(onTickPillHold)
    ui.onTickHold = { hold -> onTickPillHoldState.value(hold) }
    WidgetIslandWiring(ui, widgetIslandOpen, onWidgetIslandChange, settings)
    // The reader text reports its scrolling here; the gesture handler refuses a tap that starts on it (see ReaderScrollTracker).
    ui.readerScroll = com.agupta07505.smartisland.ui.expanded.LocalReaderScroll.current
    ui.stepDrag = com.agupta07505.smartisland.ui.expanded.LocalStepDrag.current
    val context = LocalContext.current
    val runner = remember(context, cb) { IslandActionRunner(context, cb) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp
    val screenCenter = screenWidth / 2f
    // Landscape (and portrait for a tap on the idle circle): when the last card goes, the collapsed cutout itself
    // slides away (up, or out to the nearer edge) and the idle circle opens at its spot, overlapping the slide.
    val pillExit = rememberLandscapePillExit(
        // The widget pill is a pill on the island too: a card arriving or leaving swaps its content, the pill neither slides out nor in.
        hasContent = com.agupta07505.smartisland.model.pillOnIsland(notifications.isNotEmpty(), widgetPill && isLandscape),
        expanded = expanded,
        isLandscape = isLandscape,
        idleLeaving = idleLeaving,
        notchMode = settings.enableNotchMode,
        screenWidthDp = screenWidth.value,
        animSpeed = settings.animationSpeed(isLandscape),
        pillOnScreen = !((settings.hideWhenIdle && notifications.isEmpty()) || (settings.autoHidePill && ui.isAutoHidden))
    )
    @Suppress("NAME_SHADOWING")
    val settings = pillExit.frozenNow ?: settings
    // Landscape: an open sibling picker sits beside the card as a SEPARATE island, so the window widens while it is
    // open and this view's own single island surface is switched off.
    val openPicture = com.agupta07505.smartisland.ui.expanded.LocalMediaExpansion.current
    val pictureBeside = expanded && com.agupta07505.smartisland.ui.expanded.pictureShownBesideCard(
        isLandscape = isLandscape,
        openKey = openPicture.key,
        openIndex = openPicture.index,
        cardKey = notifications.getOrNull(safeIndex)?.key,
        cardMediaCount = notifications.getOrNull(safeIndex)?.media?.size ?: 0,
        selectorOpen = openPicture.selectorOpen
    )
    val pickerSideBySideNow = pictureBeside || (isLandscape && expanded && siblingPickerOpen &&
        com.agupta07505.smartisland.model.siblingIndices(notifications, safeIndex).isNotEmpty())
    // Landscape: when the picker closes, the two-island layout (window width, separate surfaces, no shell clip) is
    // kept for the length of the close so the picker island can shrink away instead of vanishing.
    var sideBySideHold by remember { mutableStateOf(false) }
    val sideBySideHoldMs = settings.animationSpeed(isLandscape).durationMs(com.agupta07505.smartisland.model.PickerSwap.CLOSE_MS)
    // A picture island is held like the picker's.
    val stepDragForHold = com.agupta07505.smartisland.ui.expanded.LocalStepDrag.current
    LaunchedEffect(pickerSideBySideNow) {
        if (pickerSideBySideNow) {
            sideBySideHold = true
        } else if (sideBySideHold) {
            androidx.compose.runtime.snapshotFlow { stepDragForHold.active }.first { !it }
            // The content says when its two islands are gone (a timer ended before the card had grown back to the
            // single island's width, so the single surface showed up around a smaller card).
            kotlinx.coroutines.withTimeoutOrNull(sideBySideHoldMs * 3L + 400L) {
                androidx.compose.runtime.snapshotFlow { stepDragForHold.splitShown }.first { !it }
            }
            kotlinx.coroutines.delay(SIDE_BY_SIDE_HOLD_MARGIN_MS)
            sideBySideHold = false
        }
    }
    // The hold also ends the moment the content has left the two-island layout (it reports that in splitShown).
    val pickerSideBySideRaw = pickerSideBySideNow || (sideBySideHold && isLandscape && expanded && stepDragForHold.splitShown)
    // Set by the watchdog below when the flag was still on with nothing open. It drops the flag until the next picker / picture.
    var forceSingle by remember { mutableStateOf(false) }
    LaunchedEffect(pickerSideBySideNow, expanded) { forceSingle = false }
    val pickerSideBySide = pickerSideBySideRaw && !forceSingle
    // What the shell's clip, shadow and surface follow: the flag above only sizes the window early; the two islands
    // are drawn when the content says so too (model/ShellStateGuard.kt).
    val twoIslandsDrawn = com.agupta07505.smartisland.model.ShellStateGuard.twoIslandsDrawn(pickerSideBySide, stepDragForHold.splitShown)
    // Natural width of the card island in the split layout, reported by the content; null until
    // measured (the window then starts at the widest the card may get and settles once measured).
    var splitCardWidthDp by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(pickerSideBySide) { if (!pickerSideBySide) splitCardWidthDp = 0f }
    val activeNotification = notifications.getOrNull(safeIndex)
    // The width each card had in the split layout the last time.
    val splitWidthCache = remember { HashMap<String, Float>() }
    val splitWidthSeed = activeNotification?.key?.let { splitWidthCache[it] } ?: 0f
    val splitWidthEffective = if (splitCardWidthDp > 0f) splitCardWidthDp else splitWidthSeed
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

    // A card that is pre-composed while collapsed reports its real height itself, so the estimate must not overwrite
    // it.
    val screenOn = rememberScreenInteractive()
    val prewarmWanted = notifications.isNotEmpty() && com.agupta07505.smartisland.model.CardPrewarm.shouldPrewarm(activeMode, screenOn)
    var expandedHeight by rememberExpandedHeight(activeMode, notifications.isEmpty(), expanded, prewarmWanted)
    AutoHideEffects(expanded, notifications, selectedIndex, settings, ui)

    // The extra cutouts beside the pill keep the state they had when the widget island opened, until it is back in the cutout.
    val holdCutouts = com.agupta07505.smartisland.model.widgetIslandHoldsExpand(widgetIslandOpen, ui.widgetIslandShown)
    val cutoutSnapshot = remember { arrayOfNulls<Pair<List<IslandNotification>, Int>>(1) }
    val heldCutouts = cutoutSnapshot[0]
    if (!holdCutouts || heldCutouts == null) cutoutSnapshot[0] = notifications to safeIndex
    val cutoutNotifications = if (holdCutouts && heldCutouts != null) heldCutouts.first else notifications
    val cutoutIndex = if (holdCutouts && heldCutouts != null) heldCutouts.second else safeIndex

    val layout = rememberIslandLayout(
        settings = settings,
        notifications = cutoutNotifications,
        expanded = expanded,
        safeIndex = cutoutIndex,
        isFullWidth = isFullWidth,
        pickerOnLeft = pickerOnLeft,
        statusBarHeight = statusBarHeight,
        pickerSideBySide = pickerSideBySide,
        surfaceTwoIslands = twoIslandsDrawn,
        splitCardWidthDp = splitWidthEffective,
        expandedHeight = expandedHeight,
        isAutoHidden = ui.isAutoHidden,
        idleClosing = idleClosing,
        launcherOpen = appsLauncherOpen && expanded,
        context = context,
        configuration = configuration,
        isLandscape = isLandscape,
        screenWidth = screenWidth,
        screenCenter = screenCenter,
        snapGeometry = pillExit.snapGeometry,
        cardSwitching = { ui.cardSwipeSnap },
        landscapePillStretch = widgetPill,
        holdArrivalPulse = holdCutouts
    )
    SideEffect {
        pillExit.record(
            // Both orientations: portrait needs the pill's spot for the slide when a tap on the idle circle starts one.
            shown = !settings.enableNotchMode && !expanded && com.agupta07505.smartisland.model.pillOnIsland(notifications.isNotEmpty(), widgetPill && isLandscape) && !layout.isHiding,
            isLandscape = isLandscape,
            settings = settings,
            // The collapsed pill's rest spot. This runs when the function recomposes, which is no
            // longer once per animation frame, so it must not read the animated size (it would be the first frame's value).
            centerXDp = layout.collapsedMainOffset.value,
            topDp = layout.collapsedTopShift.value,
            widthDp = settings.width,
            heightDp = settings.height
        )
    }
    val pickerLeftFraction = rememberUpdatedState(layout.pickerEdgeFraction)
    val pickerOnLeftState = rememberUpdatedState(pickerOnLeft)
    // True once the single island's surface has faded back in after the two landscape islands (flips twice per split,
    // so reading it here does not recompose per frame of the fade).
    val layoutForSurface by rememberUpdatedState(layout)
    val surfaceBack by remember { androidx.compose.runtime.derivedStateOf { layoutForSurface.singleSurfaceAlpha >= 0.98f } }
    // Close hand-off: the content keeps its two-island layout (the same rectangle at progress 0) until the shell is
    // the single island again: the flag is off, the surface is back, and the window has had two frames to follow.
    LaunchedEffect(pickerSideBySideNow, twoIslandsDrawn, surfaceBack) {
        if (!pickerSideBySideNow && !twoIslandsDrawn && surfaceBack) {
            androidx.compose.runtime.withFrameNanos { }
            androidx.compose.runtime.withFrameNanos { }
            stepDragForHold.shellSingleReady = true
        }
    }
    // Watchdog.
    val notificationsNow by rememberUpdatedState(notifications)
    val safeIndexNow by rememberUpdatedState(safeIndex)
    val selectedIndexNow by rememberUpdatedState(selectedIndex)
    LaunchedEffect(expanded, pickerSideBySide, pickerSideBySideNow, twoIslandsDrawn, sideBySideHold) {
        fun reasonNow(): String? = com.agupta07505.smartisland.model.ShellStateGuard.stuckReason(
            expanded = expanded && layoutForSurface.expandedShown,
            atRest = shellAtRest(ui),
            overlayFlag = pickerSideBySide,
            splitWanted = pickerSideBySideNow,
            twoIslandsDrawn = twoIslandsDrawn,
            singleSurfaceAlpha = layoutForSurface.singleSurfaceAlpha,
            radiusDp = layoutForSurface.safeRadius.value
        )
        androidx.compose.runtime.snapshotFlow { reasonNow() }.collectLatest { first ->
            if (first == null) return@collectLatest
            kotlinx.coroutines.delay(com.agupta07505.smartisland.model.ShellStateGuard.waitMs(sideBySideHold, sideBySideHoldMs))
            val reason = reasonNow() ?: return@collectLatest
            com.agupta07505.smartisland.util.AppLogRecorder.w(
                "ShellState",
                "Shell state stuck: reason=$reason pickerSideBySide=$pickerSideBySide now=$pickerSideBySideNow hold=$sideBySideHold " +
                    "splitShown=${stepDragForHold.splitShown} radius=${layoutForSurface.safeRadius.value} " +
                    "singleSurfaceAlpha=${layoutForSurface.singleSurfaceAlpha} cardPop=${ui.cardPop.value} cardDragX=${ui.cardDragX} " +
                    "card=${notificationsNow.getOrNull(safeIndexNow)?.key} " +
                    "page=${com.agupta07505.smartisland.model.appGroupPageOf(notificationsNow, safeIndexNow)} selected=$selectedIndexNow"
            )
            if (com.agupta07505.smartisland.model.ShellStateGuard.canForceSingle(reason)) forceSingle = true
        }
    }
    // No single expanded island (collapsed, or the two landscape islands).
    LaunchedEffect(expanded, pickerSideBySide) {
        if (!expanded || pickerSideBySide) cb.currentOnExpandedSurfaceBounds(null)
    }
    val switchScaleAnim = rememberSwitchScaleAnim(selectedIndex) {
        System.currentTimeMillis() - ui.lastCardSwipeMs < SWIPE_SWITCH_BOUNCE_SKIP_MS
    }
    // The collapsed content fades in as the idle cutout grows into the pill when a card arrives.
    val collapsedContentFade = rememberCollapsedContentFade(
        activeNotification,
        settings.animationSpeed(isLandscape),
        fadeOut = pillExit.leavingNow
    )
    val secondary = rememberIslandSecondary(
        settings = settings,
        notifications = cutoutNotifications,
        safeIndex = cutoutIndex,
        expanded = expanded,
        layout = layout,
        activeNotification = cutoutNotifications.getOrNull(cutoutIndex),
        isFullWidth = isFullWidth,
        isLandscape = isLandscape,
        screenWidth = screenWidth,
        screenCenter = screenCenter,
        landscapeDot = landscapeDot,
        widgetPill = widgetPill,
        userInteractionTimestamp = ui.userInteractionTimestamp,
        switchScaleAnim = switchScaleAnim,
        forceMerge = idleClosing,
        holdMerge = pillExit.leavingNow,
        forcedMergeMs = if (idleClosing) com.agupta07505.smartisland.model.IdleCloseSequence.plan(settings.animationSpeed(isLandscape)).step1Ms else 0
    )
    AnimScaleProbeEffect()
    MergeTouchHapticEffect(layout, secondary, cb)

    Box(
        modifier = modifier
            .islandOutsideTap(expanded, cb, isInputActive, onReplyStateChanged)
            .widgetIslandOutsideTap(widgetIslandOpen && !expanded) { ui.onWidgetGlyphTap?.invoke() }
            .islandTopInset { layout.topShift.coerceAtLeast(0.dp).roundToPx() },
        contentAlignment = Alignment.TopCenter
    ) {
        HiddenPillTouchTarget(settings, layout, cb, ui)

        // Notch mode, collapsed: square top corners. The radius itself is read in the draw phase (IslandShellModifiers.kt).
        val squareTop = settings.enableNotchMode && !expanded

        // Landscape, expanded: the collapsed pill stays at its own spot beside / above the card.
        CornerPill(settings, layout, secondary, cb, ui, runner, scope, context)

        // Landscape: the collapsed cutout lives in the top corner nearest the camera.
        val cornerLockY: () -> Dp = { if (isLandscape && !settings.enableNotchMode) layout.collapsedTopShift - layout.topShift else 0.dp }

        MergeBubbleBackdrop(settings, layout, secondary, pillExit, switchScaleAnim, cornerLockY, cb)

        // The shell's opacity fades between the shared value and the media value.
        val palette = com.agupta07505.smartisland.ui.expanded.LocalIslandPalette.current
        val mediaShell = expanded && activeMode == IslandMode.Music
        val mediaShellOpacity = androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (mediaShell) settings.mediaOpacity else settings.opacity,
            // When the pill has left and the island takes the idle settings, the opacity jumps (the idle dot covers
            // the spot meanwhile); a 200 ms fade from the pill's opacity showed as a dark box fading into the circle.
            animationSpec = if (pillExit.phase == com.agupta07505.smartisland.model.PillPhase.Settling && landscapeDot != null) {
                androidx.compose.animation.core.snap()
            } else {
                androidx.compose.animation.core.tween(
                    durationMillis = settings.animationSpeed(isLandscape).durationMs(MEDIA_SHELL_OPACITY_FADE_MS)
                )
            },
            label = "mediaShellOpacity"
        )
        // Tells the pill exit when the shell has really reached the idle settings, so the hand-over from the idle dot waits for it.
        SideEffect {
            pillExit.dotCoversHandover = landscapeDot != null
            pillExit.shellSettled = {
                val wantWidth = if (layout.isHiding && layout.hidingAlpha <= 0.01f) 0f else settings.width
                val wantHeight = if (layout.isHiding && layout.hidingAlpha <= 0.01f) 0f else settings.height
                kotlin.math.abs(layout.safeWidth.value - wantWidth) < SHELL_SETTLED_DP &&
                    kotlin.math.abs(layout.safeHeight.value - wantHeight) < SHELL_SETTLED_DP &&
                    kotlin.math.abs(mediaShellOpacity.value - settings.opacity) < SHELL_SETTLED_OPACITY
            }
        }

        // Landscape, the last card went while the island was open (a dismissed card, a call that ended): the shell
        // must not shrink down onto the idle circle.
        val closingOntoIdle: () -> Boolean = {
            isLandscape && !settings.enableNotchMode && landscapeDot != null && notifications.isEmpty() && !widgetPill && !appsLauncherOpen &&
                pillExit.phase == com.agupta07505.smartisland.model.PillPhase.None &&
                !pillExit.shellSettled()
        }

        // Portrait: does the pill's glyph-free centre band reach into the camera cutout (by 1 dp or more)? Decides where a long press
        // opens the Open Apps shortcut (model/PortraitPillTap.kt, longPressOpensApps).
        val cutoutArea = rememberCameraCutoutArea(settings, isLandscape)
        val centreOverlapsCutout = com.agupta07505.smartisland.model.centreBandOverlapsCutout(
            bandCentreXDp = layout.collapsedMainOffset.value,
            bandWidthDp = settings.width * com.agupta07505.smartisland.model.PILL_CENTER_ZONE_SHARE,
            bandTopDp = settings.yOffset,
            bandHeightDp = settings.height,
            cutoutCentreXDp = cutoutArea?.centreXDp,
            cutoutWidthDp = cutoutArea?.widthDp ?: 0f,
            cutoutTopDp = cutoutArea?.topDp ?: 0f,
            cutoutHeightDp = cutoutArea?.heightDp ?: 0f
        )

        // The collapsed pill is not drawn while the widget island is out. Touches still reach the pill underneath
        // (alpha does not change hit testing), so the glyph tap and the outside tap keep working.
        val pillHiddenByIsland = ui.widgetIslandShown && !expanded
        val pillContentAlpha = androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (pillHiddenByIsland) 0f else 1f,
            animationSpec = if (pillHiddenByIsland) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(120),
            label = "pillContentAlpha"
        )
        val pillContentFade = Modifier.graphicsLayer { alpha = pillContentAlpha.value }
        Box(
            modifier = Modifier
                .graphicsLayer { alpha = if (ui.widgetIslandCovers() && !expanded) 0f else 1f }
                .islandPillSurface(settings.forCard(mediaShell), layout, secondary, pillExit, switchScaleAnim, cb, ui, squareTop, pickerSideBySide, twoIslandsDrawn, { mediaShellOpacity.value }, reportBounds = surfaceBack, shellHidden = { closingOntoIdle() || FaceIdShell.covering }, palette = palette)
                .islandMainGestures(
                    settings = settings,
                    isInputActive = isInputActive,
                    isLandscape = isLandscape,
                    cb = cb,
                    ui = ui,
                    runner = runner,
                    scope = scope,
                    context = context,
                    pickerLeftFraction = pickerLeftFraction,
                    pickerOnLeft = pickerOnLeftState,
                    centreOverlapsCutout = centreOverlapsCutout,
                    pillHasContent = com.agupta07505.smartisland.model.pillOnIsland(notifications.isNotEmpty(), widgetPill && isLandscape)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            if (layout.collapsedShown) {
                CollapsedContentLayer(
                    layout = layout,
                    fade = collapsedContentFade,
                    activeMode = activeMode,
                    settings = settings,
                    ui = ui,
                    animSpeed = settings.animationSpeed(isLandscape),
                    idleSuppressed = idleWidgetsSuppressed || tickPillHold,
                    modifier = Modifier.align(Alignment.TopCenter).then(pillContentFade)
                )
                // The clock widget's tick on the full hour, over whatever the pill shows (also while the idle widgets
                // are held off after a tap or long press).
                val tickCanHold = isLandscape && !settings.enableNotchMode
                HourChimeLayer(settings, ui, settings.animationSpeed(isLandscape), Modifier.align(Alignment.TopCenter).then(pillContentFade), canHold = tickCanHold)
                // The sunrise / afternoon / sunset / midnight glyphs (Day moments widget).
                DayMomentLayer(settings, ui, settings.animationSpeed(isLandscape), Modifier.align(Alignment.TopCenter).then(pillContentFade), canHold = tickCanHold)
                // The weather change tick over whatever the pill shows (unless the pill's own weather widget does it).
                WeatherTickLayer(settings, ui, settings.animationSpeed(isLandscape), Modifier.align(Alignment.TopCenter).then(pillContentFade), canHold = tickCanHold)
            }

            ExpandedCardHost(
                expanded = expanded,
                layout = layout,
                settings = settings,
                notifications = notifications,
                activeNotification = activeNotification,
                prewarmWanted = prewarmWanted,
                launcherApps = launcherApps,
                launcherOpen = appsLauncherOpen,
                selectedIndex = selectedIndex,
                onPageSelected = onPageSelected,
                onOpenNotification = onOpenNotification,
                onLaunchApp = onLaunchApp,
                onAppAction = onAppAction,
                onToggleExpanded = onToggleExpanded,
                onReplyStateChanged = onReplyStateChanged,
                onTextExpandedChanged = onTextExpandedChanged,
                siblingPickerOpen = siblingPickerOpen,
                onSiblingSelected = onSiblingSelected,
                onSiblingPickerClose = onSiblingPickerClose,
                isLandscape = isLandscape,
                pickerOnLeft = pickerOnLeft,
                configuration = configuration,
                onHeightMeasured = { expandedHeight = it },
                onSplitCardWidthMeasured = { measured ->
                    activeNotification?.key?.let { key ->
                        if (splitWidthCache.size > SPLIT_WIDTH_CACHE_MAX) splitWidthCache.clear()
                        splitWidthCache[key] = measured
                    }
                    // No state write (no recomposition) when the width is what the window already uses.
                    if (kotlin.math.abs(measured - splitWidthEffective) > SPLIT_WIDTH_SAME_DP) splitCardWidthDp = measured
                },
                ui = ui,
                animSpeed = settings.animationSpeed(isLandscape)
            )

            // A small white handle at the bottom of the expanded island: pull it down to open the notification in a floating window.
            FloatingWindowHandle(
                // Floating window off in settings: no white pill and no pull / squeeze animation at all.
                enabled = settings.floatingWindowEnabled && expanded && !pickerSideBySide && !isInputActive && activeNotification != null,
                visible = settings.floatingWindowEnabled && !pickerSideBySide && !isInputActive && activeNotification != null,
                fade = { layout.expandedAlpha },
                cb = cb,
                ui = ui
            )
        }

        SecondaryBubble(settings, layout, secondary, pillExit, switchScaleAnim, cornerLockY, cutoutNotifications, onPageSelected, cb, ui)
        WidgetIslandLayer(
            open = widgetIslandOpen,
            expanded = expanded,
            hasCards = notifications.isNotEmpty(),
            importantCard = notifications.any { com.agupta07505.smartisland.model.cardClosesWidgetIsland(it.mode) },
            snapCount = widgetIslandSnap,
            settings = settings,
            islandLayout = layout,
            ui = ui,
            screenWidthDp = screenWidth.value,
            isLandscape = isLandscape,
            animSpeed = settings.animationSpeed(isLandscape),
            onOpenChange = onWidgetIslandChange,
            onShownChange = onWidgetIslandShownChange,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        TertiaryPill(settings, layout, secondary, switchScaleAnim, cornerLockY, screenCenter, onPageSelected, cb)

        // Portrait: no separate idle pill. While expanded, the pill's glyph-free centre closes the island.
        val closeZone = rememberExpandedCloseZone(settings, isLandscape)
        // Collapsed, centre band clear of the camera cutout: a long press on the cutout itself hides / shows the pill. The window
        // is the pill's row, so the target sits at the cutout's spot inside it (the service adds the same rectangle to the touchable region).
        val cutoutTarget = cutoutArea?.let { com.agupta07505.smartisland.model.cutoutTouchTarget(it) }
        CutoutLongPressTarget(
            enabled = !expanded && !isLandscape && !settings.enableNotchMode && cutoutTarget != null && !centreOverlapsCutout,
            width = (cutoutTarget?.widthDp ?: 0f).dp,
            height = (cutoutTarget?.heightDp ?: 0f).dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationX = (cutoutTarget?.centreXDp ?: 0f).dp.toPx()
                    translationY = ((cutoutTarget?.topDp ?: 0f) - settings.yOffset).coerceAtLeast(0f).dp.toPx()
                },
            onLongPress = {
                ui.touch()
                IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Hold)
                val longPress = SwipeAction.fromId(cb.currentSettings.longPressAction, SwipeAction.TogglePill)
                if (longPress != SwipeAction.None) {
                    runner.runLongPress(longPress)
                }
            }
        )
        PortraitPillCenterTarget(
            enabled = expanded && com.agupta07505.smartisland.model.portraitCenterTapIsIdleControl(
                isLandscape = isLandscape,
                notchMode = settings.enableNotchMode,
                hasCards = notifications.isNotEmpty()
            ) && !pickerSideBySide,
            zoneWidth = closeZone.widthDp.dp,
            zoneHeight = closeZone.heightDp.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    // On a detected camera cutout: its own spot. Otherwise the pill's sideways offset.
                    translationX = (closeZone.offsetXDp?.dp ?: layout.collapsedMainOffset).toPx()
                    translationY = closeZone.topDp.dp.toPx()
                },
            onTap = {
                com.agupta07505.smartisland.util.AppLogRecorder.i("IdleDot", "portrait centre target tapped while expanded")
                ui.touch()
                cb.currentOnIdleDotTap()
            },
            onLongPress = {
                ui.touch()
                IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Hold)
                val longPress = SwipeAction.fromId(cb.currentSettings.longPressAction, SwipeAction.TogglePill)
                if (longPress != SwipeAction.None) {
                    runner.runLongPress(longPress)
                }
            }
        )

        // Landscape idle dot; portrait has none, its pill centre is the control. Drawn last, above the pill, so it
        // can always be pressed.
        LandscapeIdleDotView(
            dot = landscapeDot,
            alpha = { if (closingOntoIdle()) 1f else pillExit.dotAlpha(secondary.idleDotAlpha) },
            topShiftDp = { layout.topShift.value },
            color = layout.pillBackgroundColor,
            onTap = {
                ui.touch()
                cb.currentOnIdleDotTap()
            },
            onLongPress = {
                ui.touch()
                IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Hold)
                val longPress = SwipeAction.fromId(cb.currentSettings.longPressAction, SwipeAction.TogglePill)
                if (longPress != SwipeAction.None) {
                    runner.runLongPress(longPress)
                }
            }
        )

        // Face ID scan (ui/FaceIdLayer.kt): drawn last, above the pill and every cutout. OverlayIsland keeps
        // everything else off while it plays.
        FaceIdLayer(
            settings = settings,
            pillColor = layout.pillBackgroundColor,
            centerOffsetX = layout.collapsedMainOffset,
            isLandscape = isLandscape,
            topOffset = layout.yOffset,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private fun shellAtRest(ui: IslandInteractionState): Boolean =
    ui.cardDragX == 0f && ui.dragOffset == 0f && ui.swipeDownPull == 0f && ui.handlePull == 0f &&
        ui.pendingCardEnterSign == 0 && ui.cardPop.value >= 1f && ui.cardLeanFromPx == 0f && !ui.stepDrag.active

private const val SHELL_SETTLED_DP = 0.6f
private const val SHELL_SETTLED_OPACITY = 0.02f

private const val MEDIA_SHELL_OPACITY_FADE_MS = 200

private const val SIDE_BY_SIDE_HOLD_MARGIN_MS = 0L

private const val SPLIT_WIDTH_CACHE_MAX = 32
private const val SPLIT_WIDTH_SAME_DP = 0.5f
