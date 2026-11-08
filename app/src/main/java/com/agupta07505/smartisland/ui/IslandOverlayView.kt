/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.res.Configuration
import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.IdleDotGeometry
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.withEdgePill
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.AutoHideHold
import com.agupta07505.smartisland.model.BubbleOrigin
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.CardPrewarm
import com.agupta07505.smartisland.model.CollapsedPulse
import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.EdgePulseOwnership
import com.agupta07505.smartisland.model.HideSink
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandPhase
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.NotchTopEdge
import com.agupta07505.smartisland.model.PILL_SPRING_DAMPING
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.PillPhase
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.TapZone
import com.agupta07505.smartisland.model.TorchCardSize
import com.agupta07505.smartisland.model.TwoShellSwap
import com.agupta07505.smartisland.model.cardClosesWidgetIsland
import com.agupta07505.smartisland.model.centreBandOverlapsCutout
import com.agupta07505.smartisland.model.pillOnIsland
import com.agupta07505.smartisland.model.portraitCenterTapIsIdleControl
import com.agupta07505.smartisland.model.widgetIslandHoldsExpand
import com.agupta07505.smartisland.ui.expanded.LocalIslandPalette
import com.agupta07505.smartisland.ui.expanded.LocalMediaExpansion
import com.agupta07505.smartisland.ui.expanded.LocalReaderScroll
import com.agupta07505.smartisland.ui.expanded.LocalStepDrag
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import kotlin.math.abs

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
    onAppAction: (String, AppShortcutAction) -> Unit = { _, _ -> },
    onToggleExpanded: () -> Unit,
    onSetExpanded: ((Boolean) -> Unit)? = null,
    onDragExpand: (Boolean) -> Boolean = { false },
    onPulseTouch: (Boolean) -> Unit = {},
    /** A tap on the pulse over an open card: the card [String] (its pulse id) is opened, its pulse is over. */
    onPulseCardOpen: (String) -> Unit = {},
    onRepulseCurrent: () -> Unit = {},
    onDismissNotification: () -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit = {},
    statusBarHeight: Float,
    idleWidgetsSuppressed: Boolean = false,
    tickPillHold: Boolean = false,
    onTickPillHold: (Boolean) -> Unit = {},
    textSelecting: Boolean = false,
    onClearTextSelection: () -> Unit = {},
    modifier: Modifier = Modifier,
    isInputActive: Boolean = false,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onTextExpandedChanged: (Boolean) -> Unit = {},
    siblingPickerOpen: Boolean = false,
    readingExpandedText: Boolean = false,
    /** Depth of the shown card (preview / reader / picture). */
    cardDepth: CardDepth = CardDepth.First,
    onCardDeeper: () -> Boolean = { false },
    onCardBack: () -> Boolean = { false },
    onSiblingSelected: (String) -> Unit = {},
    onSiblingPickerClose: () -> Unit = {},
    onOpenSiblingPicker: () -> Boolean = { false },
    onDismissAllNotifications: () -> Unit = {},
    isFullWidth: Boolean = true,
    hasIdleStoredCards: Boolean = false,
    /** Hold on the pill hid it (the view model's pillHidden). */
    pillHidden: Boolean = false,
    /** A fullscreen app hides the island (the view model's fullscreenHidden). */
    fullscreenHidden: Boolean = false,
    onRevealIdleCards: () -> Unit = {},
    onSwitchEvent: (forward: Boolean) -> Unit = {},
    onSwitchNotification: ((forward: Boolean) -> Boolean)? = null,
    onSwitchSibling: (forward: Boolean) -> Boolean = { false },
    onTogglePillHidden: () -> Unit = {},
    onExpandedSurfaceBounds: (Rect?) -> Unit = {},
    onUserEngaged: () -> Unit = {},
    /** A tap swap begins (true) or is over (false). */
    onSwapBusyChange: (Boolean) -> Unit = {},
    /** Swipe moves or fires on the island: the idle timeout starts counting again. */
    onSwipeActive: () -> Unit = {},
    onHandleHeld: (Boolean) -> Unit = {},
    onDragActive: () -> Unit = {},
    onHandleTap: () -> Unit = {},
    autoExpanded: Boolean = false,
    autoCollapsePinned: Boolean = false,
    pickerOnLeft: Boolean = false,
    landscapeDot: IdleDotGeometry? = null,
    edgeCamera: EdgeCamera? = null,
    widgetPill: Boolean = false,
    onIdleDotTap: () -> Unit = {},
    onOpenAppsLauncher: () -> Unit = {},
    idleClosing: Boolean = false,
    appsLauncherOpen: Boolean = false,
    idleLeaving: Boolean = false,
    widgetIslandOpen: Boolean = false,
    onWidgetIslandChange: (Boolean) -> Unit = {},
    onWidgetIslandShownChange: (Boolean) -> Unit = {},
    /** Secondary bubble is out beside the pill (not merged, not hidden). */
    onSecondaryApartChange: (Boolean) -> Unit = {},
    widgetIslandSnap: Int = 0,
    collapsedPulse: CollapsedPulse.State = CollapsedPulse.State(),
    /** The collapsed pill pulses ([com.agupta07505.smartisland.data.collapsedPulses]); null: portrait only. */
    pulseDriven: Boolean? = null,
    /** Lock screen hides content: the wide pulse shows the glyph without the title. */
    pulseRedacted: Boolean = false
) {
    val safeIndex = selectedIndex.coerceIn(0, (notifications.size - 1).coerceAtLeast(0))
    val scope = rememberCoroutineScope()
    val ui = remember { IslandInteractionState() }
    val onSwapBusyState = rememberUpdatedState(onSwapBusyChange)
    ui.onSwapBusy = { busy -> onSwapBusyState.value(busy) }
    // The service can outlive this composition. Clear its busy flag if the view goes away mid-swap.
    DisposableEffect(ui) { onDispose { onSwapBusyState.value(false) } }
    val selectFromPill = rememberCardSelect(ui, scope, expanded, notifications, safeIndex, onPageSelected)
    val expandedNowState = rememberUpdatedState(expanded)
    val onToggleNow = rememberUpdatedState(onToggleExpanded)
    val wiring = rememberIslandWiring(
        actions = IslandActions(
            expand = ExpandActions(
                toggle = onToggleExpanded,
                setExpanded = onSetExpanded ?: { target -> if (expandedNowState.value != target) onToggleNow.value() },
                dragExpand = onDragExpand,
                reportSurfaceBounds = onExpandedSurfaceBounds
            ),
            card = CardActions(
                selectPage = selectFromPill,
                switchEvent = onSwitchEvent,
                switchNotification = onSwitchNotification,
                deeper = onCardDeeper,
                back = onCardBack,
                open = onOpenNotification,
                dismiss = onDismissNotification,
                dismissAll = onDismissAllNotifications,
                clearTextSelection = onClearTextSelection
            ),
            sibling = SiblingActions(
                switchSibling = onSwitchSibling,
                openPicker = onOpenSiblingPicker,
                closePicker = onSiblingPickerClose
            ),
            idle = IdleActions(
                revealCards = onRevealIdleCards,
                dotTap = onIdleDotTap,
                togglePillHidden = onTogglePillHidden
            ),
            pulse = PulseActions(touch = onPulseTouch, repulseCurrent = onRepulseCurrent),
            system = SystemActions(
                openAppsLauncher = onOpenAppsLauncher,
                openFloatingWindow = onOpenFloatingWindow,
                openNotificationShade = onOpenNotificationShade
            )
        ),
        view = IslandViewState(
            settings = settings,
            notifications = notifications,
            selectedIndex = safeIndex,
            expanded = expanded,
            cardDepth = cardDepth,
            mediaExpansion = LocalMediaExpansion.current,
            readingExpandedText = readingExpandedText,
            siblingPickerOpen = siblingPickerOpen,
            hasIdleStoredCards = hasIdleStoredCards,
            textSelecting = textSelecting
        )
    )
    ExpandCollapseHapticEffect(expanded, wiring, launcherOpen = appsLauncherOpen)

    WireInteractionCallbacks(
        ui, onUserEngaged, onSwipeActive, onDragActive, onHandleHeld, onHandleTap, onTickPillHold
    )
    WidgetIslandWiring(ui, widgetIslandOpen, onWidgetIslandChange, settings)
    // The reader text reports its scrolling here
    ui.readerScroll = LocalReaderScroll.current
    ui.stepDrag = LocalStepDrag.current
    val context = LocalContext.current
    val runner = remember(context, wiring) { IslandActionRunner(context, wiring) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    ui.tapSwapSpeed = settings.animationSpeed(isLandscape)
    val screenWidth = configuration.screenWidthDp.dp
    val screenCenter = screenWidth / 2f
    // Landscape (and portrait for a tap on the idle circle)
    val edgeStyleEarly = isLandscape && edgeCamera != null &&
        LandscapeLayoutStyle.isEdge(settings.landscapeLayoutStyle)
    // Edge layout: the hold on the camera middle hides the pill whatever it carries
    @Suppress("NAME_SHADOWING")
    val widgetPill = widgetPill && !(edgeStyleEarly && pillHidden)
    val pillExit = rememberLandscapePillExit(
        // the widget pill is a pill on the island too
        hasContent = pillOnIsland(notifications.isNotEmpty(), widgetPill && isLandscape),
        expanded = expanded,
        isLandscape = isLandscape,
        idleLeaving = idleLeaving,
        notchMode = settings.enableNotchMode || edgeStyleEarly,
        screenWidthDp = screenWidth.value,
        animSpeed = settings.animationSpeed(isLandscape),
        pillOnScreen = !((settings.hideWhenIdle && notifications.isEmpty())
            || (settings.autoHidePill && ui.isAutoHidden))
    )
    @Suppress("NAME_SHADOWING")
    val settings = pillExit.frozenNow ?: settings
    val edgeStyle = isLandscape && edgeCamera != null &&
        LandscapeLayoutStyle.isEdge(settings.landscapeLayoutStyle)
    // The long-press hide, or auto-hide with nothing live on the island: the edge layout hides the same way for both
    val edgeHiddenNow = pillHidden || EdgePillRules.autoHiddenIdle(
        settings.autoHidePill, ui.isAutoHidden, notifications.isNotEmpty()
    )
    val edge = rememberEdgePill(
        edgeStyle = edgeStyle,
        edgeCamera = edgeCamera,
        notifications = notifications,
        safeIndex = safeIndex,
        hasIdleStoredCards = hasIdleStoredCards,
        pillHidden = edgeHiddenNow,
        expanded = expanded,
        animationSpeed = settings.animationSpeed(isLandscape)
    )
    val edgeCardsAway = edge.cardsAway
    val edgeIdleHidden = edge.idleHidden
    val edgeLayoutOn = edge.layoutOn
    val edgePresence = edge.presence
    val edgeFade = edge.fade
    val edgeExiting = edge.exiting
    val edgeHideRunning = edge.hideRunning
    val edgeLayerNotifications = edge.layerNotifications
    val edgeLayerIndex = edge.layerIndex
    val edgeDensity = edge.density
    val edgeCutRect = edge.cutRect
    val edgeCutShape = edge.cutShape
    val edgeAlive by edge.alive
    // The edge layout runs on its own settings
    val layoutSettings = if (edgeLayoutOn && edgeCamera != null) settings.withEdgePill(edgeCamera) else settings
    val sideBySide = rememberSideBySide(ui, settings, isLandscape, expanded, siblingPickerOpen, notifications,
        safeIndex)
    val pickerSideBySide = sideBySide.active
    val twoIslandsDrawn = sideBySide.twoIslandsDrawn
    val activeNotification = notifications.getOrNull(safeIndex)
    val splitWidth = rememberSplitWidth(pickerSideBySide, activeNotification?.key)
    var splitCardWidthDp by splitWidth.cardWidthDp
    val splitWidthCache = splitWidth.cache
    val splitWidthEffective = splitWidth.effective
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

    // a card that is pre-composed while collapsed reports its real height itself
    val animSpeedNow = settings.animationSpeed(isLandscape)
    val screenOn = rememberScreenInteractive()
    val prewarmWanted = notifications.isNotEmpty() && CardPrewarm.shouldPrewarm(activeMode, screenOn)
    var expandedHeight by rememberExpandedHeight(activeMode, notifications.isEmpty(), expanded, prewarmWanted)
    AutoHideEffects(
        expanded, notifications, selectedIndex, settings, ui,
        pulseBusy = (pulseDriven ?: !isLandscape) && collapsedPulse.busy,
        holdLayout = AutoHideHold.applies(isLandscape, settings.enableNotchMode, edgeStyle)
    )

    val holdCutouts = widgetIslandHoldsExpand(widgetIslandOpen, ui.widgetIslandShown)
    val cutoutCards = rememberCutoutCards(holdCutouts, notifications, safeIndex)
    val cutoutNotifications = cutoutCards.notifications
    val cutoutIndex = cutoutCards.index

    val pulsingId = collapsedPulse.pulsing?.id
    val pulseCard = if (pulsingId == null) null else cutoutNotifications.firstOrNull {
        CollapsedPulse.entryOf(it).id == pulsingId
    }
    // The pulse over an open card stands in for the cutout on its spot: a tap on it is a tap on that cutout, the open
    // island swaps to the card that pulses (the shells trade places on the pulse's own bounds), and its pulse is over
    val openPulsingCard: (Int, () -> androidx.compose.ui.geometry.Rect?) -> Unit = openPulse@{ cutoutId, bounds ->
        val id = pulsingId ?: return@openPulse
        val index = cutoutNotifications.indexOfFirst { CollapsedPulse.entryOf(it).id == id }
        if (index < 0) return@openPulse
        ui.userTouched()
        if (turnAroundOnCutoutTap(ui, wiring, index, selectFromPill)) return@openPulse
        ui.markCutout(cutoutId, bounds)
        onPulseCardOpen(id)
        selectFromPill(index)
    }
    val bubbleSwell = rememberBubbleSwell()
    val layout = rememberIslandLayout(
        settings = layoutSettings,
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
        expandedWidthRatio = if (activeMode == IslandMode.Flashlight) TorchCardSize.WIDTH_FRACTION else EXPANDED_WIDTH_RATIO,
        isAutoHidden = ui.isAutoHidden,
        userHidden = edgeIdleHidden,
        idleClosing = idleClosing,
        launcherOpen = appsLauncherOpen,
        context = context,
        configuration = configuration,
        isLandscape = isLandscape,
        screenWidth = screenWidth,
        screenCenter = screenCenter,
        cardSwitching = { ui.cardSwipeSnap },
        landscapePillStretch = widgetPill,
        pillOpen = ui.pillOpen,
        edgeCamera = if (edgeLayoutOn) edgeCamera else null,
        bubbleOrigin = { ui.openedFromBubble },
        pulseDriven = pulseDriven ?: !isLandscape,
        edgeStyle = isLandscape && LandscapeLayoutStyle.isEdge(settings.landscapeLayoutStyle),
        pulseCard = pulseCard,
        pulseRedacted = pulseRedacted,
        bubbleSwell = bubbleSwell.scale
    )
    SideEffect {
        pillExit.record(
            shown = !settings.enableNotchMode && !edgeStyleEarly && !expanded
                && pillOnIsland(notifications.isNotEmpty(), widgetPill && isLandscape) && !layout.isHiding,
            isLandscape = isLandscape,
            settings = settings,
            centerXDp = layout.collapsedMainOffset.value,
            topDp = layout.collapsedTopShift.value,
            widthDp = settings.width,
            heightDp = settings.height
        )
    }
    val pickerLeftFraction = rememberUpdatedState(layout.pickerEdgeFraction)
    val pickerOnLeftState = rememberUpdatedState(pickerOnLeft)
    val layoutState = rememberUpdatedState(layout)
    val layoutForSurface by layoutState
    BubbleOriginEffects(ui, layoutState, expandedNowState, isLandscape)
    val surfaceBack by remember { derivedStateOf { layoutForSurface.singleSurfaceAlpha >= 0.98f } }
    val edgeSurface = rememberEdgeSurface(layoutState, edge, edgeCamera, expanded, pillHidden, notifications)
    val edgeActive = edgeSurface.active
    val edgePillOwnsSurface = edgeSurface.ownsSurface
    val edgeShellSurface = edgeSurface.shellSurface
    val edgeCutOn = edgeSurface.cutOn
    ShellStateEffects(ui, wiring, layoutState, sideBySide, surfaceBack, expanded, notifications, safeIndex, selectedIndex)
    val switchScaleAnim = rememberSwitchScaleAnim(selectedIndex) {
        // Selection also changes during swaps and on close. Skip the extra bounce in those cases.
        ui.tapSwapActive || System.currentTimeMillis() - ui.lastCardSwipeMs < SWIPE_SWITCH_BOUNCE_SKIP_MS ||
            ui.islandPhase(expandedNowState.value) == IslandPhase.Collapsing
    }
    val collapsedContentFade = rememberCollapsedContentFade(
        activeNotification,
        animSpeedNow,
        fadeOut = pillExit.leavingNow
    )
    val bubblePulsingId = if (pulseCard != null && layout.peakFade != null && !expanded) pulsingId else null
    val swellPulsingId = if (pulseCard != null && layout.edgePulseCard != null && !expanded) pulsingId else bubblePulsingId
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
        forcedMergeMs = if (idleClosing) idleClosePlan(animSpeedNow, edgeLayoutOn && edgeCamera != null).step1Ms else 0,
        tapSwapRunning = ui.tapSwapActive,
        onFlightChange = ui::setBubbleFlight,
        bubbleOpen = ui.openedFromBubble,
        pulsingId = bubblePulsingId,
        pulseBusy = layout.peakFade != null && collapsedPulse.busy,
        swell = bubbleSwell.scale
    )
    BubbleSwellEffect(bubbleSwell, swellPulsingId, secondary.cards.secondaryNotification?.key, animSpeedNow, collapsedPulse.morphedFromId)
    AnimScaleProbeEffect()
    MergeTouchHapticEffect(layout, secondary, wiring)
    SecondaryApartEffect(secondary, ui, wiring, onSecondaryApartChange)

    // Hiding sinks the island into the camera (model/HideSink.kt): auto-hide, hide when idle, the edge pill's hold, and
    // a fullscreen app all move 1 presence value, 1 shown to 0 hidden. Landscape classic keeps its fade
    val fullscreenPresence = animateFloatAsState(
        targetValue = if (fullscreenHidden) 0f else 1f,
        animationSpec = spring(
            dampingRatio = PILL_SPRING_DAMPING,
            stiffness = animSpeedNow.stiffness(PILL_SPRING_STIFFNESS)
        ),
        label = "fullscreenPresence"
    )
    val sinkHole = rememberCameraCutoutArea(settings, isLandscape, withNotchHole = true, assumeCentre = true)
    val sinkPivot = remember(
        layout.sinks, edgeStyle, edgeCamera, edgeCutRect, sinkHole, settings.width, settings.height,
        settings.yOffset, settings.enableNotchMode, isLandscape, layout.collapsedMainOffset
    ) {
        when {
            edgeStyle && edgeCamera != null && edgeCutRect != null -> HideSink.edgePivot(edgeCamera, edgeCutRect)
            layout.sinks -> HideSink.portraitPivot(
                sinkHole, settings.enableNotchMode, isLandscape, settings.yOffset,
                settings.width, settings.height, layout.collapsedMainOffset.value
            )
            else -> null
        }
    }
    val hidePresence: () -> Float = {
        val hiding = if (layoutState.value.sinks) {
            minOf(layoutState.value.hidingAlpha, if (edgeStyle) edgeFade.value else 1f)
        } else 1f
        minOf(hiding, fullscreenPresence.value)
    }
    Box(
        modifier = modifier
            // Edge layout keeps the lens clear for the shell too (only while the edge layout is on)
            .edgeLensClear(if (edgeStyle && (edgeLayoutOn || edgeAlive)) edgeCamera else null)
            .hideSink(sinkPivot, hidePresence)
            // Typing in the app below should leave an auto-opened card alone.
            .islandOutsideTap(expanded && !autoExpanded, wiring, isInputActive, autoCollapsePinned, onReplyStateChanged)
            .widgetIslandOutsideTap(widgetIslandOpen && !expanded) { ui.onWidgetGlyphTap?.invoke() },
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            modifier = Modifier.fillMaxSize().islandTopInset { layout.topShift.coerceAtLeast(0.dp).roundToPx() }
                // Edge layout with a translucent pill
                .edgePillCutout(edgeCutOn, edgeCutRect, edgeCutShape),
            contentAlignment = Alignment.TopCenter
        ) {
            // The classic pill's reveal target; the edge pill hides and reveals itself
            if (!edgeLayoutOn) HiddenPillTouchTarget(settings, layout, wiring, ui)

            val notchMode = settings.enableNotchMode
            val shellEdge = rememberShellEdge(
                notchMode, layout, wiring, ui, edgeCamera, edgeLayoutOn, edgeShellSurface, edgePresence, edgeDensity
            )
            val topRoundness = shellEdge.topRoundness
            val edgeCentreDp = shellEdge.centreDp
            val edgeShellSide = shellEdge.side
            val edgeShellMotion = shellEdge.motion

            if (!edgeLayoutOn) CornerPill(settings, layout, secondary, wiring, ui, runner, scope, context)

            // landscape: the collapsed cutout lives in the top corner nearest the camera
            val cornerLockY: () -> Dp =
                { if (isLandscape && !settings.enableNotchMode) layout.collapsedTopShift - layout.topShift else 0.dp }

            if (!edgeLayoutOn) MergeBubbleBackdrop(settings, layout, secondary, pillExit, switchScaleAnim, cornerLockY,
                wiring, ui, screenWidth.value)

            val palette = LocalIslandPalette.current
            val shellOpacity = rememberShellOpacity(
                expanded, activeMode, settings, layoutSettings, layout, pillExit, landscapeDot, animSpeedNow
            )
            val mediaShell = shellOpacity.mediaShell
            val mediaShellOpacity = shellOpacity.opacity

            // landscape, the last card went while the island was open
            val closingOntoIdle: () -> Boolean = {
                isLandscape && !settings.enableNotchMode && !edgeStyle && landscapeDot != null
                    && notifications.isEmpty() && !widgetPill && !appsLauncherOpen &&
                    pillExit.phase == PillPhase.None &&
                    !pillExit.shellSettled()
            }

            val cutoutArea = rememberCameraCutoutArea(settings, isLandscape, withNotchHole = true)
            val centreOverlapsCutout = centreBandOverlapsCutout(
                bandCentreXDp = layout.collapsedMainOffset.value + layout.cameraBand.shiftDp,
                bandWidthDp = layout.cameraBand.widthDp,
                bandTopDp = if (settings.enableNotchMode) 0f else settings.yOffset,
                bandHeightDp = settings.height,
                cutoutCentreXDp = cutoutArea?.centreXDp,
                cutoutWidthDp = cutoutArea?.widthDp ?: 0f,
                cutoutTopDp = cutoutArea?.topDp ?: 0f,
                cutoutHeightDp = cutoutArea?.heightDp ?: 0f
            )

            val pillFades = rememberPillFades(ui, expanded, edgeExiting, edgeActive, edgeShellSurface)
            val pillContentFade = pillFades.contentFade
            val edgeShellAlpha = pillFades.edgeShellAlpha
            // edge pill: both swap shells meet the pill's own corner radius, as the plain collapse does
            val swapShellRadius: (Float) -> Dp = { open ->
                val pillRadius = edgeCamera?.tuning?.cornerRadiusDp
                if (edgeLayoutOn && pillRadius != null) {
                    TwoShellSwap.radiusAt(pillRadius, layout.safeRadius.value, open).dp
                } else layout.safeRadius
            }
            // Two-shell swap: the outgoing shell collapses into the cutout behind the real (incoming) shell
            SwapGhostShell(
                ui = ui,
                radius = { open -> swapShellRadius(open) },
                topRoundness = { open ->
                    if (notchMode && !edgeLayoutOn) NotchTopEdge.swapRoundness(open) else 1f
                },
                edgeSide = edgeShellSide?.let { it.onLeft },
                color = { if (palette.isLight) palette.surface else layout.pillBackgroundColor },
                opacity = { mediaShellOpacity.value * layout.singleSurfaceAlpha },
                widthDamping = swapWidthDamping(edgeLayoutOn)
            )
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = if ((ui.widgetIslandCovers() && !expanded)
                        || (edgeStyle
                        && (edgePillOwnsSurface || EdgePulseOwnership.ownsSurface(edge.pulseOwnsSurface.value, expanded)))) 0f else edgeShellAlpha.value * (if (edgeLayoutOn) EdgePillRules.exitAlpha(edgePresence.value) else 1f) }
                    .islandPillSurface(layoutSettings.forCard(mediaShell), layout, secondary, pillExit, switchScaleAnim,
                        wiring, ui, topRoundness, pickerSideBySide, twoIslandsDrawn, { mediaShellOpacity.value },
                        reportBounds = surfaceBack, shellHidden = { closingOntoIdle() || FaceIdShell.covering },
                        palette = palette, edgeSide = edgeShellSide, edgeCentreDp = edgeCentreDp,
                        edgeMotion = edgeShellMotion, screenWidthDp = screenWidth.value,
                        shellRadius = { swapShellRadius(ui.swapRealOpenOrNull(wiring.view.expanded) ?: 1f) })
                    .islandMainGestures(
                        settings = settings,
                        isInputActive = isInputActive,
                        isLandscape = isLandscape,
                        wiring = wiring,
                        ui = ui,
                        runner = runner,
                        scope = scope,
                        context = context,
                        pickerLeftFraction = pickerLeftFraction,
                        pickerOnLeft = pickerOnLeftState,
                        centreOverlapsCutout = centreOverlapsCutout,
                        pillHasContent = pillOnIsland(notifications.isNotEmpty(), widgetPill && isLandscape),
                        cameraBand = layout.cameraBand,
                        slotSide = layout.slotSide,
                        cameraSide = layout.cameraSide,
                        pulseCentreShiftDp = { layoutState.value.pulseCentreShiftDp },
                        pulseRunning = { (pulseDriven ?: !isLandscape) && collapsedPulse.pulsing != null }
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                val bubbleShell = BubbleOrigin.blends(
                    ui.openedFromBubble, expanded, edgeLayout = edgeLayoutOn, fingerLed = false
                )
                CollapsedShellLayers(
                    layout = layout,
                    collapsedContentFade = collapsedContentFade,
                    activeMode = activeMode,
                    activeNotification = activeNotification,
                    settings = settings,
                    ui = ui,
                    animSpeedNow = animSpeedNow,
                    idleSuppressed = idleWidgetsSuppressed || tickPillHold,
                    pillContentFade = pillContentFade,
                    edgeLayoutOn = edgeLayoutOn,
                    bubbleShell = bubbleShell,
                    isLandscape = isLandscape
                )

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
                        if (abs(measured - splitWidthEffective) > SPLIT_WIDTH_SAME_DP) splitCardWidthDp = measured
                    },
                    ui = ui,
                    animSpeed = animSpeedNow
                )

                FloatingWindowHandle(
                    enabled = expanded && !pickerSideBySide && !isInputActive && activeMode == IslandMode.Notification,
                    pullEnabled = settings.floatingWindowEnabled,
                    visible = !pickerSideBySide && !isInputActive && activeMode == IslandMode.Notification,
                    paused = autoCollapsePinned,
                    fade = { layout.expandedAlpha },
                    wiring = wiring,
                    ui = ui
                )
            }

            if (!edgeLayoutOn) {
                SecondaryBubble(settings, layout, secondary, pillExit, switchScaleAnim, cornerLockY,
                    cutoutNotifications, selectFromPill, wiring, ui, screenWidth.value)
            }
            WidgetIslandLayer(
                open = widgetIslandOpen,
                expanded = expanded,
                hasCards = notifications.isNotEmpty(),
                importantCard = notifications.any { cardClosesWidgetIsland(it.mode) },
                snapCount = widgetIslandSnap,
                settings = settings,
                islandLayout = layout,
                ui = ui,
                screenWidthDp = screenWidth.value,
                isLandscape = isLandscape,
                animSpeed = animSpeedNow,
                onOpenChange = onWidgetIslandChange,
                onShownChange = onWidgetIslandShownChange,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            if (!edgeLayoutOn) PickerGlideBubble(settings, layout, secondary, cornerLockY)
            if (!edgeLayoutOn) TertiaryPill(settings, layout, secondary, switchScaleAnim, cornerLockY, screenCenter,
                selectFromPill, wiring, ui)

            IdleTouchTargets(
                settings, layout, expanded, isLandscape, notifications.isNotEmpty(), pickerSideBySide,
                cutoutArea, centreOverlapsCutout, landscapeDot, edgeLayoutOn, edgeHideRunning, edgeExiting,
                closingOntoIdle, pillExit, secondary, ui, wiring, runner, scope, context
            )

            PulseOverCardPill(
                layout, settings, animSpeedNow, ui, Modifier.align(Alignment.TopCenter),
                // the camera band keeps what it does on the open island: tap puts it away, hold runs the long press
                bandActive = portraitCenterTapIsIdleControl(isLandscape, settings.enableNotchMode,
                    notifications.isNotEmpty()) && !pickerSideBySide,
                isLandscape = isLandscape,
                onHold = wiring.actions.pulse.touch,
                onBandTap = {
                    ui.touch()
                    routeIslandTap(ui, wiring, scope, TapZone.CameraBand)
                },
                onBandLongPress = {
                    ui.touch()
                    IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength),
                        HapticKind.Hold)
                    val longPress = SwipeAction.fromId(wiring.view.settings.longPressAction, SwipeAction.TogglePill)
                    if (longPress != SwipeAction.None) runner.runLongPress(longPress)
                },
                onTap = openPulsingCard
            )

            // face ID scan (ui/FaceIdLayer.kt): drawn last, above the pill and every cutout
            FaceIdLayer(
                settings = settings,
                pillColor = layout.pillBackgroundColor,
                centerOffsetX = layout.collapsedMainOffset,
                isLandscape = isLandscape,
                topOffset = layout.yOffset,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // The edge pill sits outside the top-inset box
        if (edgeStyle && edgeCamera != null && (edgeLayoutOn || edgeAlive)) {
            EdgePillOverlay(
                edge, edgeSurface, edgeCamera, settings, expanded, layout, screenWidth, wiring, runner, ui,
                switchScaleAnim, selectFromPill, animSpeedNow, idleClosing, openPulsingCard
            )
        }

        if (edgeCamera != null && EdgePillRules.hiddenTargetActive(
                edgeStyle, edgeHiddenNow, notifications.size
            ) && !expanded
        ) {
            EdgeHiddenHoldTarget(edgeCamera, ui, wiring, runner, context)
        }
    }
}
