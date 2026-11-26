/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.StatusBarHider
import com.agupta07505.smartisland.util.StatusBarPlan
import com.agupta07505.smartisland.util.StatusBarReapplier
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutostartReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SmartIslandSettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        runCatchingLogged("AutostartReceiver", "Autostart broadcast callback failed") {
            val action = intent.action ?: return@runCatchingLogged
            if (action != Intent.ACTION_BOOT_COMPLETED &&
                action != Intent.ACTION_MY_PACKAGE_REPLACED &&
                action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
                action != Intent.ACTION_USER_PRESENT
            ) return@runCatchingLogged

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    runSuspendCatchingLogged(
                        "AutostartReceiver",
                        "Failed handling autostart broadcast"
                    ) {
                        val settings = settingsRepository.settings.first()
                        if (settings.enabled && ShizukuManager.hasPermission()) {
                            ShizukuManager.reapplySavedSetup(context)
                        }
                        // USER_PRESENT is left to the overlay service, which already reapplies on unlock
                        if (settings.statusBarHidden && action != Intent.ACTION_USER_PRESENT &&
                            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
                        ) {
                            handleStatusBar(action, settings.statusBarHidePersistent, settings.statusBarPlan())
                        }
                    }
                } finally {
                    runCatchingLogged("AutostartReceiver", "goAsync finish failed") {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun handleStatusBar(action: String, persistent: Boolean, plan: StatusBarPlan) {
        // Shizuku comes up some seconds after boot
        val ready = StatusBarReapplier.awaitShizuku(timeoutMs = SHIZUKU_WAIT_MS)
        val boot = action == Intent.ACTION_BOOT_COMPLETED
        if (boot && !persistent) {
            settingsRepository.setStatusBarHidden(false)
            if (ready) StatusBarHider.restore() else AppLogRecorder.w(TAG, "boot: Shizuku not ready, restore skipped")
            return
        }
        if (!ready) {
            AppLogRecorder.w(TAG, "$action: Shizuku not ready, the overlay service retries")
            return
        }
        for (gap in StatusBarReapplier.PASS_GAPS_MS) {
            if (gap > 0) delay(gap)
            StatusBarHider.hide(plan)
        }
    }

    private companion object {
        const val TAG = "AutostartReceiver"
        const val SHIZUKU_WAIT_MS = 8_000L
    }
}
