/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Opens a system or external activity without allowing a missing/OEM-restricted
 * destination to crash Smart Island.
 */
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
