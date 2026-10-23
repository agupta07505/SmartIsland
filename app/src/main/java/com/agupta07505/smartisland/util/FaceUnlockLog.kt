/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

enum class FaceUnlockLogEvent {
    Start,

    Success,

    Failure,

    End
}

enum class UnlockType(val label: String) {
    Face("Face"),
    Fingerprint("Fingerprint"),

    Credential("PIN / pattern / password")
}

object FaceUnlockLog {
    // "face" as a word, not Surface, Interface or Facebook
    private val FACE = Regex("Face(?![a-z])|FACE|(?<![A-Za-z])face(?![a-z])")

    private val IGNORED = listOf(
        "ResetLockout",
        "UpdateActiveUser",
        "(Generate|Revoke)Challenge",
        "PendingLockoutReset",
        "Resetting face lockout",
        "onFlip",
        "\\bFace up\\b",
        "Enumerate",
        "InternalCleanup",
        "(Enroll|Remove)\\w*Client"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private val FAILURE = listOf(
        "onAuthenticationFailed",
        "onAuthenticationError",
        "onAuthenticated\\(false\\)",
        "authenticated\\s*[=:]\\s*false",
        "Failed\\(biometricSourceType=FACE",
        "onLockout(Permanent|Timed)",
        "FACE_ERROR_LOCKOUT",
        "(auth(entication)?|unlock|match|recogni\\w*)\\W{0,3}(failed|failure|error|rejected|timed? ?out)"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private val END = Regex("\\[Finishing\\].*Face\\w*AuthenticationClient.*success:\\s*false", RegexOption.IGNORE_CASE)

    // "success: true" also prints on timeout, so it is not a match
    private val FINISHED_OK = Regex("\\[Finishing\\].*Face\\w*AuthenticationClient.*success:\\s*true", RegexOption.IGNORE_CASE)

    private const val MATCHED = "onAuthenticated\\(true\\)\\W+ID:\\s*[1-9]\\d*"

    private val FINGERPRINT_MATCHED = Regex("onAuthenticated\\(true\\).*Fingerprint\\w*AuthenticationClient")

    private val MIUI_KEYGUARD_FINGERPRINT = Regex("miui_keyguard(?:\\(\\s*\\d+\\))?:\\s*unlock keyguard by fp\\b")

    fun isFingerprintMatch(line: String): Boolean =
        FINGERPRINT_MATCHED.containsMatchIn(line) || MIUI_KEYGUARD_FINGERPRINT.containsMatchIn(line)

    private val SUCCESS = listOf(
        MATCHED,
        "Succeeded\\(biometricSourceType=FACE",
        "onAuthenticationSucceeded",
        "onAuthenticated.*authenticated\\s*[=:]\\s*true",
        "authenticated\\s*[=:]\\s*true",
        "onFaceAuthenticated",
        "FACE_AUTH_SUCCESS",
        "(auth(entication)?|unlock|match|recogni\\w*)\\W{0,3}(succe\\w+|passed|accepted)\\b"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private val STRONG_SUCCESS = listOf(
        MATCHED,
        "Succeeded\\(biometricSourceType=FACE",
        "face unlock success and keyguard dismiss"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    // errorId 5 is a cancel, not a failure
    private val MIUI_FACE_ERROR = Regex("FaceService:\\s*onError\\b.*errorId:\\s*(\\d+)")

    fun isStrongSuccess(line: String): Boolean = STRONG_SUCCESS.any { it.containsMatchIn(line) }

    private val START = listOf(
        "\\[?started\\]?:?\\s*\\{.*Face\\w*AuthenticationClient",
        "Started\\(biometricSourceType=FACE",
        "startAuthentication",
        "startHalOperation",
        "start(ed|ing)?\\W{0,3}(face\\W{0,3})?(auth(entication)?|detect(ion)?|scan|recogni\\w*|listening)",
        "(auth(entication)?|detect(ion)?|scan|recogni\\w*|listening)\\W{0,3}start(ed|ing)?",
        "startFace\\w*",
        "startListeningForFace",
        "FaceService:\\s*authenticate,\\s*packName:\\s*[A-Za-z0-9_.]+",
        "FaceAuthenticationClient.*(start|run|begin)"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    // failures first, so "authenticated=false" is never a success
    fun classify(line: String): FaceUnlockLogEvent? {
        if (!FACE.containsMatchIn(line)) return null
        if (IGNORED.any { it.containsMatchIn(line) }) return null
        if (FINISHED_OK.containsMatchIn(line)) return null
        MIUI_FACE_ERROR.find(line)?.let { return if (it.groupValues[1] == "5") null else FaceUnlockLogEvent.End }
        if (END.containsMatchIn(line)) return FaceUnlockLogEvent.End
        if (FAILURE.any { it.containsMatchIn(line) }) return FaceUnlockLogEvent.Failure
        if (SUCCESS.any { it.containsMatchIn(line) }) return FaceUnlockLogEvent.Success
        if (START.any { it.containsMatchIn(line) }) return FaceUnlockLogEvent.Start
        return null
    }

    private val CLIENT_OWNER = Regex("(?:owner=|packName:\\s*)([A-Za-z0-9_.]+)")

    private fun isKeyguardOwner(owner: String): Boolean {
        val o = owner.lowercase()
        return o == "android" || o == "system" || o.contains("systemui") || o.contains("keyguard")
    }

    fun clientOwner(line: String): String? = CLIENT_OWNER.find(line)?.groupValues?.get(1)

    fun isAppScanStart(line: String): Boolean {
        if (classify(line) != FaceUnlockLogEvent.Start) return false
        val owner = clientOwner(line) ?: return false
        return !isKeyguardOwner(owner)
    }

    private val APP_UNLOCKED = Regex("AppLockManagerService(?:\\(\\s*\\d+\\))?:\\s*unlockPackage:\\s*packageName\\s*=\\s*([A-Za-z0-9_.]+)")

    private val APP_LOCK_REQUIRED = Regex("AppLockManagerService(?:\\(\\s*\\d+\\))?:\\s*requireUnlock\\s*=\\s*true")

    fun appUnlockedPackage(line: String): String? = APP_UNLOCKED.find(line)?.groupValues?.get(1)

    fun isAppLockRequired(line: String): Boolean = APP_LOCK_REQUIRED.containsMatchIn(line)

    private val PROMPT_DISMISSED = Regex(
        "BiometricService(?:\\(\\s*\\d+\\))?:\\s*(?:onSystemEvent:\\s*1\\b|onDismissed:\\s*3\\b)"
    )

    fun isAppPromptDismissed(line: String): Boolean = PROMPT_DISMISSED.containsMatchIn(line)

    private val BOUNCER_SHOWN = Regex("STARTED transition:.*\\bto=PRIMARY_BOUNCER\\b")

    fun isBouncerShown(line: String): Boolean = BOUNCER_SHOWN.containsMatchIn(line)

    private val SOURCE_SUCCEEDED = Regex("Succeeded\\(biometricSourceType=(\\w+)")

    private val CREDENTIAL_UNLOCK = Regex("on_device_unlocked\\(user_id=\\d+,\\s*password\\.is_some\\(\\)=true\\)")

    fun classifyUnlock(line: String): UnlockType? {
        if (isFingerprintMatch(line)) return UnlockType.Fingerprint
        SOURCE_SUCCEEDED.find(line)?.let { m ->
            return when (m.groupValues[1].uppercase()) {
                "FACE" -> UnlockType.Face
                "FINGERPRINT" -> UnlockType.Fingerprint
                else -> null
            }
        }
        if (CREDENTIAL_UNLOCK.containsMatchIn(line)) return UnlockType.Credential
        if (classify(line) == FaceUnlockLogEvent.Success) return UnlockType.Face
        return null
    }

    private val CAPTURE_KEYWORDS = Regex(
        "biometric|keyguard|unlock|on_device_locked|userpresent|screen_on|wakeup|goingtosleep",
        RegexOption.IGNORE_CASE
    )

    private val CAPTURE_NOISE = Regex(
        "mgulk|Camera frame arrival|FaceHandler handle msg|AppFDFT_SW|MtkCam/|SurfaceFlinger|ctl\\.interface_start|" +
            "isUserUnlocked|NearbySharing|BinderNative|AiAi|AccessibilityWindowsPopulator"
    )

    fun isCaptureCandidate(line: String): Boolean {
        if (CAPTURE_NOISE.containsMatchIn(line)) return false
        return FACE.containsMatchIn(line) || CAPTURE_KEYWORDS.containsMatchIn(line)
    }

    fun summarize(lines: List<String>): Map<FaceUnlockLogEvent, Int> {
        val counts = HashMap<FaceUnlockLogEvent, Int>()
        for (line in lines) {
            val event = classify(line) ?: continue
            counts[event] = (counts[event] ?: 0) + 1
        }
        return counts
    }

    private val TIME = Regex("(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})")

    private fun timeOfDayMs(line: String): Long? {
        val m = TIME.find(line) ?: return null
        val (h, min, s, ms) = m.destructured
        return ((h.toLong() * 60 + min.toLong()) * 60 + s.toLong()) * 1000 + ms.toLong()
    }

    const val SAME_UNLOCK_MS = 2000L

    fun summarizeUnlocks(lines: List<String>): Map<UnlockType, Int> {
        val counts = HashMap<UnlockType, Int>()
        val lastAt = HashMap<UnlockType, Long>()
        for (line in lines) {
            val type = classifyUnlock(line) ?: continue
            val at = timeOfDayMs(line)
            val prev = lastAt[type]
            if (at != null && prev != null && at - prev in 0..SAME_UNLOCK_MS) {
                lastAt[type] = at
                continue
            }
            if (at != null) lastAt[type] = at
            counts[type] = (counts[type] ?: 0) + 1
        }
        return counts
    }

    fun annotation(line: String): String? {
        val parts = ArrayList<String>(2)
        classify(line)?.let { parts.add("face:${it.name}") }
        classifyUnlock(line)?.let { parts.add("unlock:${it.name}") }
        if (isBouncerShown(line)) parts.add("pin-screen:Shown")
        if (isAppScanStart(line)) parts.add("app-scan:Start(${clientOwner(line)})")
        if (isAppLockRequired(line)) parts.add("app-lock:Required")
        if (isAppPromptDismissed(line)) parts.add("prompt:Dismissed")
        appUnlockedPackage(line)?.let { parts.add("app-lock:Unlocked($it)") }
        return if (parts.isEmpty()) null else parts.joinToString(" ")
    }

    const val CAPTURE_TAG = "FaceUnlockCapture"
    const val CAPTURE_MARKER_TEXT = "SMARTISLAND_FACE_CAPTURE_START"

    fun extractCapture(dump: String, maxLines: Int = 1200): List<String> {
        val all = dump.lineSequence().toList()
        val marker = all.indexOfLast { it.contains(CAPTURE_MARKER_TEXT) }
        val after = if (marker >= 0) all.subList(marker + 1, all.size) else all
        return after.filter { it.isNotBlank() && isCaptureCandidate(it) }.takeLast(maxLines)
    }
}
