/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticStyleTest {
    @Test
    fun offNeverVibrates() {
        HapticKind.entries.forEach { assertNull(HapticStrength.OFF.spec(it)) }
    }

    @Test
    fun everyOtherLevelHasASpecForEveryKind() {
        listOf(HapticStrength.LIGHT, HapticStrength.NORMAL, HapticStrength.STRONG).forEach { strength ->
            HapticKind.entries.forEach { kind ->
                val spec = strength.spec(kind)
                assertNotNull(spec)
                assertTrue(spec!!.amplitude in 1..255)
                assertTrue(spec.durationMs > 0)
            }
        }
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
    fun holdIsTheHeaviestAndExpandBeatsCollapse() {
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

    @Test
    fun mergeTouchIsTheLightestTickOfAll() {
        listOf(HapticStrength.LIGHT, HapticStrength.NORMAL, HapticStrength.STRONG).forEach { strength ->
            val merge = strength.spec(HapticKind.Merge)!!
            HapticKind.entries.filter { it != HapticKind.Merge }.forEach { kind ->
                assertTrue(merge.amplitude < strength.spec(kind)!!.amplitude)
                assertTrue(merge.durationMs <= strength.spec(kind)!!.durationMs)
            }
            assertEquals(Effect.Tick, merge.effect)
        }
    }

    @Test
    fun lockIsADullBuzzNotATick() {
        listOf(HapticStrength.LIGHT, HapticStrength.NORMAL, HapticStrength.STRONG).forEach { strength ->
            val lock = strength.spec(HapticKind.Lock)!!
            val swipe = strength.spec(HapticKind.Swipe)!!
            assertTrue(lock.durationMs > swipe.durationMs)
            assertTrue(lock.amplitude < swipe.amplitude)
        }
        assertEquals(Effect.Click, HapticStrength.NORMAL.spec(HapticKind.Lock)!!.effect)
    }
}
