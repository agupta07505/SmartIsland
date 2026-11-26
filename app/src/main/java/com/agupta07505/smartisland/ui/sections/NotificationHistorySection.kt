/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.INotificationHistoryRepository
import com.agupta07505.smartisland.data.NotificationHistoryEntry
import com.agupta07505.smartisland.data.NotificationHistoryRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.di.SmartIslandRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppNotificationSummary(
    val packageName: String,
    val appName: String,
    val count: Int
)

private val AppSummarySaver: Saver<AppNotificationSummary?, Any> = listSaver(
    save = { summary ->
        if (summary == null) emptyList<Any>() else listOf(summary.packageName, summary.appName, summary.count)
    },
    restore = { saved ->
        if (saved.size < 3) null
        else AppNotificationSummary(saved[0] as String, saved[1] as String, saved[2] as Int)
    }
)

@Composable
fun NotificationHistorySection(
    settings: SmartIslandSettings = SmartIslandSettings.Default,
    repository: SmartIslandSettingsRepository? = null,
    historyRepository: INotificationHistoryRepository? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolvedHistoryRepo = remember(historyRepository, context) {
        historyRepository ?: runCatching {
            SmartIslandRepositories.historyRepository(context)
        }.getOrElse {
            NotificationHistoryRepository(context)
        }
    }

    val historyEntries by resolvedHistoryRepo.history.collectAsState(initial = emptyList())
    // Dialog and filter state is saved so rotating the phone keeps an open dialog open
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val selectedFilterPackageState = rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFilterPackage by selectedFilterPackageState
    val showClearAllConfirmState = rememberSaveable { mutableStateOf(false) }
    var showClearAllConfirm by showClearAllConfirmState
    val showAppFilterDialogState = rememberSaveable { mutableStateOf(false) }
    var showAppFilterDialog by showAppFilterDialogState
    val showDeleteByAppDialogState = rememberSaveable { mutableStateOf(false) }
    var showDeleteByAppDialog by showDeleteByAppDialogState
    val packageToDeleteConfirmState = rememberSaveable(stateSaver = AppSummarySaver) {
        mutableStateOf<AppNotificationSummary?>(null)
    }
    // Only the id is saved; the entry is looked up again in the live history list
    val selectedEntryIdForDetailsState = rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedEntryIdForDetails by selectedEntryIdForDetailsState
    val selectedEntryForDetails = selectedEntryIdForDetails?.let { id -> historyEntries.firstOrNull { it.id == id } }
    var retentionMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val appSummaries = remember(historyEntries) { summarizeByApp(historyEntries) }

    val filteredEntries = remember(historyEntries, searchQuery, selectedFilterPackage) {
        filterEntries(historyEntries, selectedFilterPackage, searchQuery)
    }

    val groupedByDay = remember(filteredEntries) {
        groupEntriesByDay(filteredEntries)
    }

    HistoryDialogs(
        historyEntries = historyEntries,
        appSummaries = appSummaries,
        selectedEntryForDetails = selectedEntryForDetails,
        resolvedHistoryRepo = resolvedHistoryRepo,
        scope = scope,
        context = context,
        showClearAllConfirmState = showClearAllConfirmState,
        packageToDeleteConfirmState = packageToDeleteConfirmState,
        showAppFilterDialogState = showAppFilterDialogState,
        showDeleteByAppDialogState = showDeleteByAppDialogState,
        selectedFilterPackageState = selectedFilterPackageState,
        selectedEntryIdForDetailsState = selectedEntryIdForDetailsState
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        historyItems(
            settings = settings,
            repository = repository,
            scope = scope,
            resolvedHistoryRepo = resolvedHistoryRepo,
            retentionMenuExpanded = retentionMenuExpanded,
            onRetentionMenuExpandedChange = { retentionMenuExpanded = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            appSummaries = appSummaries,
            selectedFilterPackage = selectedFilterPackage,
            onSelectPackage = { selectedFilterPackage = it },
            historyEntries = historyEntries,
            filteredEntries = filteredEntries,
            groupedByDay = groupedByDay,
            onOpenAppFilter = { showAppFilterDialog = true },
            onOpenDeleteByApp = { showDeleteByAppDialog = true },
            onClearAll = { showClearAllConfirm = true },
            onOpenDetails = { selectedEntryIdForDetails = it }
        )
    }
}

private fun LazyListScope.historyItems(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository?,
    scope: CoroutineScope,
    resolvedHistoryRepo: INotificationHistoryRepository,
    retentionMenuExpanded: Boolean,
    onRetentionMenuExpandedChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    appSummaries: List<AppNotificationSummary>,
    selectedFilterPackage: String?,
    onSelectPackage: (String?) -> Unit,
    historyEntries: List<NotificationHistoryEntry>,
    filteredEntries: List<NotificationHistoryEntry>,
    groupedByDay: Map<String, List<NotificationHistoryEntry>>,
    onOpenAppFilter: () -> Unit,
    onOpenDeleteByApp: () -> Unit,
    onClearAll: () -> Unit,
    onOpenDetails: (Long) -> Unit
) {
    item(key = "header_master_toggle") {
        HistoryMasterCard(
            enabled = settings.enableNotificationHistory,
            repository = repository,
            scope = scope
        )
    }

    if (!settings.enableNotificationHistory) {
        item(key = "header_disabled_banner") {
            HistoryDisabledCard(repository = repository, scope = scope)
        }
    } else {
        item(key = "header_controls") {
            HistoryControlsCard(
                retentionHours = settings.notificationHistoryRetentionHours,
                retentionMenuExpanded = retentionMenuExpanded,
                onRetentionMenuExpandedChange = onRetentionMenuExpandedChange,
                onRetentionSelected = { hours ->
                    if (repository != null) {
                        scope.launch {
                            repository.setNotificationHistoryRetentionHours(hours)
                            resolvedHistoryRepo.cleanupOldEntries(hours)
                        }
                    }
                },
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                appSummaries = appSummaries,
                selectedPackage = selectedFilterPackage,
                onSelectPackage = onSelectPackage,
                totalCount = historyEntries.size,
                filteredCount = filteredEntries.size,
                onOpenAppFilter = onOpenAppFilter,
                onOpenDeleteByApp = onOpenDeleteByApp,
                onClearAll = onClearAll
            )
        }

        if (groupedByDay.isEmpty()) {
            item(key = "empty_history_placeholder") {
                EmptyHistoryCard()
            }
        } else {
            groupedByDay.forEach { (dateHeader, entriesForDay) ->
                item(key = "date_header_$dateHeader") {
                    HistoryDayHeader(dateHeader = dateHeader, count = entriesForDay.size)
                }

                items(
                    items = entriesForDay,
                    key = { it.id }
                ) { entry ->
                    NotificationHistoryItemCard(
                        entry = entry,
                        onCardClick = { onOpenDetails(entry.id) },
                        onDelete = {
                            scope.launch { resolvedHistoryRepo.deleteEntry(entry.id) }
                        }
                    )
                }
            }
        }
    }
}

private fun summarizeByApp(entries: List<NotificationHistoryEntry>): List<AppNotificationSummary> {
    return entries
        .groupBy { it.packageName }
        .map { (pkg, appEntries) ->
            AppNotificationSummary(
                packageName = pkg,
                appName = appEntries.firstOrNull()?.appName ?: pkg,
                count = appEntries.size
            )
        }
        .sortedByDescending { it.count }
}

private fun filterEntries(
    entries: List<NotificationHistoryEntry>,
    packageName: String?,
    query: String
): List<NotificationHistoryEntry> {
    var list = entries
    if (!packageName.isNullOrBlank()) {
        list = list.filter { it.packageName == packageName }
    }
    if (query.isNotBlank()) {
        val q = query.trim().lowercase(Locale.getDefault())
        list = list.filter {
            it.appName.lowercase(Locale.getDefault()).contains(q) ||
                it.title.lowercase(Locale.getDefault()).contains(q) ||
                it.text.lowercase(Locale.getDefault()).contains(q) ||
                it.packageName.lowercase(Locale.getDefault()).contains(q)
        }
    }
    return list
}

private fun groupEntriesByDay(entries: List<NotificationHistoryEntry>): Map<String, List<NotificationHistoryEntry>> {
    val result = linkedMapOf<String, MutableList<NotificationHistoryEntry>>()
    val calToday = Calendar.getInstance()
    val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val calEntry = Calendar.getInstance()
    val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    for (entry in entries) {
        calEntry.timeInMillis = entry.postTimeMillis
        val header = when {
            isSameDay(calToday, calEntry) -> "Today"
            isSameDay(calYesterday, calEntry) -> "Yesterday"
            else -> fullDateFormat.format(Date(entry.postTimeMillis))
        }
        val list = result.getOrPut(header) { mutableListOf() }
        list.add(entry)
    }
    return result
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
