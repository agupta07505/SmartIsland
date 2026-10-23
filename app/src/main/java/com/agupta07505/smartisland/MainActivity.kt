/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SmartIslandHomeScreen
import com.agupta07505.smartisland.ui.SmartIslandTheme
import com.agupta07505.smartisland.util.SystemServiceRecovery
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository

    private var requestedSection by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedSection = intent?.getStringExtra(com.agupta07505.smartisland.util.UtilityActions.EXTRA_OPEN_SECTION)
        SystemServiceRecovery.requestRecovery(this)

        setContent {
            val themeSettings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = SmartIslandSettings.Default
            )
            SmartIslandTheme(
                themeMode = themeSettings.themeMode,
                liquidGlass = themeSettings.liquidGlassTheme,
                materialYou = themeSettings.materialYou,
                accentColor = themeSettings.accentColor
            ) {
                SmartIslandHomeScreen(
                    repository = settingsRepository,
                    notificationRepository = notificationRepository,
                    openSection = requestedSection,
                    onOpenSectionHandled = { requestedSection = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedSection = intent.getStringExtra(com.agupta07505.smartisland.util.UtilityActions.EXTRA_OPEN_SECTION)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // The app widget setup screen reports back by request code; the result is also passed on as usual.
        com.agupta07505.smartisland.util.UtilityWidgets.onActivityResult(requestCode, resultCode)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        SystemServiceRecovery.requestRecovery(this)
    }
}
