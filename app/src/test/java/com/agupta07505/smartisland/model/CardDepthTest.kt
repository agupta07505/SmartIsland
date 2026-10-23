/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardDepthTest {

    private fun walkDeeper(media: Int, text: Boolean): List<CardDepth> {
        val out = mutableListOf(CardDepth.First)
        var s = CardSteps.First
        while (true) {
            s = CardSteps.deeper(s, media, text) ?: break
            out += s.depth
        }
        return out
    }

    @Test
    fun onePictureSkipsThePreview() {
        assertEquals(listOf(CardDepth.First, CardDepth.Reader, CardDepth.PictureMid, CardDepth.Picture), walkDeeper(1, false))
        // The arrow does the same: straight to the reader, never the preview.
        assertEquals(CardDepth.Reader, CardSteps.arrow(CardSteps.First, 1).depth)
    }

    @Test
    fun severalPicturesKeepThePreview() {
        assertEquals(
            listOf(CardDepth.First, CardDepth.Preview, CardDepth.Reader, CardDepth.Selector),
            walkDeeper(3, false)
        )
        assertEquals(CardDepth.Preview, CardSteps.arrow(CardSteps.First, 3).depth)
    }

    @Test
    fun aLongTextWithoutPicturesHasOnlyTheReader() {
        assertEquals(listOf(CardDepth.First, CardDepth.Reader), walkDeeper(0, true))
    }

    @Test
    fun aCardWithNothingToOpenHasNoDeeperStep() {
        assertEquals(listOf(CardDepth.First), walkDeeper(0, false))
    }

    @Test
    fun theOnlyPictureOpensFromTheReaderAtHalfSize() {
        val picture = CardSteps.deeper(CardSteps(reader = true), 1, false)!!
        assertEquals(0, picture.pictureIndex)
        assertEquals(CardDepth.PictureMid, picture.depth)
        assertNull(CardSteps.deeper(CardSteps.deeper(picture, 1, false)!!, 1, false))
    }

    @Test
    fun oneImageHasTheHalfStepBeforeTheFullPicture() {
        assertEquals(
            listOf(CardDepth.First, CardDepth.Reader, CardDepth.PictureMid, CardDepth.Picture),
            walkDeeper(1, false)
        )
    }

    @Test
    fun severalImagesOpenTheSelectorAndOnlyATapOpensOne() {
        assertEquals(
            listOf(CardDepth.First, CardDepth.Preview, CardDepth.Reader, CardDepth.Selector),
            walkDeeper(3, false)
        )
        val selector = CardSteps.deeper(CardSteps(reader = true), 3, false)!!
        assertNull(CardSteps.deeper(selector, 3, false))
        val full = CardSteps.pickFromSelector(selector, 1)
        assertEquals(CardDepth.Picture, full.depth)
        assertEquals(1, full.pictureIndex)
        // Swipe up: full -> selector -> preview.
        val back = CardSteps.back(full, 3)!!
        assertEquals(CardDepth.Selector, back.depth)
        assertEquals(CardDepth.Preview, CardSteps.back(back, 3)!!.depth)
    }

    @Test
    fun aCardWithStickersIsCountedAsHavingNoPictures() {
        // The caller passes 0 for it: no preview, no picture steps, only a cut-off text opens the reader.
        assertEquals(listOf(CardDepth.First), walkDeeper(0, false))
        assertEquals(listOf(CardDepth.First, CardDepth.Reader), walkDeeper(0, true))
    }

    @Test
    fun oneDragMovesExactlyOneStepEachWay() {
        val reader = CardSteps(reader = true)
        val mid = CardSteps.deeper(reader, 1, false)!!
        val full = CardSteps.deeper(mid, 1, false)!!
        assertEquals(CardDepth.PictureMid, mid.depth)
        assertEquals(CardDepth.Picture, full.depth)
        assertEquals(mid, CardSteps.back(full, 1))
        assertEquals(CardDepth.Reader, CardSteps.back(mid, 1)!!.depth)
        // The go-back of a short pull undoes exactly what the drag opened.
        assertEquals(mid, CardSteps.back(CardSteps.deeper(mid, 1, false)!!, 1))
    }

    @Test
    fun halfPictureHeightIsHalfwayBetweenTheCardMinimumAndFull() {
        assertEquals(177.5f, PictureSize.MID_DP, 0.001f)
        assertEquals(PictureSize.FULL_DP, PictureSize.heightDp(false), 0f)
        assertEquals(PictureSize.MID_DP, PictureSize.heightDp(true), 0f)
    }

    @Test
    fun theHalfFlagOnlyCountsWhileThePictureOfThatCardIsOpen() {
        val state = CardStepState(key = "a", reader = true, pictureHalf = true)
        assertEquals(CardDepth.PictureMid, state.steps("a", "a", 0).depth)
        assertEquals(CardDepth.Reader, state.steps("a", null, 0).depth)
        assertEquals(CardDepth.Picture, state.steps("b", "b", 0).depth)
    }

    @Test
    fun swipeUpWalksBackOneStepAtATime() {
        val picture = CardSteps(reader = true, preview = true, pictureIndex = 2)
        val half = CardSteps.back(picture, 3)!!
        assertEquals(CardDepth.PictureMid, half.depth)
        val reader = CardSteps.back(half, 3)!!
        assertEquals(CardDepth.Reader, reader.depth)
        assertEquals(CardDepth.Preview, CardSteps.back(reader, 3)!!.depth)
        assertEquals(CardDepth.First, CardSteps.back(CardSteps(preview = true), 3)!!.depth)
        assertNull(CardSteps.back(CardSteps.First, 3))
    }

    @Test
    fun backFromTheReaderOfOnePictureOrTextGoesToTheFirstState() {
        assertEquals(CardDepth.First, CardSteps.back(CardSteps(reader = true), 1)!!.depth)
        assertEquals(CardDepth.First, CardSteps.back(CardSteps(reader = true), 0)!!.depth)
    }

    @Test
    fun theArrowInTheReaderClosesStraightBackToTheFirstState() {
        assertEquals(CardDepth.First, CardSteps.arrow(CardSteps(reader = true), 3).depth)
        assertEquals(CardDepth.Reader, CardSteps.arrow(CardSteps(preview = true), 3).depth)
        assertEquals(CardDepth.Reader, CardSteps.arrow(CardSteps.First, 0).depth)
    }

    @Test
    fun theStepStateBelongsToOneCardAndOtherCardsReadTheFirstState() {
        val state = CardStepState(key = "a", reader = true)
        assertEquals(CardDepth.Reader, state.steps("a", null, 0).depth)
        assertEquals(CardDepth.First, state.steps("b", null, 0).depth)
        assertEquals(CardDepth.First, state.steps(null, null, 0).depth)
    }

    @Test
    fun anOpenPictureOfTheCardMakesItThePictureStepAndAnotherCardsPictureDoesNot() {
        val state = CardStepState(key = "a", reader = true)
        assertEquals(CardDepth.Picture, state.steps("a", "a", 1).depth)
        assertEquals(1, state.steps("a", "a", 1).pictureIndex)
        assertEquals(CardDepth.Reader, state.steps("a", "b", 1).depth)
    }
}
