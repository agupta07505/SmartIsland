/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.agupta07505.smartisland.util.KeyguardGate

/**
 * An invisible activity that exists only to call `KeyguardManager.requestDismissKeyguard`, which needs an
 * Activity (Rule_Book §8.3). It shows over the lockscreen, asks the user to unlock, hands the result to
 * [KeyguardGate] (which runs or drops the action the island cached) and finishes.
 */
class UnlockTrampolineActivity : Activity() {

    private var reported = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager == null || !keyguardManager.isKeyguardLocked) {
            report(success = true)
            return
        }
        runCatching {
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = report(success = true)
                override fun onDismissCancelled() = report(success = false)
                override fun onDismissError() = report(success = false)
            })
        }.onFailure { report(success = false) }
    }

    override fun onDestroy() {
        // Torn down before the keyguard answered (rotated, killed): the cached action must not linger.
        if (!reported && !isChangingConfigurations) KeyguardGate.onDismissFailed()
        super.onDestroy()
    }

    private fun report(success: Boolean) {
        if (reported) return
        reported = true
        if (success) KeyguardGate.onDismissSucceeded() else KeyguardGate.onDismissFailed()
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
