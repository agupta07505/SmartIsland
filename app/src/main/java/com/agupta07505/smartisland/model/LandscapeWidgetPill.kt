/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Landscape idle widgets live in the collapsed cutout (the landscape pill), not in the small idle circle.
 *
 * While no card is on the island and the idle widgets are on, the island keeps the collapsed pill on screen (same
 * size and spot as the pill that carries a card) and the widgets draw in its left and right areas, like portrait.
 * The idle circle stays at its own spot as the toggle button (see [shouldShowIdleDot]); its tap works as before.
 *
 * This file is the pure decision. The pill geometry comes from `forOrientation(hasContent = true)`, the pill's
 * draw is in ui/IdleWidgets.kt, the state machine that slides the pill away reads [pillOnIsland].
 */

/**
 * Whether the landscape pill stays on screen to carry the idle widgets.
 *
 * @param isLandscape the display is in landscape (portrait has its own pill, nothing changes there)
 * @param widgetsEnabled the idle widgets master switch
 * @param widgetIds the stored comma list of switched-on widgets ([IdleWidgets.parse]); empty or unreadable means none
 * @param pillHidden the user hid the pill with the long press: only calls show, so no widget pill either
 * @param shadeOpen the notification shade is pulled down: every event is hidden and only the idle circle stays
 * @param widgetsSuppressed the widgets are held off on purpose ("off after tap to close" / "off after long press to hide",
 *   see [IdleWidgetOff]): they draw nothing then, so the pill must not stay on screen empty; the idle circle takes over
 * @param emptyPillShown the empty pill was asked for with a tap on the idle cutout (long press on it opens the app shortcuts)
 * @param tickHold a tick (hourly, sunrise/sunset moments, weather change; ui/TickPill.kt) is due or playing: the pill is on the island for its
 *   length even though it was hidden by a long press or the widgets are held off, and goes again when the tick is done
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

/**
 * Whether the collapsed pill is on the island at all in landscape: a card is there, or the widget pill carries the idle
 * widgets. The pill's slide-away state machine, the idle dot and the dot's touch region all key on this, so with the
 * widget pill on, a card arriving or leaving swaps the pill's content instead of sliding the pill in or out.
 */
fun pillOnIsland(hasCards: Boolean, widgetPill: Boolean): Boolean = hasCards || widgetPill
