/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.search

import java.text.Normalizer

/**
 * One searchable setting row, already resolved to the strings the user sees in their language.
 *
 * @param id stable id, also used as the row key for scrolling and highlighting (may be shared with
 *   the screen the row lives on, see [SettingsSearchIndex])
 * @param title the row's title as shown on screen
 * @param subtitle the row's description, if it has one
 * @param keywords extra words people may type (aliases, English names, technical terms)
 * @param screen the name of the screen or tab the row lives on, shown under the result and
 *   searched too, so "timing" finds every row on the Timing screen
 */
data class SearchableSetting(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val keywords: List<String> = emptyList(),
    val screen: String = ""
)

/**
 * Pure matching for the settings search box. No Android classes, so it is unit-tested on the JVM.
 */
object SettingsSearch {
    const val MAX_RESULTS = 25

    private val combiningMarks = Regex("\\p{Mn}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    /** Lower-cases, strips accents and turns every run of punctuation into one space. */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed
            .replace(combiningMarks, "")
            .lowercase()
            .replace(separators, " ")
            .trim()
    }

    fun tokens(query: String): List<String> =
        normalize(query).split(' ').filter { it.isNotEmpty() }

    /** Matching rows for [query], best first. A blank query matches nothing. */
    fun search(items: List<SearchableSetting>, query: String): List<SearchableSetting> {
        val words = tokens(query)
        if (words.isEmpty()) return emptyList()
        return items
            .mapIndexedNotNull { index, item ->
                val score = score(item, words)
                if (score > 0) Triple(item, score, index) else null
            }
            .sortedWith(compareByDescending<Triple<SearchableSetting, Int, Int>> { it.second }.thenBy { it.third })
            .take(MAX_RESULTS)
            .map { it.first }
    }

    /** Total score of [item] for [words], or 0 when any word finds no match at all. */
    internal fun score(item: SearchableSetting, words: List<String>): Int {
        val title = normalize(item.title)
        val titleWords = title.split(' ').filter { it.isNotEmpty() }
        // The compact forms (no separators) let "wifi" find "Wi-Fi".
        val titleCompact = title.replace(" ", "")
        val keywordTexts = item.keywords.map { normalize(it) }
        val keywordCompact = keywordTexts.map { it.replace(" ", "") }
        val rest = normalize(item.subtitle + " " + item.screen)
        val restCompact = rest.replace(" ", "")

        var total = 0
        for (word in words) {
            val best = when {
                title.startsWith(word) -> 100
                titleWords.any { it.startsWith(word) } -> 80
                title.contains(word) -> 60
                titleCompact.contains(word) -> 55
                keywordTexts.any { it.startsWith(word) } || keywordCompact.any { it.startsWith(word) } -> 50
                keywordTexts.any { it.contains(word) } || keywordCompact.any { it.contains(word) } -> 40
                rest.contains(word) || restCompact.contains(word) -> 20
                else -> 0
            }
            if (best == 0) return 0
            total += best
        }
        return total
    }
}
