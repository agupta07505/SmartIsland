/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PickerSwap
import com.agupta07505.smartisland.model.StepDrag
import com.agupta07505.smartisland.model.TorchCardSize
import kotlin.math.roundToInt

class CardHeightCache {
    val heights = mutableStateOf(emptyMap<String, Dp>())
    /** Content signature of each card at measure time. */
    val signatures = mutableMapOf<String, Int>()
}

/** Pager bookkeeping between compositions: written by effects, never read to draw. */
internal class PagerSyncState {
    var committedGroups: List<String>? = null

    var draggedByFinger = false

    var shapeChangedDuringDrag = false
}

@Composable
fun IslandExpandedContent(
    notifications: List<IslandNotification>,
    launcherApps: List<LaunchableApp>?,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit = { _, _ -> },
    onCollapse: () -> Unit,
    statusBarHeight: Dp,
    onHeightMeasured: (Dp) -> Unit,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onTextExpandedChanged: (Boolean) -> Unit = {},
    siblingPickerOpen: Boolean = false,
    onSiblingSelected: (String) -> Unit = {},
    onSiblingPickerClose: () -> Unit = {},
    sideBySidePicker: Boolean = false,
    pickerOnLeft: Boolean = false,
    splitCardMaxWidth: Dp = 560.dp,
    splitPickerWidth: Dp = 320.dp,
    singleIslandWidth: Dp = 420.dp,
    onSplitCardWidthMeasured: (Dp) -> Unit = {},
    // True while a swipe swaps the card itself: the pager jumps to the page
    snapPageSync: () -> Boolean = { false },
    sideBarsAlpha: () -> Float = { 1f },
    drawnCardKey: Array<String?>? = null,
    heightCache: CardHeightCache? = null
) {
    if (notifications.isEmpty()) {
        EmptyAppsIsland(settings, launcherApps, onLaunchApp, onAppAction, onHeightMeasured, modifier)
        return
    }

    val ownHeightCache = remember { CardHeightCache() }
    val cardHeightCache = heightCache ?: ownHeightCache
    val pageHeights by cardHeightCache.heights
    val mediaExpansion = LocalMediaExpansion.current
    // Landscape draws the picture as its own island
    val mediaOpenKey = if (sideBySidePicker) null else mediaExpansion.key
    val cardStepsAccess = LocalCardSteps.current

    ForgetStaleCardHeights(notifications, selectedIndex, cardHeightCache)

    val pager = rememberExpandedPager(notifications, selectedIndex, siblingPickerOpen, onPageSelected, snapPageSync)
    val pageCards = pager.pageCards

    val bottomPadding = 16.dp

    Column(modifier = modifier.fillMaxWidth().wrapContentHeight()) {

        val pageHeight = rememberPagerHeight(
            pager, pageHeights, cardStepsAccess, mediaOpenKey, sideBySidePicker, siblingPickerOpen
        )
        val currentPage = pageHeight.page
        val currentNotification = pageHeight.notification
        val targetHeight = pageHeight.targetHeight
        // The gesture supplies the dragged step's height (the shell reads it directly)
        val stepDrag = LocalStepDrag.current

        val split = rememberSplitState(
            notifications, pageCards, currentPage, selectedIndex, siblingPickerOpen, sideBySidePicker,
            settings, mediaExpansion, cardStepsAccess, currentNotification, stepDrag, targetHeight, onHeightMeasured
        )
        val sideBySide = split.sideBySide
        val shownHeight = split.shownHeight

        val portraitPickerOpen = split.pickerShown && !sideBySide
        val portraitSwap = rememberPortraitSwap(portraitPickerOpen, settings, sideBySidePicker)
        val swap = portraitSwap.progress
        val cardLayerShown by portraitSwap.cardLayerShown
        val pickerLayerShown by portraitSwap.pickerLayerShown

        // Landscape with the picker open: 2 islands with a gap
        val splitCard = if (sideBySide) currentNotification else null
        if (splitCard != null) {
            SplitIslands(
                splitCard, split, settings, targetHeight, stepDrag, mediaExpansion, cardStepsAccess, cardHeightCache,
                bottomPadding, launcherApps, onOpenNotification, onLaunchApp, onAppAction, onCollapse,
                onReplyStateChanged, onTextExpandedChanged, onSiblingSelected, onSiblingPickerClose,
                pickerOnLeft, splitCardMaxWidth, splitPickerWidth, singleIslandWidth, onSplitCardWidthMeasured
            )
        }

        // single-island layout: the card layer
        if (!sideBySide) Box(modifier = Modifier.fillMaxWidth()) {
            if (cardLayerShown) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(shownHeight)
                    .graphicsLayer {
                        val p = swap.value
                        alpha = PickerSwap.cardAlpha(p)
                        scaleX = PickerSwap.cardScale(p)
                        scaleY = PickerSwap.cardScale(p)
                        // Notch mode: the swap scales and fades in place
                        transformOrigin = if (settings.enableNotchMode) TransformOrigin.Center
                        else TransformOrigin(0.5f, 0f)
                    },
                verticalAlignment = rememberStepAwareAlignment()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(targetHeight)
                ) {
                    OverscanScrim(currentNotification, cardHeightCache, targetHeight, settings, sideBarsAlpha)
                    CardPager(
                        pager, notifications, settings, drawnCardKey, cardHeightCache,
                        bottomPadding, launcherApps, onOpenNotification, onLaunchApp, onAppAction, onCollapse,
                        onReplyStateChanged, onTextExpandedChanged
                    )
                    NeighbourProbes(pager, cardHeightCache, stepDrag, siblingPickerOpen, bottomPadding, settings,
                        launcherApps)
                }
            }
            if (pickerLayerShown) {
                PortraitPicker(split, currentNotification, portraitPickerOpen, swap, settings, onSiblingSelected,
                    onSiblingPickerClose)
            }
        }
    }
}

