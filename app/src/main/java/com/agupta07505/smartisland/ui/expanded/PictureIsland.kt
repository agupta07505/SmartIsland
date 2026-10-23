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

/** Height of the landscape picture island when the card beside it is shorter. */
internal val PICTURE_ISLAND_MIN_HEIGHT = 200.dp

/**
 * Landscape: an open picture, or the image selector, is its own island beside the card, on the side away from the
 * camera.
 */
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
 * Height of the landscape island beside the card for its step: the half-size picture and the selector are halfway
 * between the card minimum and the full picture ([PICTURE_MID_HEIGHT]); the full picture is the tallest.
 */
internal fun pictureIslandStepHeight(fullSize: Boolean): Dp = if (fullSize) EXPANDED_MEDIA_HEIGHT else PICTURE_MID_HEIGHT

/** True for the steps whose picture or selector is an island beside the card (landscape): the selector, the half and the full picture. */
internal fun stepsOpenBesideCard(steps: com.agupta07505.smartisland.model.CardSteps): Boolean = when (steps.depth) {
    com.agupta07505.smartisland.model.CardDepth.Selector,
    com.agupta07505.smartisland.model.CardDepth.PictureMid,
    com.agupta07505.smartisland.model.CardDepth.Picture -> true
    else -> false
}

/**
 * Landscape: the one progress of the two islands (0 = card alone, 1 = side by side) while a step follows the finger:
 * a step that opens the island runs it up with the drag, one that closes it runs it down.
 */
internal fun besideDragSplitProgress(opening: Boolean, dragProgress: Float): Float =
    (if (opening) dragProgress else 1f - dragProgress).coerceIn(0f, 1f)

/** Where a picture opens relative to the card: inside it (portrait), or in the island on the left / right of it (landscape). */
internal enum class PictureArrowSide { Inside, Left, Right }

/**
 * Landscape: the picture island is on the side away from the camera (the sibling picker's side), so its arrows are
 * there too. [pickerOnLeft] is true when the camera is on the right.
 */
internal fun pictureArrowSide(isLandscape: Boolean, pickerOnLeft: Boolean): PictureArrowSide = when {
    !isLandscape -> PictureArrowSide.Inside
    pickerOnLeft -> PictureArrowSide.Left
    else -> PictureArrowSide.Right
}

internal val LocalPictureArrowSide = compositionLocalOf { PictureArrowSide.Inside }
