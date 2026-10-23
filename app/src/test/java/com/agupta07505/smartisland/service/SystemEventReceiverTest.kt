/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
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
    fun testPowerDisconnectedPostsBriefUnpluggedCard() {
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

        verify {
            repo.postNotification(
                match {
                    it.key == "system_battery" &&
                        it.category == "battery_plugged_out" &&
                        it.text == "80%"
                },
                autoExpand = true
            )
        }
    }

    @Test
    fun testPowerConnectedPostsPluggedInCardThenNothingElseImmediately() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        fun plugIntent(action: String) = mockk<Intent>().also {
            every { it.action } returns action
            every { it.hasExtra(BatteryManager.EXTRA_LEVEL) } returns true
            every { it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 60
            every { it.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
            every { it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns BatteryManager.BATTERY_PLUGGED_AC
            every { it.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING
        }

        receiver.onReceive(context, plugIntent(Intent.ACTION_POWER_CONNECTED))
        // The battery broadcast that follows a plug-in must not replace the plug card.
        receiver.onReceive(context, plugIntent(Intent.ACTION_BATTERY_CHANGED))

        verify(exactly = 1) {
            repo.postNotification(
                match { it.category == "battery_plugged_in" && it.title == "Plugged in" && it.text == "60%" },
                autoExpand = true
            )
        }
        verify(exactly = 0) {
            repo.postNotification(match { it.category == "battery_charging" }, any())
        }
    }

    @Test
    fun testChargeLimitPauseDoesNotCountAsUnplugged() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        fun batteryIntent(plugged: Int, status: Int) = mockk<Intent>().also {
            every { it.action } returns Intent.ACTION_BATTERY_CHANGED
            every { it.hasExtra(BatteryManager.EXTRA_LEVEL) } returns true
            every { it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
            every { it.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
            every { it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns plugged
            every { it.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns status
        }

        // Sticky broadcast on registration while charging: adopted silently.
        receiver.onReceive(context, batteryIntent(BatteryManager.BATTERY_PLUGGED_AC, BatteryManager.BATTERY_STATUS_CHARGING))
        // A charge limit pauses charging: still plugged, status NOT_CHARGING.
        receiver.onReceive(context, batteryIntent(BatteryManager.BATTERY_PLUGGED_AC, BatteryManager.BATTERY_STATUS_NOT_CHARGING))

        verify(exactly = 0) {
            repo.postNotification(match { it.category == "battery_plugged_out" }, any())
        }
        verify(exactly = 0) {
            repo.postNotification(any(), autoExpand = true)
        }
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

    @Test
    fun testSmartwatchConnectionIgnored() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        val watchDevice = mockk<BluetoothDevice>()
        val bluetoothClass = mockk<BluetoothClass>()
        every { bluetoothClass.majorDeviceClass } returns BluetoothClass.Device.Major.WEARABLE
        every { bluetoothClass.deviceClass } returns BluetoothClass.Device.WEARABLE_WRIST_WATCH
        every { watchDevice.bluetoothClass } returns bluetoothClass
        every { watchDevice.name } returns "Galaxy Watch6"
        every { watchDevice.address } returns "11:22:33:44:55:66"

        val intent = mockk<Intent>()
        every { intent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
        every { intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns watchDevice
        @Suppress("DEPRECATION")
        every { intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns watchDevice
        every { intent.getIntExtra(any(), -1) } returns -1

        receiver.onReceive(context, intent)

        // Smartwatch connection should NOT post any notification or expand the island
        verify(exactly = 0) { repo.postNotification(any(), any()) }
    }

    @Test
    fun testAudioDeviceConnectionAllowedAndDebounced() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        val audioDevice = mockk<BluetoothDevice>()
        val bluetoothClass = mockk<BluetoothClass>()
        every { bluetoothClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { bluetoothClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { audioDevice.bluetoothClass } returns bluetoothClass
        every { audioDevice.name } returns "Galaxy Buds Pro"
        every { audioDevice.address } returns "AA:BB:CC:DD:EE:01"

        val intent = mockk<Intent>()
        every { intent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
        every { intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns audioDevice
        @Suppress("DEPRECATION")
        every { intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns audioDevice
        every { intent.getIntExtra(any(), -1) } returns 85

        // First connection: autoExpand = true
        receiver.onReceive(context, intent)
        verify(exactly = 1) {
            repo.postNotification(
                match { it.key == "system_bluetooth" && it.title == "Galaxy Buds Pro" && it.text == "Connected • 85%" },
                autoExpand = true
            )
        }

        // Rapid second connection event for same device: autoExpand = false (debounced)
        receiver.onReceive(context, intent)
        verify(exactly = 1) {
            repo.postNotification(
                match { it.key == "system_bluetooth" },
                autoExpand = false
            )
        }
    }

    @Test
    fun testWearableDisconnectDoesNotDismissActiveAudioDeviceNotification() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        val audioDevice = mockk<BluetoothDevice>()
        val audioBtClass = mockk<BluetoothClass>()
        every { audioBtClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { audioBtClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { audioDevice.bluetoothClass } returns audioBtClass
        every { audioDevice.name } returns "Galaxy Buds Pro"
        every { audioDevice.address } returns "AA:BB:CC:DD:EE:01"

        // Connect audio device
        val connectIntent = mockk<Intent>()
        every { connectIntent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
        every { connectIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns audioDevice
        @Suppress("DEPRECATION")
        every { connectIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns audioDevice
        every { connectIntent.getIntExtra(any(), -1) } returns 90
        receiver.onReceive(context, connectIntent)

        // Now an unrelated watch disconnects
        val watchDevice = mockk<BluetoothDevice>()
        every { watchDevice.address } returns "99:88:77:66:55:44"
        val watchDisconnectIntent = mockk<Intent>()
        every { watchDisconnectIntent.action } returns BluetoothDevice.ACTION_ACL_DISCONNECTED
        every { watchDisconnectIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns watchDevice
        @Suppress("DEPRECATION")
        every { watchDisconnectIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns watchDevice

        receiver.onReceive(context, watchDisconnectIntent)

        // The audio device's system_bluetooth notification must NOT be removed!
        verify(exactly = 0) { repo.removeNotification("system_bluetooth") }

        // Now the actual audio device disconnects
        val audioDisconnectIntent = mockk<Intent>()
        every { audioDisconnectIntent.action } returns BluetoothDevice.ACTION_ACL_DISCONNECTED
        every { audioDisconnectIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns audioDevice
        @Suppress("DEPRECATION")
        every { audioDisconnectIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns audioDevice

        receiver.onReceive(context, audioDisconnectIntent)

        // Now it SHOULD be removed
        verify(exactly = 1) { repo.removeNotification("system_bluetooth") }
    }
}
