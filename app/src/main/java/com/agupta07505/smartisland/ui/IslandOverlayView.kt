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
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.Dp
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
    val screenCenterPx = displayMetrics.widthPixels / 2f
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
        if (it) statusBarHeight.dp else 0.dp
    }
    val radius by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandRadius") {
        if (it) 34.dp else settings.cornerRadius.dp
    }
    val animatedXOffset by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandXOffset") {
        if (it) 0.dp else settings.xOffset.dp
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

    val activeNotification = notifications.getOrNull(selectedIndex)
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

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
        contentAlignment = Alignment.TopStart
    ) {
        // Debug touch bounds visualization (only visible in debug builds)
        if (com.agupta07505.smartisland.BuildConfig.DEBUG) {
            if (currentExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Red.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "DEBUG: FRAME INTERCEPT ACTIVE",
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        modifier = Modifier.padding(top = statusBarHeight.dp + 4.dp, start = 8.dp)
                    )
                }
            } else {
                val debugWidth = settings.width.dp + 12.dp
                val debugHeight = settings.height.dp + 16.dp
                
                Box(
                    modifier = Modifier
                        .width(debugWidth)
                        .height(debugHeight)
                        .graphicsLayer {
                            val currentWidth = size.width
                            val desiredCenterX = screenCenterPx + animatedXOffset.toPx()
                            translationX = desiredCenterX - (currentWidth / 2f)
                            translationY = 0f
                        }
                        .border(1.dp, Color.Green.copy(alpha = 0.5f), RoundedCornerShape(radius + 6.dp))
                ) {
                    Text(
                        text = "TOUCH BOUNDS",
                        color = Color.Green.copy(alpha = 0.7f),
                        fontSize = 6.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                    )
                }
            }
        }
        // Stack Indicator Brackets: concentric parentheses curves drawn behind the pill when notifications > 1
        if (notifications.size > 1 && collapsedAlpha > 0f) {
            Canvas(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .graphicsLayer {
                        val currentWidth = size.width
                        val desiredCenterX = screenCenterPx + animatedXOffset.toPx()
                        translationX = desiredCenterX - (currentWidth / 2f)
                        translationY = yOffset.toPx() + dragOffset
                        alpha = collapsedAlpha
                    }
            ) {
                val h = size.height
                val w = size.width
                val r = radius.toPx().coerceAtMost(h / 2f)
                val gap = STACK_INDICATOR_GAP_DP.dp.toPx()
                val strokeW = STACK_INDICATOR_STROKE_DP.dp.toPx()
                val rArc = r + gap

                // Left Arc: concentric bracket curve on the left
                drawArc(
                    color = Color.Black,
                    startAngle = STACK_LEFT_ARC_START,
                    sweepAngle = STACK_INDICATOR_ARC_SWEEP,
                    useCenter = false,
                    topLeft = Offset(-gap - strokeW / 2f, h / 2f - rArc - strokeW / 2f),
                    size = Size(rArc * 2f + strokeW, rArc * 2f + strokeW),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Right Arc: concentric bracket curve on the right
                drawArc(
                    color = Color.Black,
                    startAngle = STACK_RIGHT_ARC_START,
                    sweepAngle = STACK_INDICATOR_ARC_SWEEP,
                    useCenter = false,
                    topLeft = Offset(w - r * 2f - gap - strokeW / 2f, h / 2f - rArc - strokeW / 2f),
                    size = Size(rArc * 2f + strokeW, rArc * 2f + strokeW),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }

        // Inner Box: The actual visible pill container, managing the black background shape and size animations
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .graphicsLayer {
                    val currentWidth = size.width
                    val desiredCenterX = screenCenterPx + animatedXOffset.toPx()
                    translationX = desiredCenterX - (currentWidth / 2f)
                    translationY = yOffset.toPx() + dragOffset
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
            // Collapsed content layer (purely visual, no gesture handlers)
            if (collapsedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = collapsedAlpha
                            scaleX = collapsedAlpha * COLLAPSED_SCALE_RANGE + COLLAPSED_SCALE_MIN
                            scaleY = collapsedAlpha * COLLAPSED_SCALE_RANGE + COLLAPSED_SCALE_MIN
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
    }
}

// Animation specs
private const val EXPANDED_WIDTH_RATIO = 0.95f
private const val SWIPE_THRESHOLD_DP = 35f
private const val COLLAPSE_ANIMATION_DELAY_MS = 500L
private const val DRAG_MAX_OFFSET_DP = 100f
private const val COLLAPSED_SCALE_MIN = 0.9f
private const val COLLAPSED_SCALE_RANGE = 0.1f

// Visual specs
private const val STACK_INDICATOR_GAP_DP = 3.5f
private const val STACK_INDICATOR_STROKE_DP = 1.5f
private const val STACK_INDICATOR_ARC_SWEEP = 70f
private const val STACK_LEFT_ARC_START = 145f
private const val STACK_RIGHT_ARC_START = 325f
