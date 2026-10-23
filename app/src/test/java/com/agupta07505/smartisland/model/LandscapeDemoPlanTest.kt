/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.data.landscapeCameraSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeDemoPlanTest {

    private val screen = OrientationState(isLandscape = true, screenWidthDp = 800f, screenHeightDp = 360f)
    private val stored = SmartIslandSettings(
        disabledEvents = setOf("music"),
        disabledEventsLandscape = setOf("call"),
        noExpandPackages = setOf("com.spotify.music"),
        landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_RIGHT,
        idleTimeoutMs = 5_000
    )

    @Test
    fun thirtyDistinctStages() {
        val stages = allLabStages()
        assertEquals(2 * 5 * 3, stages.size)
        assertEquals(stages.size, stages.map { it.title }.toSet().size)
    }

    @Test
    fun filtersNarrowTheMatrix() {
        assertEquals(15, labStages(LabRotation.REVERSE, null, null).size)
        assertEquals(6, labStages(null, LabIdle.MIDDLE, null).size)
        assertEquals(1, labStages(LabRotation.NORMAL, LabIdle.LEFT, LabPill.RIGHT).size)
    }

    @Test
    fun overrideForcesTheStageLayoutAndClearsWhatCouldHideCards() {
        val stage = LabStage(LabRotation.NORMAL, LabIdle.LEFT, LabPill.LEFT)
        val out = LabOverride(stage, mergeStyle = "bubble").applyTo(stored)
        assertEquals(SmartIslandSettings.LANDSCAPE_POSITION_LEFT, out.landscapePillPosition)
        assertEquals(SmartIslandSettings.LANDSCAPE_POSITION_LEFT, out.landscapeCirclePosition)
        assertEquals("bubble", out.mergeStyle)
        assertEquals("bubble", out.mergeStyleLandscape)
        assertTrue(out.disabledEvents.isEmpty() && out.disabledEventsLandscape.isEmpty() && out.noExpandPackages.isEmpty())
        assertEquals(stored.enabled, out.enabled) // the lab never switches the island on or off
        assertTrue(out.idleTimeoutMs > 60_000)
    }

    @Test
    fun portraitIsNeverTouched() {
        val portrait = OrientationState(isLandscape = false, screenWidthDp = 360f)
        assertEquals(portrait, LabOverride(allLabStages().first()).applyTo(portrait))
    }

    @Test
    fun reverseMirrorsCameraAndRotation() {
        val normal = LabOverride(LabStage(LabRotation.NORMAL, LabIdle.AUTO_CAMERA, LabPill.MIDDLE)).applyTo(screen)
        val reverse = LabOverride(LabStage(LabRotation.REVERSE, LabIdle.AUTO_CAMERA, LabPill.MIDDLE)).applyTo(screen)
        assertFalse(normal.reversed)
        assertTrue(reverse.reversed)
        assertTrue(normal.camera!!.xOffsetDp < 0f)
        assertEquals(-normal.camera!!.xOffsetDp, reverse.camera!!.xOffsetDp, 0.001f)
        val noCamera = LabOverride(LabStage(LabRotation.REVERSE, LabIdle.AUTO_NO_CAMERA, LabPill.MIDDLE)).applyTo(screen)
        assertNull(noCamera.camera)
        assertEquals(SmartIslandSettings.LANDSCAPE_POSITION_RIGHT, noCamera.rotationSide)
    }

    @Test
    fun expectedExitDirectionMatchesTheRealPlacementInEveryStage() {
        for (stage in allLabStages()) {
            val lab = LabOverride(stage)
            val settings = lab.applyTo(stored).forOrientation(lab.applyTo(screen), hasContent = true)
            val actual = pillExitDirection(settings.xOffset, 800f)
            val expected = expectedExitDirection(stage, 800f, stored.landscapeWidth, stored.landscapeCircleEdgeInset)
            assertEquals(stage.title, expected, actual)
        }
    }

    @Test
    fun expectedPickerSideMatchesTheViewModelRuleInEveryStage() {
        for (stage in allLabStages()) {
            val lab = LabOverride(stage)
            val cameraSide = lab.applyTo(stored).landscapeCameraSide(lab.applyTo(screen))
            val onLeft = cameraSide == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
            assertEquals(stage.title, onLeft, expectedPickerOnLeft(stage))
        }
    }

    @Test
    fun scriptStartsAndEndsOnAnEmptyIsland() {
        for (quick in listOf(true, false)) {
            val steps = labStepsFor(quick)
            assertEquals(LabAction.RemoveAll, steps.first().actions.first())
            assertEquals(LabAction.RemoveAll, steps.last().actions.last())
            assertEquals(steps.size, steps.map { it.id }.toSet().size)
        }
    }

    @Test
    fun quickRunIsShorterAndKeepsEntryExpandExit() {
        val quick = labStepsFor(true).map { it.id }
        assertTrue(quick.containsAll(listOf("enter", "expand", "exit")))
        assertTrue(labStepsFor(true).size < labStepsFor(false).size)
        assertTrue(labStageDurationMs(true) < labStageDurationMs(false))
    }

    @Test
    fun interruptComesBackInsideTheExit() {
        assertTrue(LAB_INTERRUPT_AFTER_MS < PILL_EXIT_MS)
    }

    @Test
    fun everyConditionIsInTheFullScript() {
        val ids = labStepsFor(false).map { it.id }.toSet()
        listOf(
            "enter", "expand", "collapse", "second_slide", "second_bubble", "third", "expand_many",
            "siblings", "call", "popup", "exit", "interrupt", "dot_tap"
        ).forEach { assertTrue(it, it in ids) }
    }
}
