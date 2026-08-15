/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SystemEventReceiverTest {

    @Test
    fun testPowerDisconnectedRemovesBatteryNotificationWhenNormal() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        
        val context = mockk<Context>()
        val pm = mockk<PowerManager>(relaxed = true)
        every { pm.isPowerSaveMode } returns false
        every { context.getSystemService(Context.POWER_SERVICE) } returns pm

        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_POWER_DISCONNECTED
        every { intent.hasExtra(BatteryManager.EXTRA_LEVEL) } returns true
        every { intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        
        receiver.onReceive(context, intent)
        
        verify { repo.removeNotification("system_battery") }
    }

    @Test
    fun testBatteryLowPostsLowBatteryNotification() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        
        val context = mockk<Context>()
        val pm = mockk<PowerManager>(relaxed = true)
        every { pm.isPowerSaveMode } returns false
        every { context.getSystemService(Context.POWER_SERVICE) } returns pm

        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_BATTERY_LOW
        every { intent.hasExtra(BatteryManager.EXTRA_LEVEL) } returns true
        every { intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 15
        every { intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        
        receiver.onReceive(context, intent)
        
        verify {
            repo.postNotification(
                match { it.key == "system_battery" && it.title == "Low Battery" && it.text == "15%" },
                autoExpand = true
            )
        }
    }
}
