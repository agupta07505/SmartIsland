/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.pickers

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.UtilityGrid
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.model.UtilityPlacement
import com.agupta07505.smartisland.model.UtilityType
import com.agupta07505.smartisland.ui.expanded.utilityIcon
import com.agupta07505.smartisland.ui.expanded.utilityLabelRes
import com.agupta07505.smartisland.util.UtilityActions
import com.agupta07505.smartisland.util.UtilityWidgets
import com.agupta07505.smartisland.util.quietly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class PendingWidget(val id: Int, val info: AppWidgetProviderInfo, val row: Int, val col: Int)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun firstEmptyUtilityCell(layout: List<UtilityPlacement>): Pair<Int, Int>? =
    (0 until UtilityGrid.TOTAL_ROWS).firstNotNullOfOrNull { r ->
        (0 until UtilityGrid.COLUMNS).firstOrNull { c -> UtilityLayout.itemAt(layout, r, c) == null }
            ?.let { c -> r to c }
    }

/** Tapped cell if a 1x1 widget fits, else the first free cell, else null (no room). */
internal fun widgetSpot(layout: List<UtilityPlacement>, row: Int, col: Int, widgetId: Int): Pair<Int, Int>? {
    val here = UtilityPlacement(UtilityType.AppWidget, row, col, widgetId = widgetId)
    return if (UtilityLayout.canPlace(layout, here)) row to col else UtilityLayout.firstFreeSpot(layout)
}

private const val NO_WIDGET = -1

private enum class AddStage { Kind, Widget }

/**
 * Utilities list, then for "App widget" the installed widgets, the bind permission and the widget's own setup screen
 */
@Composable
fun UtilityAddFlow(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    row: Int,
    col: Int,
    onPlaced: (String) -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layout = remember(settings.utilityLayout) { UtilityLayout.decode(settings.utilityLayout) }
    // The setup screen answers later, so finishing reads the layout as it is then
    val latestLayout by rememberUpdatedState(layout)
    var stage by remember { mutableStateOf(AddStage.Kind) }
    var pending by remember { mutableStateOf<PendingWidget?>(null) }
    // Survives the app being killed while the bind / setup screen is in front
    var savedPendingId by rememberSaveable { mutableIntStateOf(NO_WIDGET) }
    val restoredPendingId = remember { savedPendingId }
    LaunchedEffect(restoredPendingId) {
        if (restoredPendingId != NO_WIDGET) {
            UtilityWidgets.deleteId(context, restoredPendingId)
            savedPendingId = NO_WIDGET
        }
    }
    fun setPending(p: PendingWidget?) {
        pending = p
        savedPendingId = p?.id ?: NO_WIDGET
    }

    fun place(placed: UtilityPlacement, base: List<UtilityPlacement>) {
        scope.launch {
            repository.setUtilityLayout(UtilityLayout.add(base, placed))
            onPlaced(placed.key)
            onClose()
        }
    }

    fun finishWidget(p: PendingWidget) {
        setPending(null)
        // With no room at all the widget id is released and the person is told why
        val current = latestLayout
        val spot = widgetSpot(current, p.row, p.col, p.id)
        if (spot == null) {
            UtilityWidgets.deleteId(context, p.id)
            Toast.makeText(context, R.string.utilities_editor_full, Toast.LENGTH_SHORT).show()
            onClose()
            return
        }
        place(UtilityPlacement(UtilityType.AppWidget, spot.first, spot.second, widgetId = p.id), current)
    }

    fun cancelWidget(p: PendingWidget) {
        setPending(null)
        UtilityWidgets.deleteId(context, p.id)
        onClose()
    }

    fun afterBound(p: PendingWidget) {
        val activity = context.findActivity()
        if (p.info.configure != null && activity != null) {
            UtilityWidgets.configure(activity, p.id) { ok -> if (ok) finishWidget(p) else cancelWidget(p) }
        } else {
            finishWidget(p)
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val p = pending ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) afterBound(p) else cancelWidget(p)
    }

    fun startAddingWidget(info: AppWidgetProviderInfo) {
        val id = UtilityWidgets.allocateId(context)
        val p = PendingWidget(id, info, row, col)
        setPending(p)
        if (UtilityWidgets.bindIfAllowed(context, id, info.provider)) afterBound(p)
        else bindLauncher.launch(UtilityWidgets.bindIntent(id, info.provider))
    }

    // The window can be swapped for the other picker while a widget is half added, its id must not stay allocated
    val latestPending by rememberUpdatedState(pending)
    DisposableEffect(Unit) {
        onDispose { latestPending?.let { UtilityWidgets.deleteId(context, it.id) } }
    }

    // Its own screen is in front, no dialog of ours is showing
    if (pending != null) return

    when (stage) {
        AddStage.Kind -> AlertDialog(
            onDismissRequest = onClose,
            title = { Text(stringResource(R.string.utilities_editor_add_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    UtilityLayout.addable(layout).forEach { type ->
                        AddRow(utilityLabelRes(type), onClick = { place(UtilityPlacement(type, row, col), layout) },
                            icon = { Icon(utilityIcon(type), contentDescription = null) })
                    }
                    AddRow(R.string.utilities_editor_add_widget, onClick = { stage = AddStage.Widget },
                        icon = { Icon(Icons.Rounded.Widgets, contentDescription = null) })
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.utilities_editor_cancel)) } }
        )

        AddStage.Widget -> {
            val providers by produceState(emptyList<AppWidgetProviderInfo>(), context) {
                value = withContext(Dispatchers.IO) { UtilityWidgets.installedProviders(context) }
            }
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text(stringResource(R.string.utilities_editor_widget_pick)) },
                text = {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        if (providers.isEmpty()) {
                            Text(stringResource(R.string.utilities_editor_widget_none),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        providers.forEach { info ->
                            val pm = context.packageManager
                            val appLabel = remember(info) {
                                quietly("") { pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName,
                                    0)).toString() }
                            }
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { startAddingWidget(info) }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(info.loadLabel(pm) ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(appLabel, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick =
                    onClose) { Text(stringResource(R.string.utilities_editor_cancel)) } }
            )
        }
    }
}

/** Choose which installed calculator the Calculator utility opens. */
@Composable
fun CalculatorAppDialog(repository: SmartIslandSettingsRepository, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val choices = remember { UtilityActions.calculatorApps(context) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.utilities_editor_calculator_app)) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                if (choices.isEmpty()) {
                    Text(stringResource(R.string.utility_no_calculator),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                choices.forEach { app: LaunchableApp ->
                    Text(
                        app.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { repository.setUtilityCalculatorPackage(app.packageName) }
                                onClose()
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.utilities_editor_cancel)) } }
    )
}

@Composable
private fun AddRow(labelRes: Int, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()
        Text(stringResource(labelRes))
    }
}
