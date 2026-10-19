/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** Top-left of the collapsed overlay window (`Gravity.TOP or Gravity.LEFT`), in screen px. */
data class AnchoredWindowPosition(val xPx: Int, val yPx: Int)

/**
 * Landscape anchoring (Rule_Book §9.2). Turned sideways, the camera hole sits on the left or right bezel, so the
 * collapsed island leaves the top centre and docks there: the main pill encloses the hole (hole at the pill's
 * outer end, [marginPx] from it) and is centred on the hole vertically. Only the small pill and the companion
 * bubble dock; the Medium card never opens in landscape (`ExpansionPolicy`). Pure, so it is unit-testable.
 */
object LandscapeAnchoring {

    /**
     * @param mainWidthPx width of the main pill.
     * @param groupWidthPx main pill plus the companion bubble and its gap, when one is shown.
     * @param mainOffsetInGroupPx how far the main pill starts from the group's left (the bubble's width plus the
     *   gap when the bubble sits on the left, else 0).
     * @param windowPaddingPx the window's horizontal padding on each side of the group (the content is centred).
     */
    fun windowPosition(
        anchor: LateralAnchor,
        screenWidthPx: Int,
        screenHeightPx: Int,
        mainWidthPx: Float,
        groupWidthPx: Float,
        mainOffsetInGroupPx: Float,
        pillHeightPx: Float,
        windowPaddingPx: Float,
        marginPx: Float
    ): AnchoredWindowPosition {
        val mainLeft = when (anchor.side) {
            LateralSide.Left -> anchor.box.left - marginPx
            LateralSide.Right -> anchor.box.right + marginPx - mainWidthPx
        }
        val maxGroupLeft = (screenWidthPx - groupWidthPx).coerceAtLeast(0f)
        val groupLeft = (mainLeft - mainOffsetInGroupPx).coerceIn(0f, maxGroupLeft)
        val maxTop = (screenHeightPx - pillHeightPx).coerceAtLeast(0f)
        val top = (anchor.centerYPx - pillHeightPx / 2f).coerceIn(0f, maxTop)
        return AnchoredWindowPosition(
            xPx = (groupLeft - windowPaddingPx).toInt(),
            yPx = top.toInt()
        )
    }
}
