/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.pickers

import com.agupta07505.smartisland.util.PickerRequest

/** The pickers [PickerActivity] can host, and how requests map onto them. No Android types, so it is plain-JVM testable. */
internal object PickerKinds {
    const val APPS = "apps"
    const val UTILITY = "utility"

    private val supported = setOf(APPS, UTILITY)

    /** [raw] when it names a picker this window can host, else null (the window then closes instead of showing nothing). */
    fun resolve(raw: String?): String? = raw?.takeIf { it in supported }

    /** The window's kind for the island's [PickerRequest] value, or null for an unknown request. */
    fun forRequest(picker: String?): String? = when (picker) {
        PickerRequest.APPS -> APPS
        PickerRequest.UTILITY -> UTILITY
        else -> null
    }
}
