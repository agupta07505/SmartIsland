/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import com.agupta07505.smartisland.model.TransferState

/** Everything the detector looks at, as plain values so it can be unit tested without Android. */
data class TransferInput(
    val packageName: String = "",
    val channelId: String = "",
    val category: String? = null,
    val title: String = "",
    val text: String = "",
    val bigText: String = "",
    val subText: String = "",
    val infoText: String = "",
    val progress: Int = 0,
    val progressMax: Int = 0,
    val indeterminate: Boolean = false,
    val isOngoing: Boolean = false,
    /** Android 16 `Notification.ProgressStyle` template (Live Updates). */
    val isProgressStyle: Boolean = false,
    /** The app asked for promoted-ongoing treatment (status bar chip). */
    val requestedPromoted: Boolean = false,
    val isMedia: Boolean = false,
    val isConversational: Boolean = false,
    val isGroupSummary: Boolean = false,
    val isMessageSync: Boolean = false,
    val actionLabels: List<String> = emptyList()
) {
    companion object {
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        private val CONVERSATIONAL_CATEGORIES = setOf(
            Notification.CATEGORY_MESSAGE, Notification.CATEGORY_EMAIL, Notification.CATEGORY_SOCIAL,
            Notification.CATEGORY_PROMO, Notification.CATEGORY_RECOMMENDATION,
            Notification.CATEGORY_REMINDER, Notification.CATEGORY_ALARM, Notification.CATEGORY_CALL,
            Notification.CATEGORY_EVENT
        )

        private inline fun <T> safe(default: T, block: () -> T): T = runCatching(block).getOrDefault(default)

        fun fromNotification(n: Notification, packageName: String, isMessageSync: Boolean): TransferInput {
            val e = n.extras
            fun cs(key: String): String = safe("") { e?.getCharSequence(key)?.toString().orEmpty() }
            val template = safe("") { e?.getString(Notification.EXTRA_TEMPLATE).orEmpty() }
            return TransferInput(
                packageName = packageName,
                channelId = safe("") { n.channelId.orEmpty() },
                category = n.category,
                title = cs(Notification.EXTRA_TITLE),
                text = cs(Notification.EXTRA_TEXT),
                bigText = cs(Notification.EXTRA_BIG_TEXT),
                subText = cs(Notification.EXTRA_SUB_TEXT),
                infoText = cs(Notification.EXTRA_INFO_TEXT),
                progress = safe(0) { e?.getInt(Notification.EXTRA_PROGRESS, 0) ?: 0 },
                progressMax = safe(0) { e?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0 },
                indeterminate = safe(false) { e?.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) == true },
                isOngoing = (n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0,
                isProgressStyle = template.endsWith("ProgressStyle"),
                requestedPromoted = safe(false) { e?.getBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, false) == true },
                isMedia = n.category == Notification.CATEGORY_TRANSPORT ||
                    safe(false) { e?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true } ||
                    safe(false) { MediaTransportDetector.hasTransportButtons(n.actions.orEmpty().map { it.title?.toString().orEmpty() }) },
                isConversational = n.category in CONVERSATIONAL_CATEGORIES ||
                    template.endsWith("MessagingStyle"),
                isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
                isMessageSync = isMessageSync,
                actionLabels = safe(emptyList()) { n.actions.orEmpty().map { it.title?.toString().orEmpty() } }
            )
        }
    }
}

data class TransferVerdict(
    val isTransfer: Boolean,
    val score: Int,
    /** 0..100 from the progress bar, or from "45%", "12 of 40 MB", "3 of 9 files" in the text. */
    val percent: Int?,
    val speed: String?,
    val eta: String?,
    val state: TransferState,
    val reasons: List<String>
) {
    /** "4.2 MB/s · 3 min left", or empty. */
    val detail: String get() = listOfNotNull(speed, eta).joinToString(" · ")
}

/** What the wording of a notification says about a transfer. All matching is case-insensitive. */
data class TransferTextFacts(
    val lower: String,
    val percent: Int?,
    val sizeRatioPercent: Int?,
    val countRatioPercent: Int?,
    val speed: String?,
    val eta: String?,
    val hasSizeOf: Boolean,
    val hasCountOf: Boolean
)

