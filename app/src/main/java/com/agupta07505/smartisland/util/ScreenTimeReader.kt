/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.usage.UsageStatsManager
import android.content.Context
import com.agupta07505.smartisland.data.AppShortcutProvider
import java.util.Calendar

data class ScreenTimeSnapshot(
    val totalMs: Long = 0L,
    val topAppName: String? = null,
    val topAppMs: Long = 0L
)

internal object ScreenTimeReader {
    fun hasPermission(context: Context): Boolean = AppShortcutProvider.hasUsageAccess(context)

    fun readToday(context: Context): ScreenTimeSnapshot {
        if (!hasPermission(context)) return ScreenTimeSnapshot()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return ScreenTimeSnapshot()

        val now = System.currentTimeMillis()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val statsMap = runCatching {
            manager.queryAndAggregateUsageStats(midnight, now)
        }.getOrNull()

        val stats = if (!statsMap.isNullOrEmpty()) {
            statsMap.values
        } else {
            runCatching {
                manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, midnight, now).orEmpty()
            }.getOrDefault(emptyList())
        }

        if (stats.isEmpty()) return ScreenTimeSnapshot()

        var total = 0L
        var topPkg: String? = null
        var maxTime = 0L
        val pm = context.packageManager

        for (s in stats) {
            val time = s.totalTimeInForeground
            if (time > 0L) {
                // Filter out system UI & android internal packages to match Digital Wellbeing
                if (s.packageName != "com.android.systemui" && s.packageName != "android") {
                    total += time
                    if (time > maxTime) {
                        maxTime = time
                        topPkg = s.packageName
                    }
                }
            }
        }

        val topAppName = topPkg?.let { pkg ->
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull() ?: pkg
        }

        return ScreenTimeSnapshot(totalMs = total, topAppName = topAppName, topAppMs = maxTime)
    }
}
