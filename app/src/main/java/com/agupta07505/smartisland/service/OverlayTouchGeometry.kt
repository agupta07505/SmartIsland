/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.view.Display
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.collapsedCompanionSizeDp
import com.agupta07505.smartisland.data.edgeCamera
import com.agupta07505.smartisland.data.shouldShowIdleDot
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CollapsedSlots
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.DisplayKey
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.model.bandCentreFromScreenCentreDp
import com.agupta07505.smartisland.model.cameraCentreInPill
import com.agupta07505.smartisland.model.centreBandOverlapsCutout
import com.agupta07505.smartisland.model.cutoutCardCount
import com.agupta07505.smartisland.model.cutoutTouchTarget
import com.agupta07505.smartisland.model.hasSecondaryCutout
import com.agupta07505.smartisland.model.notchCompanionPlan
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.ui.collapsedGroupSpan
import com.agupta07505.smartisland.ui.collapsedTopShiftDp
import com.agupta07505.smartisland.util.CameraCutoutDetector
import com.agupta07505.smartisland.util.DetectedCutoutInfo

/**
 * Camera cutout band, collapsed group, edge pill and idle dot rectangles, plus the side-camera placement they depend on
 * The service owns the lifecycle, everything here is read-only against the view model
 */
internal class OverlayTouchGeometry(
    private val service: Context,
    private val viewModelHolder: OverlayViewModelHolder,
    private val rotation: OverlayRotationController,
    private val touchRegionSupported: () -> Boolean,
) {
    private val viewModel: IslandViewModel get() = viewModelHolder.get()
    private val resources get() = service.resources

    private var cachedCutoutKey: DisplayKey? = null
    private var cachedCutout: DetectedCutoutInfo? = null
    private var cachedCutoutGeneration = -1

    fun invalidateCutout() {
        cachedCutout = null
    }

    private fun currentDisplayKey(): DisplayKey {
        val metrics = resources.displayMetrics
        val rotation = (service.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: rotation.lastRotation
        return DisplayKey(metrics.widthPixels, metrics.heightPixels, rotation, metrics.densityDpi)
    }

    private fun detectedCutout(): DetectedCutoutInfo {
        val key = currentDisplayKey()
        val generation = CameraCutoutDetector.generation
        cachedCutout?.let { if (key == cachedCutoutKey && generation == cachedCutoutGeneration) return it }
        return CameraCutoutDetector.detect(service).also {
            cachedCutout = it
            cachedCutoutKey = key
            cachedCutoutGeneration = generation
        }
    }

    private fun sideCameraNow(settings: SmartIslandSettings, isLandscapeNow: Boolean): Pair<CameraSide, CutoutArea?> {
        val screenWidthDp = rotation.screenWidthPxNow() / resources.displayMetrics.density
        val area = CameraCutoutDetector.layoutAreaFor(settings, detectedCutout(), isLandscapeNow, screenWidthDp)
        return SideCamera.sideOf(area?.centreXDp) to area
    }

    /** Companion circle side: away from a side camera, else the user's choice. */
    fun circleLeftNow(settings: SmartIslandSettings, isLandscapeNow: Boolean): Boolean =
        SideCamera.circleLeft(
            sideCameraNow(settings, isLandscapeNow).first,
            settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
        )

    /** Collapsed pill's left edge before the screen-edge clamp (px). */
    fun placedMainLeftPx(settings: SmartIslandSettings, isLandscapeNow: Boolean, screenWidthPx: Float, density: Float,
            mainWidthPx: Float): Float {
        val (side, area) = sideCameraNow(settings, isLandscapeNow)
        if (side == CameraSide.Centre || area == null) {
            return screenWidthPx / 2f + settings.xOffset * density - mainWidthPx / 2f
        }
        return SideCamera.desiredLeftDp(
            side, screenWidthPx / density, area.centreXDp, area.widthDp, mainWidthPx / density, settings.xOffset
        ) * density
    }

    fun cutoutTouchRect(
        settingsVal: SmartIslandSettings,
        isLandscapeNow: Boolean,
        windowHeightPx: Int,
        mainLeftPx: Float,
        ignoreBandOverlap: Boolean = false
    ): Rect? {
        if (!touchRegionSupported()) return null
        val density = resources.displayMetrics.density
        val screenWidth = rotation.screenWidthPxNow()
        val screenWidthDp = screenWidth / density
        val area = CameraCutoutDetector.areaFor(settingsVal, detectedCutout(), isLandscapeNow, screenWidthDp)
            ?: return null
        // the notch's window starts at the screen's top, the pill's at its offset
        val windowTopDp = if (settingsVal.enableNotchMode) 0f else settingsVal.yOffset
        val pillLeftDp = mainLeftPx / density
        val holeInPillDp = cameraCentreInPill(
            screenWidthDp / 2f, area.centreXDp, pillLeftDp + settingsVal.width / 2f, settingsVal.width
        )
        // a side camera has its own band (model/SideCamera.kt)
        val sideBand = holeInPillDp?.let {
            SideCamera.zone(
                SideCamera.sideOf(area.centreXDp), settingsVal.width, it, area.widthDp
            )?.band
        }
        val band = sideBand ?: CollapsedSlots.zone(settingsVal.width, holeInPillDp, area.widthDp).band
        val overlaps = centreBandOverlapsCutout(
            bandCentreXDp = bandCentreFromScreenCentreDp(
                screenWidthDp, pillLeftDp, settingsVal.width, band
            ),
            bandWidthDp = band.widthDp,
            bandTopDp = windowTopDp,
            bandHeightDp = settingsVal.height,
            cutoutCentreXDp = area.centreXDp,
            cutoutWidthDp = area.widthDp,
            cutoutTopDp = area.topDp,
            cutoutHeightDp = area.heightDp
        )
        if (overlaps && !ignoreBandOverlap) return null
        val target = cutoutTouchTarget(area)
        val left = (screenWidth / 2f + (target.centreXDp - target.widthDp / 2f) * density).toInt().coerceAtLeast(0)
        val right = (screenWidth / 2f + (target.centreXDp + target.widthDp / 2f) * density).toInt()
            .coerceAtMost(screenWidth)
        val top = ((target.topDp - windowTopDp) * density).toInt().coerceAtLeast(0)
        val bottom = (((target.topDp + target.heightDp) - windowTopDp) * density).toInt()
            .coerceAtMost(windowHeightPx)
        return if (right > left && bottom > top) Rect(left, top, right, bottom) else null
    }

    class CollapsedGroupTouch(
        val rect: Rect,
        val startPx: Float,
        val endPx: Float,
        val heightPx: Float,
        /** Main pill's left edge (px), after the edge clamp. */
        val mainLeftPx: Float = 0f
    )

    fun edgeTouchRectPx(
        settingsVal: SmartIslandSettings,
        isLandscapeNow: Boolean,
        visibleCount: Int
    ): Rect? {
        if (!isLandscapeNow || !viewModelHolder.ready) return null
        if (!LandscapeLayoutStyle.isEdge(settingsVal.landscapeLayoutStyle)) return null
        // The pill is hidden (long press)
        edgeHiddenTouchRectPx(settingsVal, isLandscapeNow, visibleCount)?.let { return it }
        val away = visibleCount == 0 && viewModel.hasIdleStoredCards.value
        if (visibleCount == 0 && !away) return null
        val camera = viewModel.orientation.value.edgeCamera(settingsVal.edgePill, withContent = visibleCount > 0)
            ?: return null
        val cards = viewModel.visibleNotifications.value
        val selected = viewModel.selectedIndex.value.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
        val hasSecondary = !away &&
            notchCompanionPlan(cards, selected, viewModel.expanded.value).earShown
        val layout = EdgePillLayoutMath.compute(camera, hasSecondary = hasSecondary)
        val bounds = EdgePillLayoutMath.touchBounds(layout)
        val density = resources.displayMetrics.density
        return Rect(
            (bounds.left * density).toInt(),
            (bounds.top * density).toInt(),
            (bounds.right * density).toInt(),
            (bounds.bottom * density).toInt()
        )
    }

    /** Edge landscape layout, pill hidden by the long press on the camera middle. */
    fun edgeHiddenTouchRectPx(
        settingsVal: SmartIslandSettings,
        isLandscapeNow: Boolean,
        visibleCount: Int
    ): Rect? {
        if (!isLandscapeNow || !viewModelHolder.ready) return null
        if (!LandscapeLayoutStyle.isEdge(settingsVal.landscapeLayoutStyle)) return null
        if (!EdgePillRules.hiddenTargetActive(true, viewModel.pillHidden.value, visibleCount)) return null
        val camera = viewModel.orientation.value.edgeCamera(settingsVal.edgePill, withContent = visibleCount > 0)
            ?: return null
        val hiddenLayout = EdgePillLayoutMath.compute(camera, hasSecondary = false)
        val target = EdgePillRules.hiddenTarget(
            hiddenLayout.pill,
            EdgePillLayoutMath.holeCentreYDp(hiddenLayout)
        )
        val density = resources.displayMetrics.density
        return Rect(
            (target.left * density).toInt(),
            (target.top * density).toInt(),
            (target.right * density).toInt(),
            (target.bottom * density).toInt()
        )
    }

    /**
     * The one place that takes a touch while a fullscreen app hides the island: the camera cutout (edge layout in
     * landscape, the cutout or the collapsed pill's own area in portrait), in the island window's own coordinates
     */
    fun fullscreenCutoutTouchRectPx(settingsVal: SmartIslandSettings, isLandscapeNow: Boolean): Rect? {
        if (!viewModelHolder.ready) return null
        if (isLandscapeNow) {
            if (!LandscapeLayoutStyle.isEdge(settingsVal.landscapeLayoutStyle)) return null
            val camera = viewModel.orientation.value.edgeCamera(settingsVal.edgePill, withContent = false)
                ?: return null
            val hiddenLayout = EdgePillLayoutMath.compute(camera, hasSecondary = false)
            val target = EdgePillRules.hiddenTarget(
                hiddenLayout.pill,
                EdgePillLayoutMath.holeCentreYDp(hiddenLayout)
            )
            val density = resources.displayMetrics.density
            return Rect(
                (target.left * density).toInt(),
                (target.top * density).toInt(),
                (target.right * density).toInt(),
                (target.bottom * density).toInt()
            )
        }
        val group = collapsedGroupTouchRect(settingsVal, false)
        // Use the configured cutout here too, even if Android reported no cutout.
        return cutoutTouchRect(settingsVal, false, group.rect.bottom, group.mainLeftPx, ignoreBandOverlap = true)
            ?: group.rect
    }

    fun collapsedGroupTouchRect(
        settingsVal: SmartIslandSettings,
        isLandscapeNow: Boolean
    ): CollapsedGroupTouch {
        val density = resources.displayMetrics.density
        val screenWidth = rotation.screenWidthPxNow()
        val isSplitMode = hasSecondaryCutout(
            cutoutCardCount(
                viewModel.visibleNotifications.value, expanded = false
            )
        )
        val isCircleLeft = circleLeftNow(settingsVal, isLandscapeNow)
        val mainWidthPx = settingsVal.width * density
        val circleSizePx = collapsedCompanionSizeDp(settingsVal) * density
        val compactGapPx = 8f * density
        // landscape: the cutout may touch the screen edge, as in the window layout
        val edgePaddingPx = if (isLandscapeNow && !settingsVal.enableNotchMode) 0f else 8f * density
        val touchPaddingXPx = 16f * density
        val topShiftPx = collapsedTopShiftDp(
            isLandscapeNow, settingsVal.enableNotchMode, settingsVal.yOffset
        ) * density
        val pillHeightPx = (settingsVal.height + 16f) * density + topShiftPx
        val span = collapsedGroupSpan(
            placedMainLeftPx(settingsVal, isLandscapeNow, screenWidth.toFloat(), density, mainWidthPx),
            screenWidth.toFloat(), edgePaddingPx, mainWidthPx, circleSizePx, compactGapPx, isSplitMode, isCircleLeft,
            if (settingsVal.enableNotchMode) settingsVal.xOffset * density else null
        )
        return CollapsedGroupTouch(
            rect = Rect(
                (span.startPx - touchPaddingXPx).toInt().coerceAtLeast(0),
                topShiftPx.toInt(),
                (span.endPx + touchPaddingXPx).toInt().coerceAtMost(screenWidth),
                pillHeightPx.toInt()
            ),
            startPx = span.startPx,
            endPx = span.endPx,
            heightPx = pillHeightPx,
            mainLeftPx = span.mainLeftPx
        )
    }

    fun idleDotRectPx(
        settings: SmartIslandSettings,
        hasCards: Boolean,
        groupStartPx: Float,
        groupEndPx: Float,
        groupBottomPx: Float,
        density: Float,
        screenWidthPx: Int,
        isLandscape: Boolean
    ): Rect? {
        val dot = viewModel.idleDot.value ?: return null
        // This rectangle serves the collapsed window only
        val shown = shouldShowIdleDot(dot = dot, hasCards = hasCards, isLandscape = isLandscape)
        if (!shown) return null
        val slackPx = 8f * density
        val centerPx = screenWidthPx / 2f + dot.centerXDp * density
        val halfPx = dot.sizeDp * density / 2f
        return Rect(
            (centerPx - halfPx - slackPx).toInt().coerceAtLeast(0),
            (dot.topDp * density - slackPx).toInt().coerceAtLeast(0),
            (centerPx + halfPx + slackPx).toInt().coerceAtMost(screenWidthPx),
            ((dot.topDp + dot.sizeDp) * density + slackPx).toInt()
        )
    }
}
