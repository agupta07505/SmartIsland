/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining notification history operations.
 */
interface INotificationHistoryRepository {
    val history: StateFlow<List<NotificationHistoryEntry>>
    suspend fun saveEntry(entry: NotificationHistoryEntry)
    suspend fun markAsOpened(notificationKey: String)
    suspend fun deleteEntry(id: Long)
    suspend fun deleteByPackage(packageName: String): Int
    suspend fun clearAll()
    suspend fun search(query: String): List<NotificationHistoryEntry>
    suspend fun cleanupOldEntries(retentionHours: Int)
    suspend fun reload()
}
