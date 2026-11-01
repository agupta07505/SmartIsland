/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.content.Context
import com.agupta07505.smartisland.util.AppLogRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHistoryRepository(
    context: Context,
    private val io: CoroutineDispatcher
) : INotificationHistoryRepository {
    @Inject constructor(@ApplicationContext context: Context) : this(context, Dispatchers.IO)

    private val dbHelper = NotificationHistoryDbHelper(context.applicationContext)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogRecorder.e("NotificationHistoryRepo", "unhandled repository coroutine failure", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + io + exceptionHandler)
    private val _history = MutableStateFlow<List<NotificationHistoryEntry>>(emptyList())
    override val history: StateFlow<List<NotificationHistoryEntry>> = _history.asStateFlow()

    init {
        scope.launch {
            reload()
        }
    }

    override suspend fun reload() = withContext(io) {
        val entries = dbHelper.getAllEntries()
        _history.value = entries
    }

    override suspend fun saveEntry(entry: NotificationHistoryEntry) = withContext(io) {
        val id = dbHelper.insertEntry(entry)
        val savedEntry = if (id > 0) entry.copy(id = id) else entry
        val current = _history.value
        // Prepend the new entry and cap at 1000 items in memory without a full table scan.
        _history.value = (listOf(savedEntry) + current).take(1000)
    }

    override suspend fun markAsOpened(notificationKey: String) = withContext(io) {
        dbHelper.markAsOpened(notificationKey)
        _history.value = _history.value.map {
            if (it.notificationKey == notificationKey) it.copy(wasOpened = true) else it
        }
    }

    override suspend fun deleteEntry(id: Long) = withContext(io) {
        // A failed delete leaves the row in the database, it would be back at the next reload
        if (dbHelper.deleteById(id) < 0) return@withContext
        _history.value = _history.value.filter { it.id != id }
    }

    override suspend fun deleteByPackage(packageName: String): Int = withContext(io) {
        val count = dbHelper.deleteByPackage(packageName)
        if (count > 0) {
            _history.value = _history.value.filter { it.packageName != packageName }
        }
        count
    }

    override suspend fun clearAll() = withContext(io) {
        if (dbHelper.deleteAll() < 0) return@withContext
        _history.value = emptyList()
    }

    override suspend fun search(query: String): List<NotificationHistoryEntry> = withContext(io) {
        if (query.isBlank()) {
            _history.value
        } else {
            dbHelper.searchEntries(query.trim())
        }
    }

    override suspend fun cleanupOldEntries(retentionHours: Int) = withContext(io) {
        if (retentionHours <= 0) return@withContext // <= 0 means keep indefinitely
        val cutoff = System.currentTimeMillis() - (retentionHours.toLong() * 3600L * 1000L)
        val deleted = dbHelper.deleteOlderThan(cutoff)
        if (deleted > 0) {
            _history.value = _history.value.filter { it.postTimeMillis >= cutoff }
        }
    }
}
