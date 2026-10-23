/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Whether the landscape pill stays on screen to carry the idle widgets.
 * @param widgetIds stored comma list of switched-on widgets ([IdleWidgets.parse]); empty or unreadable means none
 * @param pillHidden the long press hid the pill: only calls show, so no widget pill either
 * @param shadeOpen the shade is pulled down: only the idle circle stays
 * @param widgetsSuppressed widgets held off on purpose (see [IdleWidgetOff]); they draw nothing, so the idle circle takes over
 * @param emptyPillShown the empty pill was asked for with a tap on the idle cutout (long press opens the app shortcuts)
 * @param tickHold a tick (hourly, day moment, weather change; ui/TickPill.kt) is due or playing: the pill stays for its length, then goes again
 */
fun landscapeWidgetPillWanted(
    isLandscape: Boolean,
    widgetsEnabled: Boolean,
    widgetIds: String,
    pillHidden: Boolean,
    shadeOpen: Boolean,
    widgetsSuppressed: Boolean = false,
    tickHold: Boolean = false,
    emptyPillShown: Boolean = false
): Boolean = isLandscape && !shadeOpen && (
    emptyPillShown ||
        (tickHold && widgetsEnabled) ||
        (widgetsEnabled && !widgetsSuppressed && !pillHidden && IdleWidgets.parse(widgetIds).isNotEmpty())
    )

/** Whether the collapsed pill is on the island in landscape: a card is there or the widget pill carries idle widgets. */
fun pillOnIsland(hasCards: Boolean, widgetPill: Boolean): Boolean = hasCards || widgetPill
