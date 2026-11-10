/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.ui.glass.LocalGlassDark
import com.agupta07505.smartisland.ui.glass.LocalLiquidGlass
import com.agupta07505.smartisland.ui.glass.glassEdge
import com.agupta07505.smartisland.ui.search.SettingsSearchEntry
import com.agupta07505.smartisland.ui.search.SettingsSearchField
import com.agupta07505.smartisland.ui.search.SettingsSearchResults
import com.agupta07505.smartisland.ui.sections.AppThemeOptions
import com.agupta07505.smartisland.ui.sections.IslandFontMixSection
import com.agupta07505.smartisland.ui.sections.IslandFontPicker
import com.agupta07505.smartisland.ui.sections.shizukuReadiness
import com.agupta07505.smartisland.ui.sections.statusBarStateLabel
import com.agupta07505.smartisland.util.StatusBarReapplier

@Composable
internal fun SettingsOverviewSection(
    settings: SmartIslandSettings,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<SettingsSearchEntry>,
    onSearchPick: (SettingsSearchEntry) -> Unit,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    onThemeModeChange: (String) -> Unit,
    onLiquidGlassChange: (Boolean) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onAccentChange: (Long) -> Unit,
    onIslandFontChange: (String) -> Unit,
    onFontMixChange: (Boolean, String, String, String) -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    val canEnable = overlayGranted && notificationGranted

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsSearchField(query = searchQuery, onQueryChange = onSearchQueryChange)

        if (searchQuery.isNotBlank()) {
            SettingsSearchResults(query = searchQuery, results = searchResults, onPick = onSearchPick)
        } else {
            // appearance: the app's own theme, then the island's colours
            AppearanceGroup(
                settings = settings,
                onThemeModeChange = onThemeModeChange,
                onLiquidGlassChange = onLiquidGlassChange,
                onMaterialYouChange = onMaterialYouChange,
                onAccentChange = onAccentChange,
                onIslandFontChange = onIslandFontChange,
                onFontMixChange = onFontMixChange,
                onNavigateTo = onNavigateTo
            )

            // features: what shows on the island and which apps may use it.
            FeaturesGroup(settings = settings, onNavigateTo = onNavigateTo)

            InteractionGroup(settings = settings, onNavigateTo = onNavigateTo)

            // system: permissions, backup, developer options, about
            SystemGroup(settings = settings, canEnable = canEnable, onNavigateTo = onNavigateTo)
        }
    }
}

@Composable
private fun AppearanceGroup(
    settings: SmartIslandSettings,
    onThemeModeChange: (String) -> Unit,
    onLiquidGlassChange: (Boolean) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onAccentChange: (Long) -> Unit,
    onIslandFontChange: (String) -> Unit,
    onFontMixChange: (Boolean, String, String, String) -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    SettingsCategoryGroup(title = stringResource(R.string.category_appearance_controls)) {
        ThemeModeSelector(selected = settings.themeMode, onSelected = onThemeModeChange)

        AppThemeOptions(
            settings = settings,
            onLiquidGlassChange = onLiquidGlassChange,
            onMaterialYouChange = onMaterialYouChange,
            onAccentChange = onAccentChange
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_color_studio_title),
            subtitle = "Colours, opacity, gradients and card styles",
            icon = Icons.Rounded.Palette,
            iconColor = BadgeViolet,
            statusText = stringResource(R.string.card_color_studio_status, (settings.opacity * 100).toInt()),
            onClick = { onNavigateTo(FeatureDetailSection.ColorStudio) }
        )

        CollapsibleCard(
            title = stringResource(R.string.island_font_title),
            stateKey = "settings_appearance_fonts",
            summary = "Typography and font mixing",
            icon = Icons.Rounded.FontDownload,
            initiallyExpanded = false
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IslandFontPicker(selected = settings.islandFont, onSelected = onIslandFontChange)
                IslandFontMixSection(settings = settings, onChange = onFontMixChange)
            }
        }
    }
}

@Composable
private fun FeaturesGroup(
    settings: SmartIslandSettings,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    SettingsCategoryGroup(title = stringResource(R.string.category_behaviors_launcher)) {
        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_notifications_privacy_title),
            subtitle = "Which apps and events show, lock screen privacy",
            icon = Icons.Rounded.Notifications,
            statusText = if (settings.showOnLockScreen) stringResource(R.string.card_notifications_privacy_status_lock) else null,
            onClick = { onNavigateTo(FeatureDetailSection.NotificationRules) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_notification_history_title),
            subtitle = "Browse notifications the island has shown",
            icon = Icons.Rounded.History,
            iconColor = BadgeIndigo,
            statusText = if (settings.enableNotificationHistory) stringResource(R.string.card_notification_history_status_active) else null,
            statusColor = if (settings.enableNotificationHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { onNavigateTo(FeatureDetailSection.NotificationHistory) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_app_shortcuts_title),
            subtitle = "Launch favourite apps from the island",
            icon = Icons.Rounded.Apps,
            iconColor = BadgeTeal,
            statusText = if (settings.enableAppShortcuts) {
                stringResource(R.string.card_app_shortcuts_status, settings.shortcutPackages.size)
            } else null,
            statusColor = if (settings.enableAppShortcuts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { onNavigateTo(FeatureDetailSection.AppShortcuts) }
        )
    }
}

