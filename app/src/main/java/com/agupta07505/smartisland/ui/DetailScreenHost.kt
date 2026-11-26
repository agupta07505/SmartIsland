/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import com.agupta07505.smartisland.ui.search.searchAnchor
import com.agupta07505.smartisland.ui.sections.AboutSection
import com.agupta07505.smartisland.ui.sections.AppShortcutsSection
import com.agupta07505.smartisland.ui.sections.AppsSection
import com.agupta07505.smartisland.ui.sections.BackupRestoreSection
import com.agupta07505.smartisland.ui.sections.CalendarSettingsCard
import com.agupta07505.smartisland.ui.sections.CustomizationsSection
import com.agupta07505.smartisland.ui.sections.DeveloperOptionsSection
import com.agupta07505.smartisland.ui.sections.FaceIdSettingsSection
import com.agupta07505.smartisland.ui.sections.GesturesSection
import com.agupta07505.smartisland.ui.sections.IdleWidgetsSettingsSection
import com.agupta07505.smartisland.ui.sections.NetSpeedSettingsCard
import com.agupta07505.smartisland.ui.sections.NotificationHistorySection
import com.agupta07505.smartisland.ui.sections.NotificationsAndPrivacySection
import com.agupta07505.smartisland.ui.sections.PermissionsSection
import com.agupta07505.smartisland.ui.sections.StatusBarHideSection
import com.agupta07505.smartisland.ui.sections.SupportSection
import com.agupta07505.smartisland.ui.sections.TimingSection
import com.agupta07505.smartisland.ui.sections.UpdatesAndDownloadsSection
import com.agupta07505.smartisland.ui.sections.WeatherSettingsCard

@Composable
internal fun DetailScreenHost(
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
    val scope = rememberCoroutineScope()

    // the history and Apps pages hold their own lazy list
    val isScrollableParent = section != FeatureDetailSection.NotificationHistory && section != FeatureDetailSection.Apps
    val scrollModifier = if (isScrollableParent) Modifier.verticalScroll(rememberScrollState()) else Modifier
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val horizontalPad = contentSidePadding(screenWidthDp)

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
        DetailHeader(section = section, onBack = onBack)

        DetailBody(
            section = section,
            settings = settings,
            repository = repository,
            overlayGranted = overlayGranted,
            notificationGranted = notificationGranted,
            batteryIgnored = batteryIgnored,
            bluetoothGranted = bluetoothGranted,
            onBluetoothClick = onBluetoothClick,
            onRefreshPermissions = onRefreshPermissions,
            onNavigateTo = onNavigateTo
        )
    }
}

@Composable
private fun DetailHeader(
    section: FeatureDetailSection,
    onBack: () -> Unit
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
            FeatureDetailSection.Diagnostics -> "Diagnostic log"
            FeatureDetailSection.Timing -> stringResource(R.string.detail_title_timing)
            FeatureDetailSection.Apps -> stringResource(R.string.detail_title_apps)
            FeatureDetailSection.IdleWidgets -> stringResource(R.string.idle_widgets_title)
            FeatureDetailSection.About -> stringResource(R.string.detail_title_about)
            FeatureDetailSection.UpdatesAndDownloads -> stringResource(R.string.detail_title_updates_downloads)
            FeatureDetailSection.SupportCommunity -> stringResource(R.string.detail_title_support_community)
            FeatureDetailSection.DeveloperOptions -> stringResource(R.string.detail_title_developer_options)
            FeatureDetailSection.FaceIdSettings -> "Face ID settings"
            FeatureDetailSection.WeatherSettings -> stringResource(R.string.weather_cfg_title)
            FeatureDetailSection.NetSpeedSettings -> stringResource(R.string.net_cfg_title)
            FeatureDetailSection.CalendarSettings -> stringResource(R.string.cal_cfg_title)
            FeatureDetailSection.StatusBarHide -> "Status bar"
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ColumnScope.DetailBody(
    section: FeatureDetailSection,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    bluetoothGranted: Boolean,
    onBluetoothClick: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    when (section) {
        FeatureDetailSection.NotificationRules -> {
            NotificationsAndPrivacySection(
                settings = settings,
                repository = repository,
                onOpenApps = { onNavigateTo(FeatureDetailSection.Apps) }
            )
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
            PermissionsDetail(
                overlayGranted = overlayGranted,
                notificationGranted = notificationGranted,
                batteryIgnored = batteryIgnored,
                bluetoothGranted = bluetoothGranted,
                onBluetoothClick = onBluetoothClick,
                onRefreshPermissions = onRefreshPermissions
            )
        }
        FeatureDetailSection.BackupRestore -> {
            BackupRestoreSection(settings = settings, repository = repository)
        }
        FeatureDetailSection.Diagnostics -> {
            DeveloperOptionsSection(settings = settings, repository = repository)
        }
        FeatureDetailSection.Timing -> {
            TimingSection(settings = settings, repository = repository)
        }
        FeatureDetailSection.Apps -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AppsSection(settings = settings, repository = repository)
            }
        }
        FeatureDetailSection.IdleWidgets -> {
            IdleWidgetsSettingsSection(
                settings = settings,
                repository = repository,
                onOpenWeatherSettings = { onNavigateTo(FeatureDetailSection.WeatherSettings) },
                onOpenNetSpeedSettings = { onNavigateTo(FeatureDetailSection.NetSpeedSettings) },
                onOpenCalendarSettings = { onNavigateTo(FeatureDetailSection.CalendarSettings) }
            )
        }
        FeatureDetailSection.About -> {
            AboutSection(settings = settings, repository = repository)
        }
        FeatureDetailSection.UpdatesAndDownloads -> {
            UpdatesAndDownloadsSection(settings = settings, repository = repository)
        }
        FeatureDetailSection.SupportCommunity -> {
            SupportSection()
        }
        FeatureDetailSection.DeveloperOptions -> {
            DeveloperOptionsSection(settings = settings, repository = repository)
        }
        FeatureDetailSection.FaceIdSettings -> {
            FaceIdSettingsSection(settings = settings, repository = repository)
        }
        FeatureDetailSection.WeatherSettings -> {
            WeatherSettingsCard(settings = settings, repository = repository,
                modifier = Modifier.searchAnchor("idle.weather"))
        }
        FeatureDetailSection.NetSpeedSettings -> {
            NetSpeedSettingsCard(settings = settings, repository = repository,
                modifier = Modifier.searchAnchor("idle.netspeed"))
        }
        FeatureDetailSection.CalendarSettings -> {
            CalendarSettingsCard(modifier = Modifier.searchAnchor("idle.calendar"))
        }
        FeatureDetailSection.StatusBarHide -> {
            StatusBarHideSection(settings = settings, repository = repository)
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun PermissionsDetail(
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    bluetoothGranted: Boolean,
    onBluetoothClick: () -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current
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
                val component = ComponentName(context, SmartIslandNotificationListenerService::class.java)
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
