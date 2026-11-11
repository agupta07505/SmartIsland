/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** A superellipse, |x|^n + |y|^n = 1: the corners ease into the edges instead of stopping at a circular arc. */
internal class SquircleShape(private val exponent: Float = 5f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val halfW = size.width / 2f
        val halfH = size.height / 2f
        val power = 2f / exponent
        for (i in 0 until STEPS) {
            val t = i * 2.0 * PI / STEPS
            val c = cos(t)
            val s = sin(t)
            val x = halfW + halfW * (sign(c) * abs(c).pow(power.toDouble())).toFloat()
            val y = halfH + halfH * (sign(s) * abs(s).pow(power.toDouble())).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    private companion object {
        const val STEPS = 96
    }
}
