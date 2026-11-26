/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Fold type: a book-style foldable (Z Fold, Pixel Fold). */
enum class FoldKind { None, Book, Flip, Other }

enum class FoldPosture(override val id: String, val unfolded: Boolean, val landscape: Boolean) : StableId {
    Standard("standard", false, false),
    FoldedPortrait("folded_portrait", false, false),
    FoldedLandscape("folded_landscape", false, true),
    UnfoldedPortrait("unfolded_portrait", true, false),
    UnfoldedLandscape("unfolded_landscape", true, true);

    val portraitTwin: FoldPosture
        get() = when (this) {
            FoldedLandscape -> FoldedPortrait
            UnfoldedLandscape -> UnfoldedPortrait
            else -> this
        }

    companion object {
        val Modes: List<FoldPosture> = listOf(FoldedPortrait, FoldedLandscape, UnfoldedPortrait, UnfoldedLandscape)

        fun fromId(id: String?): FoldPosture? = enumByIdOrNull(id)
    }
}

data class FoldDeviceInfo(val kind: FoldKind, val source: String) {
    val foldable: Boolean get() = kind != FoldKind.None

    companion object {
        val Plain = FoldDeviceInfo(FoldKind.None, "none")
    }
}

object FoldModels {
    fun kindOf(manufacturer: String?, model: String?, device: String?, hasHingeSensor: Boolean): FoldDeviceInfo {
        val maker = manufacturer.orEmpty().trim().uppercase()
        val m = model.orEmpty().trim().uppercase()
        val d = device.orEmpty().trim().uppercase()

        // Samsung: SM-F9xx / SM-W20xx are the Z Fold line, SM-F7xx the Z Flip line
        if (maker == "SAMSUNG" || m.startsWith("SM-")) {
            when {
                m.startsWith("SM-F9") || m.startsWith("SM-W2") ->
                    return FoldDeviceInfo(FoldKind.Book, "model:samsung-fold")
                m.startsWith("SM-F7") -> return FoldDeviceInfo(FoldKind.Flip, "model:samsung-flip")
            }
        }
        val name = "$m $d"
        when {
            name.contains("FLIP") || name.contains("RAZR") -> return FoldDeviceInfo(FoldKind.Flip, "model:name-flip")
            name.contains("FOLD") || name.contains("MAGIC V") || name.contains("MATE X") ||
                name.contains("FIND N") || name.contains("PHANTOM V") ->
                    return FoldDeviceInfo(FoldKind.Book, "model:name-fold")
        }
        if (hasHingeSensor) return FoldDeviceInfo(FoldKind.Other, "sensor:hinge-angle")
        return FoldDeviceInfo.Plain
    }
}

object FoldPostureRules {
    const val BOOK_UNFOLDED_MIN_SHORT_DP = 540f

    const val FLIP_COVER_MAX_RATIO = 1.4f

    const val COVER_MAX_SHORT_DP = 300f

    fun postureOf(kind: FoldKind, widthDp: Float, heightDp: Float): FoldPosture {
        if (kind == FoldKind.None || widthDp <= 0f || heightDp <= 0f) return FoldPosture.Standard
        val landscape = widthDp > heightDp
        val short = minOf(widthDp, heightDp)
        val long = maxOf(widthDp, heightDp)
        val unfolded = when (kind) {
            FoldKind.Flip -> short >= COVER_MAX_SHORT_DP && long / short > FLIP_COVER_MAX_RATIO
            FoldKind.Book, FoldKind.Other -> short >= BOOK_UNFOLDED_MIN_SHORT_DP
            FoldKind.None -> false
        }
        return when {
            unfolded && landscape -> FoldPosture.UnfoldedLandscape
            unfolded -> FoldPosture.UnfoldedPortrait
            landscape -> FoldPosture.FoldedLandscape
            else -> FoldPosture.FoldedPortrait
        }
    }
}
