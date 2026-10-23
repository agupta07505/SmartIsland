/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt

/**
 * A card step (preview, reader, picture) following the finger (model/StepDrag.kt). The gesture handler writes it; the
 * expanded content reads it to run the island's height and the card's brightness.
 */
class StepDragState {
    var active by mutableStateOf(false)

    /**
     * The finger is up and the step has finished or gone back: the real card is the only copy drawn again, while the
     * shell may still hand its height back.
     */
    var settled by mutableStateOf(false)

    /**
     * The expanded content is drawing the two landscape islands (picker or picture open, or shrinking away). The
     * overlay keeps its two-island window and surface until this is false.
     */
    var splitShown by mutableStateOf(false)

    /**
     * The overlay's side of the close hand-off: the shell is the single island again (surface, clip and window back).
     */
    var shellSingleReady by mutableStateOf(true)

    /**
     * A left/right swipe that may bring another card is being dragged: 0 = none, 1 = next page likely, -1 = previous,
     * 2 = card change with no side preference. The content measures the neighbours at once.
     */
    var swipeNeighbour by mutableIntStateOf(0)

    /**
     * Landscape split: the ONE split progress (0 = card only, 1 = card and the island beside it), set by the content
     * while it draws the two islands, null otherwise.
     */
    var splitProgressNow: (() -> Float)? = null

    private var splitShellLatched = false
    private var splitShellLatchedAtMs = 0L

    /** The width (dp) the shell must have for the landscape split, or negative when it follows its own. */
    fun splitShellWidthDp(splitNow: Boolean, expanded: Boolean, single: Float, total: Float, own: Float): Float {
        val reader = splitProgressNow
        if (splitNow && expanded && reader != null) {
            splitShellLatched = true
            splitShellLatchedAtMs = android.os.SystemClock.uptimeMillis()
            return com.agupta07505.smartisland.model.SplitShell.width(single, total, reader())
        }
        if (splitShellLatched) {
            val expired = android.os.SystemClock.uptimeMillis() - splitShellLatchedAtMs > com.agupta07505.smartisland.model.SplitShell.HANDOVER_MAX_MS
            if (!expanded || expired || com.agupta07505.smartisland.model.SplitShell.handoverDone(own, single)) {
                splitShellLatched = false
            } else {
                return single
            }
        }
        return -1f
    }

    /** The single island's height (dp) and the split row's height (dp), kept up to date by the expanded content. */
    var splitSingleHeightDp: Float = 0f
    var splitTotalHeightDp: Float = 0f
    private var splitHeightLatched = false
    private var splitHeightLatchedAtMs = 0L

    /** The height (dp) the shell must have for the landscape split, or negative when it follows its own. */
    fun splitShellHeightDp(splitNow: Boolean, expanded: Boolean, own: Float): Float {
        val reader = splitProgressNow
        val single = splitSingleHeightDp
        val total = splitTotalHeightDp
        if (splitNow && expanded && reader != null && single > 0f && total > 0f) {
            splitHeightLatched = true
            splitHeightLatchedAtMs = android.os.SystemClock.uptimeMillis()
            return com.agupta07505.smartisland.model.SplitShell.height(single, total, reader())
        }
        if (splitHeightLatched) {
            val expired = android.os.SystemClock.uptimeMillis() - splitHeightLatchedAtMs > com.agupta07505.smartisland.model.SplitShell.HANDOVER_MAX_MS
            if (!expanded || expired || single <= 0f ||
                kotlin.math.abs(own - single) <= com.agupta07505.smartisland.model.SplitShell.HANDOVER_TOLERANCE_DP
            ) {
                splitHeightLatched = false
            } else {
                return single
            }
        }
        return -1f
    }

    /** 0..1: how far the island has gone from the old step's height to the new step's. */
    var progress by mutableFloatStateOf(0f)

    /**
     * The shell's height (dp) while a step follows the finger, set on every move; negative = not in use. The shell
     * reads it at measure time and skips its own height spring.
     */
    var heightDp by mutableFloatStateOf(-1f)

    var fromDp: Float = 0f

    var targetDp: Float = 0f

    var shellHeightDp: Float = 0f

    var cardKey: String? = null

    /** The step the card was at before the drag; the fading copy keeps showing it. */
    var fromSteps: com.agupta07505.smartisland.model.CardSteps? = null

    fun currentHeightDp(): Float = com.agupta07505.smartisland.model.StepDrag.heightDp(fromDp, targetDp, progress)

    fun reset() {
        active = false
        settled = false
        progress = 0f
        heightDp = -1f
        cardKey = null
    }
}

val LocalStepDrag = compositionLocalOf { StepDragState() }

/**
 * Where a card sits when its slot is taller than the card: centred, but pinned to the TOP while a card step follows
 * the finger or is handed over.
 */
internal class StepAwareVerticalAlignment(private val stepDrag: StepDragState) : Alignment.Vertical {
    override fun align(size: Int, space: Int): Int =
        if (stepDrag.active) 0 else ((space - size) / 2f).roundToInt()
}

@Composable
internal fun rememberStepAwareAlignment(): Alignment.Vertical {
    val stepDrag = LocalStepDrag.current
    return remember(stepDrag) { StepAwareVerticalAlignment(stepDrag) }
}
