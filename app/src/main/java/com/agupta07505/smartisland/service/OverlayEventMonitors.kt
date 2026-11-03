/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.util.ConnectivityEventMonitor
import com.agupta07505.smartisland.util.DeviceStateEventMonitor
import com.agupta07505.smartisland.util.HotspotStateMonitor
import com.agupta07505.smartisland.util.TransferSpeedMonitor
import com.agupta07505.smartisland.util.WiredDeviceEventMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class OverlayEventMonitors(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: INotificationRepository,
    private val settings: () -> SmartIslandSettings,
    private val shadeOpen: StateFlow<Boolean>
) {
    private var connectivity: ConnectivityEventMonitor? = null
    private var wiredDevice: WiredDeviceEventMonitor? = null
    private var deviceState: DeviceStateEventMonitor? = null
    private var hotspot: HotspotStateMonitor? = null
    private var transferSpeed: TransferSpeedMonitor? = null

    fun start() {
        connectivity = ConnectivityEventMonitor(
            context = context,
            scope = scope,
            repository = repository,
            settingsProvider = settings,
            shadeOpen = shadeOpen
        ).also { it.start() }

        hotspot = HotspotStateMonitor(
            context = context,
            scope = scope,
            repository = repository,
            settingsProvider = settings
        ).also { it.start() }

        wiredDevice = WiredDeviceEventMonitor(
            context = context,
            scope = scope,
            repository = repository,
            settingsProvider = settings
        ).also { it.start() }

        deviceState = DeviceStateEventMonitor(
            context = context,
            scope = scope,
            repository = repository,
            settingsProvider = settings,
            shadeOpen = shadeOpen
        ).also { it.start() }

        transferSpeed = TransferSpeedMonitor(
            context = context,
            scope = scope,
            repository = repository,
            enabled = {
                val s = settings()
                s.enabled && s.detectTransfersBySpeed &&
                    (s.isEventEnabled(IslandEvent.DOWNLOAD.id) ||
                        s.isEventEnabled(IslandEvent.UPLOAD.id))
            }
        ).also { it.start() }
    }

    fun stop() {
        connectivity?.stop()
        connectivity = null
        wiredDevice?.stop()
        wiredDevice = null
        deviceState?.stop()
        deviceState = null
        hotspot?.stop()
        hotspot = null
        transferSpeed?.stop()
        transferSpeed = null
    }
}
