/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

object StatusBarHider {

    private const val TAG = "StatusBarHider"

    private const val DEMO_EXIT = "am broadcast -a com.android.systemui.demo -e command exit"
    private val TABLES = listOf("secure", "system", "global")

    internal fun hideCommands(plan: StatusBarPlan): List<String> = buildList {
        add(DEMO_EXIT)
        add("cmd statusbar send-disable-flag none")
        if (plan.disableFlags.isNotEmpty()) add("cmd statusbar send-disable-flag ${plan.disableFlags.joinToString(" ")}")
        if (plan.immersive) {
            add("settings put global policy_control immersive.status=*")
        } else {
            add("settings delete global policy_control")
        }
        val slots = plan.iconBlacklist
        if (slots.isEmpty()) {
            TABLES.forEach { add("settings put $it icon_blacklist \"\"") }
            TABLES.forEach { add("settings delete $it icon_blacklist") }
        } else {
            TABLES.forEach { add("settings put $it icon_blacklist \"${slots.joinToString(",")}\"") }
        }
    }

    suspend fun hide(plan: StatusBarPlan = StatusBarPlan.Everything): Result<Unit> {
        if (!ShizukuManager.hasPermission()) return Result.failure(IllegalStateException("Shizuku permission not granted"))
        return ShizukuManager.runShellForOutput(hideCommands(plan).joinToString("; ")).map { }
            .onFailure { AppLogRecorder.w(TAG, "hide failed", it) }
    }

    /** Puts every flag and setting back to the system default. */
    suspend fun restore(): Result<Unit> {
        if (!ShizukuManager.hasPermission()) return Result.failure(IllegalStateException("Shizuku permission not granted"))
        val commands = buildList {
            add("settings put global policy_control \"\"")
            add("settings delete global policy_control")
            // An empty value first wakes the ContentObservers, then the key is deleted
            TABLES.forEach { add("settings put $it icon_blacklist \"\"") }
            TABLES.forEach { add("settings delete $it icon_blacklist") }
            add("cmd statusbar send-disable-flag none")
            add("cmd statusbar send-disable-flag-2 none")
            add(DEMO_EXIT)
        }
        return ShizukuManager.runShellForOutput(commands.joinToString("; ")).map { }
            .onFailure { AppLogRecorder.w(TAG, "restore failed", it) }
    }

    internal fun matches(plan: StatusBarPlan, policy: String?, blacklist: String?): Boolean {
        if (plan.isEmpty) return true
        val slots = blacklist?.trim().orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() && it != "null" }
            .toSet()
        if (plan.immersive) {
            val hasPolicy = !policy.isNullOrEmpty() && policy.contains("immersive.status")
            return hasPolicy || ("clock" in slots && "battery" in slots)
        }
        val wanted = plan.iconBlacklist
        return wanted.isEmpty() || slots.containsAll(wanted)
    }

    /** Device itself reports the hide of [plan]. */
    suspend fun isHidden(plan: StatusBarPlan = StatusBarPlan.Everything): Boolean? {
        if (!ShizukuManager.hasPermission()) return null
        val policy = ShizukuManager.runShellForOutput("settings get global policy_control").getOrNull()?.trim()
        val blacklist = ShizukuManager.runShellForOutput("settings get secure icon_blacklist").getOrNull()?.trim()
        if (policy == null && blacklist == null) return null
        return matches(plan, policy, blacklist)
    }
}
