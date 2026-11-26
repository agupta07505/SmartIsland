/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.util.AppComponents
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Names that outlive a release without going through the settings DataStore: the notification history
 * database, the SharedPreferences files, the debug log and font folders, the broadcast actions that pending
 * intents already handed to the system carry, the manifest component names, and the event ids saved inside
 * settings and backups. Renaming any of them silently drops the user's data or breaks an intent that is already
 * scheduled, so each is pinned here to the literal value. Changing 1 needs a migration first.
 *
 * Preference keys and the backup JSON are covered by [PersistedKeysTest] and [SettingsBackupCompatTest].
 * Like those, this reads source files, so it needs no Android classes.
 */
class PersistedContractsTest {

    private val mainSource = "src/main/java/com/agupta07505/smartisland"

    private val stringConstant = Regex("""const val (\w+) = "([^"]*)"""")

    private fun constantsIn(relativePath: String): Map<String, String> {
        val file = File("$mainSource/$relativePath")
        assertTrue("missing source file $relativePath", file.exists())
        return stringConstant.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun assertConstants(relativePath: String, expected: Map<String, String>) {
        val actual = constantsIn(relativePath)
        for ((name, value) in expected) {
            assertEquals("$relativePath: $name", value, actual[name])
        }
    }

    @Test
    fun historyDbKeepsSchema() {
        val path = "data/NotificationHistoryDbHelper.kt"
        assertConstants(
            path,
            mapOf(
                "DATABASE_NAME" to "smart_island_notification_history.db",
                "TABLE_HISTORY" to "notification_history",
                "COL_ID" to "_id",
                "COL_NOTIF_KEY" to "notification_key",
                "COL_PACKAGE" to "package_name",
                "COL_APP_NAME" to "app_name",
                "COL_TITLE" to "title",
                "COL_TEXT" to "text",
                "COL_SUB_TEXT" to "sub_text",
                "COL_POST_TIME" to "post_time",
                "COL_CATEGORY" to "category",
                "COL_CHANNEL_ID" to "channel_id",
                "COL_MODE" to "mode",
                "COL_ACTIONS_JSON" to "actions_json",
                "COL_WAS_OPENED" to "was_opened",
                "COL_WAS_DISMISSED" to "was_dismissed"
            )
        )
        val text = File("$mainSource/$path").readText()
        assertTrue("DATABASE_VERSION changed; add an onUpgrade migration and update this test",
            text.contains("private const val DATABASE_VERSION = 1"))

        val columnTypes = Regex("""^\s+\$(COL_\w+) ([A-Z][A-Z0-9 ]*?)(?:,)?$""", RegexOption.MULTILINE)
            .findAll(text.substringAfter("CREATE TABLE").substringBefore("\"\"\".trimIndent()"))
            .associate { it.groupValues[1] to it.groupValues[2].trim() }
        assertEquals(
            mapOf(
                "COL_ID" to "INTEGER PRIMARY KEY AUTOINCREMENT",
                "COL_NOTIF_KEY" to "TEXT NOT NULL",
                "COL_PACKAGE" to "TEXT NOT NULL",
                "COL_APP_NAME" to "TEXT NOT NULL",
                "COL_TITLE" to "TEXT",
                "COL_TEXT" to "TEXT",
                "COL_SUB_TEXT" to "TEXT",
                "COL_POST_TIME" to "INTEGER NOT NULL",
                "COL_CATEGORY" to "TEXT",
                "COL_CHANNEL_ID" to "TEXT",
                "COL_MODE" to "TEXT NOT NULL",
                "COL_ACTIONS_JSON" to "TEXT",
                "COL_WAS_OPENED" to "INTEGER DEFAULT 0",
                "COL_WAS_DISMISSED" to "INTEGER DEFAULT 0"
            ),
            columnTypes
        )
        assertTrue(text.contains("CREATE INDEX idx_notif_post_time"))
        assertTrue(text.contains("CREATE INDEX idx_notif_pkg"))
    }

    @Test
    fun persistedStorageNames() {
        assertConstants("util/InAppStopwatch.kt", mapOf("PREFS" to "in_app_stopwatch"))
        assertConstants("util/InAppTimer.kt", mapOf("PREFS" to "in_app_timer"))
        assertConstants("util/IdleWidgetData.kt", mapOf("PREFS" to "idle_widget_weather"))
        assertConstants("util/ShizukuMethodCheck.kt", mapOf("PREFS" to "shizuku_method_checks"))
        assertConstants("util/FoldDetector.kt", mapOf("DEV_PREFS" to "smart_island_dev"))
        assertConstants(
            "util/ShizukuManager.kt",
            mapOf("SETUP_PREFS" to "shizuku_setup", "SETUP_SCOPE_KEY" to "scope")
        )
        assertConstants("util/AppLogRecorder.kt", mapOf("STATE_PREFS" to "smartisland_log_state"))
        assertTrue(
            File("$mainSource/util/NetUsageStore.kt").readText().contains("""getSharedPreferences("net_usage"""")
        )

        assertConstants("util/AppLogRecorder.kt", mapOf("LOG_FILE_NAME" to "smartisland_debug_log.txt"))
        assertConstants("util/CustomFonts.kt", mapOf("DIR" to "fonts"))
    }

    @Test
    fun intentActionsKeepTheirStrings() {
        assertConstants(
            "util/InAppStopwatch.kt",
            mapOf(
                "ACTION_LAP" to "com.agupta07505.smartisland.stopwatch.LAP",
                "ACTION_TOGGLE" to "com.agupta07505.smartisland.stopwatch.TOGGLE",
                "ACTION_RESET" to "com.agupta07505.smartisland.stopwatch.RESET",
                "ACTION_NOOP" to "com.agupta07505.smartisland.stopwatch.NOOP"
            )
        )
        assertConstants(
            "util/InAppTimer.kt",
            mapOf(
                "ACTION_TOGGLE" to "com.agupta07505.smartisland.timer.TOGGLE",
                "ACTION_ADD_MINUTE" to "com.agupta07505.smartisland.timer.ADD_MINUTE",
                "ACTION_STOP" to "com.agupta07505.smartisland.timer.STOP",
                "ACTION_FINISH" to "com.agupta07505.smartisland.timer.FINISH",
                "ACTION_NOOP" to "com.agupta07505.smartisland.timer.NOOP"
            )
        )
        assertConstants(
            "util/NotificationViewInspector.kt",
            mapOf("ACTION_CLICK" to "com.agupta07505.smartisland.action.PRESS_NOTIFICATION_VIEW")
        )
        assertConstants(
            "util/DemoEvents.kt",
            mapOf("NOOP_ACTION" to "com.agupta07505.smartisland.action.DEMO_NOOP")
        )
    }

    @Test
    fun componentsDeclaredInManifest() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val classNames = listOf(
            AppComponents.MAIN_ACTIVITY,
            AppComponents.OVERLAY_SERVICE,
            AppComponents.LISTENER_SERVICE,
            AppComponents.TIMER_RECEIVER,
            AppComponents.STOPWATCH_RECEIVER
        )
        for (className in classNames) {
            val relative = className.removePrefix("com.agupta07505.smartisland")
            assertTrue("$className is not declared in AndroidManifest.xml as \"$relative\"",
                manifest.contains("android:name=\"$relative\""))
        }
    }

    @Test
    fun savedEventIdsAreThePinnedOnes() {
        val golden = File("src/test/resources/persisted_event_ids.txt")
            .readLines().filter { it.isNotBlank() }.toSet()
        val actual = IslandEvent.entries.map { it.id }.toSet()
        assertEquals("event ids removed or renamed; saved settings still hold them", emptySet<String>(), golden - actual)
        assertEquals("new event ids missing from persisted_event_ids.txt", emptySet<String>(), actual - golden)
    }
}
