/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process

data class LaunchableApp(
    val packageName: String,
    val label: String
)

object AppShortcutProvider {
    fun selectedApps(context: Context, packages: Set<String>): List<LaunchableApp> =
        packages.mapNotNull { packageName ->
            runCatching {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                LaunchableApp(
                    packageName = packageName,
                    label = context.packageManager.getApplicationLabel(info).toString()
                )
            }.getOrNull()
        }

    fun installedApps(context: Context): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .map { info ->
                LaunchableApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(context.packageManager).toString()
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun hasUsageAccess(context: Context): Boolean {
        return runCatching {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    fun shortcuts(
        context: Context,
        selectedPackages: Set<String>,
        includeRecent: Boolean,
        limit: Int = 8
    ): List<LaunchableApp> {
        val installed = installedApps(context)
        val byPackage = installed.associateBy { it.packageName }
        val selected = selectedPackages.mapNotNull(byPackage::get)
        if (!includeRecent || !hasUsageAccess(context)) return selected.take(limit)

        val end = System.currentTimeMillis()
        val start = end - RECENT_WINDOW_MS
        val usage = runCatching {
            val usageManager = context.getSystemService(UsageStatsManager::class.java)
            usageManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()
        }.getOrDefault(emptyList())
            .sortedByDescending { it.lastTimeUsed }
            .mapNotNull { byPackage[it.packageName] }
            .distinctBy { it.packageName }

        return (selected + usage)
            .distinctBy { it.packageName }
            .take(limit)
    }

    private const val RECENT_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L
}
