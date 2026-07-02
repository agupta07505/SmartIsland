package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

@Composable
fun IslandOverlayView(
    settings: SmartIslandSettings,
    expanded: Boolean,
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onToggleExpanded: () -> Unit,
    statusBarHeight: Float,
    modifier: Modifier = Modifier
) {
    // Fix #1: rememberUpdatedState ensures the lambda is always fresh
    // even though pointerInput(Unit) never restarts its coroutine
    val currentOnToggle by rememberUpdatedState(onToggleExpanded)

    val displayMetrics = LocalContext.current.resources.displayMetrics
    val expandedWidth = ((displayMetrics.widthPixels / displayMetrics.density) * 0.95f).dp
    val transition = updateTransition(targetState = expanded, label = "islandTransition")

    val sizeSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.7f,
        stiffness = 300f
    )
    val alphaSpec = tween<Float>(
        durationMillis = 280,
        easing = FastOutSlowInEasing
    )

    val width by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandWidth") {
        if (it) expandedWidth else settings.width.dp
    }
    val height by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandHeight") {
        if (it) (160.dp + statusBarHeight.dp) else settings.height.dp
    }
    val radius by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandRadius") {
        if (it) 34.dp else settings.cornerRadius.dp
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

    val activeNotification = notifications.getOrNull(selectedIndex)
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

    // Outer Box: Fills the entire WindowManager window bounds (which are padded for easy touch)
    Box(
        modifier = modifier
            .fillMaxSize()
            // Make the entire window bounds touch-sensitive to expand/collapse easily!
            .pointerInput(Unit) {
                detectTapGestures {
                    currentOnToggle()
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Inner Box: The actual visible pill container (no background here so that top status bar region is transparent)
        Box(
            modifier = Modifier
                .size(width = width, height = height)
        ) {
            // Collapsed content layer (purely visual, no gesture handlers)
            if (collapsedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = collapsedAlpha }
                        .clip(RoundedCornerShape(radius))
                        .background(Color.Black)
                ) {
                    IslandCollapsedContent(mode = activeMode, notification = activeNotification)
                }
            }

            // Expanded content layer — only compose when truly expanded (not during collapse)
            // Fix #3: This prevents expanded IconButtons/clickables from stealing taps
            // during the collapse animation
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = expandedAlpha }
                ) {
                    IslandExpandedContent(
                        notifications = notifications,
                        selectedIndex = selectedIndex,
                        onPageSelected = onPageSelected,
                        onOpenNotification = onOpenNotification,
                        onCollapse = onToggleExpanded,
                        statusBarHeight = statusBarHeight.dp
                    )
                }
            }
        }
    }
}
