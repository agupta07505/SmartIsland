        /*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.agupta07505.smartisland.ui.sections.BackupRestoreSection
import com.agupta07505.smartisland.ui.sections.CustomizationsSection
import com.agupta07505.smartisland.ui.sections.DeveloperOptionsSection
import com.agupta07505.smartisland.ui.sections.GesturesSection
import com.agupta07505.smartisland.ui.sections.NotificationHistorySection
import com.agupta07505.smartisland.ui.sections.NotificationsAndPrivacySection
import com.agupta07505.smartisland.ui.sections.PermissionsSection
import com.agupta07505.smartisland.ui.sections.UpdatesAndDownloadsSection
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
    NotificationHistory,
    ColorStudio,
    GesturesGuide,
    PermissionsCenter,
    BackupRestore,
    UpdatesAndDownloads,
    AboutApp,
    SupportCommunity,
    DeveloperOptions
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
    val batteryRefreshHandler = remember {
    Handler(Looper.getMainLooper())
    }
    DisposableEffect(Unit) {
    onDispose {
        batteryRefreshHandler.removeCallbacksAndMessages(null)
        }
    }

   fun refreshBatteryOptimizationWithRetry() {
    batteryRefreshHandler.removeCallbacksAndMessages(null)

    val initialState = isBatteryOptimizationIgnored(context)
    batteryIgnored = initialState

    var attempts = 0

    val refreshRunnable = object : Runnable {
        override fun run() {
            val currentState = isBatteryOptimizationIgnored(context)
            batteryIgnored = currentState
            attempts++

            if (currentState != initialState || attempts >= 8) {
                return
            }

            batteryRefreshHandler.postDelayed(this, 150L)
        }
    }

    batteryRefreshHandler.postDelayed(refreshRunnable, 150L)}
    var bluetoothGranted by remember { mutableStateOf(isBluetoothConnectPermissionGranted(context)) }

    val bluetoothPermissionLauncher =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        bluetoothGranted = granted

        if (granted) {
            SystemServiceRecovery.resetAccessibilityRefreshAttempt()
            SystemServiceRecovery.requestRecovery(context)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
           if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = isAccessibilityServiceEnabled(context)
                notificationGranted = isNotificationListenerEnabled(context)
                refreshBatteryOptimizationWithRetry()
                bluetoothGranted = isBluetoothConnectPermissionGranted(context)
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

    val canEnable = overlayGranted && notificationGranted
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
                val screenWidthDp = LocalConfiguration.current.screenWidthDp
                val horizontalPad = when {
                    screenWidthDp < 360 -> 14.dp
                    screenWidthDp > 600 -> 28.dp
                    else -> 20.dp
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = horizontalPad,
                            end = horizontalPad,
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    Toast.makeText(context, context.getString(R.string.toast_cleared_test_notifications), Toast.LENGTH_SHORT).show()
                                }
                            )

                        }

                        StudioTab.Position -> {
                            PositionsSection(
                                settings = settings,
                                repository = resolvedRepository,
                                onNavigateToBackup = {
                                    transitionDirection = 1
                                    activeDetailSection = FeatureDetailSection.BackupRestore
                                }
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
                    bluetoothGranted = bluetoothGranted,
                    onBack = {
                        transitionDirection = -1
                        activeDetailSection = null
                    },
                   onBluetoothClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (!bluetoothGranted) {
                            bluetoothPermissionLauncher.launch(
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        } else {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                },
                    onRefreshPermissions = {
                        overlayGranted = isAccessibilityServiceEnabled(context)
                        notificationGranted = isNotificationListenerEnabled(context)
                        refreshBatteryOptimizationWithRetry()
                        bluetoothGranted = isBluetoothConnectPermissionGranted(context)
                    },
                    onNavigateTo = { section ->
                        transitionDirection = 1
                        activeDetailSection = section
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
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
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
            !canEnable -> stringResource(R.string.status_setup_needed)
            isIslandEnabled -> stringResource(R.string.status_active)
            else -> stringResource(R.string.status_ready)
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.master_switch_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (canEnable) {
                        if (enabled) stringResource(R.string.master_switch_active_desc)
                        else stringResource(R.string.master_switch_ready_desc)
                    } else stringResource(R.string.master_switch_missing_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                enabled = canEnable || enabled,
                onCheckedChange = onCheckedChange
            )
        }

        if (!canEnable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSetupPermissionsClick)
                    .background(Color(0xFFE88C25).copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.btn_grant_required_permissions),
                        color = Color(0xFFE88C25),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
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
    data class ModeItem(val mode: IslandMode, val icon: ImageVector)
    val modes = remember {
        listOf(
            ModeItem(IslandMode.Music, Icons.Rounded.MusicNote),
            ModeItem(IslandMode.IncomingCall, Icons.Rounded.Call),
            ModeItem(IslandMode.Notification, Icons.Rounded.Notifications),
            ModeItem(IslandMode.Battery, Icons.Rounded.BatteryChargingFull),
            ModeItem(IslandMode.LiveActivity, Icons.Rounded.Navigation),
            ModeItem(IslandMode.Navigation, Icons.Rounded.Explore),
            ModeItem(IslandMode.DownloadUpload, Icons.Rounded.FileDownload),
            ModeItem(IslandMode.Hotspot, Icons.Rounded.WifiTethering),
            ModeItem(IslandMode.Bluetooth, Icons.Rounded.BluetoothConnected),
            ModeItem(IslandMode.Flashlight, Icons.Rounded.FlashlightOn),
            ModeItem(IslandMode.ScreenRecording, Icons.Rounded.Videocam),
            ModeItem(IslandMode.Timer, Icons.Rounded.HourglassBottom),
            ModeItem(IslandMode.Stopwatch, Icons.Rounded.AvTimer)
        )
    }
    // Resolve labels via composable context
    val modeLabels = listOf(
        stringResource(R.string.mode_music_player),
        stringResource(R.string.mode_incoming_call),
        stringResource(R.string.mode_notification),
        stringResource(R.string.mode_battery_charge),
        stringResource(R.string.mode_live_activity),
        stringResource(R.string.mode_turn_navigation),
        stringResource(R.string.mode_file_transfer),
        stringResource(R.string.mode_hotspot_share),
        stringResource(R.string.mode_bluetooth),
        stringResource(R.string.mode_flashlight),
        stringResource(R.string.mode_screen_recording),
        stringResource(R.string.mode_timer),
        stringResource(R.string.mode_stopwatch)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.simulation_lab_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.simulation_lab_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.btn_clear_all_test_notifications),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = when {
                    maxWidth >= 330.dp -> 3
                    maxWidth >= 210.dp -> 2
                    else -> 1
                }
                val chunkedIndices = remember(modes.size, columns) {
                    modes.indices.chunked(columns)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunkedIndices.forEach { rowIndices ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowIndices.forEach { index ->
                                val item = modes[index]
                                ModeChipButton(
                                    label = modeLabels[index],
                                    icon = item.icon,
                                    isSelected = activeMode == item.mode,
                                    onClick = { onModeSelect(item.mode) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(columns - rowIndices.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChipButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) iconTint.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) iconTint.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(10.dp)
            )
            .bounceClick(onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) iconTint else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    val canEnable = overlayGranted && notificationGranted

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Section 1: Features
        SettingsCategoryGroup(title = stringResource(R.string.category_behaviors_launcher)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_notifications_privacy_title),
                icon = Icons.Rounded.Notifications,
                statusText = if (settings.showOnLockScreen) stringResource(R.string.card_notifications_privacy_status_lock) else null,
                onClick = { onNavigateTo(FeatureDetailSection.NotificationRules) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_app_shortcuts_title),
                icon = Icons.Rounded.Apps,
                statusText = if (settings.enableAppShortcuts) {
                    stringResource(R.string.card_app_shortcuts_status, settings.shortcutPackages.size)
                } else null,
                statusColor = if (settings.enableAppShortcuts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onNavigateTo(FeatureDetailSection.AppShortcuts) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_notification_history_title),
                icon = Icons.Rounded.History,
                statusText = if (settings.enableNotificationHistory) stringResource(R.string.card_notification_history_status_active) else null,
                statusColor = if (settings.enableNotificationHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onNavigateTo(FeatureDetailSection.NotificationHistory) }
            )
        }

        // Section 2: Appearance
        SettingsCategoryGroup(title = stringResource(R.string.category_appearance_controls)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_color_studio_title),
                icon = Icons.Rounded.Palette,
                statusText = stringResource(R.string.card_color_studio_status, (settings.opacity * 100).toInt()),
                onClick = { onNavigateTo(FeatureDetailSection.ColorStudio) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_gestures_guide_title),
                icon = Icons.Rounded.Gesture,
                onClick = { onNavigateTo(FeatureDetailSection.GesturesGuide) }
            )
        }

        // Section 3: System
        SettingsCategoryGroup(title = stringResource(R.string.category_system_core)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_permissions_setup_title),
                icon = Icons.Rounded.Shield,
                statusText = if (!canEnable) stringResource(R.string.status_action_required) else null,
                statusColor = if (canEnable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                onClick = { onNavigateTo(FeatureDetailSection.PermissionsCenter) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_backup_restore_title),
                icon = Icons.Rounded.SettingsBackupRestore,
                onClick = { onNavigateTo(FeatureDetailSection.BackupRestore) }
            )
        }

        // Section 4: About
        SettingsCategoryGroup(title = stringResource(R.string.category_about_community)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_updates_downloads_title),
                icon = Icons.Rounded.CloudDownload,
                onClick = { onNavigateTo(FeatureDetailSection.UpdatesAndDownloads) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_about_app_title),
                icon = Icons.Rounded.Info,
                statusText = "v${com.agupta07505.smartisland.BuildConfig.VERSION_NAME}",
                onClick = { onNavigateTo(FeatureDetailSection.AboutApp) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_support_requests_title),
                icon = Icons.Rounded.People,
                onClick = { onNavigateTo(FeatureDetailSection.SupportCommunity) }
            )
        }

        SettingsCategoryGroup(title = stringResource(R.string.category_developer_options)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_developer_options_title),
                icon = Icons.Rounded.Tune,
                statusText = if (settings.recordLogs) stringResource(R.string.status_recording_active) else null,
                statusColor = if (settings.recordLogs) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                onClick = { onNavigateTo(FeatureDetailSection.DeveloperOptions) }
            )
        }
    }
}

@Composable
private fun SettingsCategoryGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
        content()
    }
}

@Composable
private fun FeatureStudioNavigationCard(
    title: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    statusText: String? = null,
    statusColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .bounceClick(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (statusText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
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
            icon = { Icon(Icons.Rounded.FlashOn, contentDescription = stringResource(R.string.tab_studio)) },
            label = { Text(stringResource(R.string.tab_studio), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Position,
            onClick = { onTabSelected(StudioTab.Position) },
            icon = { Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.tab_position)) },
            label = { Text(stringResource(R.string.tab_position), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Settings,
            onClick = { onTabSelected(StudioTab.Settings) },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.tab_settings)) },
            label = { Text(stringResource(R.string.tab_settings), fontWeight = FontWeight.SemiBold) },
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
    bluetoothGranted: Boolean,
    onBack: () -> Unit,
    onBluetoothClick: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isScrollableParent = section != FeatureDetailSection.NotificationHistory
    val scrollModifier = if (isScrollableParent) Modifier.verticalScroll(rememberScrollState()) else Modifier
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val horizontalPad = when {
        screenWidthDp < 360 -> 14.dp
        screenWidthDp > 600 -> 28.dp
        else -> 20.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(scrollModifier)
            .padding(
                start = horizontalPad,
                end = horizontalPad,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = if (isScrollableParent) 28.dp else 12.dp
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
                FeatureDetailSection.NotificationRules -> stringResource(R.string.detail_title_notifications_privacy)
                FeatureDetailSection.AppShortcuts -> stringResource(R.string.detail_title_app_shortcuts)
                FeatureDetailSection.NotificationHistory -> stringResource(R.string.detail_title_notification_history)
                FeatureDetailSection.ColorStudio -> stringResource(R.string.detail_title_color_studio)
                FeatureDetailSection.GesturesGuide -> stringResource(R.string.detail_title_gestures_guide)
                FeatureDetailSection.PermissionsCenter -> stringResource(R.string.detail_title_permissions_center)
                FeatureDetailSection.BackupRestore -> stringResource(R.string.detail_title_backup_restore)
                FeatureDetailSection.UpdatesAndDownloads -> stringResource(R.string.detail_title_updates_downloads)
                FeatureDetailSection.AboutApp -> stringResource(R.string.detail_title_about_app)
                FeatureDetailSection.SupportCommunity -> stringResource(R.string.detail_title_support_community)
                FeatureDetailSection.DeveloperOptions -> stringResource(R.string.detail_title_developer_options)
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
            FeatureDetailSection.NotificationHistory -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    NotificationHistorySection(settings = settings, repository = repository)
                }
            }
            FeatureDetailSection.ColorStudio -> {
                CustomizationsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.GesturesGuide -> {
                GesturesSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.PermissionsCenter -> {
                PermissionsSection(
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    batteryIgnored = batteryIgnored,
                    bluetoothGranted = bluetoothGranted,
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
                    onBluetoothClick = onBluetoothClick,

                    onRefreshPermissions = onRefreshPermissions
                )
            }
            FeatureDetailSection.BackupRestore -> {
                BackupRestoreSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.UpdatesAndDownloads -> {
                UpdatesAndDownloadsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.AboutApp -> {
                AboutSection(
                    settings = settings,
                    repository = repository,
                    onNavigateToUpdates = { onNavigateTo(FeatureDetailSection.UpdatesAndDownloads) }
                )
            }
            FeatureDetailSection.SupportCommunity -> {
                SupportSection()
            }
            FeatureDetailSection.DeveloperOptions -> {
                DeveloperOptionsSection(settings = settings, repository = repository)
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
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
                return true
            }

        val pm =
            context.getSystemService(Context.POWER_SERVICE)
                as? android.os.PowerManager

        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
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
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.welcome_desc),
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
                            stringResource(R.string.star_on_github),
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
                            stringResource(R.string.join_telegram),
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
                    Text(stringResource(R.string.btn_get_started), fontWeight = FontWeight.Bold)
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

private fun isBluetoothConnectPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return true
    }

    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
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
