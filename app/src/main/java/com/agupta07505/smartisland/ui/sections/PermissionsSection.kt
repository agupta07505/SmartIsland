/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import com.agupta07505.smartisland.ui.semantic
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.ClipData
import android.content.ClipboardManager
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.ui.PermissionCard
import com.agupta07505.smartisland.ui.components.SmartIslandSwitch
import com.agupta07505.smartisland.util.HeadsUpNotificationManager
import com.agupta07505.smartisland.util.OemAutostartUtil
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.safeStartActivity
import kotlinx.coroutines.launch

@Composable
fun PermissionsSection(
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean = false,
    onOverlayClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onRefreshPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExecutingShizuku by remember { mutableStateOf(false) }
    var isOemAutostartEnabled by remember { mutableStateOf(batteryIgnored) }
    var isOverlayWarningDisabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Shizuku 1-Tap Auto Setup Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
                                fontWeight = FontWeight.SemiBold,
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
                                color = if (ShizukuManager.hasPermission()) MaterialTheme.semantic.success else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        enabled = !isExecutingShizuku,
                        onClick = {
                            when {
                                !ShizukuManager.isInstalled(context) -> {
                                    Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_LONG).show()
                                }
                                !ShizukuManager.isBinderAvailable() -> {
                                    Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_LONG).show()
                                }
                                !ShizukuManager.hasPermission() -> {
                                    ShizukuManager.requestPermission()
                                }
                                else -> {
                                    isExecutingShizuku = true
                                    scope.launch {
                                        val result = ShizukuManager.autoGrantAllPermissions(context)
                                        isExecutingShizuku = false
                                        result.onSuccess { msg ->
                                            Toast.makeText(context, context.getString(R.string.shizuku_success), Toast.LENGTH_LONG).show()
                                            isOemAutostartEnabled = true
                                            isOverlayWarningDisabled = true
                                            onRefreshPermissions()
                                        }.onFailure { err ->
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
                            text = if (isExecutingShizuku) stringResource(R.string.shizuku_btn_running) else stringResource(R.string.shizuku_btn_run),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.shizuku_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        // Required Permission 1: Accessibility
        PermissionCard(
            title = stringResource(R.string.perm_accessibility_title),
            description = stringResource(R.string.perm_accessibility_desc),
            granted = overlayGranted,
            buttonText = stringResource(R.string.btn_grant),
            onClick = onOverlayClick
        )

        // Required Permission 2: Notification Listener
        PermissionCard(
            title = stringResource(R.string.perm_notification_title),
            description = stringResource(R.string.perm_notification_desc),
            granted = notificationGranted,
            buttonText = stringResource(R.string.btn_grant),
            onClick = onNotificationClick
        )

        // Recommended Permission 3: Battery Optimization
        PermissionCard(
            title = stringResource(R.string.perm_battery_title),
            description = stringResource(R.string.perm_battery_desc),
            granted = batteryIgnored,
            buttonText = stringResource(R.string.btn_grant),
            onClick = onBatteryClick
        )

        // Recommended: OEM background lock, pop-up notifications off, permission alerts (Rule_Book 7.2, 10.2)
        OnboardingExtrasSection()

        // Overlay System Warning Card
        val warningIconColor = if (isOverlayWarningDisabled) MaterialTheme.semantic.success else MaterialTheme.colorScheme.onSurfaceVariant
        val warningBgColor = if (isOverlayWarningDisabled) MaterialTheme.semantic.success.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = null,
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
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isOverlayWarningDisabled) {
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.semantic.success.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Configured",
                                        color = MaterialTheme.semantic.success,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
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

        // OEM Autostart & Kill Protection Card
        val oemIconColor = if (isOemAutostartEnabled) MaterialTheme.semantic.success else MaterialTheme.colorScheme.tertiary
        val oemBgColor = if (isOemAutostartEnabled) MaterialTheme.semantic.success.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = null,
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
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isOemAutostartEnabled) {
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.semantic.success.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Configured",
                                        color = MaterialTheme.semantic.success,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
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
                                Text("Shizuku", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

        // OEM Lock in Background Interactive Guide (Item 11)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Lock in Background Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pin app in Recents to prevent OEM termination",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Android does not provide an API to lock apps programmatically. Follow these steps in Recents:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "1. Open your Recent Apps (swipe up & hold or tap Recents button)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "2. Press and hold Smart Island\'s app card or tap the app icon header",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "3. Tap \'Lock\' or the Padlock 🔒 icon so the lock icon appears",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "This tells the system memory cleaner never to clear Smart Island when tapping \'Clear All\'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }

        // Heads-Up Notification (HUN) Suppression Card (Item 8)
        val lifecycleOwner = LocalLifecycleOwner.current
        var writeSecureGranted by remember { mutableStateOf(HeadsUpNotificationManager.hasWriteSecureSettings(context)) }
        var hunSuppressed by remember { mutableStateOf(HeadsUpNotificationManager.isHunSuppressed(context)) }

        DisposableEffect(lifecycleOwner, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    writeSecureGranted = HeadsUpNotificationManager.hasWriteSecureSettings(context)
                    hunSuppressed = HeadsUpNotificationManager.isHunSuppressed(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Suppress Pop-up Banners",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hides heads-up banners so only the island renders notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    SmartIslandSwitch(
                        checked = hunSuppressed,
                        enabled = writeSecureGranted,
                        onCheckedChange = { suppress ->
                            val ok = HeadsUpNotificationManager.setHunSuppressed(context, suppress)
                            if (ok) {
                                hunSuppressed = HeadsUpNotificationManager.isHunSuppressed(context)
                            } else {
                                android.widget.Toast.makeText(context, "Failed to update heads-up setting", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                if (!writeSecureGranted) {
                    val command = HeadsUpNotificationManager.adbGrantCommand(context)
                    Text(
                        text = "Requires WRITE_SECURE_SETTINGS via ADB or Shizuku:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = command,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("adb grant", command))
                            android.widget.Toast.makeText(context, "Command copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Command", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
