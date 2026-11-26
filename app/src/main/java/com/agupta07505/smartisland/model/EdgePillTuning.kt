/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

data class EdgePillTuning(
    val widthDp: Float = DEFAULT_WIDTH,
    val heightDp: Float = DEFAULT_HEIGHT,
    val xOffsetDp: Float = 0f,
    val yOffsetDp: Float = 0f,
    val cornerRadiusDp: Float = DEFAULT_RADIUS,
    val opacity: Float = 1f,
    val bubbleGapDp: Float = 8f,
    val cardGapDp: Float = 8f,
    val bubblePos: Int = BUBBLE_AUTO,
    val flatEdge: Boolean = false,
    val contentSameSize: Boolean = false,
    val contentPercent: Float = DEFAULT_CONTENT_PERCENT,
    val cameraManual: Boolean = false,
    val cameraYDp: Float = DEFAULT_CAMERA_Y,
    val cameraLengthDp: Float = DEFAULT_CAMERA_LENGTH
) {
    fun withPortraitSizes(): EdgePillTuning = copy(widthDp = DEFAULT_WIDTH, heightDp = DEFAULT_HEIGHT,
            cornerRadiusDp = DEFAULT_RADIUS)

    fun contentHeightDp(screenHeightDp: Float): Float {
        if (contentSameSize) return heightDp
        val room = if (screenHeightDp > 0f) (screenHeightDp - 2f * CONTENT_SCREEN_MARGIN_DP)
            .coerceAtLeast(heightDp) else Float.MAX_VALUE
        return (heightDp * contentPercent / 100f).coerceAtMost(room).coerceAtLeast(heightDp)
    }

    fun coerced(): EdgePillTuning = EdgePillTuning(
        widthDp = widthDp.fin(DEFAULT_WIDTH).coerceIn(WIDTH.start, WIDTH.endInclusive),
        heightDp = heightDp.fin(DEFAULT_HEIGHT).coerceIn(HEIGHT.start, HEIGHT.endInclusive),
        xOffsetDp = if (flatEdge) 0f else xOffsetDp.fin(0f).coerceIn(X_OFFSET.start, X_OFFSET.endInclusive),
        yOffsetDp = yOffsetDp.fin(0f).coerceIn(Y_OFFSET.start, Y_OFFSET.endInclusive),
        cornerRadiusDp = cornerRadiusDp.fin(DEFAULT_RADIUS).coerceIn(RADIUS.start, RADIUS.endInclusive),
        opacity = opacity.fin(1f).coerceIn(OPACITY.start, OPACITY.endInclusive),
        bubbleGapDp = bubbleGapDp.fin(8f).coerceIn(BUBBLE_GAP.start, BUBBLE_GAP.endInclusive),
        cardGapDp = cardGapDp.fin(8f).coerceIn(CARD_GAP.start, CARD_GAP.endInclusive),
        bubblePos = if (bubblePos in BUBBLE_AUTO..BUBBLE_BELOW) bubblePos else BUBBLE_AUTO,
        flatEdge = flatEdge,
        contentSameSize = contentSameSize,
        contentPercent = contentPercent.fin(DEFAULT_CONTENT_PERCENT)
            .coerceIn(CONTENT_PERCENT.start, CONTENT_PERCENT.endInclusive),
        cameraManual = cameraManual,
        cameraYDp = cameraYDp.fin(DEFAULT_CAMERA_Y).coerceIn(CAMERA_Y.start, CAMERA_Y.endInclusive),
        cameraLengthDp = cameraLengthDp.fin(DEFAULT_CAMERA_LENGTH)
            .coerceIn(CAMERA_LENGTH.start, CAMERA_LENGTH.endInclusive)
    )

    /** One line of `key=value` pairs, for the settings store and the backup file. */
    fun encode(): String = listOf(
        "pw" to widthDp, "ph" to heightDp, "x" to xOffsetDp, "y" to yOffsetDp, "r" to cornerRadiusDp,
        "o" to opacity,
        "bg" to bubbleGapDp, "cg" to cardGapDp, "bp" to bubblePos.toFloat(),
        "fe" to (if (flatEdge) 1f else 0f),
        "cs" to (if (contentSameSize) 1f else 0f), "cp" to contentPercent,
        "cm" to (if (cameraManual) 1f else 0f), "cy" to cameraYDp, "cl" to cameraLengthDp
    ).joinToString(";") { (k, v) -> "$k=$v" }

    companion object {
        const val DEFAULT_HEIGHT = 112f

        const val DEFAULT_WIDTH = 34f

        /** The flat (teardrop, notch) look reaches across the hole, so it is thicker; also the top of the width range. */
        const val DEFAULT_FLAT_WIDTH = 60f

        fun defaultWidthFor(flatEdge: Boolean): Float = if (flatEdge) DEFAULT_FLAT_WIDTH else DEFAULT_WIDTH

        const val DEFAULT_RADIUS = 22f

        const val DEFAULT_CAMERA_Y = 16f
        const val DEFAULT_CAMERA_LENGTH = 14f
        val CAMERA_Y = 0f..480f
        val CAMERA_LENGTH = 4f..60f

        val WIDTH = 24f..60f
        val HEIGHT = 76f..180f
        val X_OFFSET = -12f..40f
        val Y_OFFSET = -120f..120f
        val RADIUS = 0f..40f
        val OPACITY = 0.3f..1f
        fun bubbleSizeFor(pillWidthDp: Float): Float = pillWidthDp

        val CONTENT_PERCENT = 100f..250f
        const val DEFAULT_CONTENT_PERCENT = 110f
        private const val CONTENT_SCREEN_MARGIN_DP = 24f

        val BUBBLE_GAP = 0f..32f
        val CARD_GAP = 0f..32f

        const val BUBBLE_AUTO = 0
        const val BUBBLE_ABOVE = 1
        const val BUBBLE_BELOW = 2

        fun decode(text: String?, flatEdgeDefault: Boolean = false): EdgePillTuning {
            if (text.isNullOrBlank()) {
                return EdgePillTuning(widthDp = defaultWidthFor(flatEdgeDefault), flatEdge = flatEdgeDefault)
            }
            val map = text.split(';').mapNotNull {
                val i = it.indexOf('=')
                if (i <= 0) null else it.substring(0, i).trim() to it.substring(i + 1).trim().toFloatOrNull()
            }.toMap()
            val d = EdgePillTuning(widthDp = defaultWidthFor(flatEdgeDefault), flatEdge = flatEdgeDefault)
            return EdgePillTuning(
                widthDp = map["pw"] ?: d.widthDp,
                heightDp = map["ph"] ?: d.heightDp,
                xOffsetDp = map["x"] ?: d.xOffsetDp,
                yOffsetDp = map["y"] ?: d.yOffsetDp,
                cornerRadiusDp = map["r"] ?: d.cornerRadiusDp,
                opacity = map["o"] ?: d.opacity,
                bubbleGapDp = map["bg"] ?: d.bubbleGapDp,
                cardGapDp = map["cg"] ?: d.cardGapDp,
                bubblePos = map["bp"]?.toInt() ?: d.bubblePos,
                flatEdge = map["fe"]?.let { it >= 0.5f } ?: d.flatEdge,
                contentSameSize = map["cs"]?.let { it >= 0.5f } ?: d.contentSameSize,
                contentPercent = map["cp"] ?: d.contentPercent,
                cameraManual = map["cm"]?.let { it >= 0.5f } ?: d.cameraManual,
                cameraYDp = map["cy"] ?: d.cameraYDp,
                cameraLengthDp = map["cl"] ?: d.cameraLengthDp
            ).coerced()
        }
    }
}

private fun Float.fin(fallback: Float): Float = if (isNaN() || isInfinite()) fallback else this
