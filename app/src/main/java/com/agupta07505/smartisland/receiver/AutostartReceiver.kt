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
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.runCatchingLogged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutostartReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SmartIslandSettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_PRESENT) {

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                runCatchingLogged("AutostartReceiver", "Failed handling autostart broadcast") {
                    val settings = settingsRepository.settings.first()
                    if (settings.enabled && ShizukuManager.hasPermission()) {
                        ShizukuManager.autoGrantAllPermissions(context)
                    }
                }
                pendingResult.finish()
            }
        }
    }
}
