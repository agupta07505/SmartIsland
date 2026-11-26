/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.ui.PermissionCard
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.util.OemAutostartUtil
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.ShizukuMethodCheck
import com.agupta07505.smartisland.util.safeStartActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PermissionsSection(
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean = false,
    bluetoothGranted: Boolean = false,
    onOverlayClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onBluetoothClick: () -> Unit = {},
    onRefreshPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isExecutingShizukuState = remember { mutableStateOf(false) }
    var isExecutingShizuku by isExecutingShizukuState
    val isRepairingAccessibilityState = remember { mutableStateOf(false) }
    var runningScope by remember { mutableStateOf<ShizukuManager.SetupScope?>(null) }
    // Asked for after the auto setup, always available from its button
    val showCheckDialogState = remember { mutableStateOf(false) }
    var showCheckDialog by showCheckDialogState
    var isChecking by remember { mutableStateOf(false) }
    var checkOutcomes by remember { mutableStateOf(ShizukuMethodCheck.saved(context)) }
    var checkHasRun by remember { mutableStateOf(ShizukuMethodCheck.hasRun(context)) }
    fun runMethodChecks(testHotspot: Boolean) {
        showCheckDialog = false
        isChecking = true
        scope.launch {
            ShizukuMethodCheck.run(context, testHotspot)
            checkOutcomes = ShizukuMethodCheck.saved(context)
            checkHasRun = true
            isChecking = false
            context.showToast(context.getString(R.string.shizuku_check_done))
        }
    }
    val isOemAutostartEnabledState = remember { mutableStateOf(batteryIgnored) }
    var isOemAutostartEnabled by isOemAutostartEnabledState
    val isOverlayWarningDisabledState = remember { mutableStateOf(false) }
    var isOverlayWarningDisabled by isOverlayWarningDisabledState
    val sensitiveAllowedState =
        remember { mutableStateOf(ShizukuManager.isSensitiveNotificationAccessAllowed(context)) }
    var sensitiveAllowed by sensitiveAllowedState
    // Check Shizuku is ready, ask for its permission, or run the chosen setup
    fun runSetup(scopeToGrant: ShizukuManager.SetupScope) {
        when {
            !ShizukuManager.isInstalled(context) || !ShizukuManager.isBinderAvailable() -> {
                context.showToast(context.getString(R.string.shizuku_not_running), long = true)
            }
            !ShizukuManager.hasPermission() -> ShizukuManager.requestPermission()
            else -> {
                isExecutingShizuku = true
                runningScope = scopeToGrant
                scope.launch {
                    val result = if (scopeToGrant == ShizukuManager.SetupScope.All) {
                        ShizukuManager.autoGrantAllPermissions(context)
                    } else {
                        ShizukuManager.autoGrantNecessaryPermissions(context)
                    }
                    isExecutingShizuku = false
                    runningScope = null
                    result.onSuccess {
                        context.showToast(context.getString(R.string.shizuku_success), long = true)
                        isOemAutostartEnabled = true
                        isOverlayWarningDisabled = true
                        sensitiveAllowed = ShizukuManager.isSensitiveNotificationAccessAllowed(context)
                        onRefreshPermissions()
                        // Setup is the moment to find out which Shizuku methods work on this phone
                        showCheckDialog = true
                    }.onFailure { err ->
                        context.showToast(
                            context.getString(R.string.shizuku_failed, err.localizedMessage ?: ""),
                            long = true
                        )
                    }
                }
            }
        }
    }
    // Re-read every time the screen comes back
    val permLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(permLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sensitiveAllowed = ShizukuManager.isSensitiveNotificationAccessAllowed(context)
            }
        }
        permLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { permLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShizukuCard(
            context = context,
            scope = scope,
            runningScope = runningScope,
            isExecutingShizukuState = isExecutingShizukuState,
            isRepairingAccessibilityState = isRepairingAccessibilityState,
            isChecking = isChecking,
            sensitiveAllowedState = sensitiveAllowedState,
            showCheckDialogState = showCheckDialogState,
            checkOutcomes = checkOutcomes,
            checkHasRun = checkHasRun,
            runSetup = { runSetup(it) },
            onRefreshPermissions = onRefreshPermissions
        )

        if (showCheckDialog) {
            ShizukuCheckDialog(
                onDismiss = { showCheckDialog = false },
                onRunChecks = { testHotspot -> runMethodChecks(testHotspot) }
            )
        }

        PermissionCard(
            title = stringResource(R.string.perm_accessibility_title),
            description = stringResource(R.string.perm_accessibility_desc),
            granted = overlayGranted,
            buttonText = stringResource(R.string.btn_grant),
            onClick = onOverlayClick
        )

        PermissionCard(
            title = stringResource(R.string.perm_notification_title),
            description = stringResource(R.string.perm_notification_desc),
            granted = notificationGranted,
            buttonText = stringResource(R.string.btn_grant),
            onClick = onNotificationClick
        )

        PermissionCard(
            title = stringResource(R.string.perm_bluetooth_title),
            description = stringResource(R.string.perm_bluetooth_desc),
            granted = bluetoothGranted,
            buttonText = stringResource(R.string.btn_grant),
            onClick = onBluetoothClick
        )

        PermissionCard(
            title = stringResource(R.string.perm_battery_title),
            description = stringResource(R.string.perm_battery_desc),
            granted = batteryIgnored,
            buttonText = stringResource(R.string.btn_grant),
            onClick = onBatteryClick
        )

        OverlayWarningCard(
            context = context,
            isOverlayWarningDisabledState = isOverlayWarningDisabledState
        )

        OemAutostartCard(
            context = context,
            scope = scope,
            isOemAutostartEnabledState = isOemAutostartEnabledState
        )
    }
}

