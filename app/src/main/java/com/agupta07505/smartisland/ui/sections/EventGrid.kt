/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

/** Width (dp) from which the Home event grid uses three columns instead of two. */
internal const val WIDE_GRID_MIN_WIDTH_DP = 600

internal fun eventGridColumns(screenWidthDp: Int): Int =
    if (screenWidthDp >= WIDE_GRID_MIN_WIDTH_DP) 3 else 2

/**
 * Splits [items] into rows of [columns]. The last row is returned short; the tile row pads it with empty cells so
 * every tile keeps the same width.
 */
internal fun <T> gridRows(items: List<T>, columns: Int): List<List<T>> =
    if (items.isEmpty()) emptyList() else items.chunked(columns.coerceAtLeast(1))
