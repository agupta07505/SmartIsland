/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.expanded.IslandExpandedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.LaunchableApp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
    onToggleExpanded: () -> Unit,
    onDismissNotification: () -> Unit,
    onOpenFloatingWindow: () -> Unit,
    statusBarHeight: Float,
    modifier: Modifier = Modifier
) {
    // Fix #1: rememberUpdatedState ensures the lambda is always fresh
    // even though pointerInput(Unit) never restarts its coroutine
    val currentOnToggle by rememberUpdatedState(onToggleExpanded)
    val currentOnDismiss by rememberUpdatedState(onDismissNotification)
    val currentOnOpenFloatingWindow by rememberUpdatedState(onOpenFloatingWindow)
    val currentExpanded by rememberUpdatedState(expanded)

    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val density = LocalDensity.current
    val screenWidth = with(density) { displayMetrics.widthPixels.toDp() }
    val screenCenter = screenWidth / 2f
    val expandedWidth = ((displayMetrics.widthPixels / displayMetrics.density) * EXPANDED_WIDTH_RATIO).dp
    val transition = updateTransition(targetState = expanded, label = "islandTransition")

    val sizeSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.6f,
        stiffness = 300f
    )
    val sizeSpecFloat = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 300f
    )
    val heightSpec = spring<androidx.compose.ui.unit.Dp>(
        // Height must never overshoot its measured content. Overshoot is
        // especially visible on the compact battery card as empty black space.
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val alphaSpec = tween<Float>(
        durationMillis = 280,
        easing = FastOutSlowInEasing
    )

    // FIX: Start from collapsed height so the transition target never jumps
    // to a hardcoded 160dp. Content measurement updates this within 1-2 frames,
    // and the spring animation smoothly interpolates to the real height.
    //
    // Keyed on `expanded` so it resets to collapsed height on every expand/collapse
    // cycle, avoiding stale measurements from a previous expansion.
    var expandedHeight by remember(expanded) {
        mutableStateOf(settings.height.dp)
    }

    val compactGap = COMPACT_INDICATOR_GAP_DP.dp
    val miniPillWidth = settings.width.dp
    val circleSize = settings.height.dp
    val compactShapes = compactNotificationShapes(notifications.size, expanded)
    val hasCompanion = notifications.size >= 2
    val collapsedGroupWidth = settings.width.dp + if (hasCompanion) compactGap + circleSize else 0.dp
    val collapsedMainLeft = (screenCenter + settings.xOffset.dp - settings.width.dp / 2f)
        .coerceIn(
            compactGap,
            (screenWidth - collapsedGroupWidth - compactGap).coerceAtLeast(compactGap)
        )
    val collapsedMainOffset = collapsedMainLeft + settings.width.dp / 2f - screenCenter
    val expandedTopOffset = if (hasCompanion) {
        statusBarHeight.dp.coerceAtLeast(circleSize + compactGap)
    } else {
        statusBarHeight.dp
    }

    val width by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandWidth") {
        if (it) expandedWidth else settings.width.dp
    }
    val height by transition.animateDp(transitionSpec = { heightSpec }, label = "islandHeight") {
        // FIX: No more 160.dp hardcoded fallback. expandedHeight is always non-null now.
        // It starts at collapsed height and gets updated to the real content height
        // within 1-2 frames. The spring animation smoothly follows.
        if (it) expandedHeight else settings.height.dp
    }
    val yOffset by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandYOffset") {
        if (it) expandedTopOffset else 0.dp
    }
    val radius by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandRadius") {
        if (it) 34.dp else settings.cornerRadius.dp
    }
    val animatedXOffset by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandXOffset") {
        if (it) 0.dp else collapsedMainOffset
    }

    val collapsedAlpha by transition.animateFloat(
        transitionSpec = { alphaSpec },
        label = "collapsedAlpha"
    ) {
        if (it) 0f else 1f
    }

    val expandedAlpha by transition.animateFloat(
        transitionSpec = { alphaSpec },
        label = "expandedAlpha"
    ) {
        if (it) 1f else 0f
    }

    val contentScale by transition.animateFloat(
        transitionSpec = { sizeSpecFloat },
        label = "contentScale"
    ) {
        if (it) 1f else 0.92f
    }

    val contentSlideY by transition.animateDp(
        transitionSpec = { sizeSpec },
        label = "contentSlideY"
    ) {
        if (it) 0.dp else (-12).dp
    }

    val safeIndex = selectedIndex.coerceIn(0, (notifications.size - 1).coerceAtLeast(0))
    val activeNotification = notifications.getOrNull(safeIndex)
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

    // Tactile spring scale bounce animation when switching active items between pill and circle
    val switchScaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(selectedIndex) {
        switchScaleAnim.animateTo(
            targetValue = 0.88f,
            animationSpec = tween(60, easing = FastOutSlowInEasing)
        )
        switchScaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    // Dual Pill (Multi-Tasking Split Island) Detection:
    // When 2 or more notifications exist (e.g. Music + Notification/Timer/Call), split into Main Pill + Secondary Bubble
    val secondaryNotification = if (notifications.size >= 2) {
        val musicIndex = notifications.indexOfFirst { it.mode == IslandMode.Music }
        if (activeNotification?.mode != IslandMode.Music && musicIndex >= 0) {
            notifications[musicIndex]
        } else {
            notifications.firstOrNull { it.key != activeNotification?.key }
        }
    } else null
    val secondaryIndex = if (secondaryNotification != null) {
        notifications.indexOfFirst { it.key == secondaryNotification.key }
    } else -1
    val tertiaryNotification = if (notifications.size >= 3) {
        notifications.firstOrNull {
            it.key != activeNotification?.key && it.key != secondaryNotification?.key
        }
    } else null
    val tertiaryIndex = if (tertiaryNotification != null) {
        notifications.indexOfFirst { it.key == tertiaryNotification.key }
    } else -1
    val isSplitMode = secondaryNotification != null
    val secondaryIsPill = compactShapes.singleOrNull() == CompactNotificationShape.MiniPill
    val showTertiaryPill = compactShapes.size == 2 && tertiaryNotification != null

    val secondaryAlpha by animateFloatAsState(
        targetValue = if (isSplitMode) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "secondaryAlpha"
    )
    val secondaryScale by animateFloatAsState(
        targetValue = if (isSplitMode) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "secondaryScale"
    )
    val secondaryBubbleWidth by animateDpAsState(
        targetValue = if (secondaryIsPill) miniPillWidth else circleSize,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "secondaryBubbleWidth"
    )
    val secondaryPillProgress = (miniPillWidth - circleSize).value.let { widthDelta ->
        if (widthDelta == 0f) {
            if (secondaryIsPill) 1f else 0f
        } else {
            ((secondaryBubbleWidth - circleSize).value / widthDelta).coerceIn(0f, 1f)
        }
    }
    val secondaryBubbleCorner by animateDpAsState(
        targetValue = if (secondaryIsPill) settings.cornerRadius.dp else circleSize / 2f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "secondaryBubbleCorner"
    )
    val tertiaryAlpha by animateFloatAsState(
        targetValue = if (showTertiaryPill) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "tertiaryAlpha"
    )
    val tertiaryScale by animateFloatAsState(
        targetValue = if (showTertiaryPill) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "tertiaryScale"
    )

    val expandedCompactX = collapsedMainLeft
    val collapsedSecondaryX = collapsedMainLeft + settings.width.dp + compactGap
    val secondaryX by animateDpAsState(
        targetValue = when {
            !expanded -> collapsedSecondaryX
            secondaryIsPill -> expandedCompactX
            else -> expandedCompactX + miniPillWidth + compactGap
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "secondaryX"
    )

    // Outer Box: Fills the entire WindowManager window bounds (which are padded for easy touch)
    val outerModifier = if (currentExpanded) {
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    currentOnToggle()
                }
            }
    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = outerModifier,
        contentAlignment = Alignment.TopCenter
    ) {

        // Inner Box: The actual visible pill container, managing the black background shape and size animations
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .graphicsLayer {
                    translationX = animatedXOffset.toPx()
                    translationY = yOffset.toPx() + dragOffset
                    scaleX = switchScaleAnim.value
                    scaleY = switchScaleAnim.value
                }
                .clip(RoundedCornerShape(radius))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (currentExpanded) {
                            SmartIslandRepositories.notificationRepository(context).resetTimer()
                        } else {
                            currentOnToggle()
                        }
                    }
                }
                .pointerInput(displayMetrics.density) {
                    var dragAccumulator = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragAccumulator = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            dragAccumulator += dragAmount
                            if (currentExpanded) {
                                change.consume()
                                dragOffset = dragAccumulator.coerceIn(
                                    -DRAG_MAX_OFFSET_DP * displayMetrics.density,
                                    DRAG_MAX_OFFSET_DP * displayMetrics.density
                                )
                            }
                        },
                        onDragEnd = {
                            val swipeUpThreshold = -SWIPE_THRESHOLD_DP * displayMetrics.density
                            val swipeDownThreshold = SWIPE_THRESHOLD_DP * displayMetrics.density
                            if (currentExpanded) {
                                if (dragOffset < swipeUpThreshold) {
                                    currentOnDismiss()
                                } else if (dragOffset > swipeDownThreshold) {
                                    currentOnOpenFloatingWindow()
                                }
                            }
                            scope.launch {
                                androidx.compose.animation.core.Animatable(dragOffset).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    dragOffset = value
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                androidx.compose.animation.core.Animatable(dragOffset).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    dragOffset = value
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Collapsed content layer (pinned to fixed pill bounds at top-center, cancelling yOffset)
            if (collapsedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .width(settings.width.dp)
                        .height(settings.height.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = collapsedAlpha
                        }
                ) {
                    IslandCollapsedContent(
                        mode = activeMode,
                        notification = activeNotification,
                        collapsedAlpha = collapsedAlpha,
                        settings = settings
                    )
                }
            }

            // Expanded content layer — only compose when truly expanded (not during collapse)
            // Fix #3: This prevents expanded IconButtons/clickables from stealing taps
            // during the collapse animation
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = expandedAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                            translationY = contentSlideY.toPx()
                        }
                ) {
                    IslandExpandedContent(
                        notifications = notifications,
                        launcherApps = launcherApps,
                        selectedIndex = selectedIndex,
                        onPageSelected = onPageSelected,
                        onOpenNotification = onOpenNotification,
                        onLaunchApp = onLaunchApp,
                        onCollapse = onToggleExpanded,
                        statusBarHeight = statusBarHeight.dp,
                        // Each mode owns its natural height. The launcher already
                        // supplies its own loading height and must not impose that
                        // minimum on compact call or battery content.
                        onHeightMeasured = { expandedHeight = it },
                        settings = settings
                    )
                }
            }
        }

        // Collapsed: secondary circle. Expanded with 2: the same item morphs
        // into a full-size pill. Expanded with 3+: it stays the circle on the right.
        if (secondaryAlpha > 0f && secondaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (secondaryX - screenCenter + secondaryBubbleWidth / 2f).roundToPx(),
                            0
                        )
                    }
                    .width(secondaryBubbleWidth)
                    .height(circleSize)
                    .graphicsLayer {
                        alpha = secondaryAlpha
                        scaleX = secondaryScale * switchScaleAnim.value
                        scaleY = secondaryScale * switchScaleAnim.value
                    }
                    .clip(RoundedCornerShape(secondaryBubbleCorner))
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (secondaryIndex >= 0) {
                            onPageSelected(secondaryIndex)
                        }
                        if (!currentExpanded) {
                            currentOnToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - secondaryPillProgress },
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryBubbleContent(
                        notification = secondaryNotification,
                        settings = settings
                    )
                }
                Box(
                    modifier = Modifier
                        .requiredWidth(miniPillWidth)
                        .height(circleSize)
                        .graphicsLayer { alpha = secondaryPillProgress },
                    contentAlignment = Alignment.Center
                ) {
                    IslandCollapsedContent(
                        mode = secondaryNotification.mode,
                        notification = secondaryNotification,
                        collapsedAlpha = 1f,
                        settings = settings
                    )
                }
            }
        }

        if (tertiaryAlpha > 0f && tertiaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (expandedCompactX - screenCenter + miniPillWidth / 2f).roundToPx(),
                            0
                        )
                    }
                    .width(miniPillWidth)
                    .height(circleSize)
                    .graphicsLayer {
                        alpha = tertiaryAlpha
                        scaleX = tertiaryScale * switchScaleAnim.value
                        scaleY = tertiaryScale * switchScaleAnim.value
                    }
                    .clip(RoundedCornerShape(settings.cornerRadius.dp))
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (tertiaryIndex >= 0) {
                            onPageSelected(tertiaryIndex)
                        }
                        if (!currentExpanded) {
                            currentOnToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                IslandCollapsedContent(
                    mode = tertiaryNotification.mode,
                    notification = tertiaryNotification,
                    collapsedAlpha = 1f,
                    settings = settings
                )
            }
        }
    }
}