object TransferTextAnalyzer {
    private const val UNIT = "(?:[kmgt]i?b|bytes?)"
    private val PERCENT = Regex("""(?<![\d.,])(\d{1,3})(?:[.,]\d+)?\s?%""")
    private val SIZE_OF = Regex(
        """(\d+(?:[.,]\d+)?)\s?($UNIT)?\s?(?:out of|of|/|von|de|sur|su|из)\s?(\d+(?:[.,]\d+)?)\s?($UNIT)""",
        RegexOption.IGNORE_CASE
    )
    private val COUNT_OF = Regex(
        """(\d+)\s?(?:out of|of|/)\s?(\d+)\s?(?:files?|items?|photos?|videos?|downloads?|uploads?|songs?|tracks?|apps?|documents?|images?)""",
        RegexOption.IGNORE_CASE
    )
    private val SPEED = Regex(
        """\d+(?:[.,]\d+)?\s?(?:[kmgt]i?b(?:/sec|/s|ps)|[kmg]bit/s|[kmg]bps)""",
        RegexOption.IGNORE_CASE
    )
    private val ETA_LEFT = Regex(
        """\d+\s?(?:h|hrs?|hours?|m|mins?|minutes?|s|secs?|seconds?)(?:\s\d+\s?(?:m|mins?|s|secs?))?\s+(?:left|remaining)""",
        RegexOption.IGNORE_CASE
    )
    private val ETA_PREFIX = Regex(
        """(?:eta|time left|remaining)\s*[:\-]?\s*\d+(?:[:.]\d+)*\s?(?:h|hrs?|m|mins?|s|secs?)?""",
        RegexOption.IGNORE_CASE
    )

    fun analyze(raw: String): TransferTextFacts {
        val percent = PERCENT.find(raw)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 }
        val size = SIZE_OF.find(raw)
        val sizeRatio = size?.let { m ->
            val unit2 = m.groupValues[4]
            val unit1 = m.groupValues[2].ifBlank { unit2 }
            val done = toBytes(m.groupValues[1], unit1)
            val total = toBytes(m.groupValues[3], unit2)
            if (done != null && total != null && total > 0.0 && done <= total * 1.001) {
                ((done / total) * 100.0).toInt().coerceIn(0, 100)
            } else {
                null
            }
        }
        val count = COUNT_OF.find(raw)
        val countRatio = count?.let { m ->
            val done = m.groupValues[1].toIntOrNull()
            val total = m.groupValues[2].toIntOrNull()
            if (done != null && total != null && total > 0 && done <= total) (done * 100 / total) else null
        }
        return TransferTextFacts(
            lower = raw.lowercase(),
            percent = percent,
            sizeRatioPercent = sizeRatio,
            countRatioPercent = countRatio,
            speed = SPEED.find(raw)?.value?.trim(),
            eta = (ETA_LEFT.find(raw) ?: ETA_PREFIX.find(raw))?.value?.trim(),
            hasSizeOf = size != null,
            hasCountOf = count != null
        )
    }

    private fun toBytes(number: String, unit: String): Double? {
        val value = number.replace(',', '.').toDoubleOrNull() ?: return null
        val u = unit.lowercase()
        val factor = when {
            u.startsWith("k") -> 1024.0
            u.startsWith("m") -> 1024.0 * 1024.0
            u.startsWith("g") -> 1024.0 * 1024.0 * 1024.0
            u.startsWith("t") -> 1024.0 * 1024.0 * 1024.0 * 1024.0
            else -> 1.0
        }
        return value * factor
    }
}

object TransferDetector {
    const val THRESHOLD = 3

    private val CHANNEL_HINTS = listOf(
        "download", "upload", "transfer", "progress", "backup", "back_up", "sync", "torrent",
        "file_operation", "fileoperation", "file_ops", "copy", "install"
    )
    private val PAUSE_LABELS = listOf(
        "pause", "pausar", "pausieren", "mettre en pause", "приостановить", "暂停", "一時停止", "일시중지"
    )
    private val RESUME_LABELS = listOf(
        "resume", "reanudar", "retomar", "reprendre", "fortsetzen", "возобновить", "继续", "再開", "다시 시작", "retry", "try again"
    )
    private val CANCEL_LABELS = listOf(
        "cancel", "cancelar", "annuler", "abbrechen", "отмена", "取消", "キャンセル", "취소", "stop"
    )
    private val FAILED_WORDS = listOf(
        "failed", "couldn't", "could not", "unsuccessful", "interrupted", "error",
        "fehlgeschlagen", "falló", "fallo", "falhou", "échec", "échoué", "non riuscito", "başarısız",
        "ошибка", "не удалось", "失败", "失敗", "失败了", "失敗しました", "실패", "فشل"
    )
    private val PAUSED_WORDS = listOf(
        "paused", "pausado", "pausada", "en pausa", "pausiert", "en pause", "in pausa", "duraklatıldı",
        "dijeda", "приостановлен", "已暂停", "暂停", "一時停止", "일시중지", "متوقف مؤقتا"
    )
    private val WAITING_WORDS = listOf(
        "waiting for", "waiting to", "queued", "pending", "starting", "connecting", "no connection",
        "retrying", "will resume", "en attente", "esperando", "en espera", "warten", "in attesa",
        "ожидание", "в очереди", "等待", "待機", "대기"
    )
    private val ZERO_FAILED = Regex("""\b0\s+(?:failed|errors?)\b""")

