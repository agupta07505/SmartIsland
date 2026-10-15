/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Exempt apps: while one of the chosen apps is in the foreground the island is totally off (no pill, no widgets, no cards).
 * One list per orientation ([landscape] picks which one this card edits), so an app can be exempt in portrait only, in landscape only, or both.
 */
@Composable
fun ExemptAppsCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    landscape: Boolean
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val staticApps = if (landscape) emptySet() else settings.islandExemptAppsStatic
    val exempt = (if (landscape) settings.islandExemptAppsLandscape else settings.islandExemptApps) + staticApps
    var picking by rememberSaveable(landscape) { mutableStateOf(false) }
    val installed by produceState(initialValue = emptyList<LaunchableApp>(), context) {
        value = withContext(Dispatchers.IO) { AppShortcutProvider.installedApps(context) }
    }
    val labels = remember(installed) { installed.associate { it.packageName to it.label } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (landscape) {
                "The island is totally off in landscape while any of these apps is open."
            } else {
                "While any of these apps is open the island is either off (hidden, no popups, no gestures) or static (the collapsed island stays in place, but does nothing)."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (exempt.isEmpty()) {
            Text("No app selected.", style = MaterialTheme.typography.bodyMedium)
        } else {
            exempt.sortedBy { (labels[it] ?: it).lowercase() }.forEach { pkg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(labels[pkg] ?: pkg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (labels[pkg] != null) {
                            Text(
                                pkg, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TextButton(onClick = { scope.launch { repository.setIslandExemptApp(landscape, pkg, false) } }) { Text("Remove") }
                }
                if (!landscape) {
                    val isStatic = pkg in staticApps
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isStatic,
                            onClick = { scope.launch { repository.setIslandExemptStatic(pkg, false) } },
                            label = { Text("Island off") }
                        )
                        FilterChip(
                            selected = isStatic,
                            onClick = { scope.launch { repository.setIslandExemptStatic(pkg, true) } },
                            label = { Text("Static island") }
                        )
                    }
                }
            }
        }
        OutlinedButton(onClick = { picking = true }) { Text("Select apps") }
    }

    if (picking) {
        var query by rememberSaveable { mutableStateOf("") }
        val shown = remember(installed, query) {
            if (query.isBlank()) installed
            else installed.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(if (landscape) "Exempt apps (landscape)" else "Exempt apps (portrait)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Search apps") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                        items(shown, key = { it.packageName }) { app ->
                            val checked = app.packageName in exempt
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { scope.launch { repository.setIslandExemptApp(landscape, app.packageName, !checked) } }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { scope.launch { repository.setIslandExemptApp(landscape, app.packageName, it) } }
                                )
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Done", fontWeight = FontWeight.Bold) } }
        )
    }
}
