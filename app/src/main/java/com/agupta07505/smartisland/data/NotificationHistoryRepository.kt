/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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

/**
 * Default implementation of [INotificationHistoryRepository] using native SQLite storage.
 */
@Singleton
class NotificationHistoryRepository @Inject constructor(
    @ApplicationContext context: Context
) : INotificationHistoryRepository {

    private val dbHelper = NotificationHistoryDbHelper(context.applicationContext)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("NotificationHistoryRepo", "Unhandled repository coroutine failure", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val _history = MutableStateFlow<List<NotificationHistoryEntry>>(emptyList())
    override val history: StateFlow<List<NotificationHistoryEntry>> = _history.asStateFlow()

    init {
        scope.launch {
            reload()
        }
    }

    override suspend fun reload() = withContext(Dispatchers.IO) {
        val entries = dbHelper.getAllEntries()
        _history.value = entries
    }

    override suspend fun saveEntry(entry: NotificationHistoryEntry) = withContext(Dispatchers.IO) {
        val id = dbHelper.insertEntry(entry)
        val savedEntry = if (id > 0) entry.copy(id = id) else entry
        val current = _history.value
        // Prepend new entry and cap at 1000 items in memory without doing a full table scan
        _history.value = (listOf(savedEntry) + current).take(1000)
    }

    override suspend fun markAsOpened(notificationKey: String) = withContext(Dispatchers.IO) {
        dbHelper.markAsOpened(notificationKey)
        _history.value = _history.value.map {
            if (it.notificationKey == notificationKey) it.copy(wasOpened = true) else it
        }
    }

    override suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteById(id)
        _history.value = _history.value.filter { it.id != id }
    }

    override suspend fun deleteByPackage(packageName: String): Int = withContext(Dispatchers.IO) {
        val count = dbHelper.deleteByPackage(packageName)
        if (count > 0) {
            _history.value = _history.value.filter { it.packageName != packageName }
        }
        count
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        dbHelper.deleteAll()
        _history.value = emptyList()
    }

    override suspend fun search(query: String): List<NotificationHistoryEntry> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            _history.value
        } else {
            dbHelper.searchEntries(query.trim())
        }
    }

    override suspend fun cleanupOldEntries(retentionHours: Int) = withContext(Dispatchers.IO) {
        if (retentionHours <= 0) return@withContext // <= 0 means keep indefinitely
        val cutoff = System.currentTimeMillis() - (retentionHours.toLong() * 3600L * 1000L)
        val deleted = dbHelper.deleteOlderThan(cutoff)
        if (deleted > 0) {
            _history.value = _history.value.filter { it.postTimeMillis >= cutoff }
        }
    }
}