/** Island with no cards: the app shortcuts on their own. */
@Composable
private fun EmptyAppsIsland(
    settings: SmartIslandSettings,
    launcherApps: List<LaunchableApp>?,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit,
    onHeightMeasured: (Dp) -> Unit,
    modifier: Modifier
) {
    if (!settings.enableAppShortcuts) {
        return
    }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(unbounded = true)
            .onSizeChanged {
                val measuredHeight = with(density) { it.height.toDp() }
                if (measuredHeight > 0.dp) {
                    val clamped = measuredHeight.coerceIn(80.dp, 260.dp)
                    onHeightMeasured(clamped)
                }
            }
    ) {
        if (settings.enableNotificationBackdrop) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(settings.notificationDotColor).copy(alpha = 0.22f),
                                Color(settings.notificationDotColor).copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 700f
                        )
                    )
            )
        }
        EmptyExpanded(settings = settings, apps = launcherApps, onLaunchApp = onLaunchApp, onAppAction = onAppAction)
    }
}

/** Drops a card's measured height when the card goes or its content changes. */
@Composable
private fun ForgetStaleCardHeights(
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    cache: CardHeightCache
) {
    var pageHeights by cache.heights
    val activeKeys = remember(notifications) { notifications.map { it.key }.toSet() }
    LaunchedEffect(activeKeys) {
        pageHeights = pageHeights.filterKeys { it in activeKeys }
    }

    // a measured height is forgotten when the card's content changes
    val heightSignatures = cache.signatures
    LaunchedEffect(notifications) {
        val shownKey = notifications.getOrNull(selectedIndex)?.key
        val stale = ArrayList<String>()
        notifications.forEach { n ->
            val signature = notificationHeightSignature(n)
            val before = heightSignatures.put(n.key, signature)
            if (before != null && before != signature && n.key != shownKey) stale += n.key
        }
        heightSignatures.keys.retainAll(notifications.mapTo(HashSet()) { it.key })
        if (stale.isNotEmpty()) pageHeights = pageHeights.filterKeys { it !in stale }
    }
}

