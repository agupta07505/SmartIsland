/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmartIslandNotificationRepositoryTest {

    @Test
    fun testPostNotification() {
        val repository = SmartIslandNotificationRepository()
        val notif = IslandNotification(
            key = "test_key",
            packageName = "com.test",
            appName = "TestApp",
            title = "Test Title",
            text = "Test Text",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )

        repository.postNotification(notif, autoExpand = false)
        val notifications = repository.notifications.value
        assertEquals(1, notifications.size)
        assertEquals("test_key", notifications[0].key)
    }

    @Test
    fun testRemoveNotification() {
        val repository = SmartIslandNotificationRepository()
        val notif1 = IslandNotification(
            key = "key1",
            packageName = "com.test",
            appName = "TestApp",
            title = "Title 1",
            text = "Text 1",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )
        val notif2 = IslandNotification(
            key = "key2",
            packageName = "com.test",
            appName = "TestApp",
            title = "Title 2",
            text = "Text 2",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )

        repository.postNotification(notif1)
        repository.postNotification(notif2)
        assertEquals(2, repository.notifications.value.size)

        repository.removeNotification("key1")
        val notifications = repository.notifications.value
        assertEquals(1, notifications.size)
        assertEquals("key2", notifications[0].key)
    }

    @Test
    fun testRemoveAllNotifications() {
        val repository = SmartIslandNotificationRepository()
        val notif1 = IslandNotification(
            key = "key1",
            packageName = "com.test",
            appName = "TestApp",
            title = "Title 1",
            text = "Text 1",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )
        val notif2 = IslandNotification(
            key = "key2",
            packageName = "com.test",
            appName = "TestApp",
            title = "Title 2",
            text = "Text 2",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )

        repository.postNotification(notif1)
        repository.postNotification(notif2)
        assertEquals(2, repository.notifications.value.size)

        repository.removeAllNotifications()
        assertTrue(repository.notifications.value.isEmpty())
    }

    @Test
    fun testRemoveNotificationsForPackage() {
        val repository = SmartIslandNotificationRepository()
        val notif1 = IslandNotification(
            key = "key1",
            packageName = "com.app.a",
            appName = "App A",
            title = "Title 1",
            text = "Text 1",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )
        val notif2 = IslandNotification(
            key = "key2",
            packageName = "com.app.a",
            appName = "App A",
            title = "Title 2",
            text = "Text 2",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )
        val notif3 = IslandNotification(
            key = "key3",
            packageName = "com.app.b",
            appName = "App B",
            title = "Title 3",
            text = "Text 3",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )

        repository.postNotification(notif1)
        repository.postNotification(notif2)
        repository.postNotification(notif3)
        assertEquals(3, repository.notifications.value.size)

        repository.removeNotificationsForPackage("com.app.a")
        val remaining = repository.notifications.value
        assertEquals(1, remaining.size)
        assertEquals("key3", remaining[0].key)
    }

    @Test
    fun testResetTimer() = runTest {
        val repository = SmartIslandNotificationRepository()
        var triggered = false

        val job = launch {
            repository.resetTimerEvent.collect {
                triggered = true
            }
        }

        // Wait using virtual time to allow coroutine collection setup
        delay(100)

        repository.resetTimer()
        advanceUntilIdle()
        assertTrue(triggered)
        job.cancel()
    }

    @Test
    fun testSendCommand() = runTest {
        val repository = SmartIslandNotificationRepository()
        val receivedCommands = mutableListOf<SmartIslandCommand>()

        val job = launch {
            repository.commands.collect {
                receivedCommands.add(it)
            }
        }

        // Wait using virtual time to allow coroutine collection setup
        delay(100)

        val command = SmartIslandCommand.CancelNotification("key_to_cancel")
        repository.sendCommand(command)
        advanceUntilIdle()

        assertEquals(1, receivedCommands.size)
        assertEquals(command, receivedCommands[0])
        job.cancel()
    }

    @Test
    fun keepsOnlyTheMostRecentFiftyNotifications() {
        val repository = SmartIslandNotificationRepository()

        repeat(60) { index ->
            repository.postNotification(
                IslandNotification(
                    key = "key_$index",
                    packageName = "com.test",
                    appName = "TestApp",
                    title = "Title $index",
                    text = "Text $index",
                    mode = IslandMode.Notification,
                    timeMillis = index.toLong()
                )
            )
        }

        val notifications = repository.notifications.value
        assertEquals(50, notifications.size)
        assertEquals("key_10", notifications.first().key)
        assertEquals("key_59", notifications.last().key)
    }

    @Test
    fun openingAnAppOnlyDropsItsOrdinaryAlerts() {
        val repository = SmartIslandNotificationRepository()
        fun post(key: String, mode: IslandMode) = repository.postNotification(
            IslandNotification(
                key = key,
                packageName = "com.clock",
                appName = "Clock",
                title = key,
                text = key,
                mode = mode,
                timeMillis = 0L
            )
        )
        post("alert", IslandMode.Notification)
        post("timer", IslandMode.Timer)
        post("stopwatch", IslandMode.Stopwatch)
        post("recorder", IslandMode.AudioRecording)
        post("screen", IslandMode.ScreenRecording)
        post("call", IslandMode.IncomingCall)
        post("download", IslandMode.DownloadUpload)
        post("nav", IslandMode.Navigation)

        repository.removeNotificationsForPackage("com.clock")

        assertEquals(
            listOf("timer", "stopwatch", "recorder", "screen", "call", "download", "nav"),
            repository.notifications.value.map { it.key }
        )
    }

    @Test
    fun openingAnAppOnlyCancelsTheOrdinaryAlertInTheShade() = runTest {
        val repository = SmartIslandNotificationRepository()
        val received = mutableListOf<SmartIslandCommand>()
        val job = launch { repository.commands.collect { received.add(it) } }
        delay(100)

        fun post(key: String, mode: IslandMode) = repository.postNotification(
            IslandNotification(
                key = key, packageName = "com.rec", appName = "Rec", title = key,
                text = key, mode = mode, timeMillis = 0L
            )
        )
        post("alert", IslandMode.Notification)
        post("recording", IslandMode.AudioRecording)

        repository.removeNotificationsForPackage("com.rec")
        advanceUntilIdle()

        assertEquals(listOf<SmartIslandCommand>(SmartIslandCommand.CancelNotification("alert")), received)
        job.cancel()
    }

    @Test
    fun testRemoveNotificationsForPackageKeepsTimerAndStopwatch() {
        val repository = SmartIslandNotificationRepository()
        val now = System.currentTimeMillis()

        val regular = IslandNotification(
            key = "regular",
            packageName = "com.android.deskclock",
            appName = "Clock",
            title = "Clock notification",
            text = "Reminder",
            mode = IslandMode.Notification,
            timeMillis = now
        )

        val timer = IslandNotification(
            key = "timer",
            packageName = "com.android.deskclock",
            appName = "Clock",
            title = "Timer",
            text = "Running",
            mode = IslandMode.Timer,
            timeMillis = now + 60_000L
        )

        val stopwatch = IslandNotification(
            key = "stopwatch",
            packageName = "com.android.deskclock",
            appName = "Clock",
            title = "Stopwatch running",
            text = "Running",
            mode = IslandMode.Stopwatch,
            timeMillis = now
        )

        repository.postNotification(regular)
        repository.postNotification(timer)
        repository.postNotification(stopwatch)

        repository.removeNotificationsForPackage("com.android.deskclock")

        val remainingKeys = repository.notifications.value.map { it.key }.toSet()

        assertEquals(setOf("timer", "stopwatch"), remainingKeys)
    }
}
