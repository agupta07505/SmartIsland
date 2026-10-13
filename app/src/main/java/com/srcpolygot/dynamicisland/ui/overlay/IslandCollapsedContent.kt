package com.srcpolygot.dynamicisland.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srcpolygot.dynamicisland.domain.model.ActiveIslandState
import com.srcpolygot.dynamicisland.domain.model.IslandMode

@Composable
fun IslandCollapsedContent(
    state: ActiveIslandState,
    widthDp: Float,
    heightDp: Float,
    cornerRadiusDp: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            state.primary?.let { ActivityDot(mode = it.mode) }
            state.secondary?.let {
                Spacer(modifier = Modifier.width(6.dp))
                ActivityDot(mode = it.mode, size = 10.dp)
            }
            state.tertiary?.let {
                Spacer(modifier = Modifier.width(6.dp))
                ActivityDot(mode = it.mode, size = 10.dp)
            }
        }
    }
}

@Composable
private fun ActivityDot(mode: IslandMode, size: Dp = 12.dp) {
    val color = when (mode) {
        IslandMode.Music -> Color(0xFF1DB954)
        IslandMode.IncomingCall, IslandMode.OngoingCall -> Color(0xFF34C759)
        IslandMode.Timer, IslandMode.Stopwatch -> Color(0xFFFF9500)
        IslandMode.Navigation -> Color(0xFF007AFF)
        IslandMode.Charging, IslandMode.Battery -> Color(0xFF30D158)
        IslandMode.CameraInUse, IslandMode.MicrophoneInUse -> Color(0xFFFF3B30)
        IslandMode.Flashlight -> Color(0xFFFFCC00)
        IslandMode.ScreenRecording -> Color(0xFFFF2D55)
        else -> Color.White.copy(alpha = 0.85f)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
