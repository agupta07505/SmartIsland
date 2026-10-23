/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

enum class FaceUnlockMode(val id: String) {
    FaceOnly("face_only"),
    Any("any");

    companion object {
        fun fromId(id: String?): FaceUnlockMode = entries.firstOrNull { it.id == id } ?: FaceOnly
    }
}

class FaceUnlockCoordinator(private val out: Output) {
    interface Output {
        fun startScan()

        fun startScan(waitMs: Int) = startScan()

        fun startAppScan()

        /** App scan shown before the system's real scan start; blinking waits for [anchor]. */
        fun startAppScanEarly() = startAppScan()

        fun anchor() {}
        fun accept()

        fun fingerprint()

        fun reject() = cancel()

        fun startFingerprintScan() {}

        fun startAppFingerprintScan() = startFingerprintScan()
        fun cancel()
        fun stop() = cancel()
        fun isScanning(): Boolean
        fun later(delayMs: Long, block: () -> Unit)
        fun note(text: String) {}
    }

    private var enabled = false
    private var appEnabled = false
    private var fingerprintEnabled = true
    private var mode = FaceUnlockMode.FaceOnly
    private var logAvailable = false
    private var screenOn = true
    private var locked = false
    private var startLineSeenAt = NONE
    private var playedStartAt = NONE
    private var pendingStartAt = NONE
    private var successAt = NONE
    private var presentAt = NONE
    private var nonFaceUnlockAt = NONE
    private var fingerprintAt = NONE
    private var screenOnAt = NONE
    private var scanStartedAt = NONE

    private var awaitingAppStart = false
    private var earlyToken = 0
    private var endToken = 0
    private var unlockToken = 0

    private var accepted = false

    private var appScan = false

    private var appPromptActive = false
    private var promptToken = 0

    private val appScanning: Boolean get() = appScan && out.isScanning()

    private val faceOnly: Boolean get() = mode == FaceUnlockMode.FaceOnly && logAvailable

    fun configure(
        enabled: Boolean,
        mode: FaceUnlockMode,
        logAvailable: Boolean,
        appEnabled: Boolean = false,
        fingerprintEnabled: Boolean = true
    ) {
        this.enabled = enabled
        this.fingerprintEnabled = fingerprintEnabled
        this.appEnabled = appEnabled
        this.mode = mode
        this.logAvailable = logAvailable
        if (out.isScanning() && (if (appScan) !appEnabled else !enabled)) out.cancel()
    }

    fun syncState(screenOn: Boolean, locked: Boolean) {
        this.screenOn = screenOn
        this.locked = locked
    }

    fun onScreenOn(locked: Boolean, now: Long) {
        screenOn = true
        this.locked = locked
        screenOnAt = now
        if (out.isScanning() && !appScan && scanStartedAt != NONE && now - scanStartedAt in 0..SCAN_KEEP_MS) {
            pendingStartAt = NONE
            return
        }
        resetUnlock()
        if (out.isScanning()) out.stop()
        val afterNonFaceUnlock = nonFaceUnlockAt != NONE && now - nonFaceUnlockAt in 0..NON_FACE_LEAD_MS
        nonFaceUnlockAt = NONE
        val afterFingerprint = fingerprintAt != NONE && now - fingerprintAt in 0..NON_FACE_LEAD_MS
        fingerprintAt = NONE
        if (afterFingerprint && fingerprintEnabled && enabled && !out.isScanning()) {
            beginFingerprintScan(now)
            return
        }
        if (afterNonFaceUnlock && faceOnly) return
        if (!enabled || !locked || out.isScanning()) return
        val startedByLog = pendingStartAt != NONE && now - pendingStartAt <= START_LEAD_MS
        pendingStartAt = NONE
        val startLinesTrusted = startLineSeenAt != NONE && now - startLineSeenAt in 0..START_TRUST_MS
        if (startedByLog || !(faceOnly && startLinesTrusted)) beginScan(now)
    }

    fun onScreenOff() {
        screenOn = false
        locked = true
        scanStartedAt = NONE
        pendingStartAt = NONE
        nonFaceUnlockAt = NONE
        fingerprintAt = NONE
        screenOnAt = NONE
        awaitingAppStart = false
        earlyToken++
        clearAppPrompt()
        resetUnlock()
        accepted = false
        if (out.isScanning()) out.stop()
    }

