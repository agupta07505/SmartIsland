/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy

/**
 * Buddy is a Kotlin port of Vorssaint's island mascot (GPL-3.0-or-later, compatible with this
 * project's GPL-3.0). Every file ported from it must carry [NOTICE] directly under the project header.
 * The Vorssaint name, logo and icon are not licensed for reuse (its TRADEMARKS.md), so the feature is
 * called "Buddy" in the UI and uses this app's own artwork.
 *
 * Package layout (filled phase by phase):
 *   buddy/model, buddy/geometry, buddy/motion, buddy/runtime, buddy/ui
 * Code names use the `Mascot` prefix because "companion" already names the split-mode circle here.
 */
object MascotAttribution {
    const val SOURCE_URL = "https://github.com/vorssaint/vorssaint-utils"
    const val LICENSE_ID = "GPL-3.0-or-later"

    /** Header line for every ported file. */
    const val NOTICE =
        "Ported from Vorssaint (c) 2026 Vorssaint, $LICENSE_ID, $SOURCE_URL"
}
