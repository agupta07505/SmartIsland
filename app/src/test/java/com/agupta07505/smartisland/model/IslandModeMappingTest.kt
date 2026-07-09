/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.Notification
import com.agupta07505.smartisland.util.toIslandMode
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class IslandModeMappingTest {

    @Test
    fun testCategoryCallMapsToIncomingCall() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_CALL
        notification.actions = null

        assertEquals(IslandMode.IncomingCall, notification.toIslandMode())
    }

    @Test
    fun testCategoryMissedCallMapsToIncomingCall() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MISSED_CALL
        notification.actions = null

        assertEquals(IslandMode.IncomingCall, notification.toIslandMode())
    }

    @Test
    fun testCategoryTransportMapsToMusic() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_TRANSPORT
        notification.actions = null

        assertEquals(IslandMode.Music, notification.toIslandMode())
    }

    @Test
    fun testCategoryProgressMapsToMusic() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_PROGRESS
        notification.actions = null

        assertEquals(IslandMode.Music, notification.toIslandMode())
    }

    @Test
    fun testMediaActionsMapsToMusic() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE
        
        val playAction = mockk<Notification.Action>()
        playAction.title = "Play track"
        
        notification.actions = arrayOf(playAction)

        assertEquals(IslandMode.Music, notification.toIslandMode())
    }

    @Test
    fun testGenericNotificationMapsToNotification() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE
        notification.actions = null

        assertEquals(IslandMode.Notification, notification.toIslandMode())
    }
}
