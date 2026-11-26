/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.data.SmartIslandSettings

data class AppFlags(
    val excluded: Boolean = false,
    val muted: Boolean = false,
    /** Never opens into the big card. */
    val noCard: Boolean = false,
    val autoExpand: AppAutoExpand = AppAutoExpand.DEFAULT,
    val hiddenOnLock: Boolean = false,
    val noCooldown: Boolean = false
) {
    val isDefault: Boolean
        get() = !excluded && !muted && !noCard && autoExpand == AppAutoExpand.DEFAULT && !hiddenOnLock && !noCooldown
}

enum class AppTag { Excluded, Muted, NoCard, AlwaysExpands, NoAutoExpand, HiddenOnLock, NoCooldown }

enum class AppsFilter { All, Customized, Muted, Excluded, NeverExpands }

enum class BulkAppAction { Mute, Exclude, Reset }

data class VisibleTags(val shown: List<AppTag>, val overflow: Int)

object AppsRules {

    const val MAX_TAGS_SHOWN = 2

    fun flagsFor(settings: SmartIslandSettings, packageName: String): AppFlags = AppFlags(
        excluded = packageName in settings.disabledNotificationPackages,
        muted = packageName in settings.disabledSoundPackages,
        noCard = packageName in settings.noExpandPackages,
        autoExpand = AppEventRules.autoExpandChoice(
            settings.noAutoExpandPackages, settings.autoExpandPackages, packageName
        ),
        hiddenOnLock = packageName in settings.hideOnLockScreenPackages,
        noCooldown = packageName in settings.notificationCooldownExcludedPackages
    )

    fun tagsFor(flags: AppFlags): List<AppTag> = buildList {
        if (flags.excluded) add(AppTag.Excluded)
        if (flags.muted) add(AppTag.Muted)
        if (!flags.excluded && flags.noCard) add(AppTag.NoCard)
        if (!flags.excluded && !flags.noCard) {
            when (flags.autoExpand) {
                AppAutoExpand.ALWAYS -> add(AppTag.AlwaysExpands)
                AppAutoExpand.NEVER -> add(AppTag.NoAutoExpand)
                AppAutoExpand.DEFAULT -> Unit
            }
        }
        if (flags.hiddenOnLock) add(AppTag.HiddenOnLock)
        if (flags.noCooldown) add(AppTag.NoCooldown)
    }

    fun visibleTags(tags: List<AppTag>, maxShown: Int = MAX_TAGS_SHOWN): VisibleTags {
        val shown = tags.take(maxShown)
        return VisibleTags(shown, tags.size - shown.size)
    }

    fun isCustomized(flags: AppFlags): Boolean = !flags.isDefault

    fun neverExpands(flags: AppFlags): Boolean = flags.noCard || flags.autoExpand == AppAutoExpand.NEVER

    fun matches(filter: AppsFilter, flags: AppFlags): Boolean = when (filter) {
        AppsFilter.All -> true
        AppsFilter.Customized -> isCustomized(flags)
        AppsFilter.Muted -> flags.muted
        AppsFilter.Excluded -> flags.excluded
        AppsFilter.NeverExpands -> neverExpands(flags)
    }

    /** Whole list, not the search result */
    fun counts(flagsList: List<AppFlags>): Map<AppsFilter, Int> =
        AppsFilter.entries.associateWith { filter -> flagsList.count { matches(filter, it) } }

    fun matchesQuery(label: String, packageName: String, query: String): Boolean {
        val q = query.trim()
        return q.isEmpty() || label.contains(q, ignoreCase = true) || packageName.contains(q, ignoreCase = true)
    }

    /** Speaker only matters while the app may notify. */
    fun soundOn(flags: AppFlags): Boolean = !flags.excluded && !flags.muted
}
