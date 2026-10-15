/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.ui.expanded.IslandExpandedContent
import com.agupta07505.smartisland.ui.expanded.trySendFirstAction
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.ui.res.painterResource
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
    onOpenNotificationShade: () -> Unit = {},
    statusBarHeight: Float,
    modifier: Modifier = Modifier,
    isInputActive: Boolean = false,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onDismissAllNotifications: () -> Unit = {},
    isFullWidth: Boolean = true
) {
    // Fix #1: rememberUpdatedState ensures the lambda is always fresh
    // even though pointerInput(Unit) never restarts its coroutine
    val currentOnToggle by rememberUpdatedState(onToggleExpanded)
    val currentOnDismiss by rememberUpdatedState(onDismissNotification)
    val currentOnDismissAll by rememberUpdatedState(onDismissAllNotifications)
    val currentOnOpenFloatingWindow by rememberUpdatedState(onOpenFloatingWindow)
    val currentOnOpenNotificationShade by rememberUpdatedState(onOpenNotificationShade)
    val currentOnOpenNotification by rememberUpdatedState(onOpenNotification)
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)
    val currentExpanded by rememberUpdatedState(expanded)
    val currentSettings by rememberUpdatedState(settings)
    val safeIndex = selectedIndex.coerceIn(0, (notifications.size - 1).coerceAtLeast(0))
    val currentNotifications by rememberUpdatedState(notifications)
    val currentSelectedIndex by rememberUpdatedState(safeIndex)
    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }
    var pillDragOffsetX by remember { mutableStateOf(0f) }
    var springBackJob by remember { mutableStateOf<Job?>(null) }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val displayMetrics = context.resources.displayMetrics
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenCenter = screenWidth / 2f
    val expandedWidth = calculateExpandedWidth(
        isLandscape = isLandscape,
        screenWidthDp = configuration.screenWidthDp.toFloat(),
        screenHeightDp = configuration.screenHeightDp.toFloat()
    ).dp
    val transition = updateTransition(targetState = expanded, label = "islandTransition")

    val sizeSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.72f,
        stiffness = 520f
    )
    val sizeSpecFloat = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 520f
    )
    val heightSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.76f,
        stiffness = 520f
    )
    val alphaSpec = tween<Float>(
        durationMillis = 190,
        easing = FastOutSlowInEasing
    )

    val activeNotification = notifications.getOrNull(safeIndex)
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

    val initialEstimatedHeight = remember(activeMode, notifications.isEmpty()) {
        if (notifications.isEmpty()) 135.dp else defaultEstimatedHeightForMode(activeMode)
    }
    var expandedHeight by remember { mutableStateOf(initialEstimatedHeight) }

    LaunchedEffect(activeMode, notifications.isEmpty()) {
        if (!expanded) {
            expandedHeight = if (notifications.isEmpty()) 135.dp else defaultEstimatedHeightForMode(activeMode)
        }
    }

    val compactGap = COMPACT_INDICATOR_GAP_DP.dp
    val miniPillWidth = settings.width.dp
    val circleSize = settings.height.dp
    val compactShapes = compactNotificationShapes(notifications.size, expanded)
    val hasCompanion = if (settings.enableNotchMode) false else notifications.size >= 2
    val isCircleLeft = settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
    val collapsedGroupWidth = settings.width.dp + if (hasCompanion) compactGap + circleSize else 0.dp

    val desiredMainLeft = screenCenter + settings.xOffset.dp - settings.width.dp / 2f
    val (minMainLeft, maxMainLeft) = when {
        !hasCompanion -> compactGap to (screenWidth - compactGap - settings.width.dp).coerceAtLeast(compactGap)
        isCircleLeft -> (compactGap + circleSize + compactGap) to (screenWidth - compactGap - settings.width.dp).coerceAtLeast(compactGap + circleSize + compactGap)
        else -> compactGap to (screenWidth - compactGap - collapsedGroupWidth).coerceAtLeast(compactGap)
    }
    val collapsedMainLeft = desiredMainLeft.coerceIn(minMainLeft, maxMainLeft)
    val mainCenter = collapsedMainLeft + settings.width.dp / 2f
    val circleLeft = if (isCircleLeft) {
        collapsedMainLeft - compactGap - circleSize
    } else {
        collapsedMainLeft + settings.width.dp + compactGap
    }
    val circleCenter = circleLeft + circleSize / 2f
    val groupStart = if (isCircleLeft && hasCompanion) circleLeft else collapsedMainLeft
    val groupEnd = if (!isCircleLeft && hasCompanion) circleLeft + circleSize else collapsedMainLeft + settings.width.dp
    val groupCenter = (groupStart + groupEnd) / 2f

    val collapsedMainOffset = if (settings.enableNotchMode) {
        settings.xOffset.dp
    } else if (isFullWidth) {
        mainCenter - screenCenter
    } else {
        mainCenter - groupCenter
    }
    val expandedTopOffset = calculateExpandedTopOffset(
        enableNotchMode = settings.enableNotchMode,
        hasCompanion = hasCompanion,
        statusBarHeightDp = statusBarHeight,
        notchHeightDp = settings.height,
        circleSizeDp = settings.height,
        compactGapDp = COMPACT_INDICATOR_GAP_DP
    ).dp
    val isIdleHiding = settings.hideWhenIdle && notifications.isEmpty()

    var isAutoHidden by remember { mutableStateOf(false) }
    var userInteractionTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // Reset auto-hide whenever active notifications change or selection changes
    LaunchedEffect(notifications.map { it.key }, selectedIndex) {
        isAutoHidden = false
        userInteractionTimestamp = System.currentTimeMillis()
    }

    // Auto-hide countdown timer when pill is collapsed and autoHidePill is enabled
    LaunchedEffect(expanded, settings.autoHidePill, settings.autoHideTimeoutSeconds, userInteractionTimestamp) {
        if (expanded || !settings.autoHidePill) {
            isAutoHidden = false
            return@LaunchedEffect
        }
        val timeoutMs = (settings.autoHideTimeoutSeconds.coerceAtLeast(1) * 1000L)
        kotlinx.coroutines.delay(timeoutMs)
        isAutoHidden = true
    }

    val isHiding = isIdleHiding || (settings.autoHidePill && isAutoHidden)
    val pillBackgroundColor = Color(settings.pillColor)

    val width by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandWidth") {
        if (it) expandedWidth else if (isHiding) 0.dp else settings.width.dp
    }
    val height by transition.animateDp(transitionSpec = { heightSpec }, label = "islandHeight") {
        if (it) expandedHeight else if (isHiding) 0.dp else settings.height.dp
    }
    val yOffset by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandYOffset") {
        if (it) expandedTopOffset else 0.dp
    }
    val radius by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandRadius") {
        if (it) 34.dp else if (isHiding) 0.dp else settings.cornerRadius.dp
    }
    val animatedXOffset by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandXOffset") {
        if (it) 0.dp else collapsedMainOffset
    }

    val collapsedAlpha by transition.animateFloat(
        transitionSpec = { alphaSpec },
        label = "collapsedAlpha"
    ) {
        if (it || isHiding) 0f else 1f
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
        if (it) 1f else 0.95f
    }

    val contentSlideY by transition.animateDp(
        transitionSpec = { sizeSpec },
        label = "contentSlideY"
    ) {
        if (it) 0.dp else (-6).dp
    }

    val safeWidth = width.coerceAtLeast(0.dp)
    val safeHeight = height.coerceAtLeast(0.dp)
    val safeRadius = radius.coerceAtLeast(0.dp)

    // Tactile spring scale bounce animation only when user switches between active notifications
    val switchScaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    var isInitialComposition by remember { mutableStateOf(true) }
    var lastSelectedIndex by remember { mutableStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (isInitialComposition) {
            isInitialComposition = false
            lastSelectedIndex = selectedIndex
            return@LaunchedEffect
        }
        if (lastSelectedIndex != selectedIndex) {
            lastSelectedIndex = selectedIndex
            switchScaleAnim.animateTo(
                targetValue = 0.92f,
                animationSpec = tween(40, easing = FastOutSlowInEasing)
            )
            switchScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 650f
                )
            )
        }
    }

    // Dual Pill (Multi-Tasking Split Island) Detection:
    // When 2 or more notifications exist (e.g. Music + Notification/Timer/Call), split into Main Pill + Secondary Bubble
    val secondaryNotification = if (!settings.enableNotchMode && notifications.size >= 2) {
        notifications.firstOrNull { it.key != activeNotification?.key }
    } else null
    val secondaryIndex = if (secondaryNotification != null) {
        notifications.indexOfFirst { it.key == secondaryNotification.key }
    } else -1
    val tertiaryNotification = if (!settings.enableNotchMode && notifications.size >= 3) {
        notifications.firstOrNull {
            it.key != activeNotification?.key && it.key != secondaryNotification?.key
        }
    } else null
    val tertiaryIndex = if (tertiaryNotification != null) {
        notifications.indexOfFirst { it.key == tertiaryNotification.key }
    } else -1
    val isSplitMode = if (settings.enableNotchMode) false else secondaryNotification != null
    val secondaryIsPill = compactShapes.singleOrNull() == CompactNotificationShape.MiniPill
    val showTertiaryPill = compactShapes.size == 2 && tertiaryNotification != null

    val secondaryAlpha by animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "secondaryAlpha"
    )
    val secondaryScale by animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
        label = "secondaryScale"
    )
    val secondaryBubbleWidth by animateDpAsState(
        targetValue = if (secondaryIsPill) miniPillWidth else circleSize,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 520f),
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
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 520f),
        label = "secondaryBubbleCorner"
    )
    val tertiaryAlpha by animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tertiaryAlpha"
    )
    val tertiaryScale by animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
        label = "tertiaryScale"
    )

    val expandedCompactX = collapsedMainLeft
    val collapsedSecondaryOffset = if (isFullWidth) {
        circleCenter - screenCenter
    } else {
        circleCenter - groupCenter
    }
    val secondaryExpandedOffset = calculateSecondaryExpandedOffset(
        secondaryIsPill = secondaryIsPill,
        isCircleLeft = isCircleLeft,
        isFullWidth = isFullWidth,
        expandedCompactX = expandedCompactX.value,
        screenCenter = screenCenter.value,
        miniPillWidth = miniPillWidth.value,
        circleSize = circleSize.value,
        compactGap = compactGap.value
    ).dp
    val secondaryOffset by animateDpAsState(
        targetValue = if (!expanded) collapsedSecondaryOffset else secondaryExpandedOffset,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 520f),
        label = "secondaryOffset"
    )

    // Outer Box: Fills the entire WindowManager window bounds (which are padded for easy touch)
    val outerModifier = if (currentExpanded) {
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    if (isInputActive) {
                        onReplyStateChanged(false)
                    }
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

        // Invisible touch target over the pill location when hiding, so tapping the area reveals the pill or opens shortcuts
        if (isHiding && !currentExpanded) {
            Box(
                modifier = Modifier
                    .width(settings.width.dp)
                    .height(settings.height.dp)
                    .graphicsLayer {
                        translationX = collapsedMainOffset.toPx()
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (settings.autoHidePill && isAutoHidden) {
                                // First tap on auto-hidden pill: awaken and reveal the pill
                                isAutoHidden = false
                                userInteractionTimestamp = System.currentTimeMillis()
                            } else if (settings.enableAppShortcuts || currentNotifications.isNotEmpty()) {
                                // Empty notifications idle hiding: expand favorite shortcuts if enabled
                                currentOnToggle()
                            }
                        }
                    }
            )
        }

        val mainShape = if (settings.enableNotchMode && !currentExpanded) {
            RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = safeRadius,
                bottomEnd = safeRadius
            )
        } else {
            RoundedCornerShape(safeRadius)
        }

        // Inner Box: The actual visible pill container, managing the black background shape and size animations
        Box(
            modifier = Modifier
                .width(safeWidth)
                .height(safeHeight)
                .graphicsLayer {
                    translationX = animatedXOffset.toPx() + (if (!currentExpanded) pillDragOffsetX else 0f)
                    translationY = yOffset.toPx() + dragOffset
                    scaleX = switchScaleAnim.value
                    scaleY = switchScaleAnim.value
                }
                .then(
                    if (settings.enableShadow && settings.shadowElevation > 0f && !isHiding) {
                        val activeMainShadow = if (currentExpanded) {
                            (settings.shadowElevation * 1.5f).dp
                        } else {
                            settings.shadowElevation.dp
                        }
                        Modifier.shadow(
                            elevation = activeMainShadow,
                            shape = mainShape,
                            clip = false,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                    } else Modifier
                )
                .clip(mainShape)
                .background(pillBackgroundColor.copy(alpha = settings.opacity))
                .pointerInput(displayMetrics.density, isInputActive) {
                    if (isInputActive) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        springBackJob?.cancel()
                        pillDragOffsetX = 0f
                        dragOffset = 0f
                        userInteractionTimestamp = System.currentTimeMillis()
                        val pressTimeMs = System.currentTimeMillis()
                        val wasExpandedAtStart = currentExpanded
                        var isHoldRegistered = false
                        var dragAccumulatorY = 0f
                        var dragAccumulatorX = 0f
                        var isDragging = false
                        var pillGestureTriggered = false

                        val holdJob = scope.launch {
                            kotlinx.coroutines.delay(HOLD_GESTURE_THRESHOLD_MS)
                            isHoldRegistered = true
                            triggerHapticVibration(context)
                        }

                        val pointerId = down.id

                        try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed && !change.changedToUp()) break

                            if (change.changedToUp()) {
                                change.consume()
                                holdJob.cancel()
                                val totalElapsedMs = System.currentTimeMillis() - pressTimeMs
                                val currentNotification = currentNotifications.getOrNull(currentSelectedIndex)

                                if (wasExpandedAtStart) {
                                    val swipeUpThreshold = -SWIPE_THRESHOLD_DP * displayMetrics.density
                                    val swipeDownThreshold = SWIPE_THRESHOLD_DP * displayMetrics.density
                                    if (isDragging && currentSettings.enableSwipeActions && dragOffset < swipeUpThreshold) {
                                        val isHold = isHoldRegistered || totalElapsedMs >= HOLD_GESTURE_THRESHOLD_MS
                                        val actionStr = if (isHold) currentSettings.swipeHoldUpAction else currentSettings.swipeUpAction
                                        val action = SwipeAction.fromId(actionStr, if (isHold) SwipeAction.DismissAll else SwipeAction.DismissCurrent)
                                        executeSwipeAction(
                                            action = action,
                                            currentNotification = currentNotification,
                                            context = context,
                                            onDismiss = currentOnDismiss,
                                            onDismissAll = currentOnDismissAll,
                                            onToggle = currentOnToggle,
                                            onOpenNotification = currentOnOpenNotification,
                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                            onPageSelected = currentOnPageSelected,
                                            notificationsSize = currentNotifications.size,
                                            currentIndex = currentSelectedIndex
                                        )
                                    } else if (isDragging && currentSettings.enableSwipeActions && dragOffset > swipeDownThreshold) {
                                        val action = SwipeAction.fromId(currentSettings.swipeDownAction, SwipeAction.FloatingWindow)
                                        executeSwipeAction(
                                            action = action,
                                            currentNotification = currentNotification,
                                            context = context,
                                            onDismiss = currentOnDismiss,
                                            onDismissAll = currentOnDismissAll,
                                            onToggle = currentOnToggle,
                                            onOpenNotification = currentOnOpenNotification,
                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                            onPageSelected = currentOnPageSelected,
                                            notificationsSize = currentNotifications.size,
                                            currentIndex = currentSelectedIndex
                                        )
                                    } else if (!isDragging || abs(dragOffset) < 10f * displayMetrics.density) {
                                        if (!isHoldRegistered) {
                                            if (currentNotification != null) {
                                                currentOnOpenNotification(currentNotification)
                                            } else {
                                                SmartIslandRepositories.notificationRepository(context).resetTimer()
                                            }
                                        }
                                    }
                                } else {
                                    // Collapsed state (In-Pill Gestures)
                                    if (!pillGestureTriggered) {
                                        val isPillSwipeEnabled = currentSettings.enablePillSwipeActions
                                        val pillThreshold = PILL_SWIPE_THRESHOLD_DP * displayMetrics.density
                                        val absX = abs(dragAccumulatorX)
                                        val absY = abs(dragAccumulatorY)

                                        if (isPillSwipeEnabled && (absX >= pillThreshold || absY >= pillThreshold)) {
                                            pillGestureTriggered = true
                                            if (absX > absY) {
                                                if (dragAccumulatorX < -pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                } else if (dragAccumulatorX > pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeRightAction, SwipeAction.NextNotification)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                }
                                            } else {
                                                if (dragAccumulatorY < -pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeUpAction, SwipeAction.DismissCurrent)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                } else if (dragAccumulatorY > pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeDownAction, SwipeAction.Expand)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                }
                                            }
                                        } else {
                                            // Tap on collapsed pill: expands or reveals
                                            if (!isHoldRegistered) {
                                                if (settings.autoHidePill && isAutoHidden) {
                                                    isAutoHidden = false
                                                    userInteractionTimestamp = System.currentTimeMillis()
                                                } else if (currentNotifications.isNotEmpty() || currentSettings.enableAppShortcuts) {
                                                    currentOnToggle()
                                                }
                                            }
                                        }
                                    }
                                }
                                break
                            } else {
                                val dragAmountY = change.positionChange().y
                                val dragAmountX = change.positionChange().x
                                if (abs(dragAmountY) > 0.5f || abs(dragAmountX) > 0.5f) {
                                    isDragging = true
                                    dragAccumulatorY += dragAmountY
                                    dragAccumulatorX += dragAmountX
                                    change.consume()
                                    if (abs(dragAccumulatorY) > 5f * displayMetrics.density || abs(dragAccumulatorX) > 5f * displayMetrics.density) {
                                        holdJob.cancel()
                                    }
                                    if (wasExpandedAtStart) {
                                        dragOffset = dragAccumulatorY.coerceIn(
                                            -DRAG_MAX_OFFSET_DP * displayMetrics.density,
                                            DRAG_MAX_OFFSET_DP * displayMetrics.density
                                        )
                                    } else {
                                        pillDragOffsetX = (dragAccumulatorX * 0.35f).coerceIn(
                                            -24f * displayMetrics.density,
                                            24f * displayMetrics.density
                                        )
                                        dragOffset = (dragAccumulatorY * 0.35f).coerceIn(
                                            -12f * displayMetrics.density,
                                            12f * displayMetrics.density
                                        )

                                        // Snappy immediate execution when swipe threshold is crossed while dragging
                                        if (!pillGestureTriggered && currentSettings.enablePillSwipeActions) {
                                            val pillThreshold = PILL_SWIPE_THRESHOLD_DP * displayMetrics.density
                                            val absX = abs(dragAccumulatorX)
                                            val absY = abs(dragAccumulatorY)

                                            if (absX >= pillThreshold || absY >= pillThreshold) {
                                                pillGestureTriggered = true
                                                holdJob.cancel()
                                                val currentNotification = currentNotifications.getOrNull(currentSelectedIndex)

                                                if (absX > absY) {
                                                    if (dragAccumulatorX < -pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    } else if (dragAccumulatorX > pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeRightAction, SwipeAction.NextNotification)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    }
                                                } else {
                                                    if (dragAccumulatorY < -pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeUpAction, SwipeAction.DismissCurrent)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    } else if (dragAccumulatorY > pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeDownAction, SwipeAction.Expand)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        } finally {
                            holdJob.cancel()
                            springBackJob = scope.launch {
                                if (pillDragOffsetX != 0f) {
                                    val startPillOffset = pillDragOffsetX
                                    launch {
                                        androidx.compose.animation.core.Animatable(startPillOffset).animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) {
                                            pillDragOffsetX = value
                                        }
                                        pillDragOffsetX = 0f
                                    }
                                }
                                if (dragOffset != 0f) {
                                    val startDrag = dragOffset
                                    launch {
                                        androidx.compose.animation.core.Animatable(startDrag).animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) {
                                            dragOffset = value
                                        }
                                        dragOffset = 0f
                                    }
                                }
                            }
                        }
                    }
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

            // Expanded content layer — smoothly fade out while collapsing
            if (expanded || expandedAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
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
                        settings = settings,
                        onReplyStateChanged = onReplyStateChanged
                    )
                }
            }
        }

        // Collapsed: secondary circle. Expanded with 2: the same item morphs
        // into a full-size pill. Expanded with 3+: it stays the circle on the right.
        if (!settings.enableNotchMode && secondaryAlpha > 0f && secondaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            secondaryOffset.roundToPx(),
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
                    .then(
                        if (settings.enableShadow && settings.shadowElevation > 0f) {
                            Modifier.shadow(
                                elevation = (settings.shadowElevation * 0.85f).dp,
                                shape = RoundedCornerShape(secondaryBubbleCorner),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(secondaryBubbleCorner))
                    .background(pillBackgroundColor.copy(alpha = settings.opacity))
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

        if (!settings.enableNotchMode && tertiaryAlpha > 0f && tertiaryNotification != null) {
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
                    .then(
                        if (settings.enableShadow && settings.shadowElevation > 0f) {
                            Modifier.shadow(
                                elevation = (settings.shadowElevation * 0.85f).dp,
                                shape = RoundedCornerShape(settings.cornerRadius.dp),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(settings.cornerRadius.dp))
                    .background(pillBackgroundColor.copy(alpha = settings.opacity))
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
        IslandMode.Bluetooth -> {
            BluetoothCollapsedRight(
                notification = notification,
                settings = settings
            )
        }
        IslandMode.Flashlight -> {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        IslandMode.Hotspot -> {
            HotspotCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Battery -> {
            BatteryCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.LiveActivity -> {
            LiveActivityCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Navigation -> {
            NavigationCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.IncomingCall -> {
            val icon = notification.largeIcon ?: notification.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = null,
                    tint = Color(settings.callColor),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
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
        IslandMode.ScreenRecording -> {
            ScreenRecordingCollapsedGlyph(settings = settings)
        }
        IslandMode.Timer -> {
            TimerCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Stopwatch -> {
            StopwatchCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Notification, IslandMode.DownloadUpload, IslandMode.Empty -> {
            NotificationGlyph(notification = notification, settings = settings)
        }
    }
}

// Animation specs
internal const val EXPANDED_WIDTH_RATIO = 0.95f

internal fun calculateExpandedWidth(
    isLandscape: Boolean,
    screenWidthDp: Float,
    screenHeightDp: Float,
    ratio: Float = EXPANDED_WIDTH_RATIO
): Float {
    return if (isLandscape) {
        val portraitWidth = minOf(screenWidthDp, screenHeightDp)
        (portraitWidth * ratio).coerceIn(340f, 440f)
    } else {
        screenWidthDp * ratio
    }
}

internal fun calculateExpandedTopOffset(
    enableNotchMode: Boolean,
    hasCompanion: Boolean,
    statusBarHeightDp: Float,
    notchHeightDp: Float = 35f,
    circleSizeDp: Float = 34f,
    compactGapDp: Float = COMPACT_INDICATOR_GAP_DP
): Float {
    return if (enableNotchMode) {
        maxOf(statusBarHeightDp, notchHeightDp) + 8f
    } else if (hasCompanion) {
        maxOf(statusBarHeightDp, circleSizeDp + compactGapDp)
    } else {
        statusBarHeightDp
    }
}

internal fun calculateSecondaryExpandedOffset(
    secondaryIsPill: Boolean,
    isCircleLeft: Boolean,
    isFullWidth: Boolean,
    expandedCompactX: Float,
    screenCenter: Float,
    miniPillWidth: Float,
    circleSize: Float,
    compactGap: Float
): Float {
    return if (secondaryIsPill) {
        val secCenter = expandedCompactX + miniPillWidth / 2f
        if (isFullWidth) secCenter - screenCenter else 0f
    } else if (isCircleLeft) {
        val secCenter = expandedCompactX - compactGap - circleSize / 2f
        if (isFullWidth) secCenter - screenCenter else -(miniPillWidth + compactGap) / 2f
    } else {
        val secCenter = expandedCompactX + miniPillWidth + compactGap + circleSize / 2f
        if (isFullWidth) secCenter - screenCenter else ((miniPillWidth + compactGap) / 2f)
    }
}
private const val SWIPE_THRESHOLD_DP = 35f
private const val PILL_SWIPE_THRESHOLD_DP = 16f
private const val DRAG_MAX_OFFSET_DP = 100f
private const val COMPACT_INDICATOR_GAP_DP = 8f
private const val HOLD_GESTURE_THRESHOLD_MS = 300L

private fun triggerHapticVibration(context: android.content.Context) {
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            val vibrator = vm?.defaultVibrator
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(60L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                return
            }
        }
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(60L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60L)
            }
        }
    }
}

internal enum class CompactNotificationShape { MiniPill, Circle }

internal fun defaultEstimatedHeightForMode(mode: IslandMode?): Dp {
    return when (mode) {
        IslandMode.Music -> 175.dp
        IslandMode.Notification -> 135.dp
        IslandMode.IncomingCall, IslandMode.Battery -> 115.dp
        IslandMode.LiveActivity, IslandMode.Navigation -> 180.dp
        IslandMode.DownloadUpload, IslandMode.Hotspot -> 160.dp
        IslandMode.Bluetooth, IslandMode.Flashlight, IslandMode.ScreenRecording,
        IslandMode.Timer, IslandMode.Stopwatch -> 115.dp
        IslandMode.Empty, null -> 135.dp
    }
}

internal fun compactNotificationShapes(
    notificationCount: Int,
    expanded: Boolean
): List<CompactNotificationShape> = when {
    notificationCount < 2 -> emptyList()
    !expanded -> listOf(CompactNotificationShape.Circle)
    notificationCount == 2 -> listOf(CompactNotificationShape.MiniPill)
    else -> listOf(CompactNotificationShape.MiniPill, CompactNotificationShape.Circle)
}

private fun trySkipMedia(
    context: android.content.Context?,
    notification: IslandNotification?,
    forward: Boolean
): Boolean {
    if (context == null) return false

    // 1. Direct notification action PendingIntent (e.g. Spotify, YouTube Music, podcasts)
    val actionSent = if (forward) {
        notification.trySendFirstAction(context, "next", "skip", "forward")
    } else {
        notification.trySendFirstAction(context, "previous", "prev", "rewind")
    }
    if (actionSent) return true

    // 2. Notification MediaSession token
    val token = notification?.mediaToken
    if (token != null) {
        val success = runCatching {
            val controller = android.media.session.MediaController(context, token)
            if (forward) controller.transportControls.skipToNext() else controller.transportControls.skipToPrevious()
            true
        }.getOrDefault(false)
        if (success) return true
    }

    // 3. Delegate to NotificationListenerService via SmartIslandCommand
    runCatching {
        val repo = SmartIslandRepositories.notificationRepository(context)
        if (forward) {
            repo.sendCommand(SmartIslandCommand.SkipNext(notification?.packageName))
        } else {
            repo.sendCommand(SmartIslandCommand.SkipPrevious(notification?.packageName))
        }
    }

    // 4. Fallback: AudioManager media key events
    runCatching {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = if (forward) android.view.KeyEvent.KEYCODE_MEDIA_NEXT else android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
    }

    return true
}

private fun tryPlayPauseMedia(
    context: android.content.Context?,
    notification: IslandNotification?
) {
    if (context == null) return

    // 1. Direct notification action PendingIntent
    val actionSent = notification.trySendFirstAction(context, "play", "pause", "resume", "toggle")
    if (actionSent) return

    // 2. Notification MediaSession token
    val token = notification?.mediaToken
    if (token != null) {
        val success = runCatching {
            val controller = android.media.session.MediaController(context, token)
            if (notification.mediaIsPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            true
        }.getOrDefault(false)
        if (success) return
    }

    // 3. Delegate to NotificationListenerService via SmartIslandCommand
    runCatching {
        val repo = SmartIslandRepositories.notificationRepository(context)
        repo.sendCommand(SmartIslandCommand.PlayPause(notification?.packageName))
    }

    // 4. Fallback: AudioManager media key events
    runCatching {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
    }
}

internal fun executeSwipeAction(
    action: SwipeAction,
    currentNotification: IslandNotification?,
    context: android.content.Context? = null,
    onDismiss: () -> Unit,
    onDismissAll: () -> Unit,
    onToggle: () -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit,
    onPageSelected: (Int) -> Unit,
    notificationsSize: Int,
    currentIndex: Int
) {
    if (context != null && action != SwipeAction.None) {
        triggerHapticVibration(context)
    }
    when (action) {
        SwipeAction.DismissCurrent -> onDismiss()
        SwipeAction.DismissAll -> onDismissAll()
        SwipeAction.Collapse -> onToggle()
        SwipeAction.Expand -> onToggle()
        SwipeAction.OpenApp -> {
            if (currentNotification != null) {
                onOpenNotification(currentNotification)
            }
        }
        SwipeAction.FloatingWindow -> onOpenFloatingWindow()
        SwipeAction.NotificationShade -> onOpenNotificationShade()
        SwipeAction.NextPrevious, SwipeAction.NextNotification -> {
            if (notificationsSize > 1) {
                val nextIndex = (currentIndex + 1) % notificationsSize
                onPageSelected(nextIndex)
            } else {
                trySkipMedia(context, currentNotification, forward = true)
            }
        }
        SwipeAction.PreviousNotification -> {
            if (notificationsSize > 1) {
                val prevIndex = (currentIndex - 1 + notificationsSize) % notificationsSize
                onPageSelected(prevIndex)
            } else {
                trySkipMedia(context, currentNotification, forward = false)
            }
        }
        SwipeAction.NextTrack -> {
            trySkipMedia(context, currentNotification, forward = true)
        }
        SwipeAction.PreviousTrack -> {
            trySkipMedia(context, currentNotification, forward = false)
        }
        SwipeAction.PlayPause -> {
            tryPlayPauseMedia(context, currentNotification)
        }
        SwipeAction.None -> {
            // Disabled / No action
        }
    }
}
