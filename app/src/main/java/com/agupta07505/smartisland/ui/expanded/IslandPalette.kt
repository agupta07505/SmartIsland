/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.agupta07505.smartisland.data.SmartIslandSettings

@Immutable
class IslandPalette private constructor(val isLight: Boolean) {
    /** Light look surface, the dark look takes the pill colour from the settings. */
    val surface: Color = pick(dark = Color.Black, light = Color(0xFFF1EFF3))

    /** Titles, big numbers, icons on the surface. */
    val text: Color = pick(dark = Color.White, light = Color.Black)

    val textSecondary: Color = pick(dark = Color(0xFFD5DAE0), light = Color(0xFF2E2D2F))

    val textTertiary: Color = pick(dark = Color(0xFFB7C0CA), light = Color(0xFF5D5C5D))

    /** Quietest readable text (units, small labels) */
    val textMuted: Color = pick(dark = Color(0xFF9CA3AF), light = Color(0xFF6C6B6D))

    val placeholder: Color = pick(dark = Color(0xFF888888), light = Color(0xFF7B7A7C))

    /** Solid button (play, answer): white glyph on dark, near-black on light. */
    val inverseFill: Color = pick(dark = Color.White, light = Color.Black)
    val onInverse: Color = pick(dark = Color.Black, light = Color.White)

    /** Pale action chip (reply actions, Telegram style buttons) and its text. */
    val chipFill: Color = pick(dark = Color(0xFFE2E8F0), light = Color(0xFFD9D8DB))
    val chipText: Color = Color(0xFF1F2937)

    /**
     * Pale blue chip for what the island offers itself (suggested replies, Open link, Copy code), so it is told apart
     * from the app's own grey action buttons, and its text
     */
    val smartChipFill: Color = pick(dark = Color(0xFFBFDBFE), light = Color(0xFFCFE0FA))
    val smartChipText: Color = pick(dark = Color(0xFF1E3A8A), light = Color(0xFF0B3A8C))

    val accentCyan: Color = pick(dark = Color(0xFF67E8F9), light = Color(0xFF0058CB))
    val danger: Color = pick(dark = Color(0xFFEF4444), light = Color(0xFFB91C1C))
    val success: Color = pick(dark = Color(0xFF10B981), light = Color(0xFF047857))
    val warning: Color = pick(dark = Color(0xFFF59E0B), light = Color(0xFFA04A08))

    /** A pale card over a pale page needs a stronger shadow to lift off (1.5x on light). */
    val shadowScale: Float = if (isLight) 1.5f else 1f

    /** Over a card's picture: black on dark, white on light (the picture is washed, not dimmed). */
    val scrim: Color = pick(dark = Color.Black, light = Color.White)

    /** Buttons, tracks and dividers: white on dark, a little weaker black on light. */
    fun tint(alpha: Float): Color =
        if (isLight) {
            Color.Black.copy(alpha = (alpha * LIGHT_TINT_SCALE).coerceIn(0f, 1f))
        } else {
            Color.White.copy(alpha = alpha)
        }

    fun pick(dark: Color, light: Color): Color = if (isLight) light else dark

    companion object {
        private const val LIGHT_TINT_SCALE = 0.75f
        val Dark = IslandPalette(isLight = false)
        val Light = IslandPalette(isLight = true)
        fun of(light: Boolean): IslandPalette = if (light) Light else Dark
    }
}

/** Dark unless the overlay says otherwise, so a preview or test keeps the original look. */
val LocalIslandPalette = compositionLocalOf { IslandPalette.Dark }

object ExpandedThemeRules {
    fun isLight(mode: String, systemDark: Boolean): Boolean = when (mode) {
        SmartIslandSettings.EXPANDED_THEME_LIGHT -> true
        SmartIslandSettings.EXPANDED_THEME_SYSTEM -> !systemDark
        else -> false
    }
}

object SystemNight {
    var isNight: Boolean? by mutableStateOf<Boolean?>(null)
        private set

    fun update(uiMode: Int) {
        isNight = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

@Composable
fun rememberSystemDark(): Boolean {
    val fromConfiguration =
        (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return SystemNight.isNight ?: fromConfiguration
}

@Composable
fun rememberIslandPalette(settings: SmartIslandSettings): IslandPalette {
    val systemDark = rememberSystemDark()
    val light = ExpandedThemeRules.isLight(settings.expandedTheme, systemDark)
    return remember(light) { IslandPalette.of(light) }
}