@Composable
private fun OverlayWarningCard(
    context: Context,
    isOverlayWarningDisabledState: MutableState<Boolean>
) {
    var isOverlayWarningDisabled by isOverlayWarningDisabledState
    val warningIconColor =
        if (isOverlayWarningDisabled) Color(0xFF0F9F6E) else MaterialTheme.colorScheme.onSurfaceVariant
    val warningBgColor = if (isOverlayWarningDisabled) Color(0xFF0F9F6E)
        .copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(warningBgColor, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOverlayWarningDisabled) Icons.Rounded.CheckCircle else Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = warningIconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Suppress System Overlay Warning",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isOverlayWarningDisabled) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F9F6E).copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Configured",
                                    color = Color(0xFF0F9F6E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = {
                        isOverlayWarningDisabled = true
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, "android")
                        }
                        context.safeStartActivity(
                            intent,
                            "Cannot open app notification settings on this device."
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(if (isOverlayWarningDisabled) "Open" else "Hide Alert", fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Opens Android system notification channels to hide the persistent \"Smart Island is displaying over other apps\" banner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun OemAutostartCard(
    context: Context,
    scope: CoroutineScope,
    isOemAutostartEnabledState: MutableState<Boolean>
) {
    var isOemAutostartEnabled by isOemAutostartEnabledState
    val oemIconColor = if (isOemAutostartEnabled) Color(0xFF0F9F6E) else MaterialTheme.colorScheme.tertiary
    val oemBgColor = if (isOemAutostartEnabled) Color(0xFF0F9F6E)
        .copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(oemBgColor, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOemAutostartEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Build,
                            contentDescription = null,
                            tint = oemIconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "OEM Autostart & Kill Protection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isOemAutostartEnabled) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F9F6E).copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Configured",
                                    color = Color(0xFF0F9F6E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ShizukuManager.hasPermission() && !isOemAutostartEnabled) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = ShizukuManager.grantOemAutostartAndKillProtection(context)
                                    result.onSuccess {
                                        isOemAutostartEnabled = true
                                        context.showToast("OEM autostart granted via Shizuku!")
                                    }.onFailure { err ->
                                        context.showToast("Error: ${err.localizedMessage}")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("Shizuku", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            isOemAutostartEnabled = true
                            OemAutostartUtil.openAutostartSettings(context)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Fix Kills", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "On Xiaomi/HyperOS, Samsung OneUI, OPPO ColorOS, and Vivo OriginOS, enable Autostart to prevent custom OEM task killers from terminating Smart Island.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
internal fun SetupOption(
    title: String,
    description: String,
    buttonText: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
        if (filled) {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        } else {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
