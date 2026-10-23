/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.ui.PermissionCard
import com.agupta07505.smartisland.util.OemAutostartUtil
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.ShizukuMethodCheck
import com.agupta07505.smartisland.util.safeStartActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
    var isExecutingShizuku by remember { mutableStateOf(false) }
    var isRepairingAccessibility by remember { mutableStateOf(false) }
    // Which of the two 1-tap setups is running, for the button label.
    var runningScope by remember { mutableStateOf<ShizukuManager.SetupScope?>(null) }
    // The "try the Shizuku methods" check: asked for after the auto setup, and always available from its button.
    var showCheckDialog by remember { mutableStateOf(false) }
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
            Toast.makeText(context, context.getString(R.string.shizuku_check_done), Toast.LENGTH_SHORT).show()
        }
    }
    var isOemAutostartEnabled by remember { mutableStateOf(batteryIgnored) }
    var isOverlayWarningDisabled by remember { mutableStateOf(false) }
    var sensitiveAllowed by remember { mutableStateOf(ShizukuManager.isSensitiveNotificationAccessAllowed(context)) }
    // One tap of either setup button: check Shizuku is ready, ask for its permission, or run the chosen setup.
    fun runSetup(scopeToGrant: ShizukuManager.SetupScope) {
        when {
            !ShizukuManager.isInstalled(context) || !ShizukuManager.isBinderAvailable() -> {
                Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, context.getString(R.string.shizuku_success), Toast.LENGTH_LONG).show()
                        isOemAutostartEnabled = true
                        isOverlayWarningDisabled = true
                        sensitiveAllowed = ShizukuManager.isSensitiveNotificationAccessAllowed(context)
                        onRefreshPermissions()
                        // Setup is the moment to find out which Shizuku methods work on this phone.
                        showCheckDialog = true
                    }.onFailure { err ->
                        Toast.makeText(context, context.getString(R.string.shizuku_failed, err.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    // Whether the app may read notifications Android marks as sensitive; read again every time the screen comes back.
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlashOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.shizuku_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val shizukuStateText = when {
                                !ShizukuManager.isInstalled(context) -> "Not Installed"
                                !ShizukuManager.isBinderAvailable() -> "Shizuku Not Running"
                                !ShizukuManager.hasPermission() -> "Permission Required"
                                else -> "Ready to Auto-Grant"
                            }
                            Text(
                                text = shizukuStateText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (ShizukuManager.hasPermission()) Color(0xFF0F9F6E) else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.shizuku_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                SetupOption(
                    title = stringResource(R.string.shizuku_option_necessary_title),
                    description = stringResource(R.string.shizuku_option_necessary_desc),
                    buttonText = stringResource(
                        if (runningScope == ShizukuManager.SetupScope.Necessary) R.string.shizuku_btn_running
                        else R.string.shizuku_option_necessary_btn
                    ),
                    filled = true,
                    enabled = !isExecutingShizuku,
                    onClick = { runSetup(ShizukuManager.SetupScope.Necessary) }
                )
                SetupOption(
                    title = stringResource(R.string.shizuku_option_all_title),
                    description = stringResource(R.string.shizuku_option_all_desc),
                    buttonText = stringResource(
                        if (runningScope == ShizukuManager.SetupScope.All) R.string.shizuku_btn_running
                        else R.string.shizuku_option_all_btn
                    ),
                    filled = false,
                    enabled = !isExecutingShizuku,
                    onClick = { runSetup(ShizukuManager.SetupScope.All) }
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRepairingAccessibility && !isExecutingShizuku,
                    onClick = {
                        when {
                            !ShizukuManager.isBinderAvailable() -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.shizuku_not_running),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            !ShizukuManager.hasPermission() -> {
                                ShizukuManager.requestPermission()
                            }
                            else -> {
                                isRepairingAccessibility = true
                                scope.launch {
                                    val result = ShizukuManager.repairAccessibility(context)
                                    isRepairingAccessibility = false
                                    result.onSuccess {
                                        Toast.makeText(
                                            context,
                                            "Accessibility service repaired via Shizuku!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onRefreshPermissions()
                                    }.onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            "Repair failed: ${err.localizedMessage ?: "Unknown error"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isRepairingAccessibility) "Repairing Accessibility..." else "Repair Accessibility (Shizuku)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    enabled = !isExecutingShizuku,
                    onClick = {
                        when {
                            !ShizukuManager.isInstalled(context) || !ShizukuManager.isBinderAvailable() -> {
                                Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_LONG).show()
                            }
                            !ShizukuManager.hasPermission() -> ShizukuManager.requestPermission()
                            else -> {
                                isExecutingShizuku = true
                                scope.launch {
                                    // On success the app is force-stopped and relaunched right after the toast.
                                    val result = ShizukuManager.grantSensitiveNotificationAccessAndRestart(context) {
                                        withContext(Dispatchers.Main) {
                                            sensitiveAllowed = ShizukuManager.isSensitiveNotificationAccessAllowed(context)
                                            Toast.makeText(context, context.getString(R.string.shizuku_sensitive_done), Toast.LENGTH_LONG).show()
                                        }
                                        delay(600)
                                    }
                                    isExecutingShizuku = false
                                    sensitiveAllowed = ShizukuManager.isSensitiveNotificationAccessAllowed(context)
                                    result.onFailure { err ->
                                        Toast.makeText(context, context.getString(R.string.shizuku_failed, err.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.shizuku_btn_sensitive),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = stringResource(
                        if (sensitiveAllowed) R.string.sensitive_status_allowed else R.string.sensitive_status_not_allowed
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (sensitiveAllowed) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error
                )
                OutlinedButton(
                    enabled = !isExecutingShizuku && !isChecking,
                    onClick = {
                        when {
                            !ShizukuManager.isInstalled(context) || !ShizukuManager.isBinderAvailable() -> {
                                Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_LONG).show()
                            }
                            !ShizukuManager.hasPermission() -> ShizukuManager.requestPermission()
                            else -> showCheckDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(if (isChecking) R.string.shizuku_check_running else R.string.shizuku_check_button),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = stringResource(R.string.shizuku_check_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                if (checkHasRun) {
                    ShizukuMethodCheck.Method.entries.forEach { method ->
                        val outcome = checkOutcomes[method] ?: ShizukuMethodCheck.Outcome.NotTested
                        Text(
                            text = stringResource(
                                when (outcome) {
                                    ShizukuMethodCheck.Outcome.Works -> R.string.shizuku_check_line_works
                                    ShizukuMethodCheck.Outcome.Missing -> R.string.shizuku_check_line_missing
                                    ShizukuMethodCheck.Outcome.NotTested -> R.string.shizuku_check_line_not_tested
                                },
                                stringResource(shizukuMethodLabel(method))
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (outcome) {
                                ShizukuMethodCheck.Outcome.Works -> Color(0xFF2E9E5B)
                                ShizukuMethodCheck.Outcome.Missing -> MaterialTheme.colorScheme.error
                                ShizukuMethodCheck.Outcome.NotTested -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        if (showCheckDialog) {
            AlertDialog(
                onDismissRequest = { showCheckDialog = false },
                title = { Text(stringResource(R.string.shizuku_check_dialog_title)) },
                text = { Text(stringResource(R.string.shizuku_check_dialog_body)) },
                confirmButton = {
                    TextButton(onClick = { runMethodChecks(testHotspot = true) }) {
                        Text(stringResource(R.string.shizuku_check_dialog_all))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { runMethodChecks(testHotspot = false) }) {
                        Text(stringResource(R.string.shizuku_check_dialog_skip_hotspot))
                    }
                }
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

        val warningIconColor = if (isOverlayWarningDisabled) Color(0xFF0F9F6E) else MaterialTheme.colorScheme.onSurfaceVariant
        val warningBgColor = if (isOverlayWarningDisabled) Color(0xFF0F9F6E).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
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
                        Text(if (isOverlayWarningDisabled) "Open" else "Hide Alert", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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

        val oemIconColor = if (isOemAutostartEnabled) Color(0xFF0F9F6E) else MaterialTheme.colorScheme.tertiary
        val oemBgColor = if (isOemAutostartEnabled) Color(0xFF0F9F6E).copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
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
                                            Toast.makeText(context, "OEM autostart granted via Shizuku!", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
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
}

private fun shizukuMethodLabel(method: ShizukuMethodCheck.Method): Int = when (method) {
    ShizukuMethodCheck.Method.Shell -> R.string.shizuku_method_shell
    ShizukuMethodCheck.Method.WifiData -> R.string.shizuku_method_wifi_data
    ShizukuMethodCheck.Method.ScreenRecorder -> R.string.shizuku_method_screen_recorder
    ShizukuMethodCheck.Method.TrimCache -> R.string.shizuku_method_trim_cache
    ShizukuMethodCheck.Method.Hotspot -> R.string.shizuku_method_hotspot
}

@Composable
private fun SetupOption(
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