/** Light look only: dims below a card that is shorter than the island. */
@Composable
private fun BoxScope.OverscanScrim(
    currentNotification: IslandNotification?,
    cache: CardHeightCache,
    targetHeight: Dp,
    settings: SmartIslandSettings,
    sideBarsAlpha: () -> Float
) {
    val pageHeights by cache.heights
    // Light look only: while the island's height catches up with a shorter card
    val overscanPalette = LocalIslandPalette.current
    if (overscanPalette.isLight && currentNotification != null) {
        val overscanFromDp = pageHeights[currentNotification.key] ?: targetHeight
        val overscanHasPicture = notificationHasPictureBackdrop(currentNotification, settings)
        val overscanScrimScale = cardScrimScale(settings)
        val overscanColor = if (overscanHasPicture) {
            Color.Black.copy(alpha = 0.85f * overscanScrimScale)
        } else {
            overscanPalette.scrim.copy(alpha = 0.45f)
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        color = overscanColor,
                        topLeft = Offset(0f, overscanFromDp.toPx()),
                        size = Size(size.width, 300.dp.toPx())
                    )
                    // the same at the sides: while the island opens

                    val sideBars = sideBarsAlpha()
                    if (overscanHasPicture && sideBars > 0f) {
                        val pageHeightPx = overscanFromDp.toPx()
                        val sideWidthPx = 48.dp.toPx()
                        val dim = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f *
                        overscanScrimScale * sideBars),
                                Color.Black.copy(alpha = 0.85f *
                        overscanScrimScale * sideBars)
                            ),
                            startY = 0f,
                            endY = pageHeightPx
                        )
                        drawRect(
                            brush = dim,
                            topLeft = Offset(-sideWidthPx, 0f),
                            size = Size(sideWidthPx, pageHeightPx)
                        )
                        drawRect(
                            brush = dim,
                            topLeft = Offset(size.width, 0f),
                            size = Size(sideWidthPx, pageHeightPx)
                        )
                    }
                }
        )
    }
}

/** Sibling list that fades in over the card in portrait. */
@Composable
private fun PortraitPicker(
    split: SplitState,
    currentNotification: IslandNotification?,
    portraitPickerOpen: Boolean,
    swap: Animatable<Float, AnimationVector1D>,
    settings: SmartIslandSettings,
    onSiblingSelected: (String) -> Unit,
    onSiblingPickerClose: () -> Unit
) {
    val pickerShown = split.pickerShown
    val pickerSiblings = split.pickerSiblings
    val siblingsHold = split.siblingsHold
    SiblingPicker(
        siblings = if (pickerShown) pickerSiblings else siblingsHold.toList(),
        currentKey = currentNotification?.key,
        // a tap during the fade-out belongs to nobody: the picker is already closing
        onSelect = { key -> if (portraitPickerOpen) onSiblingSelected(key) },
        onClose = { if (portraitPickerOpen) onSiblingPickerClose() },
        tapSlop = GestureSensitivity.fromId(settings.gestureSensitivity).scaled(PICKER_TAP_SLOP_DP).dp,
        // The list is white text straight on the island surface
        modifier = Modifier.graphicsLayer {
            val p = swap.value
            alpha = PickerSwap.pickerAlpha(p)
            scaleX = PickerSwap.pickerScale(p)
            scaleY = PickerSwap.pickerScale(p)
            transformOrigin = if (settings.enableNotchMode) TransformOrigin.Center
            else TransformOrigin(0.5f, 0f)
        }.background(
            expandedSurfaceColor(settings, LocalIslandPalette.current).copy(
                alpha = siblingPickerBackingAlpha(settings.opacity, SIBLING_PICKER_SURFACE_OPACITY)
            )
        )
    )
}

