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

/** Colours of the expanded island, in two looks. */
@Immutable
class IslandPalette private constructor(val isLight: Boolean) {
    /** The island's surface colour in the light look (the dark look draws the pill colour from the settings). */
    val surface: Color = if (isLight) Color(0xFFF1EFF3) else Color.Black

    /** Titles, big numbers, icons on the surface. */
    val text: Color = if (isLight) Color.Black else Color.White

    /** Body text under a title. */
    val textSecondary: Color = if (isLight) Color(0xFF2E2D2F) else Color(0xFFD5DAE0)

    /** Captions, hints, sub labels. */
    val textTertiary: Color = if (isLight) Color(0xFF5D5C5D) else Color(0xFFB7C0CA)

    /** The quietest readable text (units, small labels). */
    val textMuted: Color = if (isLight) Color(0xFF6C6B6D) else Color(0xFF9CA3AF)

    val placeholder: Color = if (isLight) Color(0xFF7B7A7C) else Color(0xFF888888)

    /** A solid button (play, answer) with a glyph on it: white on dark, near-black on light. */
    val inverseFill: Color = if (isLight) Color.Black else Color.White
    val onInverse: Color = if (isLight) Color.White else Color.Black

    /** The pale action chip (reply actions, Telegram style buttons) and its text. */
    val chipFill: Color = if (isLight) Color(0xFFD9D8DB) else Color(0xFFE2E8F0)
    val chipText: Color = Color(0xFF1F2937)

    /** Accent colours that must stay readable on the surface. */
    val accentCyan: Color = if (isLight) Color(0xFF0058CB) else Color(0xFF67E8F9)
    val danger: Color = if (isLight) Color(0xFFB91C1C) else Color(0xFFEF4444)
    val success: Color = if (isLight) Color(0xFF047857) else Color(0xFF10B981)
    val warning: Color = if (isLight) Color(0xFFA04A08) else Color(0xFFF59E0B)

    /** How much stronger the island's shadow is: a pale card over a pale page needs more of it to lift off (1.5x on light). */
    val shadowScale: Float = if (isLight) 1.5f else 1f

    /** The colour of the scrim over a card's picture backdrop: black on dark, white on light (so the picture is washed, not dimmed). */
    val scrim: Color = if (isLight) Color.White else Color.Black

    /**
     * A soft overlay for buttons, tracks and dividers: white on dark, black on light (a little weaker, as black reads
     * stronger than white).
     */
    fun tint(alpha: Float): Color =
        if (isLight) Color.Black.copy(alpha = (alpha * LIGHT_TINT_SCALE).coerceIn(0f, 1f)) else Color.White.copy(alpha = alpha)

    /** A solid colour that differs per look (a field background, a divider): [dark] is the original value, [light] its light twin. */
    fun pick(dark: Color, light: Color): Color = if (isLight) light else dark

    companion object {
        private const val LIGHT_TINT_SCALE = 0.75f
        val Dark = IslandPalette(isLight = false)
        val Light = IslandPalette(isLight = true)
        fun of(light: Boolean): IslandPalette = if (light) Light else Dark
    }
}

/** The palette of the island being drawn; dark unless the overlay says otherwise, so a preview or test keeps the original look. */
val LocalIslandPalette = compositionLocalOf { IslandPalette.Dark }

/** Which look an expanded island has for a setting value. "system" is light when the phone is not in dark mode. */
object ExpandedThemeRules {
    fun isLight(mode: String, systemDark: Boolean): Boolean = when (mode) {
        SmartIslandSettings.EXPANDED_THEME_LIGHT -> true
        SmartIslandSettings.EXPANDED_THEME_SYSTEM -> !systemDark
        else -> false
    }
}

/** The phone's night mode as the overlay service last saw it. */
object SystemNight {
    var isNight: Boolean? by mutableStateOf<Boolean?>(null)
        private set

    fun update(uiMode: Int) {
        isNight = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

@Composable
fun rememberSystemDark(): Boolean {
    val fromConfiguration = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return SystemNight.isNight ?: fromConfiguration
}

/** The palette for the current setting; follows the phone live when the setting is "system". */
@Composable
fun rememberIslandPalette(settings: SmartIslandSettings): IslandPalette {
    val systemDark = rememberSystemDark()
    val light = ExpandedThemeRules.isLight(settings.expandedTheme, systemDark)
    return remember(light) { IslandPalette.of(light) }
}
