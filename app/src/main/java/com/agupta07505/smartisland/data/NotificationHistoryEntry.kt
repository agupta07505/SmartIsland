/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

/**
 * Represents a single persisted on-device notification record in the notification history.
 */
data class NotificationHistoryEntry(
    val id: Long = 0L,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String? = null,
    val postTimeMillis: Long,
    val category: String? = null,
    val channelId: String? = null,
    val mode: String = "Notification",
    val actionTitles: List<String> = emptyList(),
    val wasOpened: Boolean = false,
    val wasDismissed: Boolean = false
)
