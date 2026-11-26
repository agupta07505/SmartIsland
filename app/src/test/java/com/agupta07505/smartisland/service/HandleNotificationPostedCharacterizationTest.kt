/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Application
import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins what [SmartIslandNotificationListenerService.handleNotificationPosted] does today, so the
 * function can be split without changing it. Each test posts one real [Notification] through the
 * handler and checks the cards that reach the repository.
 *
 * Run: ./gradlew :app:testDebugUnitTest --tests "*Characterization*"
 * A test that fails after a refactor means the refactor changed behaviour; a test that fails
 * before any refactor means the case needs to be adjusted to what the code really does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HandleNotificationPostedCharacterizationTest {

    private class FakeRepository : INotificationRepository {
        private val cards = MutableStateFlow<List<IslandNotification>>(emptyList())
        override val notifications: StateFlow<List<IslandNotification>> = cards
        override val autoExpandEvent: SharedFlow<String> = MutableSharedFlow()
        override val resetTimerEvent: SharedFlow<Unit> = MutableSharedFlow()
        override val commands: SharedFlow<SmartIslandCommand> = MutableSharedFlow()
        val autoExpandFlags = mutableListOf<Boolean>()

        override fun postNotification(notification: IslandNotification, autoExpand: Boolean) {
            autoExpandFlags += autoExpand
            cards.value = cards.value.filterNot { it.key == notification.key } + notification
        }
        override fun removeNotification(key: String) {
            cards.value = cards.value.filterNot { it.key == key }
        }
        override fun removeNotificationsForPackage(packageName: String) {
            cards.value = cards.value.filterNot { it.packageName == packageName }
        }
        override fun removeAllNotifications() { cards.value = emptyList() }
        override fun resetTimer() {}
        override fun sendCommand(command: SmartIslandCommand) {}
    }

    private lateinit var repo: FakeRepository
    private lateinit var service: SmartIslandNotificationListenerService
    private val settings = SmartIslandSettings.Default.copy(enabled = true)

    @Before fun setUp() {
        repo = FakeRepository()
        service = Robolectric.buildService(SmartIslandNotificationListenerService::class.java).get()
        service.notificationRepository = repo
    }

    private fun sbn(
        pkg: String,
        key: String,
        notification: Notification,
        postTime: Long = 1_000L
    ): StatusBarNotification = mockk(relaxed = true) {
        every { packageName } returns pkg
        every { this@mockk.key } returns key
        every { this@mockk.notification } returns notification
        every { this@mockk.postTime } returns postTime
    }

    private fun plain(title: String, text: String): Notification {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .build()
    }

    @Test fun islandsOwnNotificationsAreIgnored() {
        service.handleNotificationPosted(sbn(service.packageName, "own", plain("a", "b")), settings)
        assertTrue(repo.notifications.value.isEmpty())
    }

    @Test fun ordinaryPostMakesOneCard() {
        service.handleNotificationPosted(sbn("com.example.chat", "k1", plain("Ana", "Hello")), settings)
        val cards = repo.notifications.value
        assertEquals(1, cards.size)
        assertEquals("k1", cards[0].key)
        assertEquals("com.example.chat", cards[0].packageName)
        assertEquals("Ana", cards[0].title)
        assertEquals("Hello", cards[0].text)
        assertEquals(IslandMode.Notification, cards[0].mode)
    }

    @Test fun twoNotificationsKeepTheirOwnCards() {
        service.handleNotificationPosted(sbn("com.example.chat", "k1", plain("Ana", "Hello")), settings)
        service.handleNotificationPosted(sbn("com.example.chat", "k2", plain("Ben", "Hi")), settings)
        assertEquals(listOf("k1", "k2"), repo.notifications.value.map { it.key })
    }

    @Test fun sameKeyUpdatesCardInPlace() {
        service.handleNotificationPosted(sbn("com.example.chat", "k1", plain("Ana", "Hello"), postTime = 1_000L), settings)
        service.handleNotificationPosted(sbn("com.example.chat", "k1", plain("Ana", "Hello again"), postTime = 2_000L), settings)
        val cards = repo.notifications.value
        assertEquals(1, cards.size)
        assertEquals("Hello again", cards[0].text)
    }

    @Test fun initialSyncPostNeverAutoExpands() {
        service.handleNotificationPosted(sbn("com.example.chat", "k1", plain("Ana", "Hello")), settings, isInitialSync = true)
        assertEquals(listOf(false), repo.autoExpandFlags)
    }
}
