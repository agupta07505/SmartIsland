/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.os.SystemClock
import android.view.inputmethod.InputMethodManager
import com.agupta07505.smartisland.util.runCatchingLogged

internal class InputMethodPackageCache(private val context: Context) {
    private var inputMethodPackages: Set<String> = emptySet()
    private var inputMethodPackagesReadAt = 0L

    fun isInputMethodPackage(pkg: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - inputMethodPackagesReadAt > INPUT_METHOD_CACHE_MS) {
            inputMethodPackagesReadAt = now
            inputMethodPackages = runCatchingLogged(TAG, "Reading input methods failed") {
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.enabledInputMethodList
                    ?.map { it.packageName }
                    ?.toSet()
            } ?: inputMethodPackages
        }
        return pkg in inputMethodPackages
    }

    private companion object {
        const val TAG = "SmartIslandOverlayService"
        const val INPUT_METHOD_CACHE_MS = 60_000L
    }
}
