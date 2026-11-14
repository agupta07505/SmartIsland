/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

        // 1. High-precision event-based screen time calculation (strictly midnight to now, matching Digital Wellbeing)
        val eventSnapshot = readTodayFromEvents(context, manager, midnight, now)
        if (eventSnapshot != null && eventSnapshot.totalMs > 0L) {
            return eventSnapshot
        }

        // 2. Fallback: UsageStats aggregation (filtered to ignore launcher, self, system UI)
        return readTodayFromUsageStats(context, manager, midnight, now)
    }

    private fun readTodayFromEvents(
        context: Context,
        manager: UsageStatsManager,
        midnight: Long,
        now: Long
    ): ScreenTimeSnapshot? {
        val events = runCatching { manager.queryEvents(midnight, now) }.getOrNull() ?: return null
        if (!events.hasNextEvent()) return null

        val pm = context.packageManager
        val homePackage = getHomePackage(pm)
        val selfPackage = context.packageName

        val event = UsageEvents.Event()
        val durations = mutableMapOf<String, Long>()

        var currentForegroundPkg: String? = null
        var currentForegroundStart: Long = 0L
        var isScreenOn = true

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val time = event.timeStamp
            val pkg = event.packageName

            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    isScreenOn = true
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    isScreenOn = false
                    if (currentForegroundPkg != null && currentForegroundStart > 0L) {
                        val duration = (time - currentForegroundStart).coerceAtLeast(0L)
                        if (duration > 0L) {
                            durations[currentForegroundPkg] = (durations[currentForegroundPkg] ?: 0L) + duration
                        }
                        currentForegroundPkg = null
                        currentForegroundStart = 0L
                    }
                }
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Close previous active app session
                    if (currentForegroundPkg != null && currentForegroundStart > 0L) {
                        val duration = (time - currentForegroundStart).coerceAtLeast(0L)
                        if (duration > 0L) {
                            durations[currentForegroundPkg] = (durations[currentForegroundPkg] ?: 0L) + duration
                        }
                        currentForegroundPkg = null
                        currentForegroundStart = 0L
                    }

                    if (isScreenOn && shouldCountPackage(pkg, homePackage, selfPackage)) {
                        currentForegroundPkg = pkg
                        currentForegroundStart = maxOf(time, midnight)
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentForegroundPkg == pkg && currentForegroundStart > 0L) {
                        val duration = (time - currentForegroundStart).coerceAtLeast(0L)
                        if (duration > 0L) {
                            durations[pkg] = (durations[pkg] ?: 0L) + duration
                        }
                        currentForegroundPkg = null
                        currentForegroundStart = 0L
                    }
                }
            }
        }

        // If an app is still in foreground right now
        if (isScreenOn && currentForegroundPkg != null && currentForegroundStart > 0L) {
            val duration = (now - currentForegroundStart).coerceAtLeast(0L)
            if (duration > 0L) {
                durations[currentForegroundPkg] = (durations[currentForegroundPkg] ?: 0L) + duration
            }
        }

        var total = 0L
        var topPkg: String? = null
        var maxTime = 0L

        for ((pkg, duration) in durations) {
            total += duration
            if (duration > maxTime) {
                maxTime = duration
                topPkg = pkg
            }
        }

        val topAppName = topPkg?.let { p ->
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString() }.getOrNull() ?: p
        }

        return ScreenTimeSnapshot(totalMs = total, topAppName = topAppName, topAppMs = maxTime)
    }

    private fun readTodayFromUsageStats(
        context: Context,
        manager: UsageStatsManager,
        midnight: Long,
        now: Long
    ): ScreenTimeSnapshot {
        val stats = runCatching {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, midnight, now).orEmpty()
        }.getOrDefault(emptyList())

        if (stats.isEmpty()) return ScreenTimeSnapshot()

        val pm = context.packageManager
        val homePackage = getHomePackage(pm)
        val selfPackage = context.packageName

        var total = 0L
        var topPkg: String? = null
        var maxTime = 0L

        for (s in stats) {
            val time = s.totalTimeInForeground
            if (time > 0L && shouldCountPackage(s.packageName, homePackage, selfPackage)) {
                val clampedTime = time.coerceAtMost(now - midnight)
                total += clampedTime
                if (clampedTime > maxTime) {
                    maxTime = clampedTime
                    topPkg = s.packageName
                }
            }
        }

        val topAppName = topPkg?.let { pkg ->
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull() ?: pkg
        }

        return ScreenTimeSnapshot(totalMs = total, topAppName = topAppName, topAppMs = maxTime)
    }

    private fun shouldCountPackage(pkg: String?, homePackage: String?, selfPackage: String): Boolean {
        if (pkg.isNullOrEmpty()) return false
        if (pkg == "android" || pkg == "com.android.systemui") return false
        if (pkg == selfPackage) return false
        if (homePackage != null && pkg == homePackage) return false
        if (pkg == "com.mi.android.globallauncher" || pkg == "com.miui.home" || pkg == "com.google.android.apps.nexuslauncher") return false
        return true
    }

    private fun getHomePackage(pm: PackageManager): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }
}
