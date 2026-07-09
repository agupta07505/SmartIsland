/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.app.Application
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository

class SmartIslandApp : Application() {
    val settingsRepository by lazy { SmartIslandSettingsRepository(applicationContext) }
    val notificationRepository: INotificationRepository by lazy { SmartIslandNotificationRepository() }
}