@Composable
private fun InteractionGroup(
    settings: SmartIslandSettings,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    SettingsCategoryGroup(title = stringResource(R.string.category_interaction)) {
        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_gestures_guide_title),
            subtitle = "Swipes, taps, long press, sensitivity and vibration",
            icon = Icons.Rounded.Gesture,
            iconColor = BadgePink,
            onClick = { onNavigateTo(FeatureDetailSection.GesturesGuide) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.idle_widgets_title),
            subtitle = stringResource(R.string.card_idle_widgets_subtitle),
            icon = Icons.Rounded.Widgets,
            iconColor = BadgeAmber,
            statusText = if (settings.idleWidgetsEnabled || settings.idleWidgetsEnabledLandscape) {
                stringResource(R.string.status_active)
            } else null,
            statusColor = MaterialTheme.colorScheme.primary,
            onClick = { onNavigateTo(FeatureDetailSection.IdleWidgets) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_apps_title),
            subtitle = stringResource(R.string.card_apps_subtitle),
            icon = Icons.Rounded.Tune,
            iconColor = BadgeIndigo,
            onClick = { onNavigateTo(FeatureDetailSection.Apps) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_timing_title),
            subtitle = stringResource(R.string.card_timing_subtitle),
            icon = Icons.Rounded.Timer,
            iconColor = BadgeSky,
            onClick = { onNavigateTo(FeatureDetailSection.Timing) }
        )

        FeatureStudioNavigationCard(
            title = "Status bar",
            subtitle = "Hide notification icons, the clock and system icons, each on its own (needs Shizuku)",
            icon = Icons.Rounded.VisibilityOff,
            iconColor = BadgeIndigo,
            statusText = if (settings.statusBarHidden) {
                statusBarStateLabel(
                    hidden = true,
                    readiness = shizukuReadiness(LocalContext.current),
                    status = StatusBarReapplier.shared.collectAsStateWithLifecycle().value
                )
            } else null,
            onClick = { onNavigateTo(FeatureDetailSection.StatusBarHide) }
        )
    }
}

@Composable
private fun SystemGroup(
    settings: SmartIslandSettings,
    canEnable: Boolean,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    SettingsCategoryGroup(title = stringResource(R.string.category_system_core)) {
        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_permissions_setup_title),
            subtitle = "Accessibility, notification access, battery",
            icon = Icons.Rounded.Shield,
            iconColor = BadgeGreen,
            statusText = if (!canEnable) stringResource(R.string.status_action_required) else null,
            statusColor = if (canEnable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
            onClick = { onNavigateTo(FeatureDetailSection.PermissionsCenter) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_backup_restore_title),
            subtitle = "Export or import all settings",
            icon = Icons.Rounded.SettingsBackupRestore,
            iconColor = BadgeBlue,
            onClick = { onNavigateTo(FeatureDetailSection.BackupRestore) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_updates_downloads_title),
            subtitle = "Check GitHub releases and download updates",
            icon = Icons.Rounded.Download,
            iconColor = BadgeTeal,
            onClick = { onNavigateTo(FeatureDetailSection.UpdatesAndDownloads) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_support_requests_title),
            subtitle = "Report issues, feature requests & community",
            icon = Icons.Rounded.People,
            iconColor = BadgePink,
            onClick = { onNavigateTo(FeatureDetailSection.SupportCommunity) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_developer_options_title),
            subtitle = "Diagnostics, log recording & developer options",
            icon = Icons.Rounded.Code,
            iconColor = BadgeSlate,
            statusText = if (settings.recordLogs) "Recording" else null,
            statusColor = MaterialTheme.colorScheme.primary,
            onClick = { onNavigateTo(FeatureDetailSection.DeveloperOptions) }
        )

        FeatureStudioNavigationCard(
            title = stringResource(R.string.card_about_title),
            subtitle = stringResource(R.string.card_about_subtitle),
            icon = Icons.Rounded.Info,
            iconColor = BadgeSlate,
            onClick = { onNavigateTo(FeatureDetailSection.About) }
        )
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

private val BadgeIndigo = Color(0xFF6366F1)
private val BadgeTeal = Color(0xFF14B8A6)
private val BadgePink = Color(0xFFEC4899)
private val BadgeAmber = Color(0xFFF59E0B)
private val BadgeSky = Color(0xFF0EA5E9)
private val BadgeViolet = Color(0xFF8B5CF6)
private val BadgeBlue = Color(0xFF3B82F6)
private val BadgeSlate = Color(0xFF64748B)
private val BadgeGreen = Color(0xFF16A34A)

@Composable
private fun FeatureStudioNavigationCard(
    title: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    statusText: String? = null,
    statusColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .glassEdge(RoundedCornerShape(14.dp), glass = LocalLiquidGlass.current, dark = LocalGlassDark.current)
            .bounceClick(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
