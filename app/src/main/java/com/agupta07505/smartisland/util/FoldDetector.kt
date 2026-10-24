/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.os.Build
import com.agupta07505.smartisland.model.FoldDeviceInfo
import com.agupta07505.smartisland.model.FoldKind
import com.agupta07505.smartisland.model.FoldModels
import com.agupta07505.smartisland.model.FoldPosture
import com.agupta07505.smartisland.model.FoldPostureRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FoldDetector {
    private const val FEATURE_HINGE_ANGLE = "android.hardware.sensor.hinge_angle"

    @Volatile
    private var cached: FoldDeviceInfo? = null

    private const val DEV_PREFS = "smart_island_dev"
    private const val KEY_EMULATE = "emulate_foldable"
    private val EMULATED = FoldDeviceInfo(FoldKind.Book, "developer:emulated")

    private val emulation = MutableStateFlow(false)

    @Volatile
    private var emulationLoaded = false

    /** Developer options: make this phone act as a book-style foldable. */
    fun emulationFlow(context: Context): StateFlow<Boolean> {
        loadEmulation(context)
        return emulation
    }

    fun setEmulating(context: Context, value: Boolean) {
        context.applicationContext.getSharedPreferences(DEV_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_EMULATE, value).apply()
        emulationLoaded = true
        emulation.value = value
    }

    private fun loadEmulation(context: Context) {
        if (emulationLoaded) return
        emulation.value = context.applicationContext.getSharedPreferences(DEV_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_EMULATE, false)
        emulationLoaded = true
    }

    fun deviceInfo(context: Context): FoldDeviceInfo {
        loadEmulation(context)
        if (emulation.value) return EMULATED
        cached?.let { return it }
        val hinge = context.packageManager.hasSystemFeature(FEATURE_HINGE_ANGLE)
        val info = FoldModels.kindOf(Build.MANUFACTURER, Build.MODEL, Build.DEVICE, hinge)
        cached = info
        return info
    }

    fun isFoldable(context: Context): Boolean = deviceInfo(context).foldable

    fun postureFor(context: Context, widthDp: Float, heightDp: Float): FoldPosture {
        val info = deviceInfo(context)
        return FoldPostureRules.postureOf(info.kind, widthDp, heightDp)
    }
}
