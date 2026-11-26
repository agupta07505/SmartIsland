/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data
internal const val MAX_PACKAGE_NAME_LENGTH = 255

internal val VALID_LOCK_SCREEN_PRIVACY_VALUES = setOf("AppIconOnly", "FullContent")
internal val VALID_CIRCLE_POSITIONS = setOf(
    SmartIslandSettings.CIRCLE_POSITION_RIGHT,
    SmartIslandSettings.CIRCLE_POSITION_LEFT
)
internal val VALID_CUTOUT_STYLES = setOf(
    SmartIslandSettings.CUTOUT_STYLE_AUTO,
    SmartIslandSettings.CUTOUT_STYLE_LEFT,
    SmartIslandSettings.CUTOUT_STYLE_CENTER,
    SmartIslandSettings.CUTOUT_STYLE_RIGHT,
    SmartIslandSettings.CUTOUT_STYLE_TEARDROP
)
internal val VALID_LANDSCAPE_SHAPES = setOf(
    SmartIslandSettings.LANDSCAPE_SHAPE_PILL,
    SmartIslandSettings.LANDSCAPE_SHAPE_CIRCLE
)
internal val VALID_LANDSCAPE_POSITIONS = setOf(
    SmartIslandSettings.LANDSCAPE_POSITION_AUTO,
    SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
    SmartIslandSettings.LANDSCAPE_POSITION_RIGHT,
    SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE
)

internal fun validDimension(
    value: Float?,
    fallback: Float,
    min: Float,
    max: Float
): Float = value?.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback

internal fun validColor(value: Long?, fallback: Long): Long =
    value?.takeIf { it != 0L } ?: fallback

internal fun validPackage(packageName: String) =
    packageName.isNotBlank() && packageName.length <= MAX_PACKAGE_NAME_LENGTH

internal fun Set<String>.validPackages(): Set<String> = filter { validPackage(it) }.toSet()
