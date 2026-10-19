/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Best-effort guess at whether the foreground app is a secure context (Rule_Book 8.2, Secure App
 * Passthrough).
 *
 * Android gives no way to read another app's FLAG_SECURE, and the accessibility service runs without
 * window-content access, so this is honest guesswork: the foreground package (from the accessibility
 * service's window-state events) is matched against a small built-in list of apps known to protect their
 * screens (password managers, authenticators, payments, DRM video) plus any [extraPackages]. A miss only
 * means the island behaves as usual; it never hides anything.
 */
class ForegroundSecureDetector(initialExtraPackages: Set<String> = emptySet()) {

    @Volatile
    private var extraPackages: Set<String> = initialExtraPackages
    private val secure = MutableStateFlow(false)

    /** True while the foreground app is believed to show a FLAG_SECURE window. */
    val foregroundSecure: StateFlow<Boolean> = secure

    fun setExtraPackages(packages: Set<String>) {
        extraPackages = packages
    }

    fun onForegroundPackage(packageName: String?) {
        secure.value = isSecurePackage(packageName, extraPackages)
    }

    companion object {
        /** Apps that are known to mark their windows secure. Exact package names. */
        val BUILT_IN_SECURE_PACKAGES: Set<String> = setOf(
            // Password managers and authenticators
            "com.x8bit.bitwarden",
            "com.agilebits.onepassword",
            "com.lastpass.lpandroid",
            "com.dashlane",
            "com.kunzisoft.keepass.free",
            "com.kunzisoft.keepass.libre",
            "com.google.android.apps.authenticator2",
            "com.azure.authenticator",
            "com.authy.authy",
            // Payments and wallets
            "com.google.android.apps.walletnfcrel",
            "com.google.android.apps.nbu.paisa.user",
            "com.paypal.android.p2pmobile",
            "com.phonepe.app",
            "net.one97.paytm",
            // DRM video
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",
            "com.disney.disneyplus",
            "com.wbd.stream"
        )

        fun isSecurePackage(packageName: String?, extraPackages: Set<String> = emptySet()): Boolean {
            if (packageName.isNullOrBlank()) return false
            return packageName in BUILT_IN_SECURE_PACKAGES || packageName in extraPackages
        }
    }
}
