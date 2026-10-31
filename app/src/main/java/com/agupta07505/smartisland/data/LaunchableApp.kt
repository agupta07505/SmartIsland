/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.agupta07505.smartisland.util.quietly

data class LaunchableApp(
    val packageName: String,
    val label: String
)

object AppShortcutProvider {
    fun selectedApps(context: Context, packages: List<String>): List<LaunchableApp> =
        packages.mapNotNull { entry ->
            if (PinnedApps.isFolder(entry)) {
                val labels = PinnedApps.members(entry).mapNotNull { appLabel(context, it) }
                if (labels.isEmpty()) null else LaunchableApp(entry, labels.joinToString(", "))
            } else {
                appLabel(context, entry)?.let { LaunchableApp(entry, it) }
            }
        }

    private fun appLabel(context: Context, packageName: String): String? = quietly {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
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
        return quietly(false) {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun shortcuts(
        context: Context,
        selectedPackages: List<String>,
        includeRecent: Boolean,
        limit: Int = PinnedApps.MAX * 2
    ): List<LaunchableApp> {
        val installed = installedApps(context)
        val byPackage = installed.associateBy { it.packageName }
        val selected = selectedPackages.mapNotNull { entry ->
            if (PinnedApps.isFolder(entry)) {
                val labels = PinnedApps.members(entry).mapNotNull { byPackage[it]?.label }
                if (labels.isEmpty()) null else LaunchableApp(entry, labels.joinToString(", "))
            } else {
                byPackage[entry]
            }
        }
        val pinnedPackages = PinnedApps.flatPackages(selectedPackages).toSet()
        if (!includeRecent || !hasUsageAccess(context)) return selected.take(limit)

        val end = System.currentTimeMillis()
        val start = end - RECENT_WINDOW_MS
        val usage = quietly(emptyList()) {
            val usageManager = context.getSystemService(UsageStatsManager::class.java)
            usageManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()
        }
            .sortedByDescending { it.lastTimeUsed }
            .mapNotNull { byPackage[it.packageName] }
            .filter { it.packageName !in pinnedPackages }
            .distinctBy { it.packageName }

        return (selected + usage)
            .distinctBy { it.packageName }
            .take(limit)
    }

    private const val RECENT_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L
}
