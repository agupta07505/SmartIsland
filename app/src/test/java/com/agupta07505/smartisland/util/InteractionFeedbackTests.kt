/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.CollapseMotion
import com.agupta07505.smartisland.model.SwipeFeel
import com.agupta07505.smartisland.model.springTimeToProgressMs
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationSpeedTest {
    @Test
    fun normalKeepsTheOldNumbers() {
        assertEquals(400f, AnimationSpeed.NORMAL.stiffness(400f), 0.0001f)
        assertEquals(220, AnimationSpeed.NORMAL.durationMs(220))
    }

    @Test
    fun snappyTiming() {
        assertTrue(AnimationSpeed.SNAPPY.stiffness(400f) > 400f)
        assertTrue(AnimationSpeed.SNAPPY.durationMs(220) < 220)
        assertTrue(AnimationSpeed.RELAXED.stiffness(400f) < 400f)
        assertTrue(AnimationSpeed.RELAXED.durationMs(220) > 220)

        assertEquals(1, AnimationSpeed.SNAPPY.durationMs(0))
    }

    @Test
    fun unknownIdsFallBackToNormal() {
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromId(null))
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromId("turbo"))
        assertEquals(AnimationSpeed.SNAPPY, AnimationSpeed.fromId("snappy"))
        assertEquals(AnimationSpeed.RELAXED, AnimationSpeed.fromId("relaxed"))
    }
}

class SystemAnimScaleTest {
    @Test
    fun badValuesOneBigCapped() {
        assertEquals(1f, SystemAnimScale.sanitize(Float.NaN), 0f)
        assertEquals(1f, SystemAnimScale.sanitize(Float.POSITIVE_INFINITY), 0f)
        assertEquals(1f, SystemAnimScale.sanitize(-2f), 0f)
        assertEquals(0f, SystemAnimScale.sanitize(0f), 0f)
        assertEquals(1.5f, SystemAnimScale.sanitize(1.5f), 0f)
        assertEquals(SystemAnimScale.MAX_SCALE, SystemAnimScale.sanitize(40f), 0f)
    }

    @Test
    fun tweenScalesLinearlyMinOneMs() {
        assertEquals(300, SystemAnimScale.scaledDurationMs(300, 1f))
        assertEquals(600, SystemAnimScale.scaledDurationMs(300, 2f))
        assertEquals(150, SystemAnimScale.scaledDurationMs(300, 0.5f))
        assertEquals(1, SystemAnimScale.scaledDurationMs(300, 0f))
        assertEquals(1, SystemAnimScale.scaledDurationMs(1, 0.1f))
    }

    @Test
    fun animationScale() {
        assertEquals(340f, SystemAnimScale.scaledStiffness(340f, 1f), 0.001f)
        assertEquals(85f, SystemAnimScale.scaledStiffness(340f, 2f), 0.001f)
        assertEquals(1360f, SystemAnimScale.scaledStiffness(340f, 0.5f), 0.001f)

        val base = springTimeToProgressMs(340f, 1f, 0.98f).toFloat()
        val doubled = springTimeToProgressMs(
            SystemAnimScale.scaledStiffness(340f, 2f), 1f, 0.98f
        ).toFloat()
        assertEquals(2f, doubled / base, 0.05f)

        assertEquals(SystemAnimScale.SNAP_STIFFNESS, SystemAnimScale.scaledStiffness(340f, 0f), 0f)
        val ms = springTimeToProgressMs(SystemAnimScale.SNAP_STIFFNESS, 1f, 0.98f)
        assertTrue("snap spring took $ms ms", ms <= 20L)
    }

    @Test
    fun defaultStateKeepsSpeedChoice() {
        // Compose applies the system scale itself by default, so AnimationSpeed adds nothing
        assertEquals(1f, SystemAnimScale.extraScale(), 0f)
        assertEquals(AnimationSpeed.NORMAL.userStiffness(400f), AnimationSpeed.NORMAL.stiffness(400f), 0.0001f)
        assertEquals(AnimationSpeed.SNAPPY.userDurationMs(300), AnimationSpeed.SNAPPY.durationMs(300))
    }
}

class TapSwapSpringTest {
    /** Time (ms) a critically damped spring of [stiffness] takes to cover 95 % of its way: wt = 4.74 */
    private fun settleMs(stiffness: Float): Float = 4.74f / sqrt(stiffness) * 1000f

