/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.agupta07505.smartisland.util.runCatchingLogged
import org.json.JSONArray

/**
 * Native on-device SQLite helper for persistent, private Notification History.
 */
class NotificationHistoryDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val TAG = "NotificationHistoryDb"
        private const val DATABASE_NAME = "smart_island_notification_history.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_HISTORY = "notification_history"
        const val COL_ID = "_id"
        const val COL_NOTIF_KEY = "notification_key"
        const val COL_PACKAGE = "package_name"
        const val COL_APP_NAME = "app_name"
        const val COL_TITLE = "title"
        const val COL_TEXT = "text"
        const val COL_SUB_TEXT = "sub_text"
        const val COL_POST_TIME = "post_time"
        const val COL_CATEGORY = "category"
        const val COL_CHANNEL_ID = "channel_id"
        const val COL_MODE = "mode"
        const val COL_ACTIONS_JSON = "actions_json"
        const val COL_WAS_OPENED = "was_opened"
        const val COL_WAS_DISMISSED = "was_dismissed"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTIF_KEY TEXT NOT NULL,
                $COL_PACKAGE TEXT NOT NULL,
                $COL_APP_NAME TEXT NOT NULL,
                $COL_TITLE TEXT,
                $COL_TEXT TEXT,
                $COL_SUB_TEXT TEXT,
                $COL_POST_TIME INTEGER NOT NULL,
                $COL_CATEGORY TEXT,
                $COL_CHANNEL_ID TEXT,
                $COL_MODE TEXT NOT NULL,
                $COL_ACTIONS_JSON TEXT,
                $COL_WAS_OPENED INTEGER DEFAULT 0,
                $COL_WAS_DISMISSED INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notif_post_time ON $TABLE_HISTORY ($COL_POST_TIME DESC)")
        db.execSQL("CREATE INDEX idx_notif_pkg ON $TABLE_HISTORY ($COL_PACKAGE)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future database migrations
    }

    fun insertEntry(entry: NotificationHistoryEntry): Long {
        return runCatchingLogged(TAG, "Failed to insert notification history entry") {
            val values = ContentValues().apply {
                put(COL_NOTIF_KEY, entry.notificationKey)
                put(COL_PACKAGE, entry.packageName)
                put(COL_APP_NAME, entry.appName)
                put(COL_TITLE, entry.title)
                put(COL_TEXT, entry.text)
                put(COL_SUB_TEXT, entry.subText)
                put(COL_POST_TIME, entry.postTimeMillis)
                put(COL_CATEGORY, entry.category)
                put(COL_CHANNEL_ID, entry.channelId)
                put(COL_MODE, entry.mode)
                put(COL_ACTIONS_JSON, JSONArray(entry.actionTitles).toString())
                put(COL_WAS_OPENED, if (entry.wasOpened) 1 else 0)
                put(COL_WAS_DISMISSED, if (entry.wasDismissed) 1 else 0)
            }
            writableDatabase.insert(TABLE_HISTORY, null, values)
        } ?: -1L
    }

    fun getAllEntries(limit: Int = 1000): List<NotificationHistoryEntry> {
        return runCatchingLogged(TAG, "Failed to fetch notification history") {
            val list = mutableListOf<NotificationHistoryEntry>()
            val cursor = readableDatabase.query(
                TABLE_HISTORY,
                null,
                null,
                null,
                null,
                null,
                "$COL_POST_TIME DESC",
                limit.toString()
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(cursorToEntry(c))
                }
            }
            list
        } ?: emptyList()
    }

    fun searchEntries(query: String, limit: Int = 200): List<NotificationHistoryEntry> {
        return runCatchingLogged(TAG, "Failed to search notification history") {
            val list = mutableListOf<NotificationHistoryEntry>()
            val searchPattern = "%$query%"
            val cursor = readableDatabase.query(
                TABLE_HISTORY,
                null,
                "$COL_APP_NAME LIKE ? OR $COL_TITLE LIKE ? OR $COL_TEXT LIKE ? OR $COL_PACKAGE LIKE ?",
                arrayOf(searchPattern, searchPattern, searchPattern, searchPattern),
                null,
                null,
                "$COL_POST_TIME DESC",
                limit.toString()
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(cursorToEntry(c))
                }
            }
            list
        } ?: emptyList()
    }

    fun deleteById(id: Long): Int {
        return runCatchingLogged(TAG, "Failed to delete entry $id") {
            writableDatabase.delete(TABLE_HISTORY, "$COL_ID = ?", arrayOf(id.toString()))
        } ?: 0
    }

    fun deleteByPackage(packageName: String): Int {
        return runCatchingLogged(TAG, "Failed to delete entries for package $packageName") {
            writableDatabase.delete(TABLE_HISTORY, "$COL_PACKAGE = ?", arrayOf(packageName))
        } ?: 0
    }

    fun deleteAll(): Int {
        return runCatchingLogged(TAG, "Failed to clear all history") {
            writableDatabase.delete(TABLE_HISTORY, null, null)
        } ?: 0
    }

    fun deleteOlderThan(cutoffMillis: Long): Int {
        return runCatchingLogged(TAG, "Failed to purge old notification history") {
            writableDatabase.delete(TABLE_HISTORY, "$COL_POST_TIME < ?", arrayOf(cutoffMillis.toString()))
        } ?: 0
    }

    fun markAsOpened(notificationKey: String): Int {
        return runCatchingLogged(TAG, "Failed to mark notification as opened") {
            val values = ContentValues().apply {
                put(COL_WAS_OPENED, 1)
            }
            writableDatabase.update(TABLE_HISTORY, values, "$COL_NOTIF_KEY = ?", arrayOf(notificationKey))
        } ?: 0
    }

    private fun cursorToEntry(cursor: Cursor): NotificationHistoryEntry {
        val actionsJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTIONS_JSON))
        val actionsList = runCatching {
            if (!actionsJson.isNullOrBlank()) {
                val jsonArr = JSONArray(actionsJson)
                List(jsonArr.length()) { jsonArr.getString(it) }
            } else emptyList()
        }.getOrElse { emptyList() }

        return NotificationHistoryEntry(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            notificationKey = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_KEY)),
            packageName = cursor.getString(cursor.getColumnIndexOrThrow(COL_PACKAGE)),
            appName = cursor.getString(cursor.getColumnIndexOrThrow(COL_APP_NAME)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)) ?: "",
            text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT)) ?: "",
            subText = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_TEXT)),
            postTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_POST_TIME)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            channelId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_ID)),
            mode = cursor.getString(cursor.getColumnIndexOrThrow(COL_MODE)) ?: "Notification",
            actionTitles = actionsList,
            wasOpened = cursor.getInt(cursor.getColumnIndexOrThrow(COL_WAS_OPENED)) == 1,
            wasDismissed = cursor.getInt(cursor.getColumnIndexOrThrow(COL_WAS_DISMISSED)) == 1
        )
    }
}
