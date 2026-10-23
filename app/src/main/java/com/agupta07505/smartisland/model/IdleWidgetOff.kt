/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Idle widgets are taken off the cutout on purpose (Settings > Idle widgets), per orientation: after a tap closed the
 * island (until it reopens or the screen turns off), after a long press hid the pill (while hidden), and while any
 * notification is stored (until none is left).
 */
object IdleWidgetOff {
    fun suppressed(
        closedByTap: Boolean,
        pillHidden: Boolean,
        offAfterTapClose: Boolean,
        offAfterLongPress: Boolean,
        hasStoredNotifications: Boolean = false,
        offWhenStored: Boolean = false
    ): Boolean = (closedByTap && offAfterTapClose) || (pillHidden && offAfterLongPress) || (hasStoredNotifications && offWhenStored)

    /**
     * Slider choices for the widget switch timer: whole seconds from [IdleWidgets.MIN_CYCLE_SECONDS] to
     * [IdleWidgets.MAX_CYCLE_SECONDS].
     */
    fun normalizeCycleSeconds(seconds: Int): Int =
        seconds.coerceIn(IdleWidgets.MIN_CYCLE_SECONDS, IdleWidgets.MAX_CYCLE_SECONDS)

    fun allIds(): String = IdleWidgets.serialize(IdleWidgetKind.values().toList())
}
