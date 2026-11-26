/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.SystemAnimScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapBlurTest {
    @Test
    fun onlyAndroid12AndUpBlurs() {
        assertFalse(SwapBlur.supported(26))
        assertFalse(SwapBlur.supported(30))
        assertTrue(SwapBlur.supported(31))
        assertTrue(SwapBlur.supported(35))
    }

    @Test
    fun blurStrengthCurve() {
        assertEquals(0f, SwapBlur.strength(1f), 0f)
        assertEquals(1f, SwapBlur.strength(0f), 0f)
        assertEquals(0, SwapBlur.level(SwapBlur.radiusPx(1f, 30f)))

        var last = -1f
        for (i in 0..100) {
            val s = SwapBlur.strength(1f - i / 100f)
            assertTrue(s >= last - 1e-6f)
            last = s
        }

        // at half alpha the content is already more than half blurred, and fully blurred by a third of its alpha
        assertTrue(SwapBlur.strength(0.5f) > 0.5f)
        assertEquals(1f, SwapBlur.strength(1f / 3f), 1e-5f)

        // outgoing fade and incoming fade (model/TapSwap) give the same blur for the same alpha
        for (i in 0..20) {
            val p = i / 20f
            val out = TapSwap.outgoingContentAlpha(p)
            val inn = TapSwap.incomingContentAlpha(p)
            assertTrue(SwapBlur.strength(out) in 0f..1f)
            assertTrue(SwapBlur.strength(inn) in 0f..1f)
        }
        // the incoming card is sharp again before its fade ends, and starts fully blurred
        assertEquals(1f, SwapBlur.strength(TapSwap.incomingContentAlpha(TapSwap.INCOMING_FADE_START)), 0f)
        assertEquals(0f, SwapBlur.strength(TapSwap.incomingContentAlpha(TapSwap.INCOMING_FADE_END)), 0f)

        assertEquals(0f, SwapBlur.strength(TapSwap.outgoingContentAlpha(TapSwap.OUTGOING_FADE_START)), 0f)
    }

    @Test
    fun blurRadiusLevels() {
        assertEquals(0, SwapBlur.level(0.4f))
        assertEquals(1, SwapBlur.level(0.6f))
        assertEquals(12, SwapBlur.level(12.2f))
        assertEquals(SwapBlur.MAX_LEVEL, SwapBlur.level(500f))
        // every density a phone has stays inside the cache at full strength
        for (density in listOf(1f, 2f, 2.75f, 3f, 3.5f, 4f)) {
            assertTrue(SwapBlur.level(SwapBlur.radiusPx(0f, SwapBlur.MAX_RADIUS_DP * density)) <= SwapBlur.MAX_LEVEL)
        }

        // a hundredth of the fade moves the blur under a pixel, so whole-pixel levels never jump
        var last = 0
        for (i in 0..100) {
            val adjacentLevel = SwapBlur.level(SwapBlur.radiusPx(1f - i / 100f, 30f))
            assertTrue(adjacentLevel >= last)
            assertTrue(adjacentLevel - last <= 2)
            last = adjacentLevel
        }
    }
}

class SwapBlurGateTest {
    private val ms = 1_000_000L

    private fun swap(gate: SwapBlurGate, gaps: List<Long>, blurred: Boolean = true) {
        gate.beginSwap()
        var t = 0L
        gate.frame(t)
        for (g in gaps) { t += g * ms; gate.frame(t) }
        gate.endSwap(blurred)
    }

    private val smooth = List(12) { 16L }
    private val janky = listOf(16L, 40L, 16L, 38L, 16L, 16L)

    @Test
    fun aSmoothSwapKeepsTheBlur() {
        val gate = SwapBlurGate()
        repeat(10) { swap(gate, smooth) }
        assertTrue(gate.enabled)
    }

    @Test
    fun jankDoesNotDropTheBlurByDefault() {
        val gate = SwapBlurGate()
        repeat(10) { swap(gate, janky) }
        assertTrue(gate.enabled)
    }

    @Test
    fun blurFallback() {
        val degradedGate = SwapBlurGate(disableOnJank = true)
        swap(degradedGate, janky)
        swap(degradedGate, janky)
        assertTrue(degradedGate.enabled)
        swap(degradedGate, janky)
        assertFalse(degradedGate.enabled)

        val disabledGate = SwapBlurGate(disableOnJank = true)
        repeat(3) { swap(disabledGate, janky) }
        assertFalse(disabledGate.enabled)
        repeat(10) { swap(disabledGate, smooth) }
        assertFalse(disabledGate.enabled)
    }

    @Test
    fun aSmoothSwapBetweenJankyOnesResetsTheCount() {
        val gate = SwapBlurGate(disableOnJank = true)
        swap(gate, janky)
        swap(gate, janky)
        swap(gate, smooth)
        swap(gate, janky)
        swap(gate, janky)
        assertTrue(gate.enabled)
    }

    @Test
    fun aSwapThatNeverBlurredTellsNothing() {
        val gate = SwapBlurGate(disableOnJank = true)
        repeat(6) { swap(gate, janky, blurred = false) }
        assertTrue(gate.enabled)
    }

