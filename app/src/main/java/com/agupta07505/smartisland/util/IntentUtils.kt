/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Swallows a missing or OEM-restricted destination instead of crashing. */
fun Context.safeStartActivity(
    intent: Intent,
    errorMessage: String? = "This setting is not available on your device.",
    fallbackIntent: Intent? = null
): Boolean {
    fun launch(candidate: Intent): Boolean {
        val resolvedIntent = Intent(candidate).apply {
            if (this@safeStartActivity !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return runCatchingLogged("SafeStartActivity", "Failed to open ${candidate.action}") {
            startActivity(resolvedIntent)
            true
        } ?: false
    }

    if (launch(intent)) return true
    if (fallbackIntent != null && launch(fallbackIntent)) return true

    if (errorMessage != null) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }
    return false
}