    fun evaluate(input: TransferInput): TransferVerdict {
        val allText = listOf(input.title, input.text, input.bigText, input.subText, input.infoText)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val facts = TransferTextAnalyzer.analyze(allText)
        val lower = facts.lower
        val labels = input.actionLabels.map { it.lowercase() }
        val reasons = mutableListOf<String>()

        val hasBar = input.progressMax > 0
        val hasIndeterminate = input.indeterminate && !hasBar
        val hasBarSignal = hasBar || hasIndeterminate
        val categoryProgress = input.category == "progress"
        val hasProgressSignal = hasBarSignal || categoryProgress || input.isProgressStyle || facts.percent != null

        val percent = when {
            hasBar -> (input.progress.toLong() * 100L / input.progressMax.toLong()).toInt().coerceIn(0, 100)
            else -> facts.percent ?: facts.sizeRatioPercent ?: facts.countRatioPercent
        }
        val state = detectState(lower, labels)

        fun no(why: String) = TransferVerdict(false, 0, percent, facts.speed, facts.eta, state, listOf(why))

        if (input.isGroupSummary) return no("group summary")
        if (input.isMessageSync) return no("message sync")
        if (input.isMedia && !hasBarSignal) return no("media without progress bar")
        if (input.isConversational && !hasBarSignal) return no("conversation without progress bar")

        var semantic = 0
        var structural = 0
        fun sem(points: Int, why: String) { semantic += points; reasons += why }
        fun str(points: Int, why: String) { structural += points; reasons += why }

        val activeWord = TransferClassifier.ACTIVE_WORDS.any { lower.contains(it) } ||
            TransferClassifier.ACTIVE_WORDS_I18N.any { lower.contains(it) }
        val genericWord = TransferClassifier.GENERIC_WORDS.any { lower.contains(it) }
        val channel = input.channelId.lowercase()
        val channelHint = channel.isNotEmpty() && CHANNEL_HINTS.any { channel.contains(it) }
        val knownApp = TransferClassifier.isKnownTransferApp(input.packageName)
        val cloudApp = TransferClassifier.isCloudApp(input.packageName)
        val hasPause = labels.any { l -> PAUSE_LABELS.any { l.contains(it) } }
        val hasResume = labels.any { l -> RESUME_LABELS.any { l.contains(it) } }
        val hasCancel = labels.any { l -> CANCEL_LABELS.any { l.contains(it) } }

        if (categoryProgress) sem(3, "category progress")
        if (hasBar) str(2, "progress bar")
        if (hasIndeterminate) str(2, "indeterminate bar")
        if (input.isProgressStyle) str(1, "progress style")
        if (input.requestedPromoted) str(1, "promoted ongoing")
        if (facts.percent != null) str(1, "percent in text")
        if (facts.eta != null) str(1, "eta in text")
        if (activeWord) sem(2, "transfer word") else if (genericWord) sem(1, "loose transfer word")
        if (facts.speed != null) sem(3, "speed in text")
        if (facts.hasSizeOf) sem(2, "size of total")
        if (facts.hasCountOf) sem(1, "count of total")
        if (channelHint && (hasProgressSignal || input.isOngoing)) sem(2, "channel name")
        if (knownApp && hasBarSignal) sem(2, "download manager, browser, file manager or store")
        if (cloudApp && (hasBarSignal || facts.percent != null)) sem(2, "cloud storage app")
        if ((hasPause || hasResume) && hasCancel && (hasProgressSignal || input.isOngoing)) sem(1, "pause and cancel buttons")
        if (input.isOngoing && (hasProgressSignal || activeWord || facts.speed != null)) str(1, "ongoing")

        // Wording only (no bar, no category, no progress style): demand real transfer evidence.
        val textOnly = !hasBarSignal && !categoryProgress && !input.isProgressStyle
        if (textOnly) {
            val speedWithContext = facts.speed != null &&
                (facts.percent != null || facts.eta != null || genericWord || activeWord)
            if (!(activeWord || facts.hasSizeOf || speedWithContext)) {
                return TransferVerdict(false, semantic + structural, percent, facts.speed, facts.eta, state, reasons + "wording too weak")
            }
        }

        val total = semantic + structural
        return TransferVerdict(
            isTransfer = semantic >= 1 && total >= THRESHOLD,
            score = total,
            percent = percent,
            speed = facts.speed,
            eta = facts.eta,
            state = state,
            reasons = reasons
        )
    }

    /** FAILED beats PAUSED beats WAITING; everything else is ACTIVE (completion is decided elsewhere). */
    internal fun detectState(lower: String, labels: List<String>): TransferState {
        val cleaned = ZERO_FAILED.replace(lower, "")
        if (FAILED_WORDS.any { cleaned.contains(it) }) return TransferState.FAILED
        val hasPause = labels.any { l -> PAUSE_LABELS.any { l.contains(it) } }
        val hasResume = labels.any { l -> RESUME_LABELS.any { l.contains(it) } }
        if (PAUSED_WORDS.any { lower.contains(it) } || (hasResume && !hasPause)) return TransferState.PAUSED
        if (WAITING_WORDS.any { lower.contains(it) }) return TransferState.WAITING
        return TransferState.ACTIVE
    }
}
