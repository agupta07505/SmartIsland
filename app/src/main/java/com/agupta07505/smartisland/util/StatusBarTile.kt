/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.data.SmartIslandSettingsRepository

object StatusBarSwitch {
    suspend fun set(repository: SmartIslandSettingsRepository, value: Boolean, source: String) {
        val serviceWasTracking = StatusBarReapplier.shared.value != StatusBarReapplier.Status.Off
        repository.setStatusBarHidden(value)
        if (!value && !serviceWasTracking && ShizukuManager.hasPermission()) {
            StatusBarHider.restore()
        }
    }
}

internal enum class StatusBarTileLabel { Off, Hidden, Applying, Failed, NeedsShizuku }

internal data class StatusBarTileModel(val active: Boolean, val label: StatusBarTileLabel)

internal fun statusBarTileModel(
    hidden: Boolean,
    shizukuReady: Boolean,
    status: StatusBarReapplier.Status
): StatusBarTileModel = when {
    !hidden -> StatusBarTileModel(false, if (shizukuReady) StatusBarTileLabel.Off else StatusBarTileLabel.NeedsShizuku)
    !shizukuReady || status == StatusBarReapplier.Status.ShizukuLost ->
        StatusBarTileModel(true, StatusBarTileLabel.NeedsShizuku)
    status == StatusBarReapplier.Status.Failed -> StatusBarTileModel(true, StatusBarTileLabel.Failed)
    status == StatusBarReapplier.Status.Applied -> StatusBarTileModel(true, StatusBarTileLabel.Hidden)
    else -> StatusBarTileModel(true, StatusBarTileLabel.Applying)
}
