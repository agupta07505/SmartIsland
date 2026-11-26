/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.agupta07505.smartisland.model.SwapBlur
import com.agupta07505.smartisland.model.SwapBlurGate

/**
 * The blur the swap puts on the card content: one effect object per whole-pixel level, made on first use and kept, so a
 * frame only picks one (no allocation, and the layer is touched only when the level changes). Nothing is returned
 * where the blur cannot or should not run: below Android 12, for content that is not seen, at a radius too small to
 * see, and once [SwapBlurGate] has found it costs frames (not while [SwapBlur.DISABLE_ON_JANK] is off).
 */
internal class SwapBlurEffects {
    private val cache = arrayOfNulls<RenderEffect>(SwapBlur.MAX_LEVEL + 1)
    private val gate = SwapBlurGate()

    /** The swap drew a blur since it began. */
    var usedThisSwap: Boolean = false
        private set

    /** Effect for card content shown at [alpha] (null: draw it sharp). */
    fun effectFor(alpha: Float, density: Float): RenderEffect? {
        if (!SwapBlur.ENABLED) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        if (alpha <= HIDDEN_ALPHA || !gate.enabled || !SwapBlur.supported(Build.VERSION.SDK_INT)) return null
        val level = SwapBlur.level(SwapBlur.radiusPx(alpha, SwapBlur.MAX_RADIUS_DP * density))
        if (level == 0) return null
        usedThisSwap = true
        return cache[level] ?: create(level).also { cache[level] = it }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun create(level: Int): RenderEffect {
        val r = level.toFloat()
        // Decal: nothing is smeared in from beyond the card's edge, the shell's own clip hides the soft rim
        return android.graphics.RenderEffect.createBlurEffect(r, r, Shader.TileMode.DECAL).asComposeRenderEffect()
    }

    fun beginSwap() {
        usedThisSwap = false
        gate.beginSwap()
    }

    fun restart() = gate.restart()

    fun frame(nowNs: Long) = gate.frame(nowNs)

    fun endSwap() {
        gate.endSwap(usedThisSwap)
        usedThisSwap = false
    }

    private companion object {
        const val HIDDEN_ALPHA = 0.01f
    }
}

/** Sets [effect] on a parked layer only when it changes (the same object every frame costs nothing). */
internal fun GraphicsLayer.setSwapBlur(effect: RenderEffect?) {
    if (renderEffect !== effect) renderEffect = effect
}
