/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.CardSteps

/** Height of the landscape picture island when the card beside it is shorter. */
internal val PICTURE_ISLAND_MIN_HEIGHT = 200.dp

internal fun pictureShownBesideCard(
    isLandscape: Boolean,
    openKey: String?,
    openIndex: Int,
    cardKey: String?,
    cardMediaCount: Int,
    selectorOpen: Boolean = false
): Boolean = isLandscape && cardKey != null && (
    (openKey != null && openKey == cardKey && openIndex in 0 until cardMediaCount) ||
        (selectorOpen && cardMediaCount >= 2)
    )

internal fun pictureIslandHeight(cardHeight: Dp): Dp = maxOf(cardHeight, PICTURE_ISLAND_MIN_HEIGHT).coerceAtMost(260.dp)

/**
 * Half picture and selector sit halfway between the card minimum and [PICTURE_MID_HEIGHT], the full picture is the
 * tallest
 */
internal fun pictureIslandStepHeight(fullSize: Boolean): Dp = if (fullSize) EXPANDED_MEDIA_HEIGHT else PICTURE_MID_HEIGHT

internal fun stepsOpenBesideCard(steps: CardSteps): Boolean = when (steps.depth) {
    CardDepth.Selector,
    CardDepth.PictureMid,
    CardDepth.Picture -> true
    else -> false
}

/**
 * Landscape, 0 = card alone, 1 = side by side
 * A step that opens the island runs it up with the drag, one that closes it runs it down
 */
internal fun besideDragSplitProgress(opening: Boolean, dragProgress: Float): Float =
    (if (opening) dragProgress else 1f - dragProgress).coerceIn(0f, 1f)

internal enum class PictureArrowSide { Inside, Left, Right }

/**
 * Landscape, on the side away from the camera like the sibling picker, so its arrows are there too
 * [pickerOnLeft] is true when the camera is on the right
 */
internal fun pictureArrowSide(isLandscape: Boolean, pickerOnLeft: Boolean): PictureArrowSide = when {
    !isLandscape -> PictureArrowSide.Inside
    pickerOnLeft -> PictureArrowSide.Left
    else -> PictureArrowSide.Right
}

internal val LocalPictureArrowSide = compositionLocalOf { PictureArrowSide.Inside }
