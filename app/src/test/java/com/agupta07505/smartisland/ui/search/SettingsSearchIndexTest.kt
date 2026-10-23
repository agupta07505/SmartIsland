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
    // The real app resolves string resources; here every id becomes "s<id>" so the structure
    // (ids, row keys, targets) can be checked on the JVM.
    private val index = buildSettingsSearchIndex { id -> "s$id" }

    @Test
    fun idsAreUniqueAndTitlesAreNotBlank() {
        val ids = index.map { it.item.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(index.all { it.item.title.isNotBlank() })
    }

    @Test
    fun everyEventOnTheHomeGridCanBeFound() {
        IslandEvent.entries.forEach { event ->
            val entry = index.single { it.item.id == "event.${event.id}" }
            assertEquals(SearchTarget.Tab(StudioTab.Studio), entry.target)
            assertEquals(eventSearchKey(event), entry.rowKey)
            assertTrue(entry.item.keywords.contains(event.shortLabel))
        }
    }

    @Test
    fun eventRowKeysAreUniqueAndStartWithTheirGroupPrefix() {
        val keys = IslandEvent.entries.map { eventSearchKey(it) }
        assertEquals(keys.size, keys.toSet().size)
        // CollapsibleCard opens a group when the key starts with "event.<group>.".
        IslandEvent.entries.forEach { event ->
            assertTrue(eventSearchKey(event).startsWith("event.${event.group}."))
        }
    }

    @Test
    fun everyDetailScreenIsReachable() {
        val reachable = index.map { it.target }.filterIsInstance<SearchTarget.Detail>().map { it.section }.toSet()
        assertEquals(FeatureDetailSection.entries.toSet(), reachable)
    }

    @Test
    fun everyTabIsReachable() {
        val tabs = index.map { it.target }.filterIsInstance<SearchTarget.Tab>().map { it.tab }.toSet()
        assertEquals(StudioTab.entries.toSet(), tabs)
    }

    @Test
    fun onlyTheOrientationPairsShareARow() {
        val shared = index.mapNotNull { it.rowKey }
            .groupingBy { it }.eachCount()
            .filterValues { it > 1 }.keys
        // Portrait and Landscape each have an entry, but they point at the same row.
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
    fun animationSpeedEntriesSwitchTheOrientationSwitch() {
        val portrait = index.single { it.item.id == "layout.anim_portrait" }
        val landscape = index.single { it.item.id == "layout.anim_landscape" }
        assertEquals(SearchTarget.Tab(StudioTab.Position, false), portrait.target)
        assertEquals(SearchTarget.Tab(StudioTab.Position, true), landscape.target)
    }

    @Test
    fun mergeStyleEntriesSwitchTheOrientationSwitch() {
        val portrait = index.single { it.item.id == "layout.merge_portrait" }
        val landscape = index.single { it.item.id == "layout.merge_landscape" }
        assertEquals(SearchTarget.Tab(StudioTab.Position, false), portrait.target)
        assertEquals(SearchTarget.Tab(StudioTab.Position, true), landscape.target)
        assertEquals("layout.merge_style", portrait.rowKey)
        assertEquals("layout.merge_style", landscape.rowKey)
    }

    @Test
    fun landscapeOnlyRowsOpenTheLandscapeSide() {
        listOf("layout.show_landscape", "layout.edge_inset").forEach { id ->
            val entry = index.single { it.item.id == id }
            assertEquals(SearchTarget.Tab(StudioTab.Position, true), entry.target)
            assertEquals(id, entry.rowKey)
        }
    }

    @Test
    fun hapticsAndShowInLandscapeLiveInOnePlaceOnly() {
        assertTrue(index.none { it.rowKey == "np.haptics" || it.rowKey == "np.show_in_landscape" })
        val haptics = index.single { it.item.id == "gestures.haptics" }
        assertEquals(SearchTarget.Detail(FeatureDetailSection.GesturesGuide), haptics.target)
        assertEquals("gestures.haptics", haptics.rowKey)
        assertEquals(1, index.count { it.item.id.contains("show_landscape") || it.item.id.contains("show_in_landscape") })
    }

    @Test
    fun gestureSensitivityEntryLeadsToTheGesturesScreen() {
        val entry = index.single { it.item.id == "gestures.sensitivity" }
        assertEquals(SearchTarget.Detail(FeatureDetailSection.GesturesGuide), entry.target)
        assertEquals("gestures.sensitivity", entry.rowKey)
    }

    @Test
    fun timingRowsLeadToTheTimingScreen() {
        listOf("timing.idle", "timing.battery", "timing.popup", "timing.merge").forEach { key ->
            val entry = index.single { it.rowKey == key }
            assertEquals(SearchTarget.Detail(FeatureDetailSection.Timing), entry.target)
        }
    }

    @Test
    fun englishAliasesFindTheRightRows() {
        val items = index.map { it.item }
        fun first(query: String) = SettingsSearch.search(items, query).firstOrNull()?.id
        assertEquals("np.show_during_dnd", first("dnd"))
        assertEquals("backup.export", first("json export"))
        assertEquals("perm.shizuku", first("shizuku"))
        assertEquals("diag.screen", first("debug"))
    }
}
