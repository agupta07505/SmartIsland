/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.components.SmartIslandSwitch
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.agupta07505.smartisland.ui.components.BackBar
import com.agupta07505.smartisland.ui.components.FilledActionButton
import com.agupta07505.smartisland.ui.components.GroupedRow
import com.agupta07505.smartisland.ui.components.GroupedSection
import com.agupta07505.smartisland.ui.components.LargeTitleHeader
import com.agupta07505.smartisland.ui.components.StatusKind
import com.agupta07505.smartisland.ui.components.StatusLabel
import com.agupta07505.smartisland.ui.components.SwitchRow
import com.agupta07505.smartisland.ui.components.TintedActionButton
import android.widget.Toast
import androidx.compose.material.icons.rounded.EmojiEmotions
import com.agupta07505.smartisland.ui.sections.BuddySection
import androidx.compose.material.icons.rounded.CheckCircle
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
    AboutApp,
    SupportCommunity,
    DeveloperOptions,
    Buddy
}

@SuppressLint("BatteryLife")
@Composable
fun SmartIslandHomeScreen(
    repository: SmartIslandSettingsRepository? = null,
    notificationRepository: INotificationRepository? = null,
    /** Bumped by MainActivity when a foreground check finds a required permission missing (Rule_Book §7.1). */
    permissionRouteRequest: Int = 0
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

    // A required grant went missing while the app was away: go straight back to the permissions setup.
    LaunchedEffect(permissionRouteRequest) {
        if (permissionRouteRequest > 0 && activeDetailSection != FeatureDetailSection.PermissionsCenter) {
            transitionDirection = 1
            activeDetailSection = FeatureDetailSection.PermissionsCenter
        }
    }

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
                val horizontalPad = if (screenWidthDp > 600) Dimens.ScreenMarginWide else Dimens.ScreenMargin
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = horizontalPad,
                            end = horizontalPad,
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)
                ) {
                    LargeTitleHeader(
                        title = when (selectedTab) {
                            StudioTab.Studio -> stringResource(R.string.app_name)
                            StudioTab.Position -> stringResource(R.string.tab_position)
                            StudioTab.Settings -> stringResource(R.string.tab_settings)
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
private fun MasterPowerCard(
    enabled: Boolean,
    canEnable: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSetupPermissionsClick: () -> Unit
) {
    GroupedSection {
        SwitchRow(
            title = stringResource(R.string.master_switch_title),
            subtitle = if (canEnable) {
                if (enabled) stringResource(R.string.master_switch_active_desc)
                else stringResource(R.string.master_switch_ready_desc)
            } else stringResource(R.string.master_switch_missing_desc),
            checked = enabled,
            enabled = canEnable || enabled,
            onCheckedChange = onCheckedChange
        )
        if (!canEnable) {
            GroupedRow(
                title = stringResource(R.string.btn_grant_required_permissions),
                onClick = onSetupPermissionsClick,
                trailing = {
                    StatusLabel(
                        text = stringResource(R.string.status_setup_needed),
                        kind = StatusKind.Warning
                    )
                }
            )
        }
    }
}

@Composable
private fun SimulationLabCard(
    activeMode: IslandMode,
    onModeSelect: (IslandMode) -> Unit,
    onClearAll: () -> Unit
) {
    data class ModeItem(val mode: IslandMode, val icon: ImageVector, val labelRes: Int)
    val modes = remember {
        listOf(
            ModeItem(IslandMode.Music, Icons.Rounded.MusicNote, R.string.mode_music_player),
            ModeItem(IslandMode.IncomingCall, Icons.Rounded.Call, R.string.mode_incoming_call),
            ModeItem(IslandMode.Notification, Icons.Rounded.Notifications, R.string.mode_notification),
            ModeItem(IslandMode.Battery, Icons.Rounded.BatteryChargingFull, R.string.mode_battery_charge),
            ModeItem(IslandMode.LiveActivity, Icons.Rounded.Navigation, R.string.mode_live_activity),
            ModeItem(IslandMode.Navigation, Icons.Rounded.Explore, R.string.mode_turn_navigation),
            ModeItem(IslandMode.DownloadUpload, Icons.Rounded.FileDownload, R.string.mode_file_transfer),
            ModeItem(IslandMode.Hotspot, Icons.Rounded.WifiTethering, R.string.mode_hotspot_share),
            ModeItem(IslandMode.Bluetooth, Icons.Rounded.BluetoothConnected, R.string.mode_bluetooth),
            ModeItem(IslandMode.Flashlight, Icons.Rounded.FlashlightOn, R.string.mode_flashlight),
            ModeItem(IslandMode.ScreenRecording, Icons.Rounded.Videocam, R.string.mode_screen_recording),
            ModeItem(IslandMode.Timer, Icons.Rounded.HourglassBottom, R.string.mode_timer),
            ModeItem(IslandMode.Stopwatch, Icons.Rounded.AvTimer, R.string.mode_stopwatch)
        )
    }

    GroupedSection(
        header = stringResource(R.string.simulation_lab_title),
        footer = stringResource(R.string.simulation_lab_desc)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val columns = if (maxWidth >= 480.dp) 3 else 2
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.chunked(columns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            ModeChipButton(
                                label = stringResource(item.labelRes),
                                icon = item.icon,
                                isSelected = activeMode == item.mode,
                                onClick = { onModeSelect(item.mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        GroupedRow(
            title = stringResource(R.string.btn_clear_all_test_notifications),
            destructive = true,
            showChevron = false,
            onClick = onClearAll
        )
    }
}

/** Selectable mode chip: 48dp tall, selection shown by fill, weight and a checkmark (not color alone). */
@Composable
private fun ModeChipButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(Dimens.ControlRadius))
            .background(if (isSelected) accent.copy(alpha = 0.14f) else MaterialTheme.semantic.fill)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Rounded.CheckCircle else icon,
            contentDescription = null,
            tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
    val sectionIcon = Dimens.SeparatorInset

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)) {
        GroupedSection(
            header = stringResource(R.string.category_behaviors_launcher),
            dividerInset = sectionIcon
        ) {
            GroupedRow(
                title = stringResource(R.string.card_notifications_privacy_title),
                icon = Icons.Rounded.Notifications,
                value = if (settings.showOnLockScreen) stringResource(R.string.card_notifications_privacy_status_lock) else null,
                onClick = { onNavigateTo(FeatureDetailSection.NotificationRules) }
            )
            GroupedRow(
                title = stringResource(R.string.card_app_shortcuts_title),
                icon = Icons.Rounded.Apps,
                value = if (settings.enableAppShortcuts) {
                    stringResource(R.string.card_app_shortcuts_status, settings.shortcutPackages.size)
                } else null,
                onClick = { onNavigateTo(FeatureDetailSection.AppShortcuts) }
            )
            GroupedRow(
                title = stringResource(R.string.card_notification_history_title),
                icon = Icons.Rounded.History,
                value = if (settings.enableNotificationHistory) stringResource(R.string.card_notification_history_status_active) else null,
                onClick = { onNavigateTo(FeatureDetailSection.NotificationHistory) }
            )
        }

        GroupedSection(
            header = stringResource(R.string.category_appearance_controls),
            dividerInset = sectionIcon
        ) {
            GroupedRow(
                title = stringResource(R.string.card_color_studio_title),
                icon = Icons.Rounded.Palette,
                value = stringResource(R.string.card_color_studio_status, (settings.opacity * 100).toInt()),
                onClick = { onNavigateTo(FeatureDetailSection.ColorStudio) }
            )
            GroupedRow(
                title = stringResource(R.string.card_gestures_guide_title),
                icon = Icons.Rounded.Gesture,
                onClick = { onNavigateTo(FeatureDetailSection.GesturesGuide) }
            )
            GroupedRow(
                title = stringResource(R.string.buddy_title),
                icon = Icons.Rounded.EmojiEmotions,
                value = stringResource(if (settings.buddyEnabled) R.string.buddy_status_on else R.string.buddy_status_off),
                onClick = { onNavigateTo(FeatureDetailSection.Buddy) }
            )
        }

        GroupedSection(
            header = stringResource(R.string.category_system_core),
            dividerInset = sectionIcon
        ) {
            GroupedRow(
                title = stringResource(R.string.card_permissions_setup_title),
                icon = Icons.Rounded.Shield,
                onClick = { onNavigateTo(FeatureDetailSection.PermissionsCenter) },
                trailing = {
                    if (!canEnable) {
                        StatusLabel(
                            text = stringResource(R.string.status_action_required),
                            kind = StatusKind.Warning
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            GroupedRow(
                title = stringResource(R.string.card_backup_restore_title),
                icon = Icons.Rounded.SettingsBackupRestore,
                onClick = { onNavigateTo(FeatureDetailSection.BackupRestore) }
            )
        }

        GroupedSection(
            header = stringResource(R.string.category_about_community),
            dividerInset = sectionIcon
        ) {
            GroupedRow(
                title = stringResource(R.string.card_about_app_title),
                icon = Icons.Rounded.Info,
                value = "v${com.agupta07505.smartisland.BuildConfig.VERSION_NAME}",
                onClick = { onNavigateTo(FeatureDetailSection.AboutApp) }
            )
            GroupedRow(
                title = stringResource(R.string.card_support_requests_title),
                icon = Icons.Rounded.People,
                onClick = { onNavigateTo(FeatureDetailSection.SupportCommunity) }
            )
        }

        GroupedSection(
            header = stringResource(R.string.category_developer_options),
            dividerInset = sectionIcon
        ) {
            GroupedRow(
                title = stringResource(R.string.card_developer_options_title),
                icon = Icons.Rounded.Tune,
                onClick = { onNavigateTo(FeatureDetailSection.DeveloperOptions) },
                trailing = {
                    if (settings.recordLogs) {
                        StatusLabel(
                            text = stringResource(R.string.status_recording_active),
                            kind = StatusKind.Danger
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}

/** Flat tab bar: no indicator pill, accent for the selected tab, secondary label for the rest. */
@Composable
private fun StudioBottomNavigationBar(
    selectedTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit
) {
    val tabs = listOf(
        Triple(StudioTab.Studio, Icons.Rounded.FlashOn, R.string.tab_studio),
        Triple(StudioTab.Position, Icons.Rounded.Tune, R.string.tab_position),
        Triple(StudioTab.Settings, Icons.Rounded.Settings, R.string.tab_settings)
    )
    Column {
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.semantic.separator)
        NavigationBar(
            containerColor = MaterialTheme.semantic.bar,
            tonalElevation = 0.dp
        ) {
            tabs.forEach { (tab, icon, labelRes) ->
                val label = stringResource(labelRes)
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(icon, contentDescription = null) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
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
    onRefreshPermissions: () -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isScrollableParent = section != FeatureDetailSection.NotificationHistory
    val scrollModifier = if (isScrollableParent) Modifier.verticalScroll(rememberScrollState()) else Modifier
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val horizontalPad = if (screenWidthDp > 600) Dimens.ScreenMarginWide else Dimens.ScreenMargin

    val title = when (section) {
        FeatureDetailSection.NotificationRules -> stringResource(R.string.detail_title_notifications_privacy)
        FeatureDetailSection.AppShortcuts -> stringResource(R.string.detail_title_app_shortcuts)
        FeatureDetailSection.NotificationHistory -> stringResource(R.string.detail_title_notification_history)
        FeatureDetailSection.ColorStudio -> stringResource(R.string.detail_title_color_studio)
        FeatureDetailSection.GesturesGuide -> stringResource(R.string.detail_title_gestures_guide)
        FeatureDetailSection.PermissionsCenter -> stringResource(R.string.detail_title_permissions_center)
        FeatureDetailSection.BackupRestore -> stringResource(R.string.detail_title_backup_restore)
        FeatureDetailSection.AboutApp -> stringResource(R.string.detail_title_about_app)
        FeatureDetailSection.SupportCommunity -> stringResource(R.string.detail_title_support_community)
        FeatureDetailSection.DeveloperOptions -> stringResource(R.string.detail_title_developer_options)
        FeatureDetailSection.Buddy -> stringResource(R.string.detail_title_buddy)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
            .then(scrollModifier)
            .padding(
                start = horizontalPad,
                end = horizontalPad,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = if (isScrollableParent) 28.dp else 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)
    ) {
        Column {
            BackBar(
                parentTitle = stringResource(R.string.tab_settings),
                onBack = onBack,
                modifier = Modifier.offset(x = (-8).dp)
            )
            LargeTitleHeader(title = title)
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
            FeatureDetailSection.BackupRestore -> {
                BackupRestoreSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.AboutApp -> {
                AboutSection(
                    settings = settings,
                    repository = repository
                )
            }
            FeatureDetailSection.SupportCommunity -> {
                SupportSection()
            }
            FeatureDetailSection.DeveloperOptions -> {
                DeveloperOptionsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.Buddy -> {
                BuddySection(settings = settings, repository = repository)
            }
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
    val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
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
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.welcome_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            FilledActionButton(
                text = stringResource(R.string.btn_get_started),
                onClick = onDismiss
            )
            TintedActionButton(
                text = stringResource(R.string.star_on_github),
                onClick = onStarClick
            )
            TintedActionButton(
                text = stringResource(R.string.join_telegram),
                onClick = onJoinCommunityClick
            )
        }
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
