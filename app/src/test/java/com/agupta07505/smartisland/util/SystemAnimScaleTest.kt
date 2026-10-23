/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemAnimScaleTest {
    @Test
    fun badValuesFallBackToOneAndBigOnesAreCapped() {
        assertEquals(1f, SystemAnimScale.sanitize(Float.NaN), 0f)
        assertEquals(1f, SystemAnimScale.sanitize(Float.POSITIVE_INFINITY), 0f)
        assertEquals(1f, SystemAnimScale.sanitize(-2f), 0f)
        assertEquals(0f, SystemAnimScale.sanitize(0f), 0f)
        assertEquals(1.5f, SystemAnimScale.sanitize(1.5f), 0f)
        assertEquals(SystemAnimScale.MAX_SCALE, SystemAnimScale.sanitize(40f), 0f)
    }

    @Test
    fun tweenLengthScalesLinearlyAndNeverDropsBelowOneMs() {
        assertEquals(300, SystemAnimScale.scaledDurationMs(300, 1f))
        assertEquals(600, SystemAnimScale.scaledDurationMs(300, 2f))
        assertEquals(150, SystemAnimScale.scaledDurationMs(300, 0.5f))
        assertEquals(1, SystemAnimScale.scaledDurationMs(300, 0f))
        assertEquals(1, SystemAnimScale.scaledDurationMs(1, 0.1f))
    }

    @Test
    fun springStiffnessFollowsTheInverseSquareSoItsTimeScalesLinearly() {
        assertEquals(340f, SystemAnimScale.scaledStiffness(340f, 1f), 0.001f)
        assertEquals(85f, SystemAnimScale.scaledStiffness(340f, 2f), 0.001f)
        assertEquals(1360f, SystemAnimScale.scaledStiffness(340f, 0.5f), 0.001f)
    }

    @Test
    fun scaledSpringReallyTakesScaleTimesAsLong() {
        val base = com.agupta07505.smartisland.model.springTimeToProgressMs(340f, 1f, 0.98f).toFloat()
        val doubled = com.agupta07505.smartisland.model.springTimeToProgressMs(
            SystemAnimScale.scaledStiffness(340f, 2f), 1f, 0.98f
        ).toFloat()
        assertEquals(2f, doubled / base, 0.05f)
    }

    @Test
    fun scaleZeroMakesTheSpringSnap() {
        assertEquals(SystemAnimScale.SNAP_STIFFNESS, SystemAnimScale.scaledStiffness(340f, 0f), 0f)
        val ms = com.agupta07505.smartisland.model.springTimeToProgressMs(SystemAnimScale.SNAP_STIFFNESS, 1f, 0.98f)
        assertTrue("snap spring took $ms ms", ms <= 20L)
    }

    @Test
    fun defaultStateChangesNothingForTheUsersSpeedChoice() {
        // Compose applies the system scale itself by default, so AnimationSpeed adds nothing.
        assertEquals(1f, SystemAnimScale.extraScale(), 0f)
        assertEquals(AnimationSpeed.NORMAL.userStiffness(400f), AnimationSpeed.NORMAL.stiffness(400f), 0.0001f)
        assertEquals(AnimationSpeed.SNAPPY.userDurationMs(300), AnimationSpeed.SNAPPY.durationMs(300))
    }
}
