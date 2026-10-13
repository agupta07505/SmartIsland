package com.srcpolygot.dynamicisland.ui.overlay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.srcpolygot.dynamicisland.domain.model.ActiveIslandState
import com.srcpolygot.dynamicisland.domain.model.IslandMode
import com.srcpolygot.dynamicisland.ui.expanded.BatteryExpanded
import com.srcpolygot.dynamicisland.ui.expanded.CallExpanded
import com.srcpolygot.dynamicisland.ui.expanded.FlashlightExpanded
import com.srcpolygot.dynamicisland.ui.expanded.HotspotExpanded
import com.srcpolygot.dynamicisland.ui.expanded.LiveActivityExpanded
import com.srcpolygot.dynamicisland.ui.expanded.MusicExpanded
import com.srcpolygot.dynamicisland.ui.expanded.NavigationExpanded
import com.srcpolygot.dynamicisland.ui.expanded.PrivacyExpanded
import com.srcpolygot.dynamicisland.ui.expanded.ScreenRecordingExpanded
import com.srcpolygot.dynamicisland.ui.expanded.TimerExpanded

@Composable
fun IslandExpandedContent(
    state: ActiveIslandState,
    modifier: Modifier = Modifier
) {
    val primary = state.primary ?: return

    when (primary.mode) {
        IslandMode.Music -> MusicExpanded(notification = primary, modifier = modifier.fillMaxWidth())
        IslandMode.IncomingCall -> CallExpanded(notification = primary, isIncoming = true, modifier = modifier.fillMaxWidth())
        IslandMode.OngoingCall -> CallExpanded(notification = primary, isIncoming = false, modifier = modifier.fillMaxWidth())
        IslandMode.Timer -> TimerExpanded(notification = primary, isStopwatch = false, modifier = modifier.fillMaxWidth())
        IslandMode.Stopwatch -> TimerExpanded(notification = primary, isStopwatch = true, modifier = modifier.fillMaxWidth())
        IslandMode.Battery, IslandMode.Charging -> BatteryExpanded(notification = primary, isCharging = primary.mode == IslandMode.Charging, modifier = modifier.fillMaxWidth())
        IslandMode.Navigation -> NavigationExpanded(notification = primary, modifier = modifier.fillMaxWidth())
        IslandMode.LiveActivity -> LiveActivityExpanded(notification = primary, progress = primary.progress, modifier = modifier.fillMaxWidth())
        IslandMode.CameraInUse, IslandMode.MicrophoneInUse -> PrivacyExpanded(notification = primary, modifier = modifier.fillMaxWidth())
        IslandMode.Flashlight -> FlashlightExpanded(isOn = true, modifier = modifier.fillMaxWidth())
        IslandMode.ScreenRecording -> ScreenRecordingExpanded(modifier = modifier.fillMaxWidth())
        IslandMode.Hotspot -> HotspotExpanded(notification = primary, modifier = modifier.fillMaxWidth())
        else -> GenericExpandedFallback(primary = primary, modifier = modifier)
    }
}

@Composable
private fun GenericExpandedFallback(
    primary: com.srcpolygot.dynamicisland.domain.model.IslandNotification,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.material3.Text(
            text = primary.title ?: primary.mode.name,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}
