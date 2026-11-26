/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class AutoExpandPerOrientationTest {

    private val portraitOff = SmartIslandSettings.Default.copy(
        noAutoExpandEvents = setOf("wifi"),
        noAutoExpandEventsLandscape = emptySet()
    )

    @Test
    fun portraitSwitchNoLandscapeLeak() {
        val landscape = portraitOff.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f, screenHeightDp = 360f))
        assertTrue(landscape.isEventAutoExpand("wifi"))
        assertFalse(portraitOff.isEventAutoExpand("wifi"))
    }

    @Test
    fun landscapeSwitchNoPortraitLeak() {
        val s = SmartIslandSettings.Default.copy(
            noAutoExpandEvents = emptySet(),
            noAutoExpandEventsLandscape = setOf("wifi")
        )
        val landscape = s.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f, screenHeightDp = 360f))
        val portrait = s.forOrientation(OrientationState(isLandscape = false, screenWidthDp = 360f))
        assertFalse(landscape.isEventAutoExpand("wifi"))
        assertTrue(portrait.isEventAutoExpand("wifi"))
    }

    @Test
    fun backupRoundTripKeepsBothSets() {
        val s = SmartIslandSettings.Default.copy(
            noAutoExpandEvents = setOf("wifi"),
            noAutoExpandEventsLandscape = emptySet()
        )
        val back = SmartIslandSettings.fromJson(s.toJson())
        assertEquals(setOf("wifi"), back.noAutoExpandEvents)
        assertEquals(emptySet<String>(), back.noAutoExpandEventsLandscape)
    }
}

/**
 * Keeps the notification-history database out of cloud backup and device transfer.
 * Reads the real manifest and rule files; it checks configuration only, not what Android backs up.
 */
class BackupRulesTest {

    private val mainDir = File("src/main")

    private fun parse(path: String): Document =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File(mainDir, path))

    private fun excludedDatabasePaths(section: Element): Set<String> =
        (0 until section.getElementsByTagName("exclude").length)
            .map { section.getElementsByTagName("exclude").item(it) as Element }
            .filter { it.getAttribute("domain") == "database" }
            .map { it.getAttribute("path") }
            .toSet()

    private val historyFiles = listOf("", "-journal", "-wal", "-shm")
        .map { DATABASE_NAME + it }
        .toSet()

    @Test
    fun backupExclusions() {
        val app = parse("AndroidManifest.xml").getElementsByTagName("application").item(0) as Element
        assertEquals("@xml/backup_rules", app.getAttributeNS(ANDROID_NS, "fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", app.getAttributeNS(ANDROID_NS, "dataExtractionRules"))

        val legacyRoot = parse("res/xml/backup_rules.xml").documentElement
        assertEquals("full-backup-content", legacyRoot.tagName)
        assertEquals(historyFiles, excludedDatabasePaths(legacyRoot))

        val extractionRoot = parse("res/xml/data_extraction_rules.xml").documentElement
        for (section in listOf("cloud-backup", "device-transfer")) {
            val nodes = extractionRoot.getElementsByTagName(section)
            assertEquals("missing <$section>", 1, nodes.length)
            assertEquals(section, historyFiles, excludedDatabasePaths(nodes.item(0) as Element))
        }

        // An <include> would silently drop everything else (settings DataStore) from backup.
        for (path in listOf("res/xml/backup_rules.xml", "res/xml/data_extraction_rules.xml")) {
            assertTrue(path, parse(path).getElementsByTagName("include").length == 0)
        }
    }

    @Test
    fun excludedNameMatchesDbName() {
        val source = File(mainDir, "java/com/agupta07505/smartisland/data/NotificationHistoryDbHelper.kt").readText()
        assertTrue(source.contains("DATABASE_NAME = \"$DATABASE_NAME\""))
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val DATABASE_NAME = "smart_island_notification_history.db"
    }
}

class CollapsedPulsesTest {
    private val landscape = OrientationState(isLandscape = true, screenWidthDp = 800f, screenHeightDp = 360f)
    private val classic = SmartIslandSettings(landscapeLayoutStyle = LandscapeLayoutStyle.CLASSIC)

