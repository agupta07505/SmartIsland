package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

@Composable
fun IslandOverlayView(
    settings: SmartIslandSettings,
    expanded: Boolean,
    mode: IslandMode,
    notification: IslandNotification?,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val expandedWidth = (configuration.screenWidthDp * 0.90f).dp
    val transition = updateTransition(targetState = expanded, label = "islandTransition")
    val animationSpec = tween<androidx.compose.ui.unit.Dp>(
        durationMillis = 320,
        easing = FastOutSlowInEasing
    )
    val width by transition.animateDp(transitionSpec = { animationSpec }, label = "islandWidth") {
        if (it) expandedWidth else settings.width.dp
    }
    val height by transition.animateDp(transitionSpec = { animationSpec }, label = "islandHeight") {
        if (it) 128.dp else settings.height.dp
    }
    val radius by transition.animateDp(transitionSpec = { animationSpec }, label = "islandRadius") {
        if (it) 34.dp else settings.cornerRadius.dp
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFF020B12))
            .clickable(onClick = onToggleExpanded)
    ) {
        if (expanded) {
            IslandExpandedContent(mode = mode, notification = notification)
        } else {
            IslandCollapsedContent(mode = mode, notification = notification)
        }
    }
}
