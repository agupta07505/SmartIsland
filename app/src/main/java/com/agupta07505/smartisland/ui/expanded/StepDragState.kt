/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.os.SystemClock
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.model.SplitShell
import com.agupta07505.smartisland.model.StepDrag
import kotlin.math.roundToInt

/** Card step (preview, reader, picture) following the finger (model/StepDrag.kt). */
class StepDragState {
    var active by mutableStateOf(false)

    /**
     * Finger up and the step finished or went back, only the real card is drawn again while the shell may still hand
     * its height back
     */
    var settled by mutableStateOf(false)

    /** Content is drawing the 2 landscape islands (or shrinking away). */
    var splitShown by mutableStateOf(false)

    /** Overlay side of the close hand-off, the shell is the single island again. */
    var shellSingleReady by mutableStateOf(true)

    /**
     * A left / right swipe that may bring another card is being dragged: 0 none, 1 next page likely, -1 previous, 2
     * card change with no side preference
     * The content measures the neighbours at once
     */
    var swipeNeighbour by mutableIntStateOf(0)

    /**
     * Landscape split progress (0 = card only, 1 = card and island beside it), set by the content while it draws the
     * 2 islands
     */
    var splitProgressNow: (() -> Float)? = null

    private val widthHandover = SplitHandover()
    private val heightHandover = SplitHandover()

    var splitSingleHeightDp: Float = 0f
    var splitTotalHeightDp: Float = 0f

    fun splitShellWidthDp(splitNow: Boolean, expanded: Boolean, single: Float, total: Float, own: Float): Float {
        val progress = splitProgressNow
        if (splitNow && expanded && progress != null) {
            widthHandover.track()
            return SplitShell.width(single, total, progress())
        }
        return widthHandover.release(expanded, own, single)
    }

    fun splitShellHeightDp(splitNow: Boolean, expanded: Boolean, own: Float): Float {
        val progress = splitProgressNow
        val single = splitSingleHeightDp
        val total = splitTotalHeightDp
        if (splitNow && expanded && progress != null && single > 0f && total > 0f) {
            heightHandover.track()
            return SplitShell.height(single, total, progress())
        }
        return heightHandover.release(expanded && single > 0f, own, single)
    }

    // Hold the single-island size until the shell catches up. Each axis settles independently.
    private class SplitHandover {
        private var lastSplitMs: Long? = null

        fun track() {
            lastSplitMs = SystemClock.uptimeMillis()
        }

        fun release(hold: Boolean, own: Float, single: Float): Float {
            val last = lastSplitMs ?: return -1f
            if (hold && SystemClock.uptimeMillis() - last <= SplitShell.HANDOVER_MAX_MS &&
                !SplitShell.handoverDone(own, single)
            ) return single

            lastSplitMs = null
            return -1f
        }
    }

    /** 0..1: how far the island has gone from the old step's height to the new step's. */
    var progress by mutableFloatStateOf(0f)

    /** Set on every move, negative = not in use. */
    var heightDp by mutableFloatStateOf(-1f)

    var fromDp: Float = 0f

    var targetDp: Float = 0f

    var shellHeightDp: Float = 0f

    var cardKey: String? = null

    /** The live reader's scroll, so the frozen copy that fades out shows the same lines. */
    var readerScroll: ScrollState? = null

    /** Step the card was at before the drag; the fading copy keeps showing it. */
    var fromSteps: CardSteps? = null

    fun currentHeightDp(): Float = StepDrag.heightDp(fromDp, targetDp, progress)

    fun reset() {
        active = false
        settled = false
        progress = 0f
        heightDp = -1f
        cardKey = null
    }
}

val LocalStepDrag = compositionLocalOf { StepDragState() }

internal class StepAwareVerticalAlignment(private val stepDrag: StepDragState) : Alignment.Vertical {
    override fun align(size: Int, space: Int): Int =
        if (stepDrag.active) 0 else ((space - size) / 2f).roundToInt()
}

@Composable
internal fun rememberStepAwareAlignment(): Alignment.Vertical {
    val stepDrag = LocalStepDrag.current
    return remember(stepDrag) { StepAwareVerticalAlignment(stepDrag) }
}
