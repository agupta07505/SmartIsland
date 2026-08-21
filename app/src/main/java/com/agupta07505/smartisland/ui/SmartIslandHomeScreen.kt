/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.ui.sections.AboutSection
import com.agupta07505.smartisland.ui.sections.AppShortcutsSection
import com.agupta07505.smartisland.ui.sections.CustomizationsSection
import com.agupta07505.smartisland.ui.sections.GesturesSection
import com.agupta07505.smartisland.ui.sections.NotificationsAndPrivacySection
import com.agupta07505.smartisland.ui.sections.PermissionsSection
import com.agupta07505.smartisland.ui.sections.PositionsSection
import com.agupta07505.smartisland.ui.sections.SupportSection
import com.agupta07505.smartisland.util.SystemServiceRecovery
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.launch

private enum class StudioTab {
    Studio,
    Position,
    Settings
}

private enum class FeatureDetailSection {
    NotificationRules,
    AppShortcuts,
    ColorStudio,
    GesturesGuide,
    PermissionsCenter,
    AboutApp,
    SupportCommunity
}

@SuppressLint("BatteryLife")
@Composable
fun SmartIslandHomeScreen(
    repository: SmartIslandSettingsRepository? = null,
    notificationRepository: INotificationRepository? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val resolvedRepository = remember(repository, context) {
        repository ?: runCatching {
            SmartIslandRepositories.settingsRepository(context)
        }.getOrElse {
            SmartIslandSettingsRepository(context.applicationContext)
        }
    }
    val resolvedNotificationRepository = remember(notificationRepository, context) {
        notificationRepository ?: runCatching {
            SmartIslandRepositories.notificationRepository(context)
        }.getOrNull()
    }

    val settings by resolvedRepository.settings.collectAsStateWithLifecycle(initialValue = SmartIslandSettings.Default)
    val scope = rememberCoroutineScope()

    var showWelcomeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!resolvedRepository.isWelcomeDialogShown()) {
            showWelcomeDialog = true
        }
    }

    if (showWelcomeDialog) {
        WelcomeDialog(
            onDismiss = {
                showWelcomeDialog = false
                scope.launch { resolvedRepository.setWelcomeDialogShown(true) }
            },
            onStarClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland"))
                runCatching { context.startActivity(intent) }
            },
            onJoinCommunityClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://telegram.me/SmartIslandApp"))
                runCatching { context.startActivity(intent) }
            }
        )
    }

    var overlayGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var notificationGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var batteryIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = isAccessibilityServiceEnabled(context)
                notificationGranted = isNotificationListenerEnabled(context)
                batteryIgnored = isBatteryOptimizationIgnored(context)
                SystemServiceRecovery.requestRecovery(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by remember { mutableStateOf(StudioTab.Studio) }
    var activeDetailSection by remember { mutableStateOf<FeatureDetailSection?>(null) }
    var transitionDirection by remember { mutableStateOf(1) } // 1 = forward, -1 = backward

    // Active preview mode for interactive live preview
    var previewMode by remember { mutableStateOf(IslandMode.Music) }

    val canEnable = overlayGranted && notificationGranted && batteryIgnored

    BackHandler(enabled = activeDetailSection != null) {
        transitionDirection = -1
        activeDetailSection = null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (activeDetailSection == null) {
                StudioBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        transitionDirection = if (tab.ordinal > selectedTab.ordinal) 1 else -1
                        selectedTab = tab
                    }
                )
            }
        }
    ) { scaffoldPadding ->
        AnimatedContent(
            targetState = activeDetailSection,
            modifier = Modifier.padding(scaffoldPadding),
            transitionSpec = {
                if (transitionDirection == 1) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            },
            label = "ScreenTransition"
        ) { detailSection ->
            if (detailSection == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Studio Top Header
                    StudioTopHeader(
                        isIslandEnabled = settings.enabled,
                        canEnable = canEnable,
                        onHealthClick = {
                            transitionDirection = 1
                            activeDetailSection = FeatureDetailSection.PermissionsCenter
                        }
                    )

                    when (selectedTab) {
                        StudioTab.Studio -> {
                            // 1. Master Power Switch Card
                            MasterPowerCard(
                                enabled = settings.enabled,
                                canEnable = canEnable,
                                onCheckedChange = { turnOn ->
                                    if (turnOn) {
                                        SystemServiceRecovery.requestRecovery(context)
                                    }
                                    scope.launch { resolvedRepository.setEnabled(turnOn) }
                                },
                                onSetupPermissionsClick = {
                                    transitionDirection = 1
                                    activeDetailSection = FeatureDetailSection.PermissionsCenter
                                }
                            )

                            // 2. Interactive Simulation Lab
                            SimulationLabCard(
                                activeMode = previewMode,
                                onModeSelect = { mode ->
                                    previewMode = mode
                                    resolvedNotificationRepository?.showDemo(mode)
                                },
                                onClearAll = {
                                    resolvedNotificationRepository?.clearTestNotifications()
                                    Toast.makeText(context, "Cleared active demo alerts", Toast.LENGTH_SHORT).show()
                                }
                            )

                            // 3. System Diagnostics Strip
                            DiagnosticsSummaryCard(
                                overlayGranted = overlayGranted,
                                notificationGranted = notificationGranted,
                                batteryIgnored = batteryIgnored,
                                onOpenDiagnostics = {
                                    transitionDirection = 1
                                    activeDetailSection = FeatureDetailSection.PermissionsCenter
                                }
                            )
                        }

                        StudioTab.Position -> {
                            PositionsSection(
                                settings = settings,
                                repository = resolvedRepository
                            )
                        }

                        StudioTab.Settings -> {
                            SettingsOverviewSection(
                                settings = settings,
                                overlayGranted = overlayGranted,
                                notificationGranted = notificationGranted,
                                batteryIgnored = batteryIgnored,
                                onNavigateTo = { section ->
                                    transitionDirection = 1
                                    activeDetailSection = section
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.made_by),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                DetailScreenHost(
                    section = detailSection,
                    settings = settings,
                    repository = resolvedRepository,
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    batteryIgnored = batteryIgnored,
                    onBack = {
                        transitionDirection = -1
                        activeDetailSection = null
                    },
                    onRefreshPermissions = {
                        overlayGranted = isAccessibilityServiceEnabled(context)
                        notificationGranted = isNotificationListenerEnabled(context)
                        batteryIgnored = isBatteryOptimizationIgnored(context)
                    }
                )
            }
        }
    }
}

@Composable
private fun StudioTopHeader(
    isIslandEnabled: Boolean,
    canEnable: Boolean,
    onHealthClick: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        runCatchingLogged("HeaderSection", "Failed to get application icon") {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = "Smart Island Logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Column {
                Text(
                    text = "Smart Island",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Floating Dynamic Overlay",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Health Status Badge
        val statusColor = when {
            !canEnable -> Color(0xFFE88C25) // Action required
            isIslandEnabled -> Color(0xFF0F9F6E) // Active
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // Ready/Off
        }
        val statusText = when {
            !canEnable -> "Setup Needed"
            isIslandEnabled -> "Active"
            else -> "Ready"
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable(onClick = onHealthClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MasterPowerCard(
    enabled: Boolean,
    canEnable: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSetupPermissionsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashOn,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Smart Island Master Switch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (canEnable) {
                                if (enabled) "Overlay active and floating above status bar"
                                else "Ready to run. Toggle switch on to start."
                            } else "Required permissions missing before start",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = enabled,
                    enabled = canEnable || enabled,
                    onCheckedChange = onCheckedChange
                )
            }

            if (!canEnable) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSetupPermissionsClick)
                        .background(Color(0xFFE88C25).copy(alpha = 0.08f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE88C25),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Grant required permissions to enable →",
                            color = Color(0xFFE88C25),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulationLabCard(
    activeMode: IslandMode,
    onModeSelect: (IslandMode) -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dynamic Simulation Lab",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "1-Tap test and simulate any dynamic island mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Modes Grid in categorized rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Media & Calls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = "Music Player",
                        icon = Icons.Rounded.MusicNote,
                        iconTint = Color(0xFFFF6B9A),
                        isSelected = activeMode == IslandMode.Music,
                        onClick = { onModeSelect(IslandMode.Music) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = "Incoming Call",
                        icon = Icons.Rounded.Call,
                        iconTint = Color(0xFF22C55E),
                        isSelected = activeMode == IslandMode.IncomingCall,
                        onClick = { onModeSelect(IslandMode.IncomingCall) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Notifications & Power
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = "Notification",
                        icon = Icons.Rounded.Notifications,
                        iconTint = Color(0xFF38BDF8),
                        isSelected = activeMode == IslandMode.Notification,
                        onClick = { onModeSelect(IslandMode.Notification) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = "Battery Charge",
                        icon = Icons.Rounded.BatteryChargingFull,
                        iconTint = Color(0xFF10B981),
                        isSelected = activeMode == IslandMode.Battery,
                        onClick = { onModeSelect(IslandMode.Battery) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Live Activities & Maps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = "Live Activity",
                        icon = Icons.Rounded.Navigation,
                        iconTint = Color(0xFF8B5CF6),
                        isSelected = activeMode == IslandMode.LiveActivity,
                        onClick = { onModeSelect(IslandMode.LiveActivity) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = "Turn Navigation",
                        icon = Icons.Rounded.Explore,
                        iconTint = Color(0xFF10B981),
                        isSelected = activeMode == IslandMode.Navigation,
                        onClick = { onModeSelect(IslandMode.Navigation) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: System Tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = "File Transfer",
                        icon = Icons.Rounded.FileDownload,
                        iconTint = Color(0xFF06B6D4),
                        isSelected = activeMode == IslandMode.DownloadUpload,
                        onClick = { onModeSelect(IslandMode.DownloadUpload) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = "Hotspot Share",
                        icon = Icons.Rounded.WifiTethering,
                        iconTint = Color(0xFFF59E0B),
                        isSelected = activeMode == IslandMode.Hotspot,
                        onClick = { onModeSelect(IslandMode.Hotspot) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 5: Hardware
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = "Bluetooth",
                        icon = Icons.Rounded.BluetoothConnected,
                        iconTint = Color(0xFF38BDF8),
                        isSelected = activeMode == IslandMode.Bluetooth,
                        onClick = { onModeSelect(IslandMode.Bluetooth) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = "Flashlight",
                        icon = Icons.Rounded.FlashlightOn,
                        iconTint = Color(0xFFF59E0B),
                        isSelected = activeMode == IslandMode.Flashlight,
                        onClick = { onModeSelect(IslandMode.Flashlight) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 6: Screen Recording & Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = "Recording",
                        icon = Icons.Rounded.Videocam,
                        iconTint = Color(0xFFEF4444),
                        isSelected = activeMode == IslandMode.ScreenRecording,
                        onClick = { onModeSelect(IslandMode.ScreenRecording) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = "Timer",
                        icon = Icons.Rounded.HourglassBottom,
                        iconTint = Color(0xFFF59E0B),
                        isSelected = activeMode == IslandMode.Timer,
                        onClick = { onModeSelect(IslandMode.Timer) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 7: Stopwatch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = "Stopwatch",
                        icon = Icons.Rounded.AvTimer,
                        iconTint = Color(0xFF06B6D4),
                        isSelected = activeMode == IslandMode.Stopwatch,
                        onClick = { onModeSelect(IslandMode.Stopwatch) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear All Test Notifications", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModeChipButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) iconTint.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (isSelected) iconTint.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DiagnosticsSummaryCard(
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    onOpenDiagnostics: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDiagnostics),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "System Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hardware permissions & background health",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadgePill(
                    label = "Accessibility",
                    isGranted = overlayGranted,
                    modifier = Modifier.weight(1f)
                )
                StatusBadgePill(
                    label = "Notifications",
                    isGranted = notificationGranted,
                    modifier = Modifier.weight(1f)
                )
                StatusBadgePill(
                    label = "Battery Saver",
                    isGranted = batteryIgnored,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusBadgePill(
    label: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isGranted) Color(0xFF0F9F6E) else Color(0xFFE88C25)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SettingsOverviewSection(
    settings: SmartIslandSettings,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    val canEnable = overlayGranted && notificationGranted && batteryIgnored

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Settings & Preferences",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Configure island behaviors, colors, gestures, system access, and updates",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Island Behaviors & App Launcher
        SettingsCategoryGroup(title = "Island Behaviors & Launcher") {
            FeatureStudioNavigationCard(
                title = "Notifications & Privacy Rules",
                subtitle = "Lock screen visibility, auto-expansion, quick actions, and app filters",
                icon = Icons.Rounded.Notifications,
                iconColor = Color(0xFF38BDF8),
                statusText = if (settings.showOnLockScreen) "Lock Screen Active" else "Standard",
                onClick = { onNavigateTo(FeatureDetailSection.NotificationRules) }
            )

            FeatureStudioNavigationCard(
                title = "App Shortcuts Launcher",
                subtitle = "Quick-launch pinned applications or recent apps from collapsed island",
                icon = Icons.Rounded.Apps,
                iconColor = Color(0xFF22D3EE),
                statusText = "${settings.shortcutPackages.size}/8 pinned",
                onClick = { onNavigateTo(FeatureDetailSection.AppShortcuts) }
            )
        }

        // Section 2: Appearance & Gesture Controls
        SettingsCategoryGroup(title = "Appearance & Controls") {
            FeatureStudioNavigationCard(
                title = "Feature Accent Colors Studio",
                subtitle = "Customize color themes for music, battery, calls, and dynamic events",
                icon = Icons.Rounded.Palette,
                iconColor = Color(0xFFA855F7),
                statusText = "11 Island Modes",
                onClick = { onNavigateTo(FeatureDetailSection.ColorStudio) }
            )

            FeatureStudioNavigationCard(
                title = "Gesture Controls & Playground",
                subtitle = "Swipe Up to dismiss, Swipe Down for floating multi-window, and swipe pagination",
                icon = Icons.Rounded.Gesture,
                iconColor = Color(0xFF6366F1),
                statusText = "Interactive Guide",
                onClick = { onNavigateTo(FeatureDetailSection.GesturesGuide) }
            )
        }

        // Section 3: System & Permissions
        SettingsCategoryGroup(title = "System & Core Services") {
            FeatureStudioNavigationCard(
                title = "Permissions & System Setup",
                subtitle = "Shizuku 1-tap auto grant, Accessibility, notification access, and OEM killers",
                icon = Icons.Rounded.Shield,
                iconColor = Color(0xFF10B981),
                statusText = if (canEnable) "All Granted" else "Action Required",
                statusColor = if (canEnable) Color(0xFF0F9F6E) else Color(0xFFE88C25),
                onClick = { onNavigateTo(FeatureDetailSection.PermissionsCenter) }
            )
        }

        // Section 4: About & Community
        SettingsCategoryGroup(title = "About & Community") {
            FeatureStudioNavigationCard(
                title = "About Smart Island",
                subtitle = "Version v${com.agupta07505.smartisland.BuildConfig.VERSION_NAME} • In-App GitHub Updates • Contributors & Commits",
                icon = Icons.Rounded.Info,
                iconColor = Color(0xFFEC4899),
                statusText = "Open Source",
                onClick = { onNavigateTo(FeatureDetailSection.AboutApp) }
            )

            FeatureStudioNavigationCard(
                title = "Support & Feature Requests",
                subtitle = "Star on GitHub, report issues, and join our active Telegram community",
                icon = Icons.Rounded.People,
                iconColor = Color(0xFFF59E0B),
                onClick = { onNavigateTo(FeatureDetailSection.SupportCommunity) }
            )
        }
    }
}

@Composable
private fun SettingsCategoryGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
        content()
    }
}

@Composable
private fun FeatureStudioNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    statusText: String? = null,
    statusColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StudioBottomNavigationBar(
    selectedTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == StudioTab.Studio,
            onClick = { onTabSelected(StudioTab.Studio) },
            icon = { Icon(Icons.Rounded.FlashOn, contentDescription = "Studio") },
            label = { Text("Studio", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Position,
            onClick = { onTabSelected(StudioTab.Position) },
            icon = { Icon(Icons.Rounded.Tune, contentDescription = "Notch & Layout") },
            label = { Text("Notch & Layout", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Settings,
            onClick = { onTabSelected(StudioTab.Settings) },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun DetailScreenHost(
    section: FeatureDetailSection,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    onBack: () -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            val title = when (section) {
                FeatureDetailSection.NotificationRules -> "Notifications & Privacy"
                FeatureDetailSection.AppShortcuts -> "App Shortcuts Launcher"
                FeatureDetailSection.ColorStudio -> "Feature Accent Colors"
                FeatureDetailSection.GesturesGuide -> "Gesture Guide & Playground"
                FeatureDetailSection.PermissionsCenter -> "Permissions & Setup Center"
                FeatureDetailSection.AboutApp -> "About Smart Island"
                FeatureDetailSection.SupportCommunity -> "Support & Feedback"
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        when (section) {
            FeatureDetailSection.NotificationRules -> {
                NotificationsAndPrivacySection(settings = settings, repository = repository)
            }
            FeatureDetailSection.AppShortcuts -> {
                AppShortcutsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.ColorStudio -> {
                CustomizationsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.GesturesGuide -> {
                GesturesSection()
            }
            FeatureDetailSection.PermissionsCenter -> {
                PermissionsSection(
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    batteryIgnored = batteryIgnored,
                    onOverlayClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onNotificationClick = {
                        val detailIntent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS").apply {
                            val component = ComponentName(context, com.agupta07505.smartisland.service.SmartIslandNotificationListenerService::class.java)
                            putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME", component.flattenToString())
                        }
                        runCatching {
                            context.startActivity(detailIntent)
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    },
                    onBatteryClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:${context.packageName}"))
                            )
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    },
                    onRefreshPermissions = onRefreshPermissions
                )
            }
            FeatureDetailSection.AboutApp -> {
                AboutSection()
            }
            FeatureDetailSection.SupportCommunity -> {
                SupportSection()
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

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, com.agupta07505.smartisland.service.SmartIslandOverlayService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@Composable
private fun WelcomeDialog(
    onDismiss: () -> Unit,
    onStarClick: () -> Unit,
    onJoinCommunityClick: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        runCatchingLogged("WelcomeDialog", "Failed to get app icon") {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = "Smart Island Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SI", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    text = "Welcome to Smart Island Studio",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "A private, customizable floating overlay experience for Android. No advertisements, zero tracking, and completely open source.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onStarClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GithubIcon(tint = MaterialTheme.colorScheme.onSecondary)
                        Text(
                            "Star on GitHub",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onJoinCommunityClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Join Telegram Community",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ElevatedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Get Started", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GithubIcon(tint: Color = Color.Black) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(12f * scaleX, 2f * scaleY)
            cubicTo(6.477f * scaleX, 2f * scaleY, 2f * scaleX, 6.477f * scaleX, 2f * scaleX, 12f * scaleY)
            cubicTo(2f * scaleX, 16.42f * scaleY, 4.865f * scaleX, 20.166f * scaleY, 8.839f * scaleX, 21.489f * scaleY)
            cubicTo(9.339f * scaleX, 21.581f * scaleY, 9.521f * scaleX, 21.272f * scaleY, 9.521f * scaleX, 21.007f * scaleY)
            cubicTo(9.521f * scaleX, 20.77f * scaleY, 9.513f * scaleX, 20.141f * scaleY, 9.508f * scaleX, 19.307f * scaleY)
            cubicTo(6.726f * scaleX, 19.91f * scaleY, 6.139f * scaleX, 17.97f * scaleY, 6.139f * scaleX, 17.97f * scaleY)
            cubicTo(5.685f * scaleX, 16.814f * scaleY, 5.029f * scaleX, 16.506f * scaleY, 5.029f * scaleX, 16.506f * scaleY)
            cubicTo(4.121f * scaleX, 15.886f * scaleY, 5.098f * scaleX, 15.898f * scaleY, 5.098f * scaleX, 15.898f * scaleY)
            cubicTo(6.101f * scaleX, 15.968f * scaleY, 6.629f * scaleX, 16.928f * scaleY, 6.629f * scaleX, 16.928f * scaleY)
            cubicTo(7.521f * scaleX, 18.457f * scaleY, 8.97f * scaleX, 18.015f * scaleY, 9.539f * scaleX, 17.759f * scaleY)
            cubicTo(9.631f * scaleX, 17.113f * scaleY, 9.889f * scaleX, 16.673f * scaleY, 10.175f * scaleX, 16.423f * scaleY)
            cubicTo(7.955f * scaleX, 16.17f * scaleY, 5.62f * scaleX, 15.313f * scaleY, 5.62f * scaleX, 11.48f * scaleY)
            cubicTo(5.62f * scaleX, 10.389f * scaleY, 6.01f * scaleX, 9.496f * scaleY, 6.649f * scaleX, 8.797f * scaleY)
            cubicTo(6.546f * scaleX, 8.544f * scaleY, 6.203f * scaleX, 7.527f * scaleY, 6.747f * scaleX, 6.15f * scaleY)
            cubicTo(6.747f * scaleX, 6.15f * scaleY, 7.587f * scaleX, 5.881f * scaleY, 9.497f * scaleX, 7.175f * scaleY)
            cubicTo(10.295f * scaleX, 6.953f * scaleY, 11.15f * scaleX, 6.842f * scaleY, 12f * scaleX, 6.838f * scaleY)
            cubicTo(12.85f * scaleX, 6.842f * scaleY, 13.705f * scaleX, 6.953f * scaleY, 14.503f * scaleX, 7.175f * scaleY)
            cubicTo(16.413f * scaleX, 5.881f * scaleY, 17.253f * scaleX, 6.15f * scaleY, 17.253f * scaleX, 6.15f * scaleY)
            cubicTo(17.797f * scaleX, 7.527f * scaleY, 17.454f * scaleX, 8.544f * scaleY, 17.351f * scaleX, 8.797f * scaleY)
            cubicTo(17.99f * scaleX, 9.496f * scaleY, 18.38f * scaleX, 10.389f * scaleY, 18.38f * scaleX, 11.48f * scaleY)
            cubicTo(18.38f * scaleX, 15.323f * scaleY, 16.041f * scaleX, 16.168f * scaleY, 13.813f * scaleX, 16.415f * scaleY)
            cubicTo(14.172f * scaleX, 16.724f * scaleY, 14.491f * scaleX, 17.334f * scaleY, 14.491f * scaleX, 18.267f * scaleY)
            cubicTo(14.491f * scaleX, 19.603f * scaleY, 14.479f * scaleX, 20.682f * scaleY, 14.479f * scaleX, 21.01f * scaleY)
            cubicTo(14.479f * scaleX, 21.277f * scaleY, 14.659f * scaleX, 21.589f * scaleY, 15.167f * scaleX, 21.489f * scaleY)
            cubicTo(19.141f * scaleX, 20.16f * scaleY, 22f * scaleX, 12f * scaleY, 22f * scaleX, 12f * scaleY)
            cubicTo(22f * scaleX, 6.477f * scaleY, 17.523f * scaleY, 2f * scaleY, 12f * scaleY, 2f * scaleY)
            close()
        }
        drawPath(path, color = tint)
    }
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
