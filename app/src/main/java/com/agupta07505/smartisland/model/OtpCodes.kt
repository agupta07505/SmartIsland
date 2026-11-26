/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

sealed interface OtpChip {
    class AppAction(val action: IslandNotificationAction) : OtpChip

    /** Android's assistant (Enhanced notifications) added the copy action, fired as it is. */
    class SystemAction(val action: IslandNotificationAction) : OtpChip

    class Detected(val code: String) : OtpChip
}

object OtpCodes {
    private val KEYWORD = Regex(
        "otp|one[- ]?time|verification|verify|security code|authentication|auth code|login code|log-in code|" +
            "sign-?in code|passcode|confirmation code|2fa|two-factor|code is|your code|" +
            "c[oó]digo de|codice di|code de|bestätigungscode|验证码|驗證碼|認証コード|確認コード|인증",
        RegexOption.IGNORE_CASE
    )

    private val LABELED = Regex(
        "(?:otp|code|pin|passcode|password|c[oó]digo|codice|kode|验证码|驗證碼|コード|인증번호)" +
            "(?:\\s+(?:is|es|è|est|ist|are))?\\s*[:：\\-–]?\\s*" +
            "((?:G-)?\\d{3,4}[- ]\\d{2,4}|\\d{4,8})(?![\\w%]|[.,:/-]\\d)",
        RegexOption.IGNORE_CASE
    )

    private val CANDIDATE = Regex(
        "(?<![\\w.,:/+#₹$€£-])((?:G-)?(?:\\d{3}[- ]\\d{3}|\\d{4,8}))(?![\\w%]|[.,:/-]\\d)",
        RegexOption.IGNORE_CASE
    )

    private val AMOUNT_BEFORE = Regex("(?:(?<![a-z])(?:rs\\.?|inr|usd|eur|amt|amount)|[₹$€£])\\s*$", RegexOption.IGNORE_CASE)

    private val DIGITS = 4..8

    private val COPY_WORDS = listOf(
        "copy", "copiar", "copia", "copier", "kopieren", "kopiëren", "复制", "複製", "コピー", "복사", "कॉपी"
    )
    private val CODE_WORDS = listOf(
        "code", "otp", "pin", "password", "passcode", "código", "codigo", "codice", "kode",
        "验证码", "驗證碼", "コード", "코드", "कोड"
    )
    private val NOT_A_CODE = listOf("link", "url", "enlace", "lien", "リンク", "链接", "鏈接")

    /**
     * The code in a message, null when it is not a code message or when the message holds several numbers and none of
     * them is labelled as the code. The separator of a two-group code is dropped
     */
    fun find(title: String, text: String, bigText: String): String? {
        if (text.startsWith(SENT_REPLY_LABEL)) return null
        val body = text.ifBlank { bigText }
        val all = listOf(title, body).filter { it.isNotBlank() }.joinToString("\n")
        if (!KEYWORD.containsMatchIn(all)) return null
        LABELED.findAll(all).firstOrNull { !isAmount(all, it.groups[1]!!.range.first) }
            ?.let { return clean(it.groupValues[1]) }
        val unlabeled = CANDIDATE.findAll(all)
            .filter { !isAmount(all, it.groups[1]!!.range.first) }
            .map { clean(it.groupValues[1]) }
            .toList()
        return unlabeled.singleOrNull()
    }

    fun isCopyAction(action: IslandNotificationAction): Boolean {
        if (action.pendingIntent == null || action.hasRemoteInput || action.viaView) return false
        val title = action.title.trim().lowercase()
        if (NOT_A_CODE.any { title.contains(it) }) return false
        if (COPY_WORDS.none { title.contains(it) }) return false
        return CODE_WORDS.any { title.contains(it) } || COPY_WORDS.any { title == it }
    }

    fun appCopyAction(notification: IslandNotification): IslandNotificationAction? =
        notification.actionIntents.firstOrNull(::isCopyAction)

    /**
     * What the Copy code chip does for a card, first that applies: the app's own copy button, Android's own (only
     * with [useAssistant], the Enhanced notifications that bring it also bring the suggested replies), a copy of the
     * code the island found in the message. No chip when [enabled] is off
     */
    fun chipFor(notification: IslandNotification, enabled: Boolean = true, useAssistant: Boolean = true): OtpChip? {
        if (!enabled || notification.mode != IslandMode.Notification) return null
        appCopyAction(notification)?.let { return OtpChip.AppAction(it) }
        if (useAssistant) {
            notification.systemCopyAction?.takeIf(::isCopyAction)?.let { return OtpChip.SystemAction(it) }
        }
        return find(notification.title, notification.text, notification.bigText)?.let { OtpChip.Detected(it) }
    }

    private fun isAmount(text: String, start: Int): Boolean =
        AMOUNT_BEFORE.containsMatchIn(text.substring(0, start))

    private fun clean(code: String): String {
        val plain = code.removePrefix("G-").removePrefix("g-")
        val joined = plain.filter { it.isDigit() }
        return if (joined.length in DIGITS) joined else plain
    }
}