internal fun clampCardHeightForMode(mode: IslandMode?, height: Dp): Dp = when (mode) {
    IslandMode.Battery -> height.coerceIn(72.dp, 104.dp)
    IslandMode.IncomingCall -> height.coerceIn(72.dp, 125.dp)
    IslandMode.CallEnded -> height.coerceIn(84.dp, 145.dp)
    IslandMode.Notification -> height.coerceIn(95.dp, NOTIFICATION_CARD_MAX_HEIGHT)
    IslandMode.Music -> height.coerceIn(115.dp, 180.dp)
    IslandMode.LiveActivity -> height.coerceIn(140.dp, 205.dp)
    IslandMode.Navigation -> height.coerceIn(NAVIGATION_EXPANDED_HEIGHT, NAVIGATION_EXPANDED_HEIGHT + 20.dp)
    IslandMode.DownloadUpload -> height.coerceIn(120.dp, 195.dp)
    IslandMode.Hotspot -> height.coerceIn(100.dp, 195.dp)
    IslandMode.Bluetooth -> height.coerceIn(72.dp, 130.dp)
    IslandMode.Flashlight -> height.coerceIn(TorchCardSize.SHORT_SCREEN_MIN_HEIGHT_DP.dp, TorchCardSize.MAX_HEIGHT_DP.dp)
    IslandMode.ScreenRecording -> height.coerceIn(72.dp, 130.dp)
    IslandMode.AudioRecording -> height.coerceIn(72.dp, 130.dp)
    IslandMode.Timer -> height.coerceIn(72.dp, 130.dp)
    IslandMode.Stopwatch -> height.coerceIn(72.dp, 130.dp)
    else -> height.coerceIn(80.dp, 160.dp)
}

internal fun cardAccentColor(notification: IslandNotification, settings: SmartIslandSettings): Color =
    settings.eventColorFor(notification)?.let { Color(it) } ?: when (notification.mode) {
        IslandMode.Battery -> Color(settings.batteryColor)
        IslandMode.IncomingCall, IslandMode.CallEnded -> Color(settings.callColor)
        IslandMode.LiveActivity -> Color(settings.liveActivityColor)
        IslandMode.Navigation -> Color(settings.navigationColor)
        IslandMode.DownloadUpload -> Color(settings.transferColor)
        IslandMode.Hotspot -> Color(settings.hotspotColor)
        IslandMode.Bluetooth -> connectivityAccent(notification.category, settings.bluetoothColor)
        IslandMode.Flashlight -> Color(settings.flashlightColor)
        IslandMode.ScreenRecording, IslandMode.AudioRecording -> Color(settings.screenRecordingColor)
        IslandMode.Timer -> Color(settings.timerColor)
        IslandMode.Stopwatch -> Color(settings.stopwatchColor)
        else -> Color(settings.notificationDotColor)
    }

private val OLD_STEP_BOTTOM_FADE = 28.dp