    fun onUserPresent(now: Long) {
        locked = false
        if (appScanning) return
        if (!enabled || !out.isScanning()) {
            resetUnlock()
            return
        }
        if (accepted) {
            resetUnlock()
            return
        }
        if (!faceOnly) {
            resetUnlock()
            acceptNow()
            return
        }
        presentAt = now
        if (!confirm()) {
            val token = ++unlockToken
            out.later(CONFIRM_GRACE_MS) {
                if (token == unlockToken && presentAt != NONE && out.isScanning()) {
                    resetUnlock()
                    out.cancel()
                }
            }
        }
    }

    fun onLog(event: FaceUnlockLogEvent, now: Long, strong: Boolean = true, fromApp: Boolean = false) {
        if (appScanning) {
            when (event) {
                FaceUnlockLogEvent.Start -> if (fromApp) {
                    awaitingAppStart = false
                    out.anchor()
                }
                FaceUnlockLogEvent.Success -> if (appEnabled && !accepted && strong) acceptNow()
                FaceUnlockLogEvent.End -> if (!accepted && !awaitingAppStart) rejectSoon(APP_END_GRACE_MS)
                FaceUnlockLogEvent.Failure -> if (!accepted && !awaitingAppStart) rejectNow()
            }
            return
        }
        if (fromApp) {
            if (event == FaceUnlockLogEvent.Start) {
                if (appEnabled && screenOn && !locked && !out.isScanning()) {
                    resetUnlock()
                    beginAppScan()
                    armAppPrompt()
                } else {
                    out.note(
                        "app scan not started: appEnabled=$appEnabled screenOn=$screenOn locked=$locked " +
                            "scanning=${out.isScanning()} appScan=$appScan"
                    )
                }
            }
            return
        }
        when (event) {
            FaceUnlockLogEvent.Start -> {
                startLineSeenAt = now
                if (!enabled || out.isScanning()) return
                if (!screenOn) {
                    pendingStartAt = now
                } else if (locked) {
                    resetUnlock()
                    beginScan(now)
                }
            }
            FaceUnlockLogEvent.Success -> {
                if (!enabled) return
                if (!out.isScanning()) {
                    val beatTheScreenOn = strong && pendingStartAt != NONE && now - pendingStartAt in 0..START_LEAD_MS
                    val lostScan = strong && screenOn && startLineSeenAt != NONE &&
                        startLineSeenAt != playedStartAt && now - startLineSeenAt in 0..SCAN_KEEP_MS
                    if (beatTheScreenOn || lostScan) {
                        pendingStartAt = NONE
                        resetUnlock()
                        beginScan(now)
                        acceptNow()
                    }
                    return
                }
                if (accepted) return
                if (!faceOnly) {
                    resetUnlock()
                    acceptNow()
                    return
                }
                if (strong) {
                    resetUnlock()
                    acceptNow()
                } else {
                    successAt = now
                    confirm()
                }
            }
            FaceUnlockLogEvent.Failure -> {
                successAt = NONE
                if (!out.isScanning()) {
                    pendingStartAt = NONE
                } else if (enabled && !accepted && !appScan) {
                    rejectNow()
                }
            }
            FaceUnlockLogEvent.End ->
                if (enabled && out.isScanning() && !accepted && successAt == NONE) rejectSoon(END_GRACE_MS)
        }
    }

    fun onUnlockType(type: UnlockType, now: Long) {
        if (type == UnlockType.Fingerprint && fingerprintEnabled && appEnabled && appPromptActive && !locked && screenOn && !out.isScanning()) {
            clearAppPrompt()
            nonFaceUnlockAt = now
            resetUnlock()
            beginFingerprintScan(now, app = true)
            return
        }
        val fingerprintPlays = type == UnlockType.Fingerprint && fingerprintEnabled && (appScanning || enabled)
        if (!appScanning && !(enabled && faceOnly) && !fingerprintPlays) return
        if (type != UnlockType.Face) nonFaceUnlockAt = now
        if (type == UnlockType.Fingerprint && fingerprintEnabled && !screenOn && !appScanning) fingerprintAt = now
        if (type == UnlockType.Fingerprint && fingerprintPlays && !appScanning && !out.isScanning() && screenOn) {
            val justWoke = screenOnAt == NONE || now - screenOnAt in 0..NON_FACE_LEAD_MS
            if (justWoke || locked) {
                resetUnlock()
                beginFingerprintScan(now)
                return
            }
        }
        if (!out.isScanning() || accepted) return
        when (type) {
            UnlockType.Face -> Unit
            UnlockType.Fingerprint -> {
                resetUnlock()
                if (fingerprintEnabled) {
                    accepted = true
                    out.fingerprint()
                } else {
                    out.cancel()
                }
            }
            UnlockType.Credential -> {
                resetUnlock()
                out.cancel()
            }
        }
    }