@Composable
private fun SecondaryBubbleContent(
    notification: IslandNotification,
    settings: SmartIslandSettings
) {
    when (notification.mode) {
        IslandMode.Music -> {
            val artwork = notification.largeIcon ?: notification.icon
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.musicVisualizerColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        IslandMode.IncomingCall -> {
            Icon(
                Icons.Rounded.Call,
                contentDescription = null,
                tint = Color(settings.callColor),
                modifier = Modifier.size(14.dp)
            )
        }
        IslandMode.Battery -> {
            Icon(
                Icons.Rounded.Bolt,
                contentDescription = null,
                tint = Color(settings.batteryColor),
                modifier = Modifier.size(14.dp)
            )
        }
        else -> {
            val artwork = notification.largeIcon ?: notification.icon
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.notificationDotColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification.appName.firstOrNull()?.uppercase() ?: "S",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Animation specs
private const val EXPANDED_WIDTH_RATIO = 0.95f
private const val SWIPE_THRESHOLD_DP = 35f
private const val DRAG_MAX_OFFSET_DP = 100f
private const val COMPACT_INDICATOR_GAP_DP = 8f

internal enum class CompactNotificationShape { MiniPill, Circle }

internal fun compactNotificationShapes(
    notificationCount: Int,
    expanded: Boolean
): List<CompactNotificationShape> = when {
    notificationCount < 2 -> emptyList()
    !expanded -> listOf(CompactNotificationShape.Circle)
    notificationCount == 2 -> listOf(CompactNotificationShape.MiniPill)
    else -> listOf(CompactNotificationShape.MiniPill, CompactNotificationShape.Circle)
}
