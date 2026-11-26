/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class LinkKind { WEB, EMAIL, PHONE, SMS, APP }

object CardLinks {
    private const val PATH = """[^\s<>"'\[\]{}]"""
    private const val LABEL = "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
    private const val TLDS = "com|net|org|in|io|co|app|dev|ly|ai|tv|gl|edu|gov|info|biz|xyz|online|site|shop|store|" +
        "link|page|us|uk|ca|au|de|fr|jp|ru|eu"
    private const val HOST_END = """(?![\w@-]|\.\w)"""

    private const val SCHEMED = """(?:https?://|www\.)$PATH+""" +
        """|(?<!\w)(?:[a-z][a-z0-9+.-]*://|(?:mailto|tel|sms|smsto):)$PATH+"""
    private const val ADDRESS = """[a-z0-9._%+-]+@(?:[a-z0-9-]+\.)+[a-z]{2,24}"""
    private const val EMAIL = """(?<![\w.%+-])$ADDRESS$HOST_END"""
    private const val DOMAIN = """(?<![\w@./:%-])(?:$LABEL\.)+""" +
        """(?:(?:$TLDS)$HOST_END(?::\d{2,5})?(?:[/?#]$PATH*)?|[a-z]{2,24}(?::\d{2,5})?/$PATH+)"""

    private const val PHONE = """(?:\+\d{1,3}[\s.-]?\(?\d{2,5}\)?(?:[\s.-]?\d{2,5}){1,4}""" +
        """|(?:\+?91[\s-]?|0)?[6-9]\d{4}[\s-]?\d{5}""" +
        """|1800[\s-]?\d{2,4}[\s-]?\d{3,4}""" +
        """|1[\s-]?800[\s-]?\d{3}[\s-]?\d{4}""" +
        """|\(?[2-9]\d{2}\)?[\s.-]\d{3}[\s.-]\d{4})"""

    private val LINK = Regex(
        "(?:$SCHEMED)|(?:$EMAIL)|(?:$DOMAIN)|(?:(?<![\\w+.@/#-])$PHONE(?![\\w@]|[-.]\\d))",
        RegexOption.IGNORE_CASE
    )
    private val SCHEME = Regex("^([a-z][a-z0-9+.-]*)://", RegexOption.IGNORE_CASE)
    private val EMAIL_ONLY = Regex(ADDRESS, RegexOption.IGNORE_CASE)
    private val PHONE_ONLY = Regex(PHONE)
    private val FILE_ENDINGS = setOf(
        "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png", "gif", "zip", "html", "htm",
        "js", "json", "xml", "css", "kt", "java", "py", "md", "csv", "mp3", "mp4", "apk", "exe"
    )
    private val DIAL_DIGITS = 10..15
    private val BLOCKED_SCHEMES = setOf("file", "content", "intent", "android-app")

    private const val TRAILING = ".,;:!?\"'…"

    /** Distinct links in the order they appear (ignoring case), without the punctuation that ends a sentence. */
    fun find(text: String): List<String> =
        LINK.findAll(text).map { trim(it.value) }
            .filter { it.isNotEmpty() && accepted(it) }
            .distinctBy { normalize(it).lowercase() }
            .toList()

    /**
     * The link a message offers to open: only when its text holds exactly 1, like Android's own Open link, where a
     * link written with its scheme or as www. counts alone. Our own reply is shown on the card as "You: ..." and is
     * not offered
     */
    fun forCard(text: String, bigText: String): String? {
        if (text.startsWith(SENT_REPLY_LABEL)) return null
        val all = find(text.ifBlank { bigText })
        return all.filter(::isSchemed).singleOrNull() ?: all.singleOrNull()
    }

    fun inSelection(selected: String, known: List<String>): String? {
        val part = selected.trim()
        val isPart = part.length >= MIN_PART && part.none { it.isWhitespace() } &&
            part.any { it == '.' || it == '/' || it == '@' }
        if (isPart) known.firstOrNull { it.contains(part, ignoreCase = true) }?.let { return it }
        return find(selected).firstOrNull()
    }

    fun kind(link: String): LinkKind = when {
        hasPrefix(link, "mailto:") -> LinkKind.EMAIL
        hasPrefix(link, "tel:") -> LinkKind.PHONE
        hasPrefix(link, "sms:", "smsto:") -> LinkKind.SMS
        hasPrefix(link, "http://", "https://", "www.") -> LinkKind.WEB
        scheme(link) != null -> LinkKind.APP
        EMAIL_ONLY.matches(link) -> LinkKind.EMAIL
        PHONE_ONLY.matches(link) -> LinkKind.PHONE
        else -> LinkKind.WEB
    }

    fun normalize(link: String): String = when (kind(link)) {
        LinkKind.EMAIL -> if (hasPrefix(link, "mailto:")) link else "mailto:$link"
        LinkKind.PHONE -> if (hasPrefix(link, "tel:")) link else "tel:" + dialable(link)
        LinkKind.WEB -> if (hasPrefix(link, "http://", "https://")) link else "https://$link"
        LinkKind.SMS, LinkKind.APP -> link
    }

    private const val MIN_PART = 3

    private fun hasPrefix(link: String, vararg prefixes: String): Boolean =
        prefixes.any { link.startsWith(it, ignoreCase = true) }

    private fun scheme(link: String): String? = SCHEME.find(link)?.groupValues?.get(1)?.lowercase()

    private fun isSchemed(link: String): Boolean =
        hasPrefix(link, "www.", "mailto:", "tel:", "sms:", "smsto:") || scheme(link) != null

    private fun dialable(number: String): String =
        number.filterIndexed { i, c -> c.isDigit() || (c == '+' && i == 0) }

    private fun accepted(link: String): Boolean = when (kind(link)) {
        LinkKind.EMAIL, LinkKind.SMS -> true
        LinkKind.APP -> scheme(link) !in BLOCKED_SCHEMES
        LinkKind.PHONE -> hasPrefix(link, "tel:") || link.count { it.isDigit() } in DIAL_DIGITS
        LinkKind.WEB -> isSchemed(link) || plausibleHost(link)
    }

    private fun plausibleHost(link: String): Boolean {
        val host = link.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
        val ending = host.substringAfterLast('.')
        return (ending == ending.lowercase() || ending == ending.uppercase()) && ending.lowercase() !in FILE_ENDINGS
    }

    private fun trim(raw: String): String {
        var end = raw.length
        while (end > 0) {
            val last = raw[end - 1]
            val unbalancedClose = last == ')' && raw.take(end).count { it == '(' } < raw.take(end).count { it == ')' }
            if (last in TRAILING || unbalancedClose) end-- else break
        }
        return raw.take(end)
    }
}
