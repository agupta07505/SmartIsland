/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import androidx.datastore.preferences.core.MutablePreferences
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.CutoutFitPlan
import com.agupta07505.smartisland.model.EdgePillFit
import com.agupta07505.smartisland.model.EdgePillTuning

internal object SettingsLayoutWriter {
    private val NOTCH_LAYOUT = listOf(175f, 35f, 0f, 0f, 20f)
    private val COMPACT_LAYOUT = listOf(112f, 34f, 0f, 10f, 20f)

    private fun readLayout(prefs: MutablePreferences): List<Float> {
        val d = SmartIslandSettings.Default
        return listOf(
            validDimension(prefs[SettingsKeys.Width], d.width, SmartIslandSettings.MIN_WIDTH,
                SmartIslandSettings.MAX_WIDTH),
            validDimension(prefs[SettingsKeys.Height], d.height, SmartIslandSettings.MIN_HEIGHT,
                SmartIslandSettings.MAX_HEIGHT),
            validDimension(prefs[SettingsKeys.XOffset], d.xOffset, SmartIslandSettings.MIN_X_OFFSET,
                SmartIslandSettings.MAX_X_OFFSET),
            validDimension(prefs[SettingsKeys.YOffset], d.yOffset, SmartIslandSettings.MIN_Y_OFFSET,
                SmartIslandSettings.MAX_Y_OFFSET),
            validDimension(prefs[SettingsKeys.CornerRadius], d.cornerRadius, SmartIslandSettings.MIN_CORNER_RADIUS,
                SmartIslandSettings.MAX_CORNER_RADIUS)
        )
    }

    private fun writeLayout(prefs: MutablePreferences, values: List<Float>) {
        val d = SmartIslandSettings.Default
        prefs[SettingsKeys.Width] = validDimension(values[0], d.width, SmartIslandSettings.MIN_WIDTH,
            SmartIslandSettings.MAX_WIDTH)
        prefs[SettingsKeys.Height] = validDimension(values[1], d.height, SmartIslandSettings.MIN_HEIGHT,
            SmartIslandSettings.MAX_HEIGHT)
        prefs[SettingsKeys.XOffset] = validDimension(values[2], d.xOffset, SmartIslandSettings.MIN_X_OFFSET,
            SmartIslandSettings.MAX_X_OFFSET)
        prefs[SettingsKeys.YOffset] = validDimension(values[3], d.yOffset, SmartIslandSettings.MIN_Y_OFFSET,
            SmartIslandSettings.MAX_Y_OFFSET)
        prefs[SettingsKeys.CornerRadius] = validDimension(values[4], d.cornerRadius,
            SmartIslandSettings.MIN_CORNER_RADIUS, SmartIslandSettings.MAX_CORNER_RADIUS)
    }

    private fun savedNotchLayout(prefs: MutablePreferences): List<Float>? =
        prefs[SettingsKeys.SavedNotchLayout]?.split(",")?.mapNotNull { v -> v.toFloatOrNull() }
            ?.takeIf { v -> v.size == 5 }

    private fun saveEdgeLook(prefs: MutablePreferences, leaving: EdgePillTuning) {
        val look = leaving.coerced()
        prefs[if (leaving.flatEdge) SettingsKeys.SavedEdgeFlatLook else SettingsKeys.SavedEdgeRoundLook] = listOf(
            look.widthDp, look.xOffsetDp, look.cornerRadiusDp, look.yOffsetDp,
            look.heightDp, look.contentPercent
        ).joinToString(",")
    }

    fun switchLayoutMode(
        prefs: MutablePreferences,
        toNotch: Boolean,
        compactFallback: CutoutFitPlan?,
        teardropPhone: Boolean
    ) {
        val currentlyNotch = prefs[SettingsKeys.EnableNotchMode] ?: teardropPhone
        if (currentlyNotch == toNotch) return
        prefs[if (currentlyNotch) SettingsKeys.SavedNotchLayout else SettingsKeys.SavedCompactLayout] =
            readLayout(prefs).joinToString(",")
        val saved = prefs[if (toNotch) SettingsKeys.SavedNotchLayout else SettingsKeys.SavedCompactLayout]
            ?.split(",")?.mapNotNull { v -> v.toFloatOrNull() }?.takeIf { v -> v.size == 5 }
        val target = saved ?: if (toNotch) {
            NOTCH_LAYOUT
        } else if (compactFallback != null) {
            listOf(compactFallback.widthDp, compactFallback.heightDp, compactFallback.xOffsetDp,
                compactFallback.yOffsetDp, compactFallback.cornerRadiusDp)
        } else {
            COMPACT_LAYOUT
        }
        prefs[SettingsKeys.EnableNotchMode] = toNotch
        writeLayout(prefs, target)
    }

