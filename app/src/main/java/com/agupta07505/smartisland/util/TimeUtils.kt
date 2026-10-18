/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatNotificationTime(timeMillis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
}

/** "Now" for something under a minute old, the clock time after that. */
fun formatNotificationTimeOrNow(timeMillis: Long, nowMillis: Long = System.currentTimeMillis()): String =
    if (nowMillis - timeMillis in 0..60_000L) "Now" else formatNotificationTime(timeMillis)
