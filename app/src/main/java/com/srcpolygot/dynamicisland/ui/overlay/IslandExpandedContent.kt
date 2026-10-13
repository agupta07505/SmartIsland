/*
 * Realistic Dynamic Island Fork
 * Expanded state – shows rich controls for the primary activity
 */

package com.srcpolygot.dynamicisland.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srcpolygot.dynamicisland.domain.model.ActiveIslandState
import com.srcpolygot.dynamicisland.domain.model.IslandMode

/**
 * Expanded Dynamic Island card.
 * Currently a clean stub that will grow into full mode-specific cards
 * (Music, Call, Timer, Navigation, Live Activity, etc.).
 */
@Composable
fun IslandExpandedContent(
    state: ActiveIslandState,
    modifier: Modifier = Modifier
) {
    val primary = state.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF1C1C1E))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = primary?.title ?: "Dynamic Island",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = primary?.text ?: modeDescription(primary?.mode),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tap outside or swipe up to collapse",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp
            )
        }
    }
}

private fun modeDescription(mode: IslandMode?): String {
    return when (mode) {
        IslandMode.Music -> "Now Playing"
        IslandMode.IncomingCall, IslandMode.OngoingCall -> "Phone Call"
        IslandMode.Timer -> "Timer running"
        IslandMode.Stopwatch -> "Stopwatch"
        IslandMode.Navigation -> "Turn-by-turn navigation"
        IslandMode.Charging, IslandMode.Battery -> "Battery status"
        IslandMode.LiveActivity -> "Live Activity"
        IslandMode.CameraInUse -> "Camera in use"
        IslandMode.MicrophoneInUse -> "Microphone in use"
        IslandMode.Flashlight -> "Flashlight on"
        IslandMode.ScreenRecording -> "Screen recording"
        IslandMode.VoiceMemo -> "Voice memo recording"
        IslandMode.Hotspot -> "Personal Hotspot"
        IslandMode.Bluetooth -> "Bluetooth connected"
        else -> "Active"
    }
}
