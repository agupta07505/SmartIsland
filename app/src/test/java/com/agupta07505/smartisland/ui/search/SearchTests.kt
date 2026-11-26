/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.search

import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.ui.FeatureDetailSection
import com.agupta07505.smartisland.ui.StudioTab
import com.agupta07505.smartisland.ui.sections.eventSearchKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchIndexTest {
    // the real app resolves string resources; here every id becomes "s<id>" so the structure
    // (ids, row keys, targets) can be checked on the JVM
    private val index = buildSettingsSearchIndex { id -> "s$id" }

    @Test
    fun idsAreUniqueAndTitlesAreNotBlank() {
        val ids = index.map { it.item.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(index.all { it.item.title.isNotBlank() })
    }

    @Test
    fun tabSearchDestinations() {
        IslandEvent.entries.forEach { event ->
            val homeEntry = index.single { it.item.id == "event.${event.id}" }
            assertEquals(SearchTarget.Tab(StudioTab.Studio), homeEntry.target)
            assertEquals(eventSearchKey(event), homeEntry.rowKey)
            assertTrue(homeEntry.item.keywords.contains(event.shortLabel))
        }

        val animationPortrait = index.single { it.item.id == "layout.anim_portrait" }
        val animationLandscape = index.single { it.item.id == "layout.anim_landscape" }
        assertEquals(SearchTarget.Tab(StudioTab.Position, false), animationPortrait.target)
        assertEquals(SearchTarget.Tab(StudioTab.Position, true), animationLandscape.target)

        val mergePortrait = index.single { it.item.id == "layout.merge_portrait" }
        val mergeLandscape = index.single { it.item.id == "layout.merge_landscape" }
        assertEquals(SearchTarget.Tab(StudioTab.Position, false), mergePortrait.target)
        assertEquals(SearchTarget.Tab(StudioTab.Position, true), mergeLandscape.target)
        assertEquals("layout.merge_style", mergePortrait.rowKey)
        assertEquals("layout.merge_style", mergeLandscape.rowKey)

        listOf("layout.show_landscape", "layout.edge_inset").forEach { id ->
            val landscapeEntry = index.single { it.item.id == id }
            assertEquals(SearchTarget.Tab(StudioTab.Position, true), landscapeEntry.target)
            assertEquals(id, landscapeEntry.rowKey)
        }
    }

    @Test
    fun eventRowKeysUniquePrefixed() {
        val keys = IslandEvent.entries.map { eventSearchKey(it) }
        assertEquals(keys.size, keys.toSet().size)
        // CollapsibleCard opens a group when the key starts with "event.<group>."
        IslandEvent.entries.forEach { event ->
            assertTrue(eventSearchKey(event).startsWith("event.${event.group}."))
        }
    }

    @Test
    fun searchIndexCoverage() {
        val reachable = index.map { it.target }.filterIsInstance<SearchTarget.Detail>().map { it.section }.toSet()
        assertEquals(FeatureDetailSection.entries.toSet(), reachable)

        val tabs = index.map { it.target }.filterIsInstance<SearchTarget.Tab>().map { it.tab }.toSet()
        assertEquals(StudioTab.entries.toSet(), tabs)
    }

    @Test
    fun onlyTheOrientationPairsShareARow() {
        val shared = index.mapNotNull { it.rowKey }
            .groupingBy { it }.eachCount()
            .filterValues { it > 1 }.keys
        // portrait and Landscape each have an entry, but they point at the same row
        assertEquals(setOf("layout.anim_speed", "layout.merge_style"), shared)
    }

    @Test
    fun rowKeysUseTheKnownPrefixes() {
        val known = listOf("event.", "np.", "timing.", "layout.", "gestures.", "idle.")
        index.mapNotNull { it.rowKey }.forEach { key ->
            assertTrue("unexpected row key $key", known.any { key.startsWith(it) })
        }
    }

    @Test
    fun detailSearchDestinations() {
        assertTrue(index.none { it.rowKey == "np.haptics" || it.rowKey == "np.show_in_landscape" })
        val haptics = index.single { it.item.id == "gestures.haptics" }
        assertEquals(SearchTarget.Detail(FeatureDetailSection.GesturesGuide), haptics.target)
        assertEquals("gestures.haptics", haptics.rowKey)
        assertEquals(1, index.count { it.item.id.contains("show_landscape") || it.item.id.contains("show_in_landscape") })

        val sensitivityEntry = index.single { it.item.id == "gestures.sensitivity" }
        assertEquals(SearchTarget.Detail(FeatureDetailSection.GesturesGuide), sensitivityEntry.target)
        assertEquals("gestures.sensitivity", sensitivityEntry.rowKey)

        listOf("timing.idle", "timing.battery", "timing.merge", "timing.pulse").forEach { key ->
            val timingEntry = index.single { it.rowKey == key }
            assertEquals(SearchTarget.Detail(FeatureDetailSection.Timing), timingEntry.target)
        }
    }

    @Test
    fun englishAliasesFindTheRightRows() {
        val items = index.map { it.item }
        fun first(query: String) = SettingsSearch.search(items, query).firstOrNull()?.id
        assertEquals("np.show_during_dnd", first("dnd"))
        assertTrue(SettingsSearch.search(items, "dnd").any { it.id == "event.dnd" })
        assertEquals("backup.export", first("json export"))
        assertEquals("perm.shizuku", first("shizuku"))
        assertEquals("diag.screen", first("debug"))
    }
}

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
    fun normalizeIgnoresCaseAccents() {
        assertEquals("wi fi", SettingsSearch.normalize("  Wi-Fi!!  "))
        assertEquals("notificacoes", SettingsSearch.normalize("Notificações"))
        assertEquals("do not disturb", SettingsSearch.normalize("DO  NOT\tDISTURB"))
    }

    @Test
    fun settingsSearchMatching() {
        val emptyItems = listOf(item("a", "Timing"))
        assertTrue(SettingsSearch.search(emptyItems, "").isEmpty())
        assertTrue(SettingsSearch.search(emptyItems, "   ").isEmpty())
        assertTrue(SettingsSearch.search(emptyItems, "---").isEmpty())

        val allWordsItems = listOf(item("i", "Idle timeout"))
        assertEquals(listOf("i"), ids(allWordsItems, "idle timeout"))
        assertEquals(listOf("i"), ids(allWordsItems, "timeout idle"))
        assertTrue(ids(allWordsItems, "idle banana").isEmpty())

        val chineseItems = listOf(item("z", "自动展开"), item("y", "手势"))
        assertEquals(listOf("z"), ids(chineseItems, "展开"))

        val cappedItems = (1..40).map { item("s$it", "Setting $it") }
        val found = SettingsSearch.search(cappedItems, "setting")
        assertEquals(SettingsSearch.MAX_RESULTS, found.size)
        assertEquals("s1", found.first().id)

        assertTrue(ids(listOf(item("a", "Timing")), "zzz").isEmpty())
    }

    @Test
    fun wifiMatchesWiFiInEitherSpelling() {
        val items = listOf(item("w", "Wi-Fi connected (shows network name)"))
        assertEquals(listOf("w"), ids(items, "wifi"))
        assertEquals(listOf("w"), ids(items, "wi-fi"))
        assertEquals(listOf("w"), ids(items, "WI FI"))
    }

    @Test
    fun accentsDoNotMatter() {
        val items = listOf(item("n", "Notificações"))
        assertEquals(listOf("n"), ids(items, "notificacoes"))
        assertEquals(listOf("n"), ids(items, "notificações"))
    }

    @Test
    fun titleStartBeatsWordBeatsKeyword() {
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
    fun keywordsSubtitleScreenSearched() {
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
    fun titleOutranksScreenName() {
        val items = listOf(
            item("screen", "Idle timeout", screen = "Timing"),
            item("title", "Timing")
        )
        assertEquals(listOf("title", "screen"), ids(items, "timing"))
    }
}
