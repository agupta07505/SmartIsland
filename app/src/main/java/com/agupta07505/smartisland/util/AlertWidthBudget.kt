/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.PillLayout

/**
 * The Android side of [PillLayout.maxAlertPillWidthDp] (Rule_Book §10.2): the display width, where the pill
 * sits and whether `WRITE_SECURE_SETTINGS` was granted (so native notification icons can be hidden). The
 * overlay service and the overlay composable both call this, so the window and the drawing agree.
 */
object AlertWidthBudget {

    fun hasSecureSettings(context: Context): Boolean =
        runCatching {
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun maxAlertPillWidthDp(context: Context, settings: SmartIslandSettings): Float {
        val oemProfile = OemDeviceRules.getOemWidthProfile(settings.deviceType)
        return PillLayout.maxAlertPillWidthDp(
            screenWidthDp = DisplayBounds.widthDp(context),
            pillCenterOffsetDp = settings.xOffset,
            iconsManaged = hasSecureSettings(context),
            capDp = oemProfile.maxPillWidthDp,
            leftMarginDp = oemProfile.leftMarginDp,
            rightMarginDp = oemProfile.rightMarginDp
        )
    }
}
