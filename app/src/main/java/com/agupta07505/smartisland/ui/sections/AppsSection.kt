/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.util.AppAutoExpand
import com.agupta07505.smartisland.util.AppFlags
import com.agupta07505.smartisland.util.AppTag
import com.agupta07505.smartisland.util.AppsFilter
import com.agupta07505.smartisland.util.AppsRules
import com.agupta07505.smartisland.util.BulkAppAction
import com.agupta07505.smartisland.util.NotificationFilter
import com.agupta07505.smartisland.util.quietly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Apps that can post island notifications, sorted by name. */
@Composable
internal fun rememberEligibleApps(): State<List<LaunchableApp>> {
    val context = LocalContext.current
    return produceState(initialValue = emptyList<LaunchableApp>(), context) {
        value = withContext(Dispatchers.IO) {
            val apps = AppShortcutProvider.installedApps(context)
                .filter { NotificationFilter.isAppEligibleForIsland(it.packageName, context.packageManager) }
            val dmPackage = "com.android.providers.downloads"
            if (apps.any { it.packageName == dmPackage }) {
                apps
            } else {
                val label = quietly {
                    val pm = context.packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(dmPackage, 0)).toString()
                }?.takeIf { it.isNotBlank() } ?: "Download Manager"
                (apps + LaunchableApp(packageName = dmPackage, label = label))
                    .sortedBy { it.label.lowercase() }
            }
        }
    }
}

