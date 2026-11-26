/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.Condition
import androidx.annotation.RequiresApi
import com.agupta07505.smartisland.model.DndModes
import com.agupta07505.smartisland.model.ModeKind
import com.agupta07505.smartisland.model.ModeStatus

object DndModeReader {
    fun hasPolicyAccess(context: Context): Boolean =
        notificationManager(context)?.isNotificationPolicyAccessGranted == true

    fun read(context: Context): ModeStatus {
        val nm = notificationManager(context) ?: return ModeStatus.Off
        val filter = nm.currentInterruptionFilter
        val rule = activeRule(nm)
        if (rule != null) return rule.copy(filter = if (DndModes.filterActive(filter)) filter else 0)
        return DndModes.fromFilter(filter)
    }

    private fun activeRule(nm: NotificationManager): ModeStatus? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
        val active = quietly {
            nm.automaticZenRules.entries.mapNotNull { (id, rule) ->
                if (nm.getAutomaticZenRuleState(id) == Condition.STATE_TRUE) rule.toStatus() else null
            }
        }.orEmpty()
        return DndModes.pick(active)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun AutomaticZenRule.toStatus(): ModeStatus {
        val kind = kindOf(this)
        // Icon lives in the package that owns the mode: the system for Android modes, the maker's app for its own
        val iconPackage = (owner?.packageName ?: configurationActivity?.packageName).orEmpty()
        return ModeStatus(
            active = true,
            kind = kind,
            name = DndModes.withName(kind, name),
            fromRule = true,
            iconRes = iconResId,
            iconPackage = iconPackage
        )
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun kindOf(rule: AutomaticZenRule): ModeKind {
        when (rule.type) {
            AutomaticZenRule.TYPE_BEDTIME -> return ModeKind.Bedtime
            AutomaticZenRule.TYPE_DRIVING -> return ModeKind.Driving
            AutomaticZenRule.TYPE_THEATER -> return ModeKind.Theater
            AutomaticZenRule.TYPE_IMMERSIVE -> return ModeKind.Gaming
            AutomaticZenRule.TYPE_SCHEDULE_TIME, AutomaticZenRule.TYPE_SCHEDULE_CALENDAR -> return ModeKind.Schedule
            else -> Unit
        }
        // An "other" type: the name tells
        return DndModes.kindFromName(rule.name.orEmpty())
    }

    private fun notificationManager(context: Context): NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
}
