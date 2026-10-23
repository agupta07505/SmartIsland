/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandEventTest {
    private fun notif(mode: IslandMode, category: String? = null, key: String = "k") =
        IslandNotification(key = key, packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L, mode = mode, category = category)

    @Test
    fun batteryCategoriesMapToSeparateEvents() {
        assertEquals(IslandEvent.PLUGGED_IN, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_plugged_in")))
        assertEquals(IslandEvent.PLUGGED_OUT, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_plugged_out")))
        assertEquals(IslandEvent.FULLY_CHARGED, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_full")))
        assertEquals(IslandEvent.LOW_BATTERY, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_low")))
        assertEquals(IslandEvent.BATTERY_SAVER, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_saver")))
    }

    @Test
    fun wifiAndVpnReuseBluetoothCardWithCategory() {
        assertEquals(IslandEvent.WIFI, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "wifi")))
        assertEquals(IslandEvent.VPN, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "vpn")))
        assertEquals(IslandEvent.BLUETOOTH, IslandEvent.forNotification(notif(IslandMode.Bluetooth)))
    }

    @Test
    fun disconnectCardsHaveTheirOwnSwitches() {
        assertEquals(IslandEvent.WIFI_DISCONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "wifi_disconnected")))
        assertEquals(IslandEvent.VPN_DISCONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "vpn_disconnected")))
        assertTrue(IslandEvent.WIFI_DISCONNECTED.canSwitchAutoExpand && IslandEvent.VPN_DISCONNECTED.canHideOnLockScreen)
        assertEquals("Connectivity", IslandEvent.WIFI_DISCONNECTED.group)
    }

    @Test
    fun usbAndHeadphoneCardsHaveTheirOwnSwitches() {
        assertEquals(IslandEvent.USB_CONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "usb_connected")))
        assertEquals(IslandEvent.USB_DISCONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "usb_disconnected")))
        assertEquals(IslandEvent.HEADPHONES_CONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "headphones_connected")))
        assertEquals(IslandEvent.HEADPHONES_DISCONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "headphones_disconnected")))
        assertTrue(IslandEvent.USB_CONNECTED.canSwitchAutoExpand && IslandEvent.HEADPHONES_DISCONNECTED.canHideOnLockScreen)
        assertEquals("Connectivity", IslandEvent.HEADPHONES_CONNECTED.group)
    }

    @Test
    fun emptyModeIsNeverGated() {
        assertNull(IslandEvent.forNotification(notif(IslandMode.Empty)))
    }

    @Test
    fun eventIdsAreUnique() {
        assertEquals(IslandEvent.entries.size, IslandEvent.entries.map { it.id }.toSet().size)
        assertTrue(IslandEvent.entries.all { it.id.length <= 64 })
    }

    @Test
    fun audioRecordingHasItsOwnEvent() {
        assertEquals(IslandEvent.AUDIO_RECORDING, IslandEvent.forNotification(notif(IslandMode.AudioRecording)))
        assertEquals(IslandEvent.AUDIO_RECORDING, IslandEvent.forListenerMode(IslandMode.AudioRecording))
        assertEquals(IslandEvent.SCREEN_RECORDING, IslandEvent.forNotification(notif(IslandMode.ScreenRecording)))
    }

    @Test
    fun transferKindSelectsItsOwnEvent() {
        fun transfer(kind: TransferKind) = notif(IslandMode.DownloadUpload).copy(transferKind = kind)
        assertEquals(IslandEvent.DOWNLOAD, IslandEvent.forNotification(notif(IslandMode.DownloadUpload)))
        assertEquals(IslandEvent.UPLOAD, IslandEvent.forNotification(transfer(TransferKind.UPLOAD)))
        assertEquals(IslandEvent.BACKUP, IslandEvent.forNotification(transfer(TransferKind.BACKUP)))
        assertEquals(IslandEvent.FILE_COPY_MOVE, IslandEvent.forNotification(transfer(TransferKind.COPY_MOVE)))
        assertEquals(IslandEvent.FILE_DELETE, IslandEvent.forNotification(transfer(TransferKind.DELETE)))
        assertEquals(IslandEvent.APP_INSTALL, IslandEvent.forNotification(transfer(TransferKind.INSTALL)))
        assertEquals(IslandEvent.UPLOAD, IslandEvent.forListenerMode(IslandMode.DownloadUpload, TransferKind.UPLOAD))
        assertEquals(IslandEvent.DOWNLOAD, IslandEvent.forListenerMode(IslandMode.DownloadUpload))
    }

    @Test
    fun everyTransferKindHasADistinctEvent() {
        val events = TransferKind.entries.map { IslandEvent.forTransfer(it) }
        assertEquals(TransferKind.entries.size, events.toSet().size)
    }

    @Test
    fun uploadsAndBackupsPointUp() {
        assertTrue(notif(IslandMode.DownloadUpload).copy(transferKind = TransferKind.UPLOAD).isUpload)
        assertTrue(notif(IslandMode.DownloadUpload).copy(transferKind = TransferKind.BACKUP).isUpload)
        assertFalse(notif(IslandMode.DownloadUpload).copy(transferKind = TransferKind.DOWNLOAD).isUpload)
    }

    @Test
    fun uploadKeyStillSelectsUploadForPlainDownloadKind() {
        assertEquals(IslandEvent.UPLOAD, IslandEvent.forNotification(notif(IslandMode.DownloadUpload, key = "upload_7")))
    }

    @Test
    fun batteryCategoriesMapToTheirEvent() {
        assertEquals(IslandEvent.PLUGGED_OUT, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_plugged_out")))
        assertEquals(IslandEvent.PLUGGED_IN, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_plugged_in")))
    }

    @Test
    fun everyEventHasAShortLabelThatFitsATile() {
        IslandEvent.entries.forEach { event ->
            assertTrue("${event.id} short label is blank", event.shortLabel.isNotBlank())
            assertTrue("${event.id} short label too long: ${event.shortLabel}", event.shortLabel.length <= 14)
            assertTrue("${event.id} short label is not shorter than the long one", event.shortLabel.length <= event.label.length)
        }
    }

    @Test
    fun shortLabelsAreUniqueWithinAGroup() {
        IslandEvent.entries.groupBy { it.group }.forEach { (group, events) ->
            assertEquals("duplicate short label in $group", events.size, events.map { it.shortLabel }.toSet().size)
        }
    }

    @Test
    fun callsHaveNoAutoExpandSwitchEverythingElseHasOne() {
        assertFalse(IslandEvent.CALL.canSwitchAutoExpand)
        assertTrue(IslandEvent.PLUGGED_IN.canSwitchAutoExpand)
        assertTrue(IslandEvent.NOTIFICATION.canSwitchAutoExpand)
        assertTrue(IslandEvent.BLUETOOTH.canSwitchAutoExpand)
    }

    @Test
    fun loudMeansNotSilent() {
        assertTrue(notif(IslandMode.Notification).isLoud)
        assertFalse(notif(IslandMode.Notification).copy(isSilent = true).isLoud)
        // Live and system cards are never flagged silent, so they are loud.
        assertTrue(notif(IslandMode.Battery, "battery_plugged_in").isLoud)
    }
}
