/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.CutoutArea

object CutoutPlacement {
    const val SIDE_INSET_DP = 40f

    /** Width a teardrop starts with when the phone's own cutout is not detected. */
    const val TEARDROP_DEFAULT_WIDTH_DP = 60f

    fun isPlaced(settings: SmartIslandSettings): Boolean =
        !settings.enableNotchMode &&
            (settings.cameraCutoutStyle != SmartIslandSettings.CUTOUT_STYLE_AUTO || settings.cameraCutoutManual)

    /** The radius slider: the hole is round, so width and height are both its diameter (12 to 60 dp). */
    val RADIUS_RANGE_DP = SmartIslandSettings.MIN_CUTOUT_WIDTH / 2f..SmartIslandSettings.MAX_CUTOUT_HEIGHT / 2f

    /** X offsets from a style's anchor that keep a hole of [radiusDp] wholly on a screen [screenWidthDp] wide. */
    fun xRangeDp(style: String, screenWidthDp: Float, radiusDp: Float): ClosedFloatingPointRange<Float> {
        val half = screenWidthDp / 2f
        val anchor = anchorXDp(style, screenWidthDp)
        val min = (-half + radiusDp - anchor).coerceIn(SmartIslandSettings.MIN_CUTOUT_X, 0f)
        val max = (half - radiusDp - anchor).coerceIn(0f, SmartIslandSettings.MAX_CUTOUT_X)
        return min..max
    }

    /** Anchor centre, dp from the screen centre. */
    fun anchorXDp(style: String, screenWidthDp: Float): Float {
        val reach = (screenWidthDp / 2f - SIDE_INSET_DP).coerceAtLeast(0f)
        return when (style) {
            SmartIslandSettings.CUTOUT_STYLE_LEFT -> -reach
            SmartIslandSettings.CUTOUT_STYLE_RIGHT -> reach
            else -> 0f
        }
    }

    /** A teardrop hangs from the top edge of the screen, so it has no top offset of its own. */
    fun isFlushWithTop(style: String): Boolean = style == SmartIslandSettings.CUTOUT_STYLE_TEARDROP

    /** The hole a style puts on the screen, whatever detection finds; null for auto. */
    fun styledArea(settings: SmartIslandSettings, screenWidthDp: Float): CutoutArea? {
        val style = settings.cameraCutoutStyle
        if (style == SmartIslandSettings.CUTOUT_STYLE_AUTO) return null
        val topDp = if (isFlushWithTop(style)) 0f else settings.cameraCutoutY
        return areaAt(settings, anchorXDp(style, screenWidthDp), topDp)
    }

    /** The hole the user placed (a style, or the manual area), null in notch mode or when none is placed. */
    fun area(settings: SmartIslandSettings, screenWidthDp: Float): CutoutArea? {
        if (settings.enableNotchMode) return null
        return styledArea(settings, screenWidthDp) ?: if (settings.cameraCutoutManual) areaAt(settings, 0f) else null
    }

    /**
     * The hole the user placed in portrait, as the landscape edge pill follows it: a style, or the manual hole (notch
     * mode follows a style only), null when none is placed
     */
    fun placedFor(settings: SmartIslandSettings, screenWidthDp: Float): CutoutArea? =
        styledArea(settings, screenWidthDp) ?: area(settings, screenWidthDp)

    /** Where a camera is assumed when nothing is detected or placed: the screen centre, at the set hole size. */
    fun centreGuess(settings: SmartIslandSettings): CutoutArea =
        CutoutArea(0f, settings.cameraCutoutY, settings.cameraCutoutWidth, settings.cameraCutoutHeight)

    private fun areaAt(
        settings: SmartIslandSettings,
        anchorXDp: Float,
        topDp: Float = settings.cameraCutoutY
    ) = CutoutArea(anchorXDp + settings.cameraCutoutX, topDp, settings.cameraCutoutWidth, settings.cameraCutoutHeight)

    /** The hole as seen in landscape, from its place on a [portraitWidthDp] x [portraitHeightDp] portrait screen. */
    fun landscapeCamera(
        area: CutoutArea,
        portraitWidthDp: Float,
        portraitHeightDp: Float,
        reversed: Boolean
    ): LandscapeCamera {
        val portrait = CutoutRect(
            left = portraitWidthDp / 2f + area.centreXDp - area.widthDp / 2f, top = area.topDp,
            width = area.widthDp, height = area.heightDp, radius = 0f, opacity = 1f
        )
        val turned = RotationMorph.rotate(portrait, portraitWidthDp, portraitHeightDp, if (reversed) 3 else 1)
        return LandscapeCamera(
            xOffsetDp = turned.left + turned.width / 2f - portraitHeightDp / 2f,
            centerYDp = turned.top + turned.height / 2f,
            widthDp = turned.width,
            lengthDp = turned.height
        )
    }
}
