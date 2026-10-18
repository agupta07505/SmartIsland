package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import org.junit.Test
import kotlin.math.abs

class IslandMotionTest {

    @Test
    fun presetsCarryTheReferenceStiffnessAndDamping() {
        assertEquals(150f to 12f, SpringPreset.Bouncy.stiffness to SpringPreset.Bouncy.damping)
        assertEquals(300f to 22f, SpringPreset.Snappy.stiffness to SpringPreset.Snappy.damping)
        assertEquals(180f to 27.5f, SpringPreset.Critical.stiffness to SpringPreset.Critical.damping)
        assertEquals(60f to 5f, SpringPreset.Loose.stiffness to SpringPreset.Loose.damping)
    }

    @Test
    fun dampingRatioIsDampingOverTwiceTheRootOfStiffness() {
        assertEquals(0.4899f, SpringPreset.Bouncy.dampingRatio, 0.001f)
        assertEquals(0.6351f, SpringPreset.Snappy.dampingRatio, 0.001f)
        assertEquals(1.0249f, SpringPreset.Critical.dampingRatio, 0.001f)
        assertEquals(0.3227f, SpringPreset.Loose.dampingRatio, 0.001f)
    }

    @Test
    fun onlyCriticalIsAtOrPastCriticalDamping() {
        assertTrue(SpringPreset.Critical.dampingRatio >= 1f)
        listOf(SpringPreset.Bouncy, SpringPreset.Snappy, SpringPreset.Loose).forEach {
            assertTrue(it.name, it.dampingRatio < 1f)
        }
    }

    @Test
    fun standardIsNotAPhysicalPresetAndKeepsAUnitRatio() {
        assertFalse(SpringPreset.Standard.isPhysical)
        assertEquals(1f, SpringPreset.Standard.dampingRatio, 0f)
        assertTrue(SpringPreset.entries.filter { it != SpringPreset.Standard }.all { it.isPhysical })
    }

    @Test
    fun unknownIdsFallBackToStandard() {
        assertEquals(SpringPreset.Standard, SpringPreset.fromId(null))
        assertEquals(SpringPreset.Standard, SpringPreset.fromId("wobbly"))
        SpringPreset.entries.forEach { assertEquals(it, SpringPreset.fromId(it.id)) }
    }

    @Test
    fun glowAppearsOnlyAboveTheSpeedThreshold() {
        assertEquals(0f, KineticGlow.blurRadius(0f), 0f)
        assertEquals(0f, KineticGlow.blurRadius(5f), 0f)
        assertTrue(KineticGlow.blurRadius(5.01f) > 0f)
    }

    @Test
    fun glowBlurIsSpeedTimesZeroPointZeroEightCappedAtTwenty() {
        assertEquals(10f, KineticGlow.blurRadius(125f), 0.001f)
        assertEquals(20f, KineticGlow.blurRadius(250f), 0.001f)
        assertEquals(20f, KineticGlow.blurRadius(5_000f), 0f)
    }

    @Test
    fun glowIgnoresTheDirectionOfTravel() {
        assertEquals(KineticGlow.blurRadius(180f), KineticGlow.blurRadius(-180f), 0f)
    }

    @Test
    fun theMotionSettingsSurviveABackupRoundTrip() {
        val original = SmartIslandSettings(springPreset = SpringPreset.Bouncy.id, kineticGlow = false)
        val restored = SmartIslandSettings.fromJson(original.toJson(appVersion = "test"))
        assertEquals(SpringPreset.Bouncy.id, restored.springPreset)
        assertFalse(restored.kineticGlow)
    }

    @Test
    fun anOldBackupWithoutMotionSettingsGetsTheDefaults() {
        val restored = SmartIslandSettings.fromJson("{\"settings\":{}}")
        assertEquals(SpringPreset.Standard.id, restored.springPreset)
        assertTrue(restored.kineticGlow)
    }

    /**
     * The reference integrates `force = -k * (x - target) - d * v` with unit mass (semi-implicit Euler). A
     * Compose spring with `dampingRatio = d / (2 * sqrt(k))` and the same stiffness must trace the same path.
     */
    @Test
    fun composeSpringMatchesTheReferenceIntegratorForEveryPreset() {
        val dt = 0.0005f
        SpringPreset.entries.filter { it.isPhysical }.forEach { preset ->
            var x = 0f
            var v = 0f
            val target = 1f
            val animation = TargetBasedAnimation(
                animationSpec = spring(dampingRatio = preset.dampingRatio, stiffness = preset.stiffness),
                typeConverter = Float.VectorConverter,
                initialValue = 0f,
                targetValue = target
            )
            var t = 0f
            var checkAt = 0.1f
            while (t < 1.5f) {
                v += (-preset.stiffness * (x - target) - preset.damping * v) * dt
                x += v * dt
                t += dt
                if (t >= checkAt) {
                    val compose = animation.getValueFromNanos((t * 1_000_000_000L).toLong())
                    assertEquals("${preset.name} at t=$t", x, compose, 0.02f)
                    checkAt += 0.1f
                }
            }
        }
    }

    @Test
    fun bouncyOvershootsAndCriticalDoesNot() {
        fun peak(preset: SpringPreset): Float {
            val animation = TargetBasedAnimation(
                animationSpec = spring(dampingRatio = preset.dampingRatio, stiffness = preset.stiffness),
                typeConverter = Float.VectorConverter,
                initialValue = 0f,
                targetValue = 1f
            )
            return (0..300).maxOf { animation.getValueFromNanos(it * 10_000_000L) }
        }
        assertTrue(peak(SpringPreset.Bouncy) > 1.1f)
        assertTrue(peak(SpringPreset.Loose) > 1.2f)
        assertTrue(abs(peak(SpringPreset.Critical) - 1f) < 0.01f)
    }
}
