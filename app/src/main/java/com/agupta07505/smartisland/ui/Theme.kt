/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.glass.GlassBackdropColors
import com.agupta07505.smartisland.ui.glass.LiquidGlassBackdrop
import com.agupta07505.smartisland.ui.glass.LocalGlassBackdropColors
import com.agupta07505.smartisland.ui.glass.LocalGlassDark
import com.agupta07505.smartisland.ui.glass.LocalLiquidGlass

// The app's theme: Material 3 colour roles, built from one accent colour (or from the wallpaper with Material You),
// drawn either flat (plain Material 3) or as liquid glass (translucent panels over a soft colour backdrop, see
// ui/glass).

private fun hsl(hue: Float, sat: Float, light: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    return Color(ColorUtils.HSLToColor(floatArrayOf(h, sat.coerceIn(0f, 1f), light.coerceIn(0f, 1f))))
}

/**
 * A full Material 3 colour scheme grown from [seed]: primary, secondary (the same hue, calmer) and tertiary (a warm-
 * shifted hue) families plus neutrals that lean slightly towards the seed's hue, so surfaces never look grey next to
 * the accent.
 */
internal fun accentColorScheme(seed: Color, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val h = hsl[0]
    val s = hsl[1].coerceIn(0.40f, 0.95f)
    val h3 = h + 58f
    // A touch of the accent in the neutrals (more in the light theme, where it shows as a tinted white).
    val n = (s * 0.16f).coerceIn(0.05f, 0.14f)
    return if (!dark) {
        lightColorScheme(
            primary = hsl(h, s, 0.42f),
            onPrimary = Color.White,
            primaryContainer = hsl(h, s * 0.95f, 0.91f),
            onPrimaryContainer = hsl(h, s, 0.16f),
            secondary = hsl(h, s * 0.40f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hsl(h, s * 0.50f, 0.91f),
            onSecondaryContainer = hsl(h, s * 0.55f, 0.15f),
            tertiary = hsl(h3, s * 0.60f, 0.42f),
            onTertiary = Color.White,
            tertiaryContainer = hsl(h3, s * 0.65f, 0.91f),
            onTertiaryContainer = hsl(h3, s * 0.70f, 0.15f),
            background = hsl(h, n * 1.8f, 0.965f),
            onBackground = hsl(h, 0.20f, 0.11f),
            surface = hsl(h, n * 1.2f, 0.99f),
            onSurface = hsl(h, 0.20f, 0.11f),
            surfaceVariant = hsl(h, n * 1.6f, 0.93f),
            onSurfaceVariant = hsl(h, 0.10f, 0.34f),
            surfaceTint = hsl(h, s, 0.42f),
            outline = hsl(h, 0.08f, 0.72f),
            outlineVariant = hsl(h, 0.10f, 0.88f),
            surfaceBright = hsl(h, n, 0.99f),
            surfaceDim = hsl(h, n * 1.4f, 0.90f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hsl(h, n * 1.4f, 0.965f),
            surfaceContainer = hsl(h, n * 1.5f, 0.945f),
            surfaceContainerHigh = hsl(h, n * 1.6f, 0.925f),
            surfaceContainerHighest = hsl(h, n * 1.7f, 0.905f)
        )
    } else {
        darkColorScheme(
            primary = hsl(h, s, 0.74f),
            onPrimary = hsl(h, s, 0.14f),
            primaryContainer = hsl(h, s * 0.60f, 0.26f),
            onPrimaryContainer = hsl(h, s * 0.75f, 0.90f),
            secondary = hsl(h, s * 0.45f, 0.76f),
            onSecondary = hsl(h, s * 0.5f, 0.14f),
            secondaryContainer = hsl(h, s * 0.35f, 0.25f),
            onSecondaryContainer = hsl(h, s * 0.50f, 0.90f),
            tertiary = hsl(h3, s * 0.55f, 0.76f),
            onTertiary = hsl(h3, s * 0.5f, 0.14f),
            tertiaryContainer = hsl(h3, s * 0.40f, 0.26f),
            onTertiaryContainer = hsl(h3, s * 0.55f, 0.90f),
            background = hsl(h, n, 0.075f),
            onBackground = hsl(h, 0.06f, 0.92f),
            surface = hsl(h, n, 0.115f),
            onSurface = hsl(h, 0.06f, 0.92f),
            surfaceVariant = hsl(h, n * 1.1f, 0.19f),
            onSurfaceVariant = hsl(h, 0.08f, 0.72f),
            surfaceTint = hsl(h, s, 0.74f),
            outline = hsl(h, 0.08f, 0.40f),
            outlineVariant = hsl(h, 0.08f, 0.24f),
            surfaceBright = hsl(h, n, 0.20f),
            surfaceDim = hsl(h, n, 0.07f),
            surfaceContainerLowest = hsl(h, n, 0.06f),
            surfaceContainerLow = hsl(h, n, 0.10f),
            surfaceContainer = hsl(h, n, 0.13f),
            surfaceContainerHigh = hsl(h, n, 0.165f),
            surfaceContainerHighest = hsl(h, n, 0.20f)
        )
    }
}

/**
 * The glass version of a scheme: the same colours, but the page background is clear (the backdrop is drawn behind
 * it), the panels (surface, surfaceVariant, surfaceContainer) are see-through so the backdrop tints them, and the
 * roles that dialogs, menus and sheets sit on stay nearly solid, so text over a dimmed screen stays readable.
 */
internal fun ColorScheme.asGlass(dark: Boolean): ColorScheme = copy(
    background = Color.Transparent,
    surface = surface.copy(alpha = if (dark) 0.58f else 0.62f),
    surfaceVariant = surfaceVariant.copy(alpha = if (dark) 0.50f else 0.55f),
    surfaceContainerLowest = surfaceContainerLowest.copy(alpha = 0.50f),
    surfaceContainerLow = surfaceContainerLow.copy(alpha = 0.58f),
    surfaceContainer = surfaceContainer.copy(alpha = if (dark) 0.72f else 0.78f),
    surfaceContainerHigh = surfaceContainerHigh.copy(alpha = 0.95f),
    surfaceContainerHighest = surfaceContainerHighest.copy(alpha = 0.96f),
    outline = if (dark) Color.White.copy(alpha = 0.34f) else Color.Black.copy(alpha = 0.22f),
    outlineVariant = if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f)
)

@Composable
fun SmartIslandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** "system" follows [darkTheme]; "light" / "dark" force one. */
    themeMode: String = "system",
    /** Liquid glass look (translucent panels over a colour backdrop). Off gives plain Material 3. */
    liquidGlass: Boolean = SmartIslandSettings.Default.liquidGlassTheme,
    /** Colours from the phone's wallpaper (Android 12+). Off builds the palette from [accentColor]. */
    materialYou: Boolean = SmartIslandSettings.Default.materialYou,
    /** The accent colour (ARGB) the palette is built from when Material You is off. */
    accentColor: Long = SmartIslandSettings.Default.accentColor,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }
    val solid = remember(materialYou, accentColor, useDark, context) {
        if (materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            accentColorScheme(Color(accentColor.toInt()), useDark)
        }
    }
    val colorScheme = if (liquidGlass) solid.asGlass(useDark) else solid

    // Keep the status / navigation bar icons readable when the chosen theme differs from the phone's.
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalLiquidGlass provides liquidGlass,
            LocalGlassDark provides useDark,
            LocalGlassBackdropColors provides remember(solid) {
                GlassBackdropColors(solid.background, solid.primary, solid.tertiary, solid.secondary)
            }
        ) {
            if (liquidGlass) {
                Box(Modifier.fillMaxSize()) {
                    LiquidGlassBackdrop(
                        base = solid.background,
                        glowA = solid.primary,
                        glowB = solid.tertiary,
                        glowC = solid.secondary,
                        dark = useDark
                    )
                    content()
                }
            } else {
                content()
            }
        }
    }
}