    @Test
    fun aSingleLateFrameIsNotJank() {
        val gate = SwapBlurGate(disableOnJank = true)
        repeat(6) { swap(gate, listOf(16L, 16L, 45L, 16L, 16L, 16L)) }
        assertTrue(gate.enabled)
    }

    @Test
    fun aPauseBetweenTwoRunsOfTheSpringIsNotAFrame() {
        val gate = SwapBlurGate(disableOnJank = true)
        repeat(6) {
            gate.beginSwap()
            var t = 0L
            gate.frame(t)
            repeat(4) { t += 16 * ms; gate.frame(t) }
            // a flip: the spring stops and starts again later
            t += 300 * ms
            gate.restart()
            gate.frame(t)
            repeat(4) { t += 16 * ms; gate.frame(t) }
            gate.endSwap(true)
        }
        assertTrue(gate.enabled)
    }

    @Test
    fun aVeryShortSwapIsNotJudged() {
        val gate = SwapBlurGate(disableOnJank = true)
        repeat(6) { swap(gate, listOf(60L, 60L)) }
        assertTrue(gate.enabled)
    }
}

class SwapBurstTest {
    private fun burst(vararg times: Long): SwapBurst = SwapBurst().also { b -> times.forEach { b.tap(it) } }

    @Test
    fun tapBurstBoost() {
        val firstTap = burst(1_000)
        assertEquals(1f, firstTap.boostAt(1_000), 0f)
        assertEquals(1f, firstTap.boostAt(1_100), 0f)

        assertEquals(1f, burst(0, 300).boostAt(300), 0f)
        assertEquals(1f, burst(0, 270).boostAt(270), 0.0001f)

        val boost = burst(0, 150).boostAt(150)
        assertTrue(boost > SwapBurst.MAX_BOOST - 0.15f)
        assertTrue(boost <= SwapBurst.MAX_BOOST)

        assertEquals(SwapBurst.MAX_BOOST, burst(0, 40).boostAt(40), 0.0001f)
        assertEquals(620f / 300f, SwapBurst.MAX_BOOST, 0.0001f)

        val expired = burst(0, 150, 300)
        assertTrue(expired.boostAt(300) > 1.5f)
        assertEquals(1f, expired.boostAt(300 + SwapBurst.DECAY_MS), 0f)
    }

