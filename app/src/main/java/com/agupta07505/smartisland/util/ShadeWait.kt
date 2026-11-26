/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val SHADE_WAIT_MAX_MS = 30_000L

/**
 * Returns once the shade is closed, at most [SHADE_WAIT_MAX_MS]. The island hides every card while the shade is open,
 * so a quick card from quick settings (airplane mode, Wi-Fi) must not start its timer before anyone can see it.
 */
internal suspend fun awaitShadeClosed(shadeOpen: StateFlow<Boolean>?) {
    if (shadeOpen == null || !shadeOpen.value) return
    withTimeoutOrNull(SHADE_WAIT_MAX_MS) { shadeOpen.first { !it } }
}
