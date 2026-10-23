/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

enum class MiuiAppLockEvent {
    PromptShown,
    ScanStart,
    FaceMatched,
    FingerprintMatched
}

class MiuiAppLockLog {
    private var tokenAt = NONE

    fun classify(line: String, now: Long): MiuiAppLockEvent? {
        if (PROMPT.containsMatchIn(line)) return MiuiAppLockEvent.PromptShown
        if (FACE_MATCH.containsMatchIn(line)) return MiuiAppLockEvent.FaceMatched
        SCAN_OWNER.find(line)?.let { match ->
            return if (isKeyguardOwner(match.groupValues[1])) null else MiuiAppLockEvent.ScanStart
        }
        if (AUTH_TOKEN.containsMatchIn(line)) {
            tokenAt = now
            return null
        }
        val owner = FINGERPRINT_DONE.find(line)?.groupValues?.get(1) ?: return null
        val fresh = tokenAt != NONE && now - tokenAt in 0..TOKEN_WINDOW_MS
        tokenAt = NONE
        return if (fresh && !isKeyguardOwner(owner)) MiuiAppLockEvent.FingerprintMatched else null
    }

    companion object {
        const val FILTER = "ConfirmAccessControl|addAuthToken|Finishing.*FingerprintAuthenticationClient|authenticate, packName"

        private const val TOKEN_WINDOW_MS = 1000L
        private const val NONE = Long.MIN_VALUE
        private const val PID = "(?:\\(\\s*\\d+\\))?"

        private val PROMPT = Regex(
            "topActivity=ComponentInfo\\{[A-Za-z0-9_.]+/com\\.miui\\.applicationlock\\.ConfirmAccessControl\\}"
        )
        private val FACE_MATCH = Regex("ConfirmAccessControl$PID:\\s*onFaceAuthenticated")
        private val SCAN_OWNER = Regex("FaceService$PID:\\s*authenticate,\\s*packName:\\s*([A-Za-z0-9_.]+)")
        private val AUTH_TOKEN = Regex("AuthenticationClient$PID:\\s*addAuthToken")
        private val FINGERPRINT_DONE = Regex(
            "\\[Finishing\\].*FingerprintAuthenticationClient.*owner=([A-Za-z0-9_.]+).*success:\\s*true"
        )

        private fun isKeyguardOwner(owner: String): Boolean {
            val o = owner.lowercase()
            return o == "android" || o.contains("systemui") || o.contains("keyguard")
        }
    }
}
