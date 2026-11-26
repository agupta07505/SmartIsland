/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

internal const val WIDE_GRID_MIN_WIDTH_DP = 600

internal fun eventGridColumns(screenWidthDp: Int): Int =
    if (screenWidthDp >= WIDE_GRID_MIN_WIDTH_DP) 3 else 2

internal fun <T> gridRows(items: List<T>, columns: Int): List<List<T>> =
    if (items.isEmpty()) emptyList() else items.chunked(columns.coerceAtLeast(1))
