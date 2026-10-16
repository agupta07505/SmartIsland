/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Semantic colors that Material 3 has no slot for. Each is named by role, never by hue,
 * and has an explicit light and dark value that meets 4.5:1 against its surface.
 * Status colors must always be paired with an icon or text (color is never the only signal).
 */
@Immutable
data class SemanticColors(
    /** Behind inset-grouped lists (systemGroupedBackground). */
    val groupedBackground: Color,
    /** Row surface inside a grouped section (secondarySystemGroupedBackground). */
    val groupedRow: Color,
    /** Hairline between rows and around grouped sections. */
    val separator: Color,
    /** Subtle fill for chips, steppers and tracks. */
    val fill: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    /** Translucent bar background; paired with a blur-free scrim on Android. */
    val bar: Color
)

private val LightSemantic = SemanticColors(
    groupedBackground = Color(0xFFF2F2F7),
    groupedRow = Color(0xFFFFFFFF),
    separator = Color(0xFFC6C6C8),
    fill = Color(0xFFE9E9EE),
    success = Color(0xFF1F7A34),
    warning = Color(0xFFA64B00),
    danger = Color(0xFFD70015),
    bar = Color(0xF2F9F9F9)
)

private val DarkSemantic = SemanticColors(
    groupedBackground = Color(0xFF000000),
    groupedRow = Color(0xFF1C1C1E),
    separator = Color(0xFF38383A),
    fill = Color(0xFF2C2C2E),
    success = Color(0xFF30D158),
    warning = Color(0xFFFF9F0A),
    danger = Color(0xFFFF453A),
    bar = Color(0xF21C1C1E)
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemantic }

/** Accessor: `MaterialTheme.semantic.success`. */
val MaterialTheme.semantic: SemanticColors
    @Composable @ReadOnlyComposable get() = LocalSemanticColors.current

// Brand burnt orange is reserved for primary actions, "on" switches and selection.
// Everything else is neutral so the island itself stays the signature element.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC73A10), // 5.2:1 on white, 4.7:1 on grouped background
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE9E2),
    onPrimaryContainer = Color(0xFF7A2208),
    secondary = Color(0xFF6C6C70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9EE),
    onSecondaryContainer = Color(0xFF1C1C1E),
    tertiary = Color(0xFF6C6C70),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9E9EE),
    onTertiaryContainer = Color(0xFF1C1C1E),
    background = LightSemantic.groupedBackground,
    onBackground = Color(0xFF000000),
    surface = LightSemantic.groupedRow,
    onSurface = Color(0xFF000000),
    surfaceVariant = LightSemantic.fill,
    onSurfaceVariant = Color(0xFF6C6C70), // 5.2:1 on white, 4.7:1 on grouped background
    outline = LightSemantic.separator,
    outlineVariant = Color(0xFFE0E0E5),
    error = LightSemantic.danger,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E8),
    onErrorContainer = Color(0xFF7A000C)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF7A4D), // 6.6:1 on the row surface
    onPrimary = Color(0xFF2B0B02),
    primaryContainer = Color(0xFF45170A),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF98989F),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color(0xFFF2F2F7),
    tertiary = Color(0xFF98989F),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF2C2C2E),
    onTertiaryContainer = Color(0xFFF2F2F7),
    background = DarkSemantic.groupedBackground,
    onBackground = Color(0xFFFFFFFF),
    surface = DarkSemantic.groupedRow,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = DarkSemantic.fill,
    onSurfaceVariant = Color(0xFF98989F), // 5.9:1 on the row surface
    outline = DarkSemantic.separator,
    outlineVariant = Color(0xFF2C2C2E),
    error = DarkSemantic.danger,
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF4A0A10),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun SmartIslandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Neutral grouped palette by default; wallpaper color is opt-in
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalSemanticColors provides if (darkTheme) DarkSemantic else LightSemantic
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SmartIslandTypography,
            shapes = SmartIslandShapes,
            content = content
        )
    }
}