    fun applyCutoutFit(
        prefs: MutablePreferences,
        plan: CutoutFitPlan,
        edgeHole: CutoutArea?,
        teardrop: Boolean?,
        teardropPhone: Boolean
    ) {
        val fit = listOf(plan.widthDp, plan.heightDp, plan.xOffsetDp, plan.yOffsetDp, plan.cornerRadiusDp)
        val wasNotch = prefs[SettingsKeys.EnableNotchMode] ?: teardropPhone
        val toNotch = teardrop ?: wasNotch
        when {
            wasNotch && !toNotch -> {
                prefs[SettingsKeys.SavedNotchLayout] = readLayout(prefs).joinToString(",")
                writeLayout(prefs, fit)
            }
            !wasNotch && toNotch -> {
                writeLayout(prefs, fit)
                prefs[SettingsKeys.SavedCompactLayout] = readLayout(prefs).joinToString(",")
                writeLayout(prefs, savedNotchLayout(prefs) ?: NOTCH_LAYOUT)
            }
            else -> writeLayout(prefs, fit)
        }
        if (teardrop != null) prefs[SettingsKeys.EnableNotchMode] = toNotch
        prefs[SettingsKeys.CameraCutoutManual] = false
        prefs[SettingsKeys.CutoutFitDone] = true
        // The landscape edge pill follows the same hole
        if (edgeHole != null) {
            var edge = EdgePillTuning.decode(prefs[SettingsKeys.EdgePill], flatEdgeDefault = teardropPhone)
            if (teardrop != null && edge.flatEdge != teardrop) {
                saveEdgeLook(prefs, edge)
                edge = edge.copy(flatEdge = teardrop)
            }
            prefs[SettingsKeys.EdgePill] = EdgePillFit.fitPortrait(edge, edgeHole).coerced().encode()
        }
    }

    fun switchEdgeLook(
        prefs: MutablePreferences,
        current: EdgePillTuning,
        toFlat: Boolean,
        firstVisit: EdgePillTuning?
    ): EdgePillTuning {
        if (current.flatEdge == toFlat) return current.copy(flatEdge = toFlat)
        saveEdgeLook(prefs, current)
        val saved = prefs[if (toFlat) SettingsKeys.SavedEdgeFlatLook else SettingsKeys.SavedEdgeRoundLook]
            ?.split(",")?.mapNotNull { v -> v.toFloatOrNull() }?.takeIf { v -> v.size == 4 || v.size == 6 }
        val result = if (saved != null) {
            current.copy(
                flatEdge = toFlat, widthDp = saved[0], xOffsetDp = saved[1], cornerRadiusDp = saved[2],
                    yOffsetDp = saved[3],
                heightDp = saved.getOrNull(4) ?: current.heightDp,
                contentPercent = saved.getOrNull(5) ?: current.contentPercent
            )
        } else {
            // The vertical offset means different things in the 2 looks
            if (toFlat && firstVisit == null) {
                current.copy(
                    flatEdge = true,
                    widthDp = EdgePillTuning.DEFAULT_FLAT_WIDTH,
                    heightDp = EdgePillTuning.DEFAULT_HEIGHT,
                    cornerRadiusDp = EdgePillTuning.DEFAULT_RADIUS,
                    contentPercent = EdgePillTuning.DEFAULT_CONTENT_PERCENT,
                    xOffsetDp = 0f,
                    yOffsetDp = 0f
                )
            } else {
                (firstVisit ?: current).copy(flatEdge = toFlat, yOffsetDp = 0f)
            }
        }.coerced()
        prefs[SettingsKeys.EdgePill] = result.encode()
        return result
    }
}
