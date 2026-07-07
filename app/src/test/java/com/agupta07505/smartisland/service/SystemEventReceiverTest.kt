/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.content.Intent
import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SystemEventReceiverTest {

    @Test
    fun testPowerDisconnectedRemovesBatteryNotification() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        
        val context = mockk<Context>()
        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_POWER_DISCONNECTED
        
        receiver.onReceive(context, intent)
        
        verify { repo.removeNotification("system_battery") }
    }
}