    @Test
    fun swapSpringStiffness() {
        assertEquals(372f, AnimationSpeed.RELAXED.userStiffness(SwipeFeel.TAP_SWAP_STIFFNESS), 0.001f)
        assertEquals(620f, AnimationSpeed.NORMAL.userStiffness(SwipeFeel.TAP_SWAP_STIFFNESS), 0.001f)
        assertEquals(1054f, AnimationSpeed.SNAPPY.userStiffness(SwipeFeel.TAP_SWAP_STIFFNESS), 0.001f)

        // relaxed is the slowest choice: still a quick swap, about a quarter of a second
        val relaxed = settleMs(AnimationSpeed.RELAXED.userStiffness(SwipeFeel.TAP_SWAP_STIFFNESS))
        val normal = settleMs(AnimationSpeed.NORMAL.userStiffness(SwipeFeel.TAP_SWAP_STIFFNESS))
        val snappy = settleMs(AnimationSpeed.SNAPPY.userStiffness(SwipeFeel.TAP_SWAP_STIFFNESS))
        assertTrue(relaxed in 235f..260f)
        assertTrue(relaxed > normal && normal > snappy)
    }

    @Test
    fun swapClockDuration() {
        AnimationSpeed.entries.forEach {
            val collapse = settleMs(it.userStiffness(CollapseMotion.STIFFNESS))
            val swap = settleMs(it.userStiffness(SwipeFeel.TAP_SWAP_STIFFNESS))
            assertTrue("$it: swap $swap ms, collapse $collapse ms", swap < collapse)
        }
    }
}

class HapticStyleTest {
    @Test
    fun offNeverVibrates() {
        HapticKind.entries.forEach { assertNull(HapticStrength.OFF.spec(it)) }
    }

    @Test
    fun hapticProfiles() {
        listOf(HapticStrength.LIGHT, HapticStrength.NORMAL, HapticStrength.STRONG).forEach { strength ->
            HapticKind.entries.forEach { kind ->
                val strengthsSpec = strength.spec(kind)
                assertNotNull(strengthsSpec)
                assertTrue(strengthsSpec!!.amplitude in 1..255)
                assertTrue(strengthsSpec.durationMs > 0)
            }
        }

        listOf(HapticStrength.LIGHT, HapticStrength.NORMAL, HapticStrength.STRONG).forEach { strength ->
            val merge = strength.spec(HapticKind.Merge)!!
            HapticKind.entries.filter { it != HapticKind.Merge }.forEach { kind ->
                assertTrue(merge.amplitude < strength.spec(kind)!!.amplitude)
                assertTrue(merge.durationMs <= strength.spec(kind)!!.durationMs)
            }
            assertEquals(Effect.Tick, merge.effect)
        }

        listOf(HapticStrength.LIGHT, HapticStrength.NORMAL, HapticStrength.STRONG).forEach { strength ->
            val lock = strength.spec(HapticKind.Lock)!!
            val swipe = strength.spec(HapticKind.Swipe)!!
            assertTrue(lock.durationMs > swipe.durationMs)
            assertTrue(lock.amplitude < swipe.amplitude)
        }
        assertEquals(Effect.Click, HapticStrength.NORMAL.spec(HapticKind.Lock)!!.effect)
    }

    @Test
    fun strongerLevelsAreNeverWeaker() {
        HapticKind.entries.forEach { kind ->
            val light = HapticStrength.LIGHT.spec(kind)!!.amplitude
            val normal = HapticStrength.NORMAL.spec(kind)!!.amplitude
            val strong = HapticStrength.STRONG.spec(kind)!!.amplitude
            assertTrue(light < normal)
            assertTrue(normal <= strong)
        }
    }

    @Test
    fun holdHeaviestExpandBeatsCollapse() {
        val n = HapticStrength.NORMAL
        assertEquals(Effect.HeavyClick, n.spec(HapticKind.Hold)!!.effect)
        assertTrue(n.spec(HapticKind.Expand)!!.amplitude > n.spec(HapticKind.Collapse)!!.amplitude)
    }

    @Test
    fun unknownIdsFallBackToNormal() {
        assertEquals(HapticStrength.NORMAL, HapticStrength.fromId("nope"))
        assertEquals(HapticStrength.NORMAL, HapticStrength.fromId(null))
        assertEquals(HapticStrength.OFF, HapticStrength.fromId("off"))
    }
}