    @Test
    fun theBoostRisesAsTheGapShrinks() {
        val gaps = listOf(270f, 240f, 210f, 180f, 150f, 130f, 100f)
        val boosts = gaps.map { SwapBurst.boostForGap(it) }
        assertTrue(boosts.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun tapBurstExpiry() {
        val steady = burst(0, 150, 300, 450)
        val jolted = burst(0, 150, 300, 450, 700)
        val before = steady.boostAt(450)
        val after = jolted.boostAt(700)
        assertTrue(after < before)
        assertTrue(after > 1f)

        val b = burst(0, 150, 300)
        b.tap(1_000)
        assertEquals(1f, b.boostAt(1_000), 0f)
        b.tap(1_150)
        assertTrue(b.boostAt(1_150) > 1.5f)
    }

    @Test
    fun boostedSwapClock() {
        AnimationSpeed.entries.forEach { speed ->
            listOf(0.5f, 1f, 2f).forEach { scale ->
                val base = SystemAnimScale.scaledStiffness(speed.userStiffness(SwipeFeel.TAP_SWAP_STIFFNESS), scale)
                val cappedBoosted = SwapClock.boosted(base, SwapBurst.MAX_BOOST)
                val cappedFrames = SwapClock.settleMs(cappedBoosted) / (1000f / 60f)
                assertTrue("$speed ${scale}x", cappedBoosted >= base)
                assertTrue("$speed ${scale}x: $cappedFrames frames", cappedFrames >= SwapClock.MIN_FRAMES - 1f || cappedBoosted == base)
            }
        }

        listOf(100f, 300f, 510f, 1_200f, 2_000f, SystemAnimScale.SNAP_STIFFNESS).forEach {
            assertTrue(SwapClock.boosted(it, 1f) == it)
            assertTrue(SwapClock.boosted(it, SwapBurst.MAX_BOOST) >= it)
        }

        // 620 settles in about 190 ms, 11 frames at 60 fps
        assertTrue(SwapClock.MAX_BOOSTED_STIFFNESS > 620f)
        assertEquals(620f, SwapClock.boosted(300f, SwapBurst.MAX_BOOST), 0.5f)
    }
}

class SwapClockTest {
    private val scales = listOf(0.5f, 1f, 2f)

    private fun stiffness(speed: AnimationSpeed, base: Float, scale: Float): Float =
        SystemAnimScale.scaledStiffness(speed.userStiffness(base), scale)

    @Test
    fun settleDuration() {
        listOf(100f, 180f, 300f, 510f, 1200f).forEach {
            val exact = springTimeToProgressMs(it, SwipeFeel.TAP_SWAP_DAMPING_RATIO, 0.95f)
            assertEquals(exact.toFloat(), SwapClock.settleMs(it).toFloat(), 1.5f)
        }

        // the swap runs on the reference recording's clock, the plain collapse's slower one made it look heavy
        AnimationSpeed.entries.forEach { speed ->
            scales.forEach { scale ->
                val swap = SwapClock.settleMs(stiffness(speed, SwipeFeel.TAP_SWAP_STIFFNESS, scale))
                val collapse = SwapClock.settleMs(stiffness(speed, CollapseMotion.STIFFNESS, scale))
                assertTrue("$speed ${scale}x: swap $swap ms, collapse $collapse ms", swap < collapse)
            }
        }

        val normal = SwapClock.settleMs(stiffness(AnimationSpeed.NORMAL, SwipeFeel.TAP_SWAP_STIFFNESS, 1f))
        assertTrue("$normal ms", normal in 170L..220L)

        AnimationSpeed.entries.forEach { speed ->
            val base = SwapClock.settleMs(stiffness(speed, SwipeFeel.TAP_SWAP_STIFFNESS, 1f)).toFloat()
            scales.forEach { scale ->
                val system = SwapClock.settleMs(stiffness(speed, SwipeFeel.TAP_SWAP_STIFFNESS, scale)).toFloat()
                assertEquals(base * scale, system, 2f)
            }
        }

        scales.forEach { scale ->
            val animation = AnimationSpeed.entries.associateWith {
                SwapClock.settleMs(stiffness(it, SwipeFeel.TAP_SWAP_STIFFNESS, scale))
            }
            assertTrue(animation.getValue(AnimationSpeed.RELAXED) > animation.getValue(AnimationSpeed.NORMAL))
            assertTrue(animation.getValue(AnimationSpeed.NORMAL) > animation.getValue(AnimationSpeed.SNAPPY))
        }
    }

    @Test
    fun watchdogLimits() {
        val normal = stiffness(AnimationSpeed.NORMAL, SwipeFeel.TAP_SWAP_STIFFNESS, 1f)
        assertEquals(TapSwap.MAX_SWAP_MS, SwapClock.watchdogMs(normal))
        assertEquals(SwapAbort.GRACE_MS, SwapClock.graceMs(normal))

        listOf(1f, 2f, 5f, 10f).forEach { scale ->
            val s = stiffness(AnimationSpeed.RELAXED, SwipeFeel.TAP_SWAP_STIFFNESS, scale)
            val settle = SwapClock.settleMs(s)
            assertTrue("watchdog at ${scale}x", SwapClock.watchdogMs(s) >= settle * 4)
            assertTrue("grace at ${scale}x", SwapClock.graceMs(s) >= settle)
        }

        val snap = SystemAnimScale.SNAP_STIFFNESS
        assertEquals(TapSwap.MAX_SWAP_MS, SwapClock.watchdogMs(snap))
        assertEquals(SwapAbort.GRACE_MS, SwapClock.graceMs(snap))
        assertTrue(SwapClock.watchdogMs(0f) > 0L)
        assertTrue(SwapClock.graceMs(-1f) > 0L)
    }

    @Test
    fun effectWindowsStayOrderedOnTheOneClock() {
        // all in progress p, so they cannot drift apart at any speed: the outgoing card has left before the incoming
        // one arrives, the pill is back while the incoming card is more than half open and before it is fully open
        assertTrue(TapSwap.OUTGOING_FADE_END <= TapSwap.INCOMING_FADE_START)
        val incomingHalf = (TapSwap.INCOMING_FADE_START + TapSwap.INCOMING_FADE_END) / 2f
        assertTrue(TapSwap.CUTOUT_BACK_PROGRESS >= incomingHalf)
        assertTrue(TapSwap.CUTOUT_BACK_PROGRESS < TapSwap.INCOMING_FADE_END)
        assertTrue(TapSwap.INCOMING_FADE_END <= TapSwap.ACTIONS_FADE_END)
    }

    @Test
    fun theBlurFollowsTheFadesSoItCrossesOnTheSameWindows() {
        val steps = (0..20).map { it / 20f }
        val outgoing = steps.map { SwapBlur.strength(TapSwap.outgoingContentAlpha(it)) }
        val incoming = steps.map { SwapBlur.strength(TapSwap.incomingContentAlpha(it)) }
        assertTrue(outgoing.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(incoming.zipWithNext().all { (a, b) -> b <= a })
        assertEquals(0f, SwapBlur.strength(TapSwap.outgoingContentAlpha(0f)), 0f)
        assertEquals(1f, SwapBlur.strength(TapSwap.outgoingContentAlpha(TapSwap.OUTGOING_FADE_END)), 0f)
        assertEquals(1f, SwapBlur.strength(TapSwap.incomingContentAlpha(TapSwap.INCOMING_FADE_START)), 0f)
        assertEquals(0f, SwapBlur.strength(TapSwap.incomingContentAlpha(TapSwap.INCOMING_FADE_END)), 0f)
    }
}
