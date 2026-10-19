/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.service.ForegroundSecureDetector
import com.agupta07505.smartisland.ui.components.GroupedRow
import com.agupta07505.smartisland.ui.components.GroupedSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen allowing users to manage secure apps (Rule_Book §8.2).
 * When these apps are in foreground, auto-expansion of ephemeral alerts is disabled.
 */
@Composable
fun SecureAppsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAppPicker by remember { mutableStateOf(false) }

    val installedApps by produceState<List<Pair<String, String>>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName.contains("wallet") || it.packageName.contains("bank") }
                .map { appInfo ->
                    val label = pm.getApplicationLabel(appInfo).toString()
                    appInfo.packageName to label
                }
                .sortedBy { it.second.lowercase() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GroupedSection(
            header = "SECURE APP PASSTHROUGH",
            footer = "When these apps are in the foreground, Smart Island suppresses auto-expanding alert previews to protect your private data and respect secure screens."
        ) {
            GroupedRow(
                title = "Add Secure App",
                subtitle = "Select an installed app to treat as secure",
                icon = Icons.Rounded.Add,
                onClick = { showAppPicker = true }
            )
        }

        // User Custom Blacklist
        if (settings.secureAppPackages.isNotEmpty()) {
            GroupedSection(header = "USER-DEFINED SECURE APPS (${settings.secureAppPackages.size})") {
                settings.secureAppPackages.sorted().forEach { pkg ->
                    val appLabel = remember(pkg) {
                        try {
                            val pm = context.packageManager
                            val info = pm.getApplicationInfo(pkg, 0)
                            pm.getApplicationLabel(info).toString()
                        } catch (_: Throwable) {
                            pkg
                        }
                    }
                    GroupedRow(
                        title = appLabel,
                        subtitle = pkg,
                        icon = Icons.Rounded.Security,
                        trailing = {
                            IconButton(onClick = {
                                scope.launch { repository.removeSecureAppPackage(pkg) }
                            }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }
        }

        // Built-in Secure Apps (Read-only)
        GroupedSection(
            header = "SYSTEM BUILT-IN SECURE APPS (${ForegroundSecureDetector.BUILT_IN_SECURE_PACKAGES.size})",
            footer = "Standard banking, authenticator, password managers and streaming apps are automatically protected."
        ) {
            ForegroundSecureDetector.BUILT_IN_SECURE_PACKAGES.sorted().forEach { pkg ->
                GroupedRow(
                    title = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    subtitle = pkg,
                    icon = Icons.Rounded.Lock,
                    trailing = {
                        Text(
                            text = "Built-in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }

    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("Select App") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    val available = installedApps.filterNot { it.first in settings.secureAppPackages || it.first in ForegroundSecureDetector.BUILT_IN_SECURE_PACKAGES }
                    items(available) { (pkg, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        repository.addSecureAppPackage(pkg)
                                        showAppPicker = false
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(pkg, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}
