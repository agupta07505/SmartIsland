/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.SmartIslandApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import com.agupta07505.smartisland.ui.sections.AboutSection
import com.agupta07505.smartisland.ui.sections.HeaderSection
import com.agupta07505.smartisland.ui.sections.PermissionsSection
import com.agupta07505.smartisland.ui.sections.PositionsSection
import com.agupta07505.smartisland.ui.sections.SupportSection
import kotlinx.coroutines.launch

private enum class HomeSection {
    Permissions,
    Positions,
    Support,
    About
}

@Composable
fun SmartIslandHomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val app = context.applicationContext as? SmartIslandApp
    val repository = remember(app, context) {
        app?.settingsRepository ?: com.agupta07505.smartisland.data.SmartIslandSettingsRepository(context.applicationContext)
    }
    val notificationRepository = remember(app) { app?.notificationRepository }
    
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
            // CRASH FIX: startForegroundService can throw
            // ForegroundServiceStartNotAllowedException on Android 12+
            try {
                ContextCompat.startForegroundService(context, Intent(context, SmartIslandOverlayService::class.java))
            } catch (e: Exception) {
                android.util.Log.e("SmartIslandHome", "Failed to start overlay service", e)
                // Disable setting to prevent crash loop
                try {
                    repository.setEnabled(false)
                } catch (_: Exception) {}
                android.widget.Toast.makeText(
                    context,
                    "Failed to start Smart Island: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } else {
            try {
                context.stopService(Intent(context, SmartIslandOverlayService::class.java))
            } catch (_: Exception) {}
        }
    }

    var activeSection by remember { mutableStateOf<HomeSection?>(null) }
    var transitionDirection by remember { mutableStateOf(1) } // 1 = forward, -1 = backward

    BackHandler(enabled = activeSection != null) {
        transitionDirection = -1
        activeSection = null
    }

    AnimatedContent(
        targetState = activeSection,
        transitionSpec = {
            if (transitionDirection == 1) {
                (slideInHorizontally(initialOffsetX = { it }) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
            } else {
                (slideInHorizontally(initialOffsetX = { -it }) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
            }
        },
        label = "SectionTransition"
    ) { targetSection ->
        if (targetSection == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                HeaderSection()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            Text(
                                text = stringResource(R.string.enable_smart_island),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (overlayGranted) stringResource(R.string.overlay_ready) else stringResource(R.string.grant_overlay),
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.quick_test_controls),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF667085),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ElevatedButton(
                                    onClick = { notificationRepository?.showDemo(IslandMode.Notification) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_notify), fontSize = 11.sp)
                                }
                                ElevatedButton(
                                    onClick = { notificationRepository?.showDemo(IslandMode.IncomingCall) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_call), fontSize = 11.sp)
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ElevatedButton(
                                    onClick = { notificationRepository?.showDemo(IslandMode.Music) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_music), fontSize = 11.sp)
                                }
                                ElevatedButton(
                                    onClick = { notificationRepository?.showDemo(IslandMode.Battery) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_battery), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.configure_features),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF667085),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )

                SectionRow(
                    title = stringResource(R.string.sec_permissions),
                    description = stringResource(R.string.sec_permissions_desc),
                    icon = Icons.Rounded.Lock,
                    iconBgColor = Color(0xFFE0E7FF),
                    iconTint = Color(0xFF4F46E5),
                    statusText = if (overlayGranted && notificationGranted) stringResource(R.string.status_active) else stringResource(R.string.status_action_required),
                    statusColor = if (overlayGranted && notificationGranted) Color(0xFF0F9F6E) else Color(0xFFE88C25),
                    onClick = {
                        transitionDirection = 1
                        activeSection = HomeSection.Permissions
                    }
                )

                SectionRow(
                    title = stringResource(R.string.sec_positions),
                    description = stringResource(R.string.sec_positions_desc),
                    icon = Icons.Rounded.Refresh,
                    iconBgColor = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    onClick = {
                        transitionDirection = 1
                        activeSection = HomeSection.Positions
                    }
                )

                SectionRow(
                    title = stringResource(R.string.sec_support),
                    description = stringResource(R.string.sec_support_desc),
                    icon = Icons.Rounded.Feedback,
                    iconBgColor = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    onClick = {
                        transitionDirection = 1
                        activeSection = HomeSection.Support
                    }
                )

                SectionRow(
                    title = stringResource(R.string.sec_about),
                    description = stringResource(R.string.sec_about_desc),
                    icon = Icons.Rounded.Info,
                    iconBgColor = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF7C3AED),
                    onClick = {
                        transitionDirection = 1
                        activeSection = HomeSection.About
                    }
                )

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.made_by),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF98A2B3)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        } else {
            when (targetSection) {
                HomeSection.Permissions -> {
                    SectionDetailScreen(
                        title = stringResource(R.string.sec_permissions),
                        onBack = {
                            transitionDirection = -1
                            activeSection = null
                        }
                    ) {
                        PermissionsSection(
                            overlayGranted = overlayGranted,
                            notificationGranted = notificationGranted,
                            onOverlayClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                                overlayGranted = Settings.canDrawOverlays(context)
                            },
                            onNotificationClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                notificationGranted = isNotificationListenerEnabled(context)
                            }
                        )
                    }
                }
                HomeSection.Positions -> {
                    SectionDetailScreen(
                        title = stringResource(R.string.sec_positions),
                        onBack = {
                            transitionDirection = -1
                            activeSection = null
                        }
                    ) {
                        PositionsSection(settings = settings, repository = repository)
                    }
                }
                HomeSection.Support -> {
                    SectionDetailScreen(
                        title = stringResource(R.string.sec_support),
                        onBack = {
                            transitionDirection = -1
                            activeSection = null
                        }
                    ) {
                        SupportSection()
                    }
                }
                HomeSection.About -> {
                    SectionDetailScreen(
                        title = stringResource(R.string.sec_about),
                        onBack = {
                            transitionDirection = -1
                            activeSection = null
                        }
                    ) {
                        AboutSection()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    statusText: String? = null,
    statusColor: Color = Color(0xFF667085),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconBgColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF667085)
                )
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF98A2B3),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SectionDetailScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
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

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun SmartIslandHomeScreenLightPreview() {
    SmartIslandTheme(darkTheme = false) {
        SmartIslandHomeScreen()
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun SmartIslandHomeScreenDarkPreview() {
    SmartIslandTheme(darkTheme = true) {
        SmartIslandHomeScreen()
    }
}