/** A notification card, with the old step fading out under a step drag. */
@Composable
private fun BoxScope.NotificationStepCard(
    notification: IslandNotification,
    bottomPadding: Dp,
    settings: SmartIslandSettings,
    onOpenNotification: (IslandNotification) -> Unit,
    onCollapse: () -> Unit,
    onReplyStateChanged: (Boolean) -> Unit,
    onTextExpandedChanged: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    // While a step follows the finger
    val stepDrag = LocalStepDrag.current
    val stepping = stepDrag.active && !stepDrag.settled && stepDrag.cardKey == notification.key
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = if (stepDrag.active && !stepDrag.settled && stepDrag.cardKey == notification.key) {
                StepDrag.newStepAlpha(stepDrag.progress)
            } else 1f
        }
    ) {
        NotificationExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = { onOpenNotification(notification) },
            onCollapse = onCollapse,
            showActions = settings.showNotificationActions,
            settings = settings,
            onReplyStateChanged = onReplyStateChanged,
            onTextExpandedChanged = onTextExpandedChanged
        )
    }
    val oldSteps = stepDrag.fromSteps
    if (stepping && oldSteps != null) {
        val naturalHeightPx = remember { IntArray(1) }
        val fadePx = with(density) { OLD_STEP_BOTTOM_FADE.toPx() }
        Box(
            modifier = Modifier
                .matchParentSize()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val shellPx = if (stepDrag.heightDp >= 0f) (stepDrag.heightDp * density.density)
                        .roundToInt() else placeable.height
                    val h = minOf(placeable.height, shellPx.coerceAtLeast(0))
                    naturalHeightPx[0] = placeable.height
                    layout(placeable.width, h) { placeable.place(0, 0) }
                }
                .clipToBounds()
                .graphicsLayer {
                    alpha = StepDrag.oldStepAlpha(stepDrag.progress)
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                // The shell shortens under the old step: what is cut off at the bottom fades out instead of ending in
                // a hard edge
                .drawWithContent {
                    drawContent()
                    if (size.height < naturalHeightPx[0]) {
                        val top = (size.height - fadePx).coerceAtLeast(0f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = top, endY = size.height
                            ),
                            topLeft = Offset(0f, top),
                            size = Size(size.width, size.height - top),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            NotificationExpanded(
                notification = notification,
                bottomPadding = bottomPadding,
                onOpenNotification = {},
                onCollapse = {},
                showActions = settings.showNotificationActions,
                settings = settings,
                frozen = true,
                stepsOverride = oldSteps
            )
        }
    }
}

@Suppress("NAME_SHADOWING")
@Composable
internal fun BoxScope.ModeCardContent(
    notification: IslandNotification,
    bottomPadding: Dp,
    settings: SmartIslandSettings,
    launcherApps: List<LaunchableApp>?,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit,
    onCollapse: () -> Unit,
    onReplyStateChanged: (Boolean) -> Unit,
    onTextExpandedChanged: (Boolean) -> Unit,
    foregroundAlpha: (() -> Float)?
) {
    // The colour the user gave this card's event reaches every part the card draws
    val settings = settings.forCard(notification)
    when (notification.mode) {
        IslandMode.Notification -> NotificationStepCard(
            notification = notification,
            bottomPadding = bottomPadding,
            settings = settings,
            onOpenNotification = onOpenNotification,
            onCollapse = onCollapse,
            onReplyStateChanged = onReplyStateChanged,
            onTextExpandedChanged = onTextExpandedChanged
        )
        IslandMode.IncomingCall -> IncomingCallExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.Music -> MusicExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            settings = settings,
            foregroundAlpha = foregroundAlpha
        )
        IslandMode.Battery -> BatteryExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            settings = settings
        )
        IslandMode.LiveActivity -> LiveActivityExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = { onOpenNotification(notification) },
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.Navigation -> NavigationExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = { onOpenNotification(notification) },
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.DownloadUpload -> DownloadExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = { onOpenNotification(notification) },
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.Hotspot -> HotspotExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = { onOpenNotification(notification) },
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.Bluetooth -> BluetoothExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.Flashlight -> FlashlightExpanded(
            bottomPadding = bottomPadding,
            settings = settings
        )
        IslandMode.ScreenRecording -> ScreenRecordingExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.AudioRecording -> AudioRecordingExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.Timer -> TimerExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = { onOpenNotification(notification) },
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.CallEnded -> CallEndedExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            settings = settings
        )
        IslandMode.Stopwatch -> StopwatchExpanded(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = { onOpenNotification(notification) },
            onCollapse = onCollapse,
            settings = settings
        )
        IslandMode.Empty -> EmptyExpanded(
            settings = settings,
            apps = launcherApps,
            onLaunchApp = onLaunchApp,
            onAppAction = onAppAction
        )
    }
}

internal fun notificationHeightSignature(n: IslandNotification): Int =
    if (liveCardTextDoesNotChangeHeight(n.mode)) {
        // a live card's text ticks (a clock, a progress, a call timer)
        listOf(n.mode, n.media.size, n.actionIntents.map { it.title }).hashCode()
    } else {
        listOf(n.mode, n.title, n.text, n.bigText, n.readerText, n.media.size, n.callStatusText,
            n.actionIntents.map { it.title }).hashCode()
    }

internal fun liveCardTextDoesNotChangeHeight(mode: IslandMode?): Boolean = when (mode) {
    IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording,
    IslandMode.IncomingCall, IslandMode.Navigation, IslandMode.DownloadUpload -> true
    else -> false
}
