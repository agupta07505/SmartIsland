/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.ui.Dimens
import com.agupta07505.smartisland.ui.PermissionCard
import com.agupta07505.smartisland.ui.components.GroupedSection
import com.agupta07505.smartisland.ui.components.StatusKind
import com.agupta07505.smartisland.ui.components.StatusLabel
import com.agupta07505.smartisland.ui.components.TintedActionButton
import com.agupta07505.smartisland.util.OemAutostartUtil
import com.agupta07505.smartisland.util.OemDeviceRules
import com.agupta07505.smartisland.util.OemIntentFactory
import com.agupta07505.smartisland.util.PermissionRecoveryNotifier
import com.agupta07505.smartisland.util.safeStartActivity

/**
 * Recommended onboarding steps beyond the required permissions:
 *  - "Lock in background / Auto-start" on MIUI, ColorOS / OnePlus and FuntouchOS / OriginOS (Rule_Book 7.2).
 *  - Turning off the system's pop-up (heads-up) notifications and bubbles, so only the island animates a
 *    new alert (10.2).
 *  - On Android 13+, permission to post the "Restore Smart Island Permissions" alert (7.1).
 */
@Composable
fun OnboardingExtrasSection() {
    val context = LocalContext.current
    val device = remember { OemDeviceRules.resolveEffectiveDevice(null) }
    var canNotify by remember { mutableStateOf(PermissionRecoveryNotifier.canPostNotifications(context)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { canNotify = PermissionRecoveryNotifier.canPostNotifications(context) }

    GroupedSection(header = stringResource(R.string.onboarding_extras_header)) {
        if (OemIntentFactory.needsBackgroundLockStep(device)) {
            GuidanceRow(
                title = stringResource(R.string.oem_background_title),
                description = stringResource(R.string.oem_background_desc, device.displayName),
                buttonText = stringResource(R.string.btn_open_settings),
                onClick = { OemAutostartUtil.openBackgroundLockSettings(context) }
            )
        }
        GuidanceRow(
            title = stringResource(R.string.popup_notifications_title),
            description = stringResource(R.string.popup_notifications_desc),
            buttonText = stringResource(R.string.btn_open_settings),
            onClick = {
                context.safeStartActivity(
                    Intent(ALL_APPS_NOTIFICATION_SETTINGS),
                    errorMessage = null,
                    fallbackIntent = Intent(NOTIFICATION_SETTINGS)
                ) || context.safeStartActivity(Intent(Settings.ACTION_SETTINGS))
            }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                title = stringResource(R.string.recovery_alerts_title),
                description = stringResource(R.string.recovery_alerts_desc),
                granted = canNotify,
                buttonText = stringResource(R.string.btn_grant),
                onClick = {
                    if (canNotify) {
                        context.safeStartActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
    }
}

/** A recommended step whose state the app cannot read: title, "Recommended", explanation, one action. */
@Composable
private fun GuidanceRow(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.RowHorizontal, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatusLabel(text = stringResource(R.string.perm_status_recommended), kind = StatusKind.Neutral)
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TintedActionButton(text = buttonText, onClick = onClick)
    }
}

// Literal actions: the all-apps list is Android 13+ and the overview page is not public API; both fall back.
private const val ALL_APPS_NOTIFICATION_SETTINGS = "android.settings.ALL_APPS_NOTIFICATION_SETTINGS"
private const val NOTIFICATION_SETTINGS = "android.settings.NOTIFICATION_SETTINGS"
