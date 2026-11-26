/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.pickers

import com.agupta07505.smartisland.util.PickerRequest

/** No Android types, so it is plain-JVM testable. */
internal object PickerKinds {
    const val APPS = "apps"
    const val UTILITY = "utility"

    private val supported = setOf(APPS, UTILITY)

    /** Null for a picker this window cannot host (it closes instead of showing nothing). */
    fun resolve(raw: String?): String? = raw?.takeIf { it in supported }

    /** Null for an unknown request */
    fun forRequest(picker: String?): String? = when (picker) {
        PickerRequest.APPS -> APPS
        PickerRequest.UTILITY -> UTILITY
        else -> null
    }
}
