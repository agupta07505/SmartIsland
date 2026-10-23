/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Which row a search result asked the screen to bring into view. [jumpTo] sets the row key and bumps [token]; every
 * [searchAnchor] with that key then scrolls itself into view and flashes.
 */
class SearchFocusState {
    var key: String? by mutableStateOf(null)
        private set
    var token: Int by mutableIntStateOf(0)
        private set

    fun jumpTo(rowKey: String?) {
        key = rowKey
        token += 1
    }

    fun clear() {
        key = null
    }
}

val LocalSearchFocus = compositionLocalOf<SearchFocusState?> { null }

/** How long a jump stays armed. Long enough for the screen transition plus the flash. */
const val SEARCH_FOCUS_ARMED_MS = 4000L

/**
 * Marks a row as a search target. When a search result with the same [key] is chosen the row is scrolled into view
 * (inside a plain `verticalScroll` parent) and flashes briefly.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.searchAnchor(key: String?): Modifier = composed {
    val focus = LocalSearchFocus.current
    if (key == null || focus == null) {
        this
    } else {
        val requester = remember { BringIntoViewRequester() }
        val flash = remember { Animatable(0f) }
        val color = MaterialTheme.colorScheme.primary
        LaunchedEffect(focus.token) {
            if (focus.key != key) return@LaunchedEffect
            // Let the screen transition and the first layout finish before scrolling.
            delay(380)
            requester.bringIntoView()
            repeat(2) {
                flash.animateTo(1f, tween(220))
                flash.animateTo(0.2f, tween(220))
            }
            flash.animateTo(1f, tween(220))
            delay(700)
            flash.animateTo(0f, tween(500))
        }
        this
            .bringIntoViewRequester(requester)
            .drawBehind {
                val alpha = flash.value
                if (alpha > 0f) {
                    drawRoundRect(
                        color = color.copy(alpha = 0.20f * alpha),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
            }
    }
}