    @Test
    fun portraitAlwaysPulses() {
        assertTrue(SmartIslandSettings().collapsedPulses(OrientationState(isLandscape = false)))
    }

    @Test
    fun classicPulsePlacement() {
        assertTrue(classic.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE)
            .collapsedPulses(landscape))

        assertTrue(classic.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
            landscapeXOffset = 30f).collapsedPulses(landscape))

        assertTrue(classic.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
            landscapeXOffset = 40f).collapsedPulses(landscape))
        assertTrue(classic.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
            landscapeXOffset = -40f).collapsedPulses(landscape))

        assertTrue(classic.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_LEFT)
            .collapsedPulses(landscape))
        assertTrue(classic.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_RIGHT)
            .collapsedPulses(landscape))
    }

    @Test
    fun edgePillPulsesWhereverTheCameraIs() {
        val edge = SmartIslandSettings(landscapeLayoutStyle = LandscapeLayoutStyle.EDGE)
        assertTrue(edge.collapsedPulses(landscape))
        assertTrue(edge.collapsedPulses(landscape.copy(camera = LandscapeCamera(-380f, 180f, 20f, 20f))))
        assertTrue(edge.collapsedPulses(landscape.copy(camera = LandscapeCamera(-380f, 60f, 20f, 20f))))
        assertTrue(edge.collapsedPulses(landscape.copy(camera = LandscapeCamera(380f, 300f, 20f, 20f))))
    }
}

class ContentPillWidthTest {
    @Test
    fun contentWidthConstraints() {
        for (w in listOf(105f, 112f, 136f, 150f)) {
            val defaultWidth = SmartIslandSettings.Default.copy(width = w, contentPillWidthPercent = 110f)
            assertEquals(w * 1.1f, defaultWidth.contentPillWidthDp(392f), 0.001f)
        }

        val idle = SmartIslandSettings.Default.copy(width = 112f, contentPillSameAsIdle = true)
        assertEquals(112f, idle.contentPillWidthDp(392f), 0.001f)

        val big = SmartIslandSettings.Default.copy(width = 170f, contentPillWidthPercent = 250f)
        assertEquals(392f - 48f, big.contentPillWidthDp(392f), 0.001f)
        val narrow = SmartIslandSettings.Default.copy(width = 170f, contentPillWidthPercent = 100f)
        assertEquals(170f, narrow.contentPillWidthDp(100f), 0.001f)

        val resized = SmartIslandSettings.Default.copy(width = 112f, contentPillWidthPercent = 110f)
        assertEquals(112f * 1.1f, resized.withContentPillWidth(392f).width, 0.001f)
    }
}

class EventScopeTest {
    private val base = SmartIslandSettings.Default

    @Test
    fun portraitOnlyNotDisabledAll() {
        val s = base.copy(disabledEvents = setOf("music"), showInLandscape = true)
        assertFalse(s.isEventDisabledEverywhere("music"))
        assertTrue(s.isEventDisabledSomewhere("music"))
    }

    @Test
    fun bothOffDisabledEverywhere() {
        val s = base.copy(
            disabledEvents = setOf("music"),
            disabledEventsLandscape = setOf("music"),
            showInLandscape = true
        )
        assertTrue(s.isEventDisabledEverywhere("music"))
    }

    @Test
    fun hiddenLandscapeProfileIgnored() {
        val portraitOff = base.copy(disabledEvents = setOf("music"), showInLandscape = false)
        assertTrue(portraitOff.isEventDisabledEverywhere("music"))

        val landscapeOff = base.copy(disabledEventsLandscape = setOf("music"), showInLandscape = false)
        assertFalse(landscapeOff.isEventDisabledSomewhere("music"))
    }

    @Test
    fun landscapeShadeOverride() {
        val s = base.copy(disabledEventsLandscape = setOf("timer"), showInLandscape = true)
        assertFalse(s.isEventDisabledEverywhere("timer"))
        assertTrue(s.isEventDisabledSomewhere("timer"))
    }
}
