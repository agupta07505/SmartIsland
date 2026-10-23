/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    private fun item(
        id: String,
        title: String,
        subtitle: String = "",
        keywords: List<String> = emptyList(),
        screen: String = ""
    ) = SearchableSetting(id, title, subtitle, keywords, screen)

    private fun ids(items: List<SearchableSetting>, query: String) =
        SettingsSearch.search(items, query).map { it.id }

    @Test
    fun normalizeIgnoresCaseAccentsAndPunctuation() {
        assertEquals("wi fi", SettingsSearch.normalize("  Wi-Fi!!  "))
        assertEquals("notificacoes", SettingsSearch.normalize("Notificações"))
        assertEquals("do not disturb", SettingsSearch.normalize("DO  NOT\tDISTURB"))
    }

    @Test
    fun blankOrPunctuationOnlyQueriesMatchNothing() {
        val items = listOf(item("a", "Timing"))
        assertTrue(SettingsSearch.search(items, "").isEmpty())
        assertTrue(SettingsSearch.search(items, "   ").isEmpty())
        assertTrue(SettingsSearch.search(items, "---").isEmpty())
    }

    @Test
    fun wifiMatchesWiFiInEitherSpelling() {
        val items = listOf(item("w", "Wi-Fi connected (shows network name)"))
        assertEquals(listOf("w"), ids(items, "wifi"))
        assertEquals(listOf("w"), ids(items, "wi-fi"))
        assertEquals(listOf("w"), ids(items, "WI FI"))
    }

    @Test
    fun accentsInTheQueryOrTheTitleDoNotMatter() {
        val items = listOf(item("n", "Notificações"))
        assertEquals(listOf("n"), ids(items, "notificacoes"))
        assertEquals(listOf("n"), ids(items, "notificações"))
    }

    @Test
    fun everyWordOfTheQueryMustMatch() {
        val items = listOf(item("i", "Idle timeout"))
        assertEquals(listOf("i"), ids(items, "idle timeout"))
        assertEquals(listOf("i"), ids(items, "timeout idle"))
        assertTrue(ids(items, "idle banana").isEmpty())
    }

    @Test
    fun titleStartBeatsTitleWordBeatsKeyword() {
        val items = listOf(
            item("keyword", "Charging", keywords = listOf("battery")),
            item("word", "Low battery"),
            item("start", "Battery saver")
        )
        assertEquals(listOf("start", "word", "keyword"), ids(items, "battery"))
    }

    @Test
    fun equalScoresKeepIndexOrder() {
        val items = listOf(item("b", "Sound one"), item("a", "Sound two"), item("c", "Sound three"))
        assertEquals(listOf("b", "a", "c"), ids(items, "sound"))
    }

    @Test
    fun keywordsSubtitleAndScreenNameAreSearched() {
        val items = listOf(
            item("dnd", "Show the island during Do Not Disturb", keywords = listOf("dnd")),
            item("priv", "Lock screen content", subtitle = "Hide private text"),
            item("idle", "Idle timeout", screen = "Timing")
        )
        assertEquals(listOf("dnd"), ids(items, "dnd"))
        assertEquals(listOf("priv"), ids(items, "private"))
        assertEquals(listOf("idle"), ids(items, "timing"))
    }

    @Test
    fun titleMatchesOutrankScreenNameMatches() {
        val items = listOf(
            item("screen", "Idle timeout", screen = "Timing"),
            item("title", "Timing")
        )
        assertEquals(listOf("title", "screen"), ids(items, "timing"))
    }

    @Test
    fun chineseTitlesMatchInsideTheText() {
        val items = listOf(item("z", "自动展开"), item("y", "手势"))
        assertEquals(listOf("z"), ids(items, "展开"))
    }

    @Test
    fun resultsAreCapped() {
        val items = (1..40).map { item("s$it", "Setting $it") }
        val found = SettingsSearch.search(items, "setting")
        assertEquals(SettingsSearch.MAX_RESULTS, found.size)
        assertEquals("s1", found.first().id)
    }

    @Test
    fun noMatchGivesAnEmptyList() {
        assertTrue(ids(listOf(item("a", "Timing")), "zzz").isEmpty())
    }
}
