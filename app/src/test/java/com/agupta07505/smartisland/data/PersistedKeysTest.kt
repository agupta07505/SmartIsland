/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.FoldPosture
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The preference key names and value types are the on-disk format of the settings DataStore.
 * Renaming a key silently drops the user's value; reusing a name with another type makes
 * DataStore throw when it reads the file. This test pins both against
 * `src/test/resources/persisted_preference_keys.txt` (1 `type name` per line).
 *
 * Adding a key: add its line to the golden file. Renaming or retyping a key needs a migration
 * in the repository, then the golden file is updated.
 */
class PersistedKeysTest {

    private val repositorySource = File(
        "src/main/java/com/agupta07505/smartisland/data/SettingsKeys.kt"
    )
    private val goldenFile = File("src/test/resources/persisted_preference_keys.txt")

    private val keyDeclaration =
        Regex("""val \w+ = (boolean|float|int|long|string|stringSet)PreferencesKey\("([^"]+)"\)""")

    private fun declaredKeys(): List<Pair<String, String>> {
        val text = repositorySource.readText()
        assertTrue("could not find the SettingsKeys object", text.contains("object SettingsKeys {"))
        return keyDeclaration.findAll(text)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()
    }

    private fun goldenKeys(): List<Pair<String, String>> =
        goldenFile.readLines().filter { it.isNotBlank() }.map {
            val (type, name) = it.trim().split(" ")
            type to name
        }

    @Test
    fun persistedKeyNames() {
        val declared = declaredKeys().map { "${it.first} ${it.second}" }.toSet()
        val golden = goldenKeys().map { "${it.first} ${it.second}" }.toSet()
        val removed = golden - declared
        val added = declared - golden
        assertTrue("persisted keys removed, renamed or retyped: $removed", removed.isEmpty())
        assertTrue("new keys missing from persisted_preference_keys.txt: $added", added.isEmpty())

        val uniqueNames = declaredKeys().map { it.second }
        assertEquals(uniqueNames.toSet().size, uniqueNames.size)

        for ((_, name) in declaredKeys()) {
            assertFalse("$name contains the fold profile separator", FoldProfiles.isProfileName(name))
        }

        val foldKeysNames = declaredKeys().map { it.second }.toSet()
        val foldKeysMissing = (FoldProfiles.PortraitKeys + FoldProfiles.LandscapeKeys) - foldKeysNames
        assertTrue("fold scoped keys with no matching preference key: $foldKeysMissing", foldKeysMissing.isEmpty())
    }

    @Test
    fun scopesDoNotOverlap() {
        assertTrue(FoldProfiles.PortraitKeys.intersect(FoldProfiles.LandscapeKeys).isEmpty())
    }

    @Test
    fun foldProfileKeyNamesKeepTheirFormat() {
        assertEquals("@", FoldProfiles.SEPARATOR)
        assertEquals("width@folded_portrait", FoldProfiles.profileName("width", FoldPosture.FoldedPortrait))
        assertEquals(
            "landscape_width@unfolded_landscape",
            FoldProfiles.profileName("landscape_width", FoldPosture.UnfoldedLandscape)
        )
        assertEquals(
            listOf("folded_portrait", "folded_landscape", "unfolded_portrait", "unfolded_landscape"),
            FoldPosture.Modes.map { it.id }
        )
    }
}
