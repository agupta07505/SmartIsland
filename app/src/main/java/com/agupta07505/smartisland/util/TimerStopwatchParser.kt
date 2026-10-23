/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object TimerStopwatchParser {

    private val CLOCK_PACKAGES = setOf(
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.android.deskclock",
        "com.miui.deskclock",
        "com.oneplus.deskclock",
        "com.coloros.alarmclock",
        "com.oplus.alarmclock",
        "com.android.bbkclock",
        "com.vivo.clock",
        "com.huawei.deskclock",
        "com.hihonor.deskclock",
        "com.motorola.blur.alarmclock",
        "com.motorola.timeweatherwidget",
        "com.sec.android.deskclock",
        "com.asus.deskclock",
        "com.lenovo.deskclock",
        "com.sonyericsson.organizer",
        "com.htc.android.worldclock",
        "com.timer.stopwatch",
        "com.hybrid.stopwatch",
        "com.sportstracklive.stopwatch",
        "com.jee.timer",
        "com.clover.timer"
    )

    private val TIMER_KEYWORDS = listOf(
        "timer", "countdown", "time's up", "times up", "time is up",
        "timer paused", "timer running", "timer expired", "timer finished",
        "remaining", "sec remaining", "min remaining"
    )

    private val STOPWATCH_WORDS = Regex("""(?<![\p{L}])(stopwatch|laps?|split|elapsed)(?![\p{L}])""")
    private val LAP_ACTION = Regex("""(?<![\p{L}])(lap|split|vuelta|tour|runde)(?![\p{L}])""")

    private val TIME_PATTERN = Pattern.compile("\\b(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,2}))?\\b")

    private val OEM_CLOCK_PACKAGES = setOf(
        "com.sec.android.app.clockpackage", "com.miui.deskclock", "com.oneplus.deskclock",
        "com.coloros.alarmclock", "com.oplus.alarmclock", "com.android.bbkclock", "com.vivo.clock",
        "com.huawei.deskclock", "com.hihonor.deskclock", "com.motorola.blur.alarmclock",
        "com.motorola.timeweatherwidget", "com.sec.android.deskclock", "com.asus.deskclock",
        "com.lenovo.deskclock", "com.sonyericsson.organizer", "com.htc.android.worldclock"
    )

    private val OEM_PACKAGE_PREFIXES = listOf(
        "com.miui.", "com.xiaomi.", "com.mi.", "com.sec.", "com.samsung.", "com.oneplus.", "com.oplus.",
        "com.oppo.", "com.coloros.", "com.heytap.", "com.realme.", "com.vivo.", "com.bbk.", "com.huawei.",
        "com.hihonor.", "com.honor.", "com.asus.", "com.lenovo.", "com.motorola.", "com.sonyericsson.",
        "com.sonymobile.", "com.htc.", "com.lge.", "com.zte.", "com.nubia.", "com.meizu.", "com.nothing.",
        "com.transsion.", "com.tecno.", "com.infinix.", "com.itel."
    )

    fun isOemClock(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg in OEM_CLOCK_PACKAGES || OEM_PACKAGE_PREFIXES.any { pkg.startsWith(it) }
    }

    fun usesShadeFixes(packageName: String): Boolean = !isOemClock(packageName)

    enum class ClockKind { Timer, Stopwatch }

    private data class SeenKind(val kind: ClockKind, val atMs: Long)
    private const val PACKAGE_MEMORY_MS = 15 * 60 * 1000L
    private val seenByKey = ConcurrentHashMap<String, ClockKind>()
    private val seenByPackage = ConcurrentHashMap<String, SeenKind>()

    fun clockKey(sbn: StatusBarNotification): String =
        "${sbn.packageName.lowercase()}#${runCatching { sbn.id }.getOrDefault(0)}"

    private fun noteKind(packageName: String, clockKey: String?, kind: ClockKind) {
        if (packageName.isNotEmpty()) seenByPackage[packageName] = SeenKind(kind, System.currentTimeMillis())
        if (clockKey != null) seenByKey[clockKey] = kind
    }

    private fun recallKind(packageName: String, clockKey: String?): ClockKind? {
        clockKey?.let { seenByKey[it] }?.let { return it }
        val seen = seenByPackage[packageName] ?: return null
        return seen.kind.takeIf { System.currentTimeMillis() - seen.atMs <= PACKAGE_MEMORY_MS }
    }

    fun forgetClockKinds() {
        seenByKey.clear()
        seenByPackage.clear()
    }

    private fun channelKind(notification: Notification): ClockKind? {
        val id = runCatching { notification.channelId }.getOrNull()?.lowercase() ?: return null
        val stopwatch = id.contains("stopwatch")
        val timer = id.contains("timer")
        return when {
            stopwatch && !timer -> ClockKind.Stopwatch
            timer && !stopwatch -> ClockKind.Timer
            else -> null
        }
    }

    fun extractFullText(notification: Notification): String {
        val extras = notification.extras ?: return ""
        val title = runCatching { extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() }.getOrNull().orEmpty()
        val text = runCatching { extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() }.getOrNull().orEmpty()
        val subText = runCatching { extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() }.getOrNull().orEmpty()
        val bigText = runCatching { extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() }.getOrNull().orEmpty()
        val infoText = runCatching { extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() }.getOrNull().orEmpty()
        val summaryText = runCatching { extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() }.getOrNull().orEmpty()
        val ticker = runCatching { notification.tickerText?.toString() }.getOrNull().orEmpty()
        val textLines = runCatching {
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" ") { it.toString() }
        }.getOrNull().orEmpty()
        return "$title $text $subText $bigText $infoText $summaryText $ticker $textLines".lowercase()
    }

    fun extractActionLabels(notification: Notification): List<String> {
        val actions = runCatching { notification.actions }.getOrNull() ?: return emptyList()
        return actions.mapNotNull {
            runCatching { it?.title?.toString()?.lowercase() }.getOrNull()
        }
    }

    private val ALARM_KEYWORDS = listOf(
        "alarm", "upcoming alarm", "snooze", "snoozed", "wake up", "wake-up",
        "alarme", "despertador", "wecker", "sveglia", "будильник", "闹钟", "アラーム"
    )

    private val SNOOZE_KEYWORDS = listOf(
        "snooze", "snoozed", "rappeler", "posponer", "schlummern", "スヌーズ", "稍后提醒", "सोएं", "स्नूज़"
    )

    private val RESUME_KEYWORDS = listOf(
        "resume", "start", "play", "continue", "unpause",
        "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить"
    )

    private val PAUSE_KEYWORDS = listOf(
        "pause", "pausa", "pausar", "sospendi", "interrompi", "onderbreek", "stoppa", "रोकें", "暂停", "一時停止", "пауза"
    )

    private val PAUSED_KEYWORDS = listOf(
        "paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено"
    )

    private val ZERO_TIME_PATTERN = Pattern.compile("\\b0+[:.]00(?::00)?\\b")

    fun isAlarm(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val packageName = sbn.packageName.lowercase()
        return isAlarm(notification, packageName)
    }

    fun isAlarm(notification: Notification, packageName: String = ""): Boolean {
        val fullText = extractFullText(notification)
        val actionLabels = extractActionLabels(notification)

        // Any snooze action or snooze keyword is uniquely an alarm feature
        if (actionLabels.any { act -> SNOOZE_KEYWORDS.any { act.contains(it) } } ||
            SNOOZE_KEYWORDS.any { fullText.contains(it) }
        ) {
            return true
        }

        if (fullText.contains("upcoming alarm") || fullText.contains("alarm firing") || fullText.contains("alarm ringing")) {
            return true
        }

        val hasTimerKeyword = TIMER_KEYWORDS.any { fullText.contains(it) } ||
            actionLabels.any { it.contains("+1") || it.contains("timer") || it.contains("add 1") }
        val hasTimerAction = actionLabels.any {
            it.contains("pause") || it.contains("resume") || it.contains("reset") || it.contains("lap")
        }

        val category = runCatching { notification.category }.getOrNull().orEmpty().lowercase()
        val isCategoryAlarm = category == "alarm" || category == "category_alarm"
        val hasAlarmKeyword = ALARM_KEYWORDS.any { fullText.contains(it) }

        if ((isCategoryAlarm || hasAlarmKeyword) && !hasTimerKeyword && !hasTimerAction) {
            return true
        }

        return false
    }

    fun isTimer(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val packageName = sbn.packageName.lowercase()
        return isTimer(notification, packageName, clockKey(sbn))
    }

    fun isTimer(notification: Notification, packageName: String = "", clockKey: String? = null): Boolean {
        if (isAlarm(notification, packageName)) {
            return false
        }

        // A paused clock stopwatch counts as a stopwatch, see isStopwatch
        if (isStopwatch(notification, packageName, clockKey)) {
            return false
        }

        val fullText = extractFullText(notification)
        val actionLabels = extractActionLabels(notification)

        if (OemDeviceRules.isNonNavigationContent(packageName, fullText) &&
            !CLOCK_PACKAGES.contains(packageName) &&
            !actionLabels.any { it.contains("+1") || it.contains("timer") || it.contains("add 1") }
        ) {
            if (OemDeviceRules.isScreenRecording(packageName, fullText, OemDeviceType.GENERIC) ||
                OemDeviceRules.isHotspot(packageName, fullText)
            ) {
                return false
            }
        }

        val extras = notification.extras
        val isChronometerCountDown = extras?.let {
            runCatching {
                it.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) ||
                it.getBoolean("android.chronometerCountDown", false)
            }.getOrDefault(false)
        } ?: false

        val isClockApp = CLOCK_PACKAGES.contains(packageName) || packageName.contains("clock") || packageName.contains("timer")

        val isStock = usesShadeFixes(packageName)
        if (isChronometerCountDown) {
            if (isStock) noteKind(packageName, clockKey, ClockKind.Timer)
            return true
        }
        // The stock clock's own channel names the feature.
        if (isStock && channelKind(notification) == ClockKind.Timer) {
            noteKind(packageName, clockKey, ClockKind.Timer)
            return true
        }
        val hasTimerAction = actionLabels.any {
            it.contains("+1") || it.contains("timer") || it.contains("add 1") || it.contains("reset timer")
        }
        val hasTimerKeyword = TIMER_KEYWORDS.any { fullText.contains(it) }
        val hasTimePattern = TIME_PATTERN.matcher(fullText).find()

        if (hasTimerAction) {
            if (isStock) noteKind(packageName, clockKey, ClockKind.Timer)
            return true
        }
        // Outside clock apps, wording and a future timestamp only count for an ongoing notification that
        // is not a message: a chat saying "timer 5:30" or a scheduled reminder is not a timer.
        val flags = runCatching { notification.flags }.getOrDefault(0)
        val isOngoing = (flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isMessage = runCatching { notification.category }.getOrNull().orEmpty().lowercase().let {
            it == Notification.CATEGORY_MESSAGE || it == Notification.CATEGORY_EMAIL || it == Notification.CATEGORY_SOCIAL
        }
        val notifWhen = runCatching { notification.`when` }.getOrDefault(0L)
        if (notifWhen > System.currentTimeMillis() && (isClockApp || isOngoing) && !isMessage) {
            if (isStock) noteKind(packageName, clockKey, ClockKind.Timer)
            return true
        }
        if (isClockApp && isTimerPaused(notification) && !actionLabels.any { it.contains("lap") }) return true
        if (isClockApp && (hasTimerKeyword || (hasTimePattern && !actionLabels.any { it.contains("lap") }))) return true
        if (hasTimerKeyword && hasTimePattern && isOngoing && !isMessage) return true

        return false
    }

    fun isStopwatch(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val packageName = sbn.packageName.lowercase()
        return isStopwatch(notification, packageName, clockKey(sbn))
    }

    fun isStopwatch(notification: Notification, packageName: String = "", clockKey: String? = null): Boolean {
        if (isAlarm(notification, packageName)) {
            return false
        }

        val fullText = extractFullText(notification)
        val actionLabels = extractActionLabels(notification)

        val category = runCatching { notification.category }.getOrNull().orEmpty().lowercase()
        if (category == "stopwatch" || category == "category_stopwatch") {
            return true
        }

        val isClockApp = CLOCK_PACKAGES.contains(packageName) || packageName.contains("clock") || packageName.contains("stopwatch")
        val hasLapAction = actionLabels.any { LAP_ACTION.containsMatchIn(it) }
        val hasStopwatchKeyword = STOPWATCH_WORDS.containsMatchIn(fullText)

        val extras = notification.extras
        val isChronometer = extras?.let {
            runCatching {
                it.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) ||
                it.getBoolean("android.showChronometer", false) ||
                it.getLong("android.chronometerBase", 0L) > 0L
            }.getOrDefault(false)
        } ?: false

        val isChronometerCountDown = extras?.let {
            runCatching {
                it.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) ||
                it.getBoolean("android.chronometerCountDown", false)
            }.getOrDefault(false)
        } ?: false

        val hasTimerKeyword = TIMER_KEYWORDS.any { fullText.contains(it) } ||
            actionLabels.any { it.contains("+1") || it.contains("timer") || it.contains("add 1") }

        // Wording alone is not enough: any app's text can contain "stopwatch", "lap" (overlap, collapse) or "split"
        // (a chat reply, a progress message).
        val isLiveCountUp = isChronometer && !isChronometerCountDown
        val trusted = isClockApp || isLiveCountUp

        val isStock = usesShadeFixes(packageName)

        // The stock clock's own channel names the feature.
        if (isStock) {
            when (channelKind(notification)) {
                ClockKind.Stopwatch -> {
                    noteKind(packageName, clockKey, ClockKind.Stopwatch)
                    return true
                }
                ClockKind.Timer -> return false
                null -> Unit
            }
        }

        // A paused clock stopwatch has no Lap button and no running chronometer, so nothing in it points to a timer;
        // what the same notification was while running decides.
        if (isStock && !isChronometerCountDown && !hasTimerKeyword && isStopwatchPaused(notification)) {
            if (recallKind(packageName, clockKey) == ClockKind.Stopwatch) return true
        }

        if (!trusted) return false

        if (hasLapAction || hasStopwatchKeyword ||
            (isClockApp && isChronometer && !isChronometerCountDown && !hasTimerKeyword) ||
            (isClockApp && fullText.contains("stopwatch"))
        ) {
            if (isStock) noteKind(packageName, clockKey, ClockKind.Stopwatch)
            return true
        }

        return false
    }

    fun isTimerFinished(notification: Notification): Boolean {
        // Paused timer is never finished
        if (isTimerPaused(notification)) return false

        val fullText = extractFullText(notification)

        val finishKeywords = listOf(
            "time's up", "times up", "time is up", "timer finished",
            "timer expired", "timer ended", "timer done"
        )
        if (finishKeywords.any { fullText.contains(it) }) return true

        val isZeroTime = ZERO_TIME_PATTERN.matcher(fullText).find()
        val flags = runCatching { notification.flags }.getOrDefault(0)
        val isOngoing = (flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isZeroTime && !isOngoing) return true

        return false
    }

    fun isTimerPaused(notification: Notification): Boolean {
        val actionLabels = extractActionLabels(notification)
        val fullText = extractFullText(notification)

        val hasResumeAction = actionLabels.any { act -> RESUME_KEYWORDS.any { act.contains(it) } }
        val hasPauseAction = actionLabels.any { act -> PAUSE_KEYWORDS.any { act.contains(it) } }
        val hasPausedKeyword = PAUSED_KEYWORDS.any { fullText.contains(it) }

        if (hasResumeAction) return true
        if (hasPausedKeyword && !hasPauseAction) return true
        return false
    }

    fun isStopwatchPaused(notification: Notification): Boolean {
        val actionLabels = extractActionLabels(notification)
        val fullText = extractFullText(notification)

        val hasResumeAction = actionLabels.any {
            it.contains("resume") || it.contains("start") || it.contains("play") || it.contains("continue") || it.contains("unpause")
        }
        val hasPauseAction = actionLabels.any { it.contains("pause") }
        val hasPausedKeyword = fullText.contains("paused") || fullText.contains("pause")

        if (hasResumeAction) return true
        if (hasPausedKeyword && !hasPauseAction) return true
        return false
    }

    /** Most reliable future deadline of a timer notification; its own deadline comes before the HyperOS fallbacks. */
    fun parseTimerDeadlineMillis(notification: Notification): Long? {
        if (isTimerPaused(notification)) return null

        val now = System.currentTimeMillis()

        val notificationDeadline = runCatching {
            notification.`when`
        }.getOrDefault(0L)

        if (notificationDeadline > now) {
            return notificationDeadline
        }

        val rawParam = runCatching {
            notification.extras?.getString("miui.focus.param")
        }.getOrNull() ?: return null

        val json = runCatching {
            org.json.JSONObject(rawParam)
        }.getOrNull() ?: return null

        val timerSystemCurrent = json.optLong("timerSystemCurrent", 0L)
        val timerTotal = json.optLong("timerTotal", 0L)

        if (timerSystemCurrent > 0L && timerTotal > 0L) {
            val reconstructedDeadline = timerSystemCurrent + timerTotal

            if (reconstructedDeadline > now) {
                return reconstructedDeadline
            }
        }

        // Last resort: the HyperOS timerWhen field.
        val timerWhen = json.optLong("timerWhen", 0L)

        return timerWhen.takeIf { it > now }
    }

    /** Running timer's deadline in ms (`when` while still ahead), keeping the sub-second part. Null when paused. */
    fun parseTimerDeadlineExact(notification: Notification, nowMs: Long = System.currentTimeMillis()): Long? {
        if (isTimerPaused(notification)) return null
        val notifWhen = runCatching { notification.`when` }.getOrDefault(0L)
        return notifWhen.takeIf { it > nowMs }
    }

    fun parseTimerRemainingSeconds(notification: Notification): Long? {
        val isPaused = isTimerPaused(notification)
        val extras = notification.extras

        // A chronometer countdown base is the most accurate source.
        if (extras != null && !isPaused) {
            val isCountDown = runCatching {
                extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) ||
                extras.getBoolean("android.chronometerCountDown", false)
            }.getOrDefault(false)
            val chronometerBase = runCatching {
                extras.getLong("android.chronometerBase", 0L)
            }.getOrDefault(0L)

            if (isCountDown && chronometerBase > 0L) {
                val diffMs = chronometerBase - SystemClock.elapsedRealtime()
                if (diffMs > 0) return (diffMs + 500L) / 1000L
            }
        }

        if (!isPaused) {
            val notifWhen = runCatching { notification.`when` }.getOrDefault(0L)
            if (notifWhen > System.currentTimeMillis()) {
                val diffSec = (notifWhen - System.currentTimeMillis() + 500L) / 1000L
                if (diffSec > 0) return diffSec
            }
        }

        val title = runCatching { extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() }.getOrNull().orEmpty()
        val text = runCatching { extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() }.getOrNull().orEmpty()
        val subText = runCatching { extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() }.getOrNull().orEmpty()
        val bigText = runCatching { extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() }.getOrNull().orEmpty()

        return parseRemainingFromFields(listOf(title, text, subText, bigText))
    }

    private fun parseRemainingFromFields(fields: List<String>): Long? {
        for (field in fields) {
            if (field.isBlank()) continue
            parseClockTimeToSeconds(field)?.let { return it }
        }
        for (field in fields) {
            if (field.isBlank()) continue
            val parsed = parseTimeStringToSeconds(field)
            if (parsed != null && parsed >= 0) return parsed
        }
        return null
    }

    /** Only the "mm:ss" / "h:mm:ss" form, no unit words. */
    fun parseClockTimeToSeconds(input: String): Long? {
        val matcher = TIME_PATTERN.matcher(input)
        if (!matcher.find()) return null
        val hours = matcher.group(1)?.toLongOrNull() ?: 0L
        val minutes = matcher.group(2)?.toLongOrNull() ?: 0L
        val seconds = matcher.group(3)?.toLongOrNull() ?: 0L
        return hours * 3600L + minutes * 60L + seconds
    }

    fun parseTimerRemainingSeconds(notification: com.agupta07505.smartisland.model.IslandNotification): Long? {
        return parseRemainingFromFields(listOf(notification.title, notification.text))
    }

    /** Accepts "04:55", "00:04:55" and "5m 30s". */
    fun parseTimeStringToSeconds(input: String): Long? {
        val matcher = TIME_PATTERN.matcher(input)
        if (matcher.find()) {
            val hours = matcher.group(1)?.toLongOrNull() ?: 0L
            val minutes = matcher.group(2)?.toLongOrNull() ?: 0L
            val seconds = matcher.group(3)?.toLongOrNull() ?: 0L
            val totalSeconds = hours * 3600L + minutes * 60L + seconds
            if (totalSeconds >= 0) return totalSeconds
        }

        val hrMatcher = Pattern.compile("(\\d+)\\s*(?:h|hr|hrs|hours?)\\b", Pattern.CASE_INSENSITIVE).matcher(input)
        val minMatcher = Pattern.compile("(\\d+)\\s*(?:m|min|mins|minutes?)\\b", Pattern.CASE_INSENSITIVE).matcher(input)
        val secMatcher = Pattern.compile("(\\d+)\\s*(?:s|sec|secs|seconds?)\\b", Pattern.CASE_INSENSITIVE).matcher(input)
        val hrs = if (hrMatcher.find()) hrMatcher.group(1)?.toLongOrNull() ?: 0L else 0L
        val mins = if (minMatcher.find()) minMatcher.group(1)?.toLongOrNull() ?: 0L else 0L
        val secs = if (secMatcher.find()) secMatcher.group(1)?.toLongOrNull() ?: 0L else 0L
        if (hrs > 0 || mins > 0 || secs > 0) {
            return hrs * 3600L + mins * 60L + secs
        }

        return null
    }

    fun parseStopwatchElapsedSeconds(notification: Notification): Long? {
        val isPaused = isStopwatchPaused(notification)
        val extras = notification.extras

        if (extras != null && !isPaused) {
            val isCountDown = runCatching {
                extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) ||
                extras.getBoolean("android.chronometerCountDown", false)
            }.getOrDefault(false)
            val chronometerBase = runCatching {
                extras.getLong("android.chronometerBase", 0L)
            }.getOrDefault(0L)

            if (!isCountDown && chronometerBase > 0L) {
                val diffMs = SystemClock.elapsedRealtime() - chronometerBase
                if (diffMs >= 0) return diffMs / 1000L
            }
        }

        val title = runCatching { extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() }.getOrNull().orEmpty()
        val text = runCatching { extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() }.getOrNull().orEmpty()
        val subText = runCatching { extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() }.getOrNull().orEmpty()
        val bigText = runCatching { extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() }.getOrNull().orEmpty()

        for (field in listOf(title, text, subText, bigText)) {
            if (field.isBlank()) continue
            val parsed = parseTimeStringToSeconds(field)
            if (parsed != null && parsed >= 0) return parsed
        }

        if (!isPaused) {
            val notifWhen = runCatching { notification.`when` }.getOrDefault(0L)
            if (notifWhen in 1..System.currentTimeMillis()) {
                val diffSec = (System.currentTimeMillis() - notifWhen) / 1000L
                if (diffSec >= 0) return diffSec
            }
        }

        return null
    }

    fun formatTime(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        val hours = safeSeconds / 3600L
        val minutes = (safeSeconds % 3600L) / 60L
        val seconds = safeSeconds % 60L

        return if (hours > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
