/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationHistoryDeleteTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun entry(key: String) = NotificationHistoryEntry(
        notificationKey = key, packageName = "p", appName = "a", title = "t", text = "x", postTimeMillis = 1L
    )

    private fun dropTable() {
        NotificationHistoryDbHelper(context).writableDatabase
            .execSQL("DROP TABLE ${NotificationHistoryDbHelper.TABLE_HISTORY}")
    }

    private fun loadedRepository(vararg keys: String): Pair<NotificationHistoryRepository, List<Long>> {
        val helper = NotificationHistoryDbHelper(context)
        val ids = keys.map { helper.insertEntry(entry(it)) }
        val repository = NotificationHistoryRepository(context)
        runBlocking { withTimeout(5_000) { repository.history.first { it.size == keys.size } } }
        return repository to ids
    }

    @Test fun deletingAMissingRowIsNotAFailure() {
        val helper = NotificationHistoryDbHelper(context)
        assertEquals(0, helper.deleteById(999L))
    }

    @Test fun deleteFailureIsDistinctFromNothingDeleted() {
        val helper = NotificationHistoryDbHelper(context)
        dropTable()
        assertEquals(-1, helper.deleteById(1L))
        assertEquals(-1, helper.deleteAll())
    }

    @Test fun deleteEntryRemovesItFromTheList() {
        val (repository, ids) = loadedRepository("a", "b")
        runBlocking { repository.deleteEntry(ids[0]) }
        assertEquals(listOf(ids[1]), repository.history.value.map { it.id })
    }

    @Test fun failedDeleteEntryKeepsItInTheList() {
        val (repository, ids) = loadedRepository("a")
        dropTable()
        runBlocking { repository.deleteEntry(ids[0]) }
        assertEquals(1, repository.history.value.size)
    }

    @Test fun clearAllEmptiesTheList() {
        val (repository, _) = loadedRepository("a", "b")
        runBlocking { repository.clearAll() }
        assertEquals(0, repository.history.value.size)
    }

    @Test fun failedClearAllKeepsTheList() {
        val (repository, _) = loadedRepository("a", "b")
        dropTable()
        runBlocking { repository.clearAll() }
        assertEquals(2, repository.history.value.size)
    }
}
