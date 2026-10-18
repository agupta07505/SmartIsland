/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.alert

/**
 * When the island may open past its small state. Pure: the view model passes in the orientation and the
 * secure-window flag, so the rules are unit-testable.
 *
 *  - Landscape (Rule_Book §9.2): the Medium card is built for top-down portrait viewing, so with alerts on the
 *    island it never opens (no auto-expand, a tap does not open it); alerts stay in the small pill or the
 *    companion bubble. The idle Large dashboard (nothing on the island) is unaffected.
 *  - Secure foreground app (§8.2): an Ephemeral alert never auto-expands over a `FLAG_SECURE` window; it
 *    shows as the small pill / companion bubble only. A tap still opens it.
 *  - Collision at arrival (§3.4): an Ephemeral alert never auto-expands over a Critical or Persistent main pill.
 */
object ExpansionPolicy {

    /** Whether the user (or code) may open the island now; [hasAlerts] false means the idle dashboard. */
    fun canExpand(hasAlerts: Boolean, isLandscape: Boolean): Boolean = !hasAlerts || !isLandscape

    /** Whether an arriving alert of tier [arriving] may open the island on its own. */
    fun allowAutoExpand(
        arriving: AlertTier,
        primary: AlertTier?,
        isLandscape: Boolean = false,
        foregroundIsSecure: Boolean = false
    ): Boolean = when {
        arriving == AlertTier.Passive -> false
        isLandscape -> false
        arriving == AlertTier.Ephemeral && foregroundIsSecure -> false
        arriving == AlertTier.Ephemeral && (primary == AlertTier.Critical || primary == AlertTier.Persistent) -> false
        else -> true
    }
}
