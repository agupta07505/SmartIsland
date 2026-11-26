/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class CardDepth { First, Preview, Reader, Selector, PictureMid, Picture }

data class CardSteps(
    val preview: Boolean = false,
    val reader: Boolean = false,
    val pictureIndex: Int? = null,
    val pictureHalf: Boolean = false,
    val selector: Boolean = false
) {
    val depth: CardDepth
        get() = when {
            pictureIndex != null -> if (pictureHalf) CardDepth.PictureMid else CardDepth.Picture
            selector -> CardDepth.Selector
            reader -> CardDepth.Reader
            preview -> CardDepth.Preview
            else -> CardDepth.First
        }

    companion object {
        val First = CardSteps()

        fun arrow(steps: CardSteps, mediaCount: Int): CardSteps = when {
            steps.reader -> First
            mediaCount >= 2 && steps.preview -> CardSteps(reader = true)
            mediaCount >= 2 -> CardSteps(preview = true)
            else -> CardSteps(reader = true)
        }

        /**
         * Swipe down: 1 step deeper, null when none. [textExpandable]: a text-only card has cut-off text to open. Two
         * or more pictures open the selector, not a picture
         */
        fun deeper(steps: CardSteps, mediaCount: Int, textExpandable: Boolean): CardSteps? = when (steps.depth) {
            CardDepth.First -> when {
                mediaCount >= 2 -> CardSteps(preview = true)
                mediaCount == 1 || textExpandable -> CardSteps(reader = true)
                else -> null
            }
            CardDepth.Preview -> CardSteps(reader = true)
            CardDepth.Reader -> when {
                mediaCount >= 2 -> CardSteps(reader = true, preview = steps.preview, selector = true)
                mediaCount >= 1 -> CardSteps(reader = true, preview = steps.preview, pictureIndex = mediaCount - 1,
                    pictureHalf = true)
                else -> null
            }
            CardDepth.Selector -> null
            CardDepth.PictureMid -> steps.copy(pictureHalf = false)
            CardDepth.Picture -> null
        }

        fun back(steps: CardSteps, mediaCount: Int): CardSteps? = when (steps.depth) {
            CardDepth.First -> null
            CardDepth.Preview -> First
            CardDepth.Reader -> if (mediaCount >= 2) CardSteps(preview = true) else First
            CardDepth.Selector -> CardSteps(preview = true)
            CardDepth.PictureMid -> CardSteps(reader = true)
            CardDepth.Picture -> if (steps.selector) steps.copy(pictureIndex = null,
                pictureHalf = false) else steps.copy(pictureHalf = true)
        }

        fun pickFromSelector(steps: CardSteps, index: Int): CardSteps =
                steps.copy(pictureIndex = index, pictureHalf = false, selector = true)
    }
}

/** Open step of 1 card ([key]), any other card is at the first state so stale state never shows. */
data class CardStepState(
    val key: String? = null,
    val preview: Boolean = false,
    val reader: Boolean = false,
    val pictureHalf: Boolean = false,
    val selector: Boolean = false,
    val textExpandable: Boolean = false
) {
    fun steps(forKey: String?, pictureKey: String?, pictureIndex: Int): CardSteps {
        if (forKey == null) return CardSteps.First
        val picture = if (pictureKey == forKey) pictureIndex else null
        if (key != forKey) return CardSteps(pictureIndex = picture)
        return CardSteps(preview = preview, reader = reader, pictureIndex = picture,
            pictureHalf = picture != null && pictureHalf, selector = selector)
    }
}

object PictureSize {
    const val CARD_MIN_DP = 95f

    const val FULL_DP = 260f

    const val MID_DP = (CARD_MIN_DP + FULL_DP) / 2f

    fun heightDp(half: Boolean): Float = if (half) MID_DP else FULL_DP
}