/** Settings > Apps: every app that can post island notifications. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installedApps by rememberEligibleApps()

    var query by rememberSaveable { mutableStateOf("") }
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val filter = AppsFilter.entries[filterIndex.coerceIn(0, AppsFilter.entries.lastIndex)]
    val sheetPackageState = rememberSaveable { mutableStateOf<String?>(null) }
    var sheetPackage by sheetPackageState
    val selectingState = rememberSaveable { mutableStateOf(false) }
    var selecting by selectingState
    val selectedState = remember { mutableStateOf(emptySet<String>()) }
    var selected by selectedState

    val flagsByPackage = remember(installedApps, settings) {
        installedApps.associate { it.packageName to AppsRules.flagsFor(settings, it.packageName) }
    }
    val counts = remember(flagsByPackage) { AppsRules.counts(flagsByPackage.values.toList()) }
    val visibleApps = remember(installedApps, flagsByPackage, query, filter) {
        installedApps.filter { app ->
            AppsRules.matchesQuery(app.label, app.packageName, query) &&
                AppsRules.matches(filter, flagsByPackage[app.packageName] ?: AppFlags())
        }
    }

    fun endSelection() {
        selecting = false
        selected = emptySet()
    }

    BackHandler(enabled = selecting) { endSelection() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppsSearchRow(
            query = query,
            onQueryChange = { query = it },
            appCount = installedApps.size,
            selecting = selecting,
            onToggleSelecting = { if (selecting) endSelection() else selecting = true }
        )

        AppsFilterRow(
            filter = filter,
            counts = counts,
            onFilterSelected = { filterIndex = it.ordinal }
        )

        AppsListBody(
            visibleApps = visibleApps,
            query = query,
            flagsByPackage = flagsByPackage,
            selectingState = selectingState,
            selectedState = selectedState,
            sheetPackageState = sheetPackageState,
            scope = scope,
            repository = repository
        )

        if (selecting) {
            SelectionBar(
                count = selected.size,
                onAction = { action ->
                    val targets = selected
                    if (targets.isNotEmpty()) {
                        scope.launch { repository.applyToApps(targets, action) }
                        context.showToast(context.getString(R.string.apps_updated_toast, targets.size))
                        endSelection()
                    }
                }
            )
        }
    }

    val sheetApp = sheetPackage?.let { pkg -> installedApps.firstOrNull { it.packageName == pkg } }
    if (sheetApp != null) {
        val resetToast = stringResource(R.string.apps_reset_toast)
        AppSheet(
            app = sheetApp,
            flags = flagsByPackage[sheetApp.packageName] ?: AppFlags(),
            repository = repository,
            onDismiss = { sheetPackage = null },
            onReset = {
                scope.launch { repository.resetApps(listOf(sheetApp.packageName)) }
                context.showToast(resetToast)
            }
        )
    } else if (sheetPackage != null && installedApps.isNotEmpty()) {
        // The app list loaded without the remembered app (uninstalled meanwhile)
        sheetPackage = null
    }
}

@Composable
private fun AppsSearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    appCount: Int,
    selecting: Boolean,
    onToggleSelecting: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.apps_search_hint, appCount)) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onToggleSelecting) {
            Text(
                text = stringResource(if (selecting) R.string.apps_cancel_select else R.string.apps_select),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AppsFilterRow(
    filter: AppsFilter,
    counts: Map<AppsFilter, Int>,
    onFilterSelected: (AppsFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppsFilter.entries.forEach { entry ->
            val label = stringResource(filterLabel(entry), counts[entry] ?: 0)
            FilterChip(
                selected = filter == entry,
                onClick = { onFilterSelected(entry) },
                label = { Text(label, fontSize = 12.sp, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun ColumnScope.AppsListBody(
    visibleApps: List<LaunchableApp>,
    query: String,
    flagsByPackage: Map<String, AppFlags>,
    selectingState: MutableState<Boolean>,
    selectedState: MutableState<Set<String>>,
    sheetPackageState: MutableState<String?>,
    scope: CoroutineScope,
    repository: SmartIslandSettingsRepository
) {
    var selecting by selectingState
    var selected by selectedState
    var sheetPackage by sheetPackageState

    if (visibleApps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = if (query.isBlank()) stringResource(R.string.no_installed_apps_found)
                else stringResource(R.string.no_apps_matching_query, query),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(visibleApps, key = { it.packageName }) { app ->
                val flags = flagsByPackage[app.packageName] ?: AppFlags()
                AppRow(
                    app = app,
                    flags = flags,
                    selecting = selecting,
                    isSelected = app.packageName in selected,
                    onClick = {
                        if (selecting) {
                            selected = if (app.packageName in selected) selected - app.packageName
                            else selected + app.packageName
                        } else {
                            sheetPackage = app.packageName
                        }
                    },
                    onLongClick = {
                        if (!selecting) {
                            selecting = true
                            selected = setOf(app.packageName)
                        }
                    },
                    onBell = { scope.launch { repository.setAppExcluded(app.packageName, !flags.excluded) } },
                    onSpeaker = { scope.launch { repository.setAppMuted(app.packageName, !flags.muted) } },
                    onCard = { scope.launch { repository.setAppNoCard(app.packageName, !flags.noCard) } }
                )
            }
        }
    }
}

private fun filterLabel(filter: AppsFilter): Int = when (filter) {
    AppsFilter.All -> R.string.apps_chip_all
    AppsFilter.Customized -> R.string.apps_chip_customized
    AppsFilter.Muted -> R.string.apps_chip_muted
    AppsFilter.Excluded -> R.string.apps_chip_excluded
    AppsFilter.NeverExpands -> R.string.apps_chip_never_expands
}

private fun tagLabel(tag: AppTag): Int = when (tag) {
    AppTag.Excluded -> R.string.apps_tag_excluded
    AppTag.Muted -> R.string.apps_tag_muted
    AppTag.NoCard -> R.string.apps_tag_no_card
    AppTag.AlwaysExpands -> R.string.apps_tag_always_expands
    AppTag.NoAutoExpand -> R.string.apps_tag_no_auto_expand
    AppTag.HiddenOnLock -> R.string.apps_tag_hidden_on_lock
    AppTag.NoCooldown -> R.string.apps_tag_no_cooldown
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: LaunchableApp,
    flags: AppFlags,
    selecting: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onBell: () -> Unit,
    onSpeaker: () -> Unit,
    onCard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selecting) {
            // The whole row is the click target
            Checkbox(checked = isSelected, onCheckedChange = null)
        }
        AppIcon(app = app, size = 40)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TagRow(flags)
        }

        if (!selecting) {
            val bellOn = !flags.excluded
            val speakerOn = AppsRules.soundOn(flags)
            val cardOn = !flags.noCard && bellOn
            val scheme = MaterialTheme.colorScheme
            val dimmed = scheme.onSurfaceVariant.copy(alpha = 0.35f)
            AppToggleIcon(
                onClick = onBell,
                imageVector = if (bellOn) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                contentDescription = stringResource(R.string.option_island_notifications),
                tint = if (bellOn) scheme.primary else scheme.error,
                iconSize = 22.dp
            )
            // Sound only matters while the app may notify
            AppToggleIcon(
                onClick = onSpeaker,
                enabled = bellOn,
                imageVector = if (speakerOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                contentDescription = stringResource(R.string.option_island_sound),
                tint = when {
                    !bellOn -> dimmed
                    speakerOn -> scheme.primary
                    else -> scheme.tertiary
                },
                iconSize = 22.dp
            )
            AppToggleIcon(
                onClick = onCard,
                enabled = bellOn,
                imageVector = Icons.Rounded.OpenInFull,
                contentDescription = stringResource(R.string.option_expanded_card),
                tint = when {
                    !bellOn -> dimmed
                    cardOn -> scheme.primary
                    else -> scheme.tertiary
                },
                iconSize = 20.dp
            )
        }
    }
}

@Composable
private fun AppToggleIcon(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color,
    iconSize: Dp,
    enabled: Boolean = true
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun TagRow(flags: AppFlags) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (flags.isDefault) {
            NeutralTag(stringResource(R.string.apps_tag_default))
        } else {
            val visible = AppsRules.visibleTags(AppsRules.tagsFor(flags))
            visible.shown.forEach { tag ->
                val (container, content) = tagColors(tag)
                StatusTag(text = stringResource(tagLabel(tag)), container = container, content = content)
            }
            if (visible.overflow > 0) {
                NeutralTag("+${visible.overflow}")
            }
        }
    }
}

@Composable
private fun tagColors(tag: AppTag): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (tag) {
        AppTag.Excluded -> scheme.errorContainer.copy(alpha = 0.7f) to scheme.onErrorContainer
        AppTag.Muted -> scheme.tertiaryContainer.copy(alpha = 0.7f) to scheme.onTertiaryContainer
        else -> scheme.primaryContainer.copy(alpha = 0.6f) to scheme.onPrimaryContainer
    }
}

@Composable
private fun NeutralTag(text: String) {
    StatusTag(
        text = text,
        container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        content = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StatusTag(
    text: String,
    container: Color,
    content: Color
) {
    Surface(shape = RoundedCornerShape(6.dp), color = container) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun AppIcon(app: LaunchableApp, size: Int) {
    AppAvatar(
        packageName = app.packageName,
        label = app.label,
        size = size.dp,
        shape = RoundedCornerShape((size / 4).dp),
        letterSize = (size / 3).sp
    )
}

@Composable
private fun SelectionBar(count: Int, onAction: (BulkAppAction) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.apps_n_selected, count),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            val enabled = count > 0
            FilledTonalButton(
                onClick = { onAction(BulkAppAction.Mute) },
                enabled = enabled,
                shape = RoundedCornerShape(10.dp)
            ) { Text(stringResource(R.string.apps_action_mute), fontSize = 12.sp) }
            Button(
                onClick = { onAction(BulkAppAction.Exclude) },
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(stringResource(R.string.apps_action_exclude), fontSize = 12.sp) }
            OutlinedButton(
                onClick = { onAction(BulkAppAction.Reset) },
                enabled = enabled,
                shape = RoundedCornerShape(10.dp)
            ) { Text(stringResource(R.string.apps_action_reset), fontSize = 12.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSheet(
    app: LaunchableApp,
    flags: AppFlags,
    repository: SmartIslandSettingsRepository,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val notificationsOn = !flags.excluded
    val cardMayExpand = !flags.noCard

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppSheetHeader(app)

            SettingsDivider()

            AppSheetSwitches(
                app = app,
                flags = flags,
                notificationsOn = notificationsOn,
                cardMayExpand = cardMayExpand,
                repository = repository,
                scope = scope
            )

            AppSheetAutoExpand(
                app = app,
                flags = flags,
                notificationsOn = notificationsOn,
                cardMayExpand = cardMayExpand,
                repository = repository,
                scope = scope
            )

            ToggleRowItem(
                title = stringResource(R.string.option_hide_on_lock_screen),
                subtitle = stringResource(R.string.option_hide_on_lock_screen_desc),
                icon = Icons.Rounded.VisibilityOff,
                checked = flags.hiddenOnLock,
                onCheckedChange = { scope.launch { repository.setAppHiddenOnLockScreen(app.packageName, it) } }
            )
            ToggleRowItem(
                title = stringResource(R.string.option_cooldown_bypass),
                subtitle = stringResource(R.string.option_cooldown_bypass_desc),
                icon = Icons.Rounded.HourglassBottom,
                checked = flags.noCooldown,
                onCheckedChange = { scope.launch { repository.setAppCooldownBypass(app.packageName, it) } }
            )

            AppSheetActions(flags = flags, onReset = onReset, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun AppSheetHeader(app: LaunchableApp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppIcon(app = app, size = 48)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.apps_sheet_saves_instantly),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppSheetSwitches(
    app: LaunchableApp,
    flags: AppFlags,
    notificationsOn: Boolean,
    cardMayExpand: Boolean,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    ToggleRowItem(
        title = stringResource(R.string.option_island_notifications),
        subtitle = if (notificationsOn) stringResource(R.string.option_island_notifications_desc)
        else stringResource(R.string.tag_excluded_from_island),
        icon = if (notificationsOn) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
        iconColor = if (notificationsOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        checked = notificationsOn,
        onCheckedChange = { on -> scope.launch { repository.setAppExcluded(app.packageName, !on) } }
    )
    ToggleRowItem(
        title = stringResource(R.string.option_island_sound),
        subtitle = if (!flags.muted) stringResource(R.string.option_island_sound_desc)
        else stringResource(R.string.tag_sound_muted),
        icon = if (!flags.muted) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
        checked = AppsRules.soundOn(flags),
        onCheckedChange = { on -> scope.launch { repository.setAppMuted(app.packageName, !on) } }
    )
    ToggleRowItem(
        title = stringResource(R.string.option_expanded_card),
        subtitle = if (cardMayExpand) stringResource(R.string.option_expanded_card_desc)
        else stringResource(R.string.tag_card_never_expands),
        icon = Icons.Rounded.OpenInFull,
        checked = cardMayExpand && notificationsOn,
        onCheckedChange = { on -> scope.launch { repository.setAppNoCard(app.packageName, !on) } }
    )
}

@Composable
private fun AppSheetAutoExpand(
    app: LaunchableApp,
    flags: AppFlags,
    notificationsOn: Boolean,
    cardMayExpand: Boolean,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    // auto-expand: Loud only is the default for every app
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.option_auto_expand),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = when {
                !(cardMayExpand && notificationsOn) -> stringResource(R.string.tag_card_never_expands)
                flags.autoExpand == AppAutoExpand.NEVER -> stringResource(R.string.tag_no_auto_expand)
                flags.autoExpand == AppAutoExpand.ALWAYS -> stringResource(R.string.option_auto_expand_always_desc)
                else -> stringResource(R.string.option_auto_expand_desc)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                AppAutoExpand.DEFAULT to R.string.auto_expand_choice_default,
                AppAutoExpand.ALWAYS to R.string.auto_expand_choice_always,
                AppAutoExpand.NEVER to R.string.auto_expand_choice_never
            ).forEach { (choice, label) ->
                PrivacySegmentButton(
                    label = stringResource(label),
                    selected = flags.autoExpand == choice,
                    onClick = { scope.launch { repository.setAppAutoExpand(app.packageName, choice) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AppSheetActions(
    flags: AppFlags,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            enabled = !flags.isDefault,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.apps_reset_app),
                color = if (flags.isDefault) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                else MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.apps_sheet_done), fontWeight = FontWeight.Bold)
        }
    }
}