    fun onBouncerShown() {
        if (appScanning) return
        if (!enabled || !faceOnly) return
        if (!out.isScanning() || accepted) return
        resetUnlock()
        out.cancel()
    }

    fun onAppUnlocked(now: Long) {
        clearAppPrompt()
        if (!appEnabled || !appScanning || accepted) return
        if (nonFaceUnlockAt != NONE && now - nonFaceUnlockAt in 0..NON_FACE_LEAD_MS * 2) return
        acceptNow()
    }

    fun onAppPromptDismissed() {
        clearAppPrompt()
        if (!appScanning || accepted) return
        resetUnlock()
        awaitingAppStart = false
        out.cancel()
    }

    private fun confirm(): Boolean {
        if (successAt == NONE || presentAt == NONE || !out.isScanning()) return false
        if (kotlin.math.abs(presentAt - successAt) > CONFIRM_WINDOW_MS) return false
        resetUnlock()
        acceptNow()
        return true
    }

    fun onAppLockRequired() {
        if (!appEnabled || !screenOn || locked || out.isScanning()) {
            out.note("app lock required, scan not started early: appEnabled=$appEnabled screenOn=$screenOn locked=$locked scanning=${out.isScanning()}")
            return
        }
        resetUnlock()
        beginAppScan(early = true)
        armAppPrompt()
        awaitingAppStart = true
        val token = ++earlyToken
        out.later(EARLY_APP_GRACE_MS) {
            if (token == earlyToken && awaitingAppStart && appScan && out.isScanning() && !accepted) {
                awaitingAppStart = false
                out.cancel()
            }
        }
    }

    private fun rejectSoon(graceMs: Long) {
        val token = ++endToken
        out.later(graceMs) {
            if (token == endToken && out.isScanning() && !accepted) out.reject()
        }
    }

    private fun rejectNow() {
        resetUnlock()
        out.reject()
    }

    private fun armAppPrompt() {
        appPromptActive = true
        val token = ++promptToken
        out.later(APP_PROMPT_KEEP_MS) {
            if (token == promptToken) appPromptActive = false
        }
    }

    private fun clearAppPrompt() {
        appPromptActive = false
        promptToken++
    }

    private fun beginScan(now: Long) {
        clearAppPrompt()
        earlyToken++
        endToken++
        awaitingAppStart = false
        accepted = false
        appScan = false
        scanStartedAt = now
        out.startScan(scanWaitMs(now))
    }

    private fun scanWaitMs(now: Long): Int {
        if (logAvailable) return FaceIdController.LOG_WAIT_MS
        val seenAt = startLineSeenAt
        if (seenAt == NONE || now - seenAt !in 0..SCAN_KEEP_MS) return FaceIdController.REAL_WAIT_MS
        val left = FaceIdController.REAL_WAIT_MS + SCREEN_ON_LAG_MS - (now - seenAt).toInt()
        return maxOf(FaceIdController.REAL_WAIT_MS, left)
    }

    private fun beginFingerprintScan(now: Long, app: Boolean = false) {
        earlyToken++
        endToken++
        scanStartedAt = if (app) NONE else now
        awaitingAppStart = false
        accepted = true
        appScan = app
        if (app) out.startAppFingerprintScan() else out.startFingerprintScan()
    }

    private fun beginAppScan(early: Boolean = false) {
        earlyToken++
        endToken++
        scanStartedAt = NONE
        awaitingAppStart = false
        accepted = false
        appScan = true
        nonFaceUnlockAt = NONE
        if (early) out.startAppScanEarly() else out.startAppScan()
    }

    private fun acceptNow() {
        playedStartAt = startLineSeenAt
        accepted = true
        out.accept()
    }

    private fun resetUnlock() {
        endToken++
        successAt = NONE
        presentAt = NONE
        unlockToken++
    }

    companion object {
        const val CONFIRM_WINDOW_MS = 4000L

        const val CONFIRM_GRACE_MS = 600L

        const val START_TRUST_MS = 10 * 60_000L

        const val START_LEAD_MS = 1500L

        const val NON_FACE_LEAD_MS = 1500L

        const val EARLY_APP_GRACE_MS = 2500L

        const val SCREEN_ON_LAG_MS = 750

        const val SCAN_KEEP_MS = 3000L

        const val APP_END_GRACE_MS = 400L

        const val END_GRACE_MS = 150L

        const val APP_PROMPT_KEEP_MS = 60_000L

        private const val NONE = Long.MIN_VALUE
    }
}
