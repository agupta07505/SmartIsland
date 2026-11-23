/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import java.io.File
import java.lang.reflect.Modifier
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Settings backup compat: absent keys, unknown keys, wrong types, fields a backup must and must not hold, defaults
 * of a fresh install
 *
 * Golden file for the defaults is `src/test/resources/settings_defaults.golden`, made by running this class once
 * with UPDATE_GOLDEN=1. Look it over before committing, [defaultsMatchTheGoldenFile] fails until it exists
 */
class SettingsBackupCompatTest {

    private val notBackedUp = setOf("foldDisabledSomewhere", "weatherCustomKey", "cardEventColor")

    private fun exported(settings: SmartIslandSettings): JSONObject =
        JSONObject(settings.toJson()).getJSONObject("settings")

    @Test
    fun emptyBackupLoadsTheDefaults() {
        assertEquals(SmartIslandSettings.Default, SmartIslandSettings.fromJson("{}"))
        assertEquals(SmartIslandSettings.Default, SmartIslandSettings.fromJson("""{"settings":{}}"""))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val json = """{"formatVersion":99,"settings":{"keyFromTheFuture":1,"nested":{"a":[1,2]}}}"""
        assertEquals(SmartIslandSettings.Default, SmartIslandSettings.fromJson(json))
    }

    @Test
    fun valuesOfTheWrongTypeFallBackToTheDefault() {
        val loaded = SmartIslandSettings.fromJson("""{"settings":{"width":"wide","enabled":"maybe","pillColor":"red"}}""")
        assertEquals(SmartIslandSettings.Default.width, loaded.width)
        assertEquals(SmartIslandSettings.Default.enabled, loaded.enabled)
        assertEquals(SmartIslandSettings.Default.pillColor, loaded.pillColor)
    }

    @Test
    fun textThatIsNotJsonThrowsSoTheCallerCanRefuseIt() {
        try {
            SmartIslandSettings.fromJson("not json")
            fail("expected a JSONException")
        } catch (expected: JSONException) {
        }
        assertNull(SmartIslandSettings.parseBackupMetadata("not json"))
    }

    @Test
    fun noFormatVersionIsV1() {
        val metadata = SmartIslandSettings.parseBackupMetadata("""{"settings":{"width":100}}""")
        assertNotNull(metadata)
        assertEquals(1, metadata!!.formatVersion)
        assertEquals(1, metadata.settingsCount)
    }

    @Test
    fun exportWritesTheCurrentFormatVersion() {
        val root = JSONObject(SmartIslandSettings.Default.toJson("1.2.3"))
        assertEquals(SmartIslandSettings.BACKUP_FORMAT_VERSION, root.getInt("formatVersion"))
        assertEquals("com.agupta07505.smartisland", root.getString("app"))
        assertEquals("1.2.3", root.getString("appVersion"))
    }

    @Test
    fun weatherApiKeyIsNeverExported() {
        val secret = "sk-test-secret-123"
        val json = SmartIslandSettings.Default.copy(weatherCustomKey = secret).toJson()
        assertFalse(json.contains(secret))
        assertEquals("", SmartIslandSettings.fromJson(json).weatherCustomKey)
    }

    @Test
    fun exportCoversAllButTransient() {
        fun fieldNames(type: Class<*>) = type.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .map { it.name }
            .toSet()
        val fields = (fieldNames(SmartIslandSettings::class.java) - "modeColors") + fieldNames(ModeColors::class.java)
        val keys = exported(SmartIslandSettings.Default).keys().asSequence().toSet()
        assertEquals("fields missing from the export", emptySet<String>(), fields - keys - notBackedUp)
        assertEquals("exported keys with no field", emptySet<String>(), keys - fields)
        assertTrue(notBackedUp.all { it in fields })
    }

    @Test
    fun defaultsRoundTrip() {
        val defaults = SmartIslandSettings.Default
        assertEquals(defaults, SmartIslandSettings.fromJson(defaults.toJson()))
    }

    @Test
    fun secondExportMatchesTheFirst() {
        val changed = SmartIslandSettings.Default.copy(enabled = true, width = 140f, pillColor = 0xFF123456L)
        val first = exported(changed)
        val second = exported(SmartIslandSettings.fromJson(changed.toJson()))
        assertEquals(first.toString(), second.toString())
    }

    private val defaultOff = com.agupta07505.smartisland.model.IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS

    private fun setsBackup(version: String, portrait: String, landscape: String?): String {
        val land = if (landscape == null) "" else ",\"noAutoExpandEventsLandscape\":$landscape"
        return """{$version"settings":{"noAutoExpandEvents":$portrait$land}}"""
    }

    @Test
    fun versionOneBackupWithAnEmptySetGetsTheNewDefault() {
        val loaded = SmartIslandSettings.fromJson(setsBackup(""""formatVersion":1,""", "[]", "[]"))
        assertEquals(defaultOff, loaded.noAutoExpandEvents)
        assertEquals(defaultOff, loaded.noAutoExpandEventsLandscape)
    }

    @Test
    fun v1CustomSetIsIgnored() {
        val loaded = SmartIslandSettings.fromJson(setsBackup(""""formatVersion":1,""", """["call"]""", """["call"]"""))
        assertEquals(defaultOff, loaded.noAutoExpandEvents)
        assertEquals(defaultOff, loaded.noAutoExpandEventsLandscape)
        assertTrue(loaded.isEventAutoExpand("call"))
    }

    @Test
    fun missingVersionCountsAsV1() {
        val loaded = SmartIslandSettings.fromJson(setsBackup("", """["wifi"]""", null))
        assertEquals(defaultOff, loaded.noAutoExpandEvents)
        assertEquals(defaultOff, loaded.noAutoExpandEventsLandscape)
    }

    @Test
    fun versionedBackupSets() {
        val currentLoaded = SmartIslandSettings.fromJson(setsBackup(""""formatVersion":2,""", """["call"]""", """["wifi"]"""))
        assertEquals(setOf("call"), currentLoaded.noAutoExpandEvents)
        assertEquals(setOf("wifi"), currentLoaded.noAutoExpandEventsLandscape)

        val futureLoaded = SmartIslandSettings.fromJson(setsBackup(""""formatVersion":99,""", """["call"]""", """["call"]"""))
        assertEquals(setOf("call"), futureLoaded.noAutoExpandEvents)
        assertEquals(setOf("call"), futureLoaded.noAutoExpandEventsLandscape)
    }

    @Test
    fun missingLandscapeBackupDefaults() {
        val loaded = SmartIslandSettings.fromJson(setsBackup(""""formatVersion":2,""", """["call"]""", null))
        assertEquals(setOf("call"), loaded.noAutoExpandEvents)
        assertEquals(defaultOff, loaded.noAutoExpandEventsLandscape)
    }

    private fun describeDefaults(): String {
        val obj = exported(SmartIslandSettings.Default)
        return obj.keys().asSequence().sorted().joinToString("\n", postfix = "\n") { "$it=${obj.get(it)}" }
    }

    @Test
    fun defaultsMatchTheGoldenFile() {
        val golden = File("src/test/resources/settings_defaults.golden")
        val actual = describeDefaults()
        if (System.getenv("UPDATE_GOLDEN") == "1") {
            golden.writeText(actual)
            return
        }
        assertTrue("settings_defaults.golden is missing; run once with UPDATE_GOLDEN=1 and review it", golden.exists())
        assertEquals("a default changed; if intended, regenerate with UPDATE_GOLDEN=1", golden.readText(), actual)
    }
}
