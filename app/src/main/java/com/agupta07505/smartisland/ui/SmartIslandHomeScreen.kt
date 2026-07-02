package com.agupta07505.smartisland.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import kotlinx.coroutines.launch

@Composable
fun SmartIslandHomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember(context) { SmartIslandSettingsRepository(context.applicationContext) }
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = SmartIslandSettings.Default)
    val scope = rememberCoroutineScope()
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notificationGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = Settings.canDrawOverlays(context)
                notificationGranted = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings.enabled, overlayGranted) {
        if (settings.enabled && overlayGranted) {
            ContextCompat.startForegroundService(context, Intent(context, SmartIslandOverlayService::class.java))
        } else {
            context.stopService(Intent(context, SmartIslandOverlayService::class.java))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Smart Island", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF101828))
        Text("A floating, animated island for notifications and quick glance states.", color = Color(0xFF667085))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Smart Island", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (overlayGranted) "Overlay service is ready" else "Grant overlay permission to start",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085)
                    )
                }
                Switch(
                    checked = settings.enabled,
                    enabled = overlayGranted,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setEnabled(enabled) }
                    }
                )
            }
        }

        PermissionCard(
            title = "Overlay permission",
            description = "Required to draw the pill above other apps.",
            granted = overlayGranted,
            buttonText = "Allow",
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
                overlayGranted = Settings.canDrawOverlays(context)
            }
        )

        PermissionCard(
            title = "Notification listener",
            description = "Lets Smart Island show incoming notification content.",
            granted = notificationGranted,
            buttonText = "Enable",
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                notificationGranted = isNotificationListenerEnabled(context)
            }
        )

        SettingsCard(settings = settings, repository = repository)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ElevatedButton(
                onClick = { SmartIslandOverlayService.showDemo(IslandMode.Notification) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = null)
                Text("Notify")
            }
            ElevatedButton(
                onClick = { SmartIslandOverlayService.showDemo(IslandMode.IncomingCall) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null)
                Text("Call")
            }
            ElevatedButton(
                onClick = { SmartIslandOverlayService.showDemo(IslandMode.Music) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null)
                Text("Music")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Island controls", style = MaterialTheme.typography.titleMedium)
            SliderSettingItem("Island width", settings.width, 76f..180f, { scope.launch { repository.setWidth(it) } })
            SliderSettingItem("Island height", settings.height, 24f..60f, { scope.launch { repository.setHeight(it) } })
            SliderSettingItem("X offset", settings.xOffset, -140f..140f, { scope.launch { repository.setXOffset(it) } })
            SliderSettingItem("Y offset", settings.yOffset, 0f..80f, { scope.launch { repository.setYOffset(it) } })
            SliderSettingItem("Corner radius", settings.cornerRadius, 8f..40f, { scope.launch { repository.setCornerRadius(it) } })
            OutlinedButton(
                onClick = { scope.launch { repository.resetPosition() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Text("Reset position")
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return enabled?.split(":")?.any {
        ComponentName.unflattenFromString(it)?.packageName == context.packageName
    } == true
}
