/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SmartIslandHomeScreen
import com.agupta07505.smartisland.ui.SmartIslandTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask the system not to kill us for battery. This reduces (does not
        // eliminate) OEM background/Recents cleanup that would otherwise disable
        // the AccessibilityService. Shown once; skipped if already granted.
        // Note: this API is a Play-Store policy gray-area; it is legitimate for an
        // always-on accessibility overlay (sideload/Telegram distribution), but if you
        // ever publish to Play, confirm it meets an allowed use case.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(android.net.Uri.parse("package:$packageName"))
                    )
                } catch (_: Exception) { /* no handler on some OEMs */ }
            }
        }

        setContent {
            SmartIslandTheme {
                SmartIslandHomeScreen(
                    repository = settingsRepository,
                    notificationRepository = notificationRepository
                )
            }
        }
    }
}
