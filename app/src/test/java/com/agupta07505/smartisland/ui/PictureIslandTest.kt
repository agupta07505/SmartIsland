/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.ui.expanded.MediaExpansion
import com.agupta07505.smartisland.ui.expanded.PictureArrowSide
import com.agupta07505.smartisland.ui.expanded.pictureArrowSide
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.ui.expanded.besideDragSplitProgress
import com.agupta07505.smartisland.ui.expanded.pictureIslandHeight
import com.agupta07505.smartisland.ui.expanded.stepsOpenBesideCard
import com.agupta07505.smartisland.ui.expanded.pictureIslandStepHeight
import com.agupta07505.smartisland.ui.expanded.pictureShownBesideCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureIslandTest {
    @Test
    fun landscapeShowsTheOpenPictureOfTheShownCardBesideIt() {
        assertTrue(pictureShownBesideCard(true, "card", 1, "card", 2))
    }

    @Test
    fun portraitKeepsThePictureInsideTheCard() {
        assertFalse(pictureShownBesideCard(false, "card", 0, "card", 2))
    }

    @Test
    fun otherCardsAndMissingPicturesAreNotShown() {
        assertFalse(pictureShownBesideCard(true, null, 0, "card", 2))
        assertFalse(pictureShownBesideCard(true, "other", 0, "card", 2))
        assertFalse(pictureShownBesideCard(true, "card", 2, "card", 2))
        assertFalse(pictureShownBesideCard(true, "card", 0, "card", 0))
    }

    @Test
    fun islandHeightIsAtLeastTwoHundredAndAtMostTheTallestIsland() {
        assertEquals(200.dp, pictureIslandHeight(120.dp))
        assertEquals(230.dp, pictureIslandHeight(230.dp))
        assertEquals(260.dp, pictureIslandHeight(300.dp))
    }

    @Test
    fun theSelectorIsShownBesideACardWithSeveralPicturesOnly() {
        assertTrue(pictureShownBesideCard(true, null, 0, "card", 3, selectorOpen = true))
        assertFalse(pictureShownBesideCard(true, null, 0, "card", 1, selectorOpen = true))
        assertFalse(pictureShownBesideCard(false, null, 0, "card", 3, selectorOpen = true))
        assertFalse(pictureShownBesideCard(true, null, 0, null, 3, selectorOpen = true))
    }

    @Test
    fun theHalfPictureAndTheSelectorAreHalfSizeAndTheFullPictureIsTheTallestIsland() {
        assertEquals(177.5.dp, pictureIslandStepHeight(fullSize = false))
        assertEquals(260.dp, pictureIslandStepHeight(fullSize = true))
    }

    @Test
    fun theIslandBesideTheCardIsOpenForTheSelectorAndBothPictureSizesOnly() {
        assertFalse(stepsOpenBesideCard(CardSteps.First))
        assertFalse(stepsOpenBesideCard(CardSteps(preview = true)))
        assertFalse(stepsOpenBesideCard(CardSteps(reader = true)))
        assertTrue(stepsOpenBesideCard(CardSteps(reader = true, selector = true)))
        assertTrue(stepsOpenBesideCard(CardSteps(reader = true, pictureIndex = 0, pictureHalf = true)))
        assertTrue(stepsOpenBesideCard(CardSteps(reader = true, pictureIndex = 0)))
    }

    @Test
    fun theDragRunsTheIslandUpWhenOpeningAndDownWhenClosing() {
        assertEquals(0f, besideDragSplitProgress(opening = true, dragProgress = 0f), 0f)
        assertEquals(0.3f, besideDragSplitProgress(opening = true, dragProgress = 0.3f), 0.0001f)
        assertEquals(1f, besideDragSplitProgress(opening = false, dragProgress = 0f), 0f)
        assertEquals(0.7f, besideDragSplitProgress(opening = false, dragProgress = 0.3f), 0.0001f)
        assertEquals(0f, besideDragSplitProgress(opening = false, dragProgress = 1.4f), 0f)
    }

    @Test
    fun expansionDefaultsToInsideTheCard() {
        assertFalse(MediaExpansion("card", 0) { _, _ -> }.besideCard)
    }

    @Test
    fun arrowsSitOnTheSideAwayFromTheCamera() {
        // pickerOnLeft = camera on the right, so the picture island (and its arrows) are on the left.
        assertEquals(PictureArrowSide.Left, pictureArrowSide(isLandscape = true, pickerOnLeft = true))
        assertEquals(PictureArrowSide.Right, pictureArrowSide(isLandscape = true, pickerOnLeft = false))
        assertEquals(PictureArrowSide.Inside, pictureArrowSide(isLandscape = false, pickerOnLeft = false))
    }
}
