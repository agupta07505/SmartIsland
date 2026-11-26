/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.ShizukuMethodCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ShizukuCard(
    context: Context,
    scope: CoroutineScope,
    runningScope: ShizukuManager.SetupScope?,
    isExecutingShizukuState: MutableState<Boolean>,
    isRepairingAccessibilityState: MutableState<Boolean>,
    isChecking: Boolean,
    sensitiveAllowedState: MutableState<Boolean>,
    showCheckDialogState: MutableState<Boolean>,
    checkOutcomes: Map<ShizukuMethodCheck.Method, ShizukuMethodCheck.Outcome>,
    checkHasRun: Boolean,
    runSetup: (ShizukuManager.SetupScope) -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val isExecutingShizuku by isExecutingShizukuState
    val sensitiveAllowed by sensitiveAllowedState
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
            ShizukuCardHeader(context)
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
            RepairAccessibilityButton(
                context = context,
                scope = scope,
                isRepairingAccessibilityState = isRepairingAccessibilityState,
                isExecutingShizuku = isExecutingShizuku,
                onRefreshPermissions = onRefreshPermissions
            )
            SensitiveAccessButton(
                context = context,
                scope = scope,
                isExecutingShizukuState = isExecutingShizukuState,
                sensitiveAllowedState = sensitiveAllowedState
            )
            Text(
                text = stringResource(
                    if (sensitiveAllowed) R.string.sensitive_status_allowed else R.string.sensitive_status_not_allowed
                ),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (sensitiveAllowed) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error
            )
            MethodCheckButton(
                context = context,
                isExecutingShizuku = isExecutingShizuku,
                isChecking = isChecking,
                showCheckDialogState = showCheckDialogState
            )
            Text(
                text = stringResource(R.string.shizuku_check_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
            MethodCheckResults(checkHasRun = checkHasRun, checkOutcomes = checkOutcomes)
        }
    }
}

@Composable
private fun ShizukuCardHeader(context: Context) {
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
}

@Composable
private fun RepairAccessibilityButton(
    context: Context,
    scope: CoroutineScope,
    isRepairingAccessibilityState: MutableState<Boolean>,
    isExecutingShizuku: Boolean,
    onRefreshPermissions: () -> Unit
) {
    var isRepairingAccessibility by isRepairingAccessibilityState
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = !isRepairingAccessibility && !isExecutingShizuku,
        onClick = {
            when {
                !ShizukuManager.isBinderAvailable() -> {
                    context.showToast(context.getString(R.string.shizuku_not_running), long = true)
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
                            context.showToast("Accessibility service repaired via Shizuku!")
                            onRefreshPermissions()
                        }.onFailure { err ->
                            context.showToast("Repair failed: ${err.localizedMessage ?: "Unknown error"}", long = true)
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
}

@Composable
private fun SensitiveAccessButton(
    context: Context,
    scope: CoroutineScope,
    isExecutingShizukuState: MutableState<Boolean>,
    sensitiveAllowedState: MutableState<Boolean>
) {
    var isExecutingShizuku by isExecutingShizukuState
    var sensitiveAllowed by sensitiveAllowedState
    OutlinedButton(
        enabled = !isExecutingShizuku,
        onClick = {
            when {
                !ShizukuManager.isInstalled(context) || !ShizukuManager.isBinderAvailable() -> {
                    context.showToast(context.getString(R.string.shizuku_not_running), long = true)
                }
                !ShizukuManager.hasPermission() -> ShizukuManager.requestPermission()
                else -> {
                    isExecutingShizuku = true
                    scope.launch {
                        // On success the app is force-stopped and relaunched right after the toast
                        val result = ShizukuManager.grantSensitiveNotificationAccessAndRestart(context) {
                            withContext(Dispatchers.Main) {
                                sensitiveAllowed = ShizukuManager.isSensitiveNotificationAccessAllowed(context)
                                context.showToast(context.getString(R.string.shizuku_sensitive_done), long = true)
                            }
                            delay(600)
                        }
                        isExecutingShizuku = false
                        sensitiveAllowed = ShizukuManager.isSensitiveNotificationAccessAllowed(context)
                        result.onFailure { err ->
                            context.showToast(
                                context.getString(R.string.shizuku_failed, err.localizedMessage ?: ""),
                                long = true
                            )
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
}

@Composable
private fun MethodCheckButton(
    context: Context,
    isExecutingShizuku: Boolean,
    isChecking: Boolean,
    showCheckDialogState: MutableState<Boolean>
) {
    var showCheckDialog by showCheckDialogState
    OutlinedButton(
        enabled = !isExecutingShizuku && !isChecking,
        onClick = {
            when {
                !ShizukuManager.isInstalled(context) || !ShizukuManager.isBinderAvailable() -> {
                    context.showToast(context.getString(R.string.shizuku_not_running), long = true)
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
}

@Composable
private fun MethodCheckResults(
    checkHasRun: Boolean,
    checkOutcomes: Map<ShizukuMethodCheck.Method, ShizukuMethodCheck.Outcome>
) {
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

@Composable
internal fun ShizukuCheckDialog(
    onDismiss: () -> Unit,
    onRunChecks: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shizuku_check_dialog_title)) },
        text = { Text(stringResource(R.string.shizuku_check_dialog_body)) },
        confirmButton = {
            TextButton(onClick = { onRunChecks(true) }) {
                Text(stringResource(R.string.shizuku_check_dialog_all))
            }
        },
        dismissButton = {
            TextButton(onClick = { onRunChecks(false) }) {
                Text(stringResource(R.string.shizuku_check_dialog_skip_hotspot))
            }
        }
    )
}

private fun shizukuMethodLabel(method: ShizukuMethodCheck.Method): Int = when (method) {
    ShizukuMethodCheck.Method.Shell -> R.string.shizuku_method_shell
    ShizukuMethodCheck.Method.WifiData -> R.string.shizuku_method_wifi_data
    ShizukuMethodCheck.Method.ScreenRecorder -> R.string.shizuku_method_screen_recorder
    ShizukuMethodCheck.Method.TrimCache -> R.string.shizuku_method_trim_cache
    ShizukuMethodCheck.Method.Hotspot -> R.string.shizuku_method_hotspot
}
