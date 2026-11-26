/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModelAutoExpandTest : IslandViewModelTestBase() {

    private fun SmartIslandNotificationRepository.postAndDeliver(
        notification: IslandNotification,
        autoExpand: Boolean = false
    ) {
        postNotification(notification, autoExpand)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun autoExpandOpensOnNew() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true)
        )
        testDispatcher.scheduler.runCurrent()

        val notif = IslandNotification(
            key = "msg_1",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alice",
            text = "Hello!",
            mode = IslandMode.Notification,
            timeMillis = clockMs
        )

        notifRepo.postAndDeliver(notif, autoExpand = true)

        assertTrue(viewModel.expanded.value)
        assertEquals(0, viewModel.selectedIndex.value)
    }

    @Test
    fun disabledNotificationAutoExpand() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = false)
        )
        testDispatcher.scheduler.runCurrent()

        val notif = IslandNotification(
            key = "msg_2",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Bob",
            text = "Hey there",
            mode = IslandMode.Notification,
            timeMillis = clockMs
        )

        notifRepo.postAndDeliver(notif, autoExpand = true)

        // should not be expanded because autoExpandOnNotification is false
        Assert.assertFalse(viewModel.expanded.value)
        // notification is still captured and selected for compact pill
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertEquals(0, viewModel.selectedIndex.value)
    }

    @Test
    fun autoExpandSparesTyping() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postAndDeliver(chat("a"))
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postAndDeliver(chat("b"), autoExpand = true)
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        // an open island is never interrupted
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun autoExpandKeepsOpenIsland() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true)
        )
        notifRepo.postAndDeliver(chat("a"))
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        notifRepo.postAndDeliver(chat("b"), autoExpand = true)
        assertTrue(viewModel.expanded.value)
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun ringingCallStillTakesAnOpenIsland() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true)
        )
        notifRepo.postAndDeliver(chat("a"))
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        notifRepo.postAndDeliver(call(ringing = true), autoExpand = true)
        assertEquals("call", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun callInterruptsReply() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postAndDeliver(chat("a"))
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postAndDeliver(call(ringing = true), autoExpand = true)
        assertEquals("call", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
        // the reply box is disposed when the card changes
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()
        assertEquals("call", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        // answered: the call is ongoing now and the answer button collapsed the island
        notifRepo.postNotification(call(ringing = false))
        viewModel.collapse()
        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
        assertEquals("a", viewModel.replyDrafts.resume.value)

        // the card opens its box: typing is active again and the request is used up
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        assertEquals(null, viewModel.replyDrafts.resume.value)
    }

    @Test
    fun typingResumesWhenTheCallIsDeclined() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postAndDeliver(chat("a"))
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        notifRepo.postAndDeliver(call(ringing = true), autoExpand = true)
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()

        notifRepo.removeNotification("call")
        viewModel.collapse()
        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals("a", viewModel.replyDrafts.resume.value)
    }

    @Test
    fun typingWaitsForRingingCall() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postAndDeliver(chat("a"))
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        notifRepo.postAndDeliver(call(ringing = true), autoExpand = true)
        viewModel.setInputActive(false)
        viewModel.collapse()
        testDispatcher.scheduler.advanceTimeBy(5_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertEquals(null, viewModel.replyDrafts.resume.value)
    }

    @Test
    fun dismissedReplyTarget() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postAndDeliver(chat("a"))
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        notifRepo.postAndDeliver(call(ringing = true), autoExpand = true)
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()

        notifRepo.removeNotification("a")
        notifRepo.removeNotification("call")
        viewModel.collapse()
        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertEquals(null, viewModel.replyDrafts.resume.value)
    }

    // auto-expand rules


    private fun batteryCard(category: String) = IslandNotification(
        key = "system_battery", packageName = "com.android.systemui", appName = "System", title = "Plugged in",
        text = "65%", category = category, mode = IslandMode.Battery, timeMillis = 10L
    )

    @Test
    fun chargerEndKeepsOlderClosed() = runTest {
        // the recording: an old notification sits in the stack
        // the charger card opens the island only when its switch is on (off by default)
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true, noAutoExpandEvents = emptySet())
        )
        notifRepo.postAndDeliver(chat("old"))
        letPulseFinish()

        notifRepo.postAndDeliver(batteryCard("battery_plugged_in"), autoExpand = true)
        assertTrue(viewModel.expanded.value)
        assertEquals("system_battery", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        notifRepo.removeNotification("system_battery")
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
    }

    @Test
    fun readCardStaysWhenOtherEnds() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a"))
        notifRepo.postAndDeliver(batteryCard("battery_plugged_in"))
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        viewModel.selectedIndex.value = viewModel.visibleNotifications.value.indexOfFirst { it.key == "a" }
        testDispatcher.scheduler.runCurrent()
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        notifRepo.removeNotification("system_battery")
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun defaultExpandsLoudOnly() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true)
        )
        testDispatcher.scheduler.runCurrent()

        notifRepo.postAndDeliver(appCard("quiet", "com.news", silent = true), autoExpand = true)
        assertFalse(viewModel.expanded.value)

        letPulseFinish()
        notifRepo.postAndDeliver(appCard("loud", "com.news", silent = false), autoExpand = true)
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun appSwitchedOnExpandsSilentOffNever() = runTest {
        val (onVm, onNotifs) = newViewModel(
            SmartIslandSettings(autoExpandPackages = setOf("com.news"))
        )
        testDispatcher.scheduler.runCurrent()
        onNotifs.postNotification(appCard("quiet", "com.news", silent = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(onVm.expanded.value)

        val (offVm, offNotifs) = newViewModel(
            SmartIslandSettings(noAutoExpandPackages = setOf("com.news"))
        )
        testDispatcher.scheduler.runCurrent()
        offNotifs.postNotification(appCard("loud", "com.news", silent = false), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertFalse(offVm.expanded.value)
    }

    @Test
    fun expandOffStaysInPill() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(noAutoExpandEvents = setOf("battery_plugged_in"))
        )
        testDispatcher.scheduler.runCurrent()

        notifRepo.postAndDeliver(batteryCard("battery_plugged_in"), autoExpand = true)
        assertFalse(viewModel.expanded.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // another event is not affected
        letPulseFinish()
        notifRepo.postAndDeliver(batteryCard("battery_full").copy(key = "system_full"), autoExpand = true)
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun chargerDefaultOnlyPulses() = runTest {
        val (viewModel, notifRepo) = newViewModel(SmartIslandSettings())
        testDispatcher.scheduler.runCurrent()

        notifRepo.postAndDeliver(batteryCard("battery_plugged_in"), autoExpand = true)
        assertFalse(viewModel.expanded.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // low battery is one of the 5 that open the island by default
        letPulseFinish()
        notifRepo.postAndDeliver(batteryCard("battery_low").copy(key = "system_low"), autoExpand = true)
        assertTrue(viewModel.expanded.value)
    }

    private fun timerCard(finished: Boolean) = IslandNotification(
        key = "timer", packageName = "com.clock", appName = "Timer", title = "Timer",
        text = if (finished) "Time's up" else "Running", mode = IslandMode.Timer, timeMillis = 10L,
        quietArrival = true, timerFinished = finished
    )

    private fun runningThenFinished(settings: SmartIslandSettings): IslandViewModel {
        val (viewModel, notifRepo) = newViewModel(settings)
        testDispatcher.scheduler.runCurrent()
        // the island's own timer: quiet while it runs, the end asks to open
        notifRepo.postAndDeliver(timerCard(finished = false), autoExpand = false)
        assertFalse(viewModel.expanded.value)
        assertNull(viewModel.collapsedPulse.value.pulsing)
        notifRepo.postAndDeliver(timerCard(finished = true), autoExpand = true)
        return viewModel
    }

    @Test
    fun finishedTimerOpensByDefault() = runTest {
        assertTrue(runningThenFinished(SmartIslandSettings()).expanded.value)
    }

    @Test
    fun finishedTimerOpensSwitchOff() = runTest {
        val viewModel = runningThenFinished(SmartIslandSettings(noAutoExpandEvents = setOf(IslandEvent.TIMER.id)))
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun timerSwitchOffOnlyPulses() = runTest {
        val viewModel = runningThenFinished(
            SmartIslandSettings(noAutoExpandEvents = setOf(IslandEvent.TIMER_FINISHED.id))
        )
        assertFalse(viewModel.expanded.value)
        assertEquals("timer", viewModel.collapsedPulse.value.pulsing?.id)
    }

    @Test
    fun ringingCallAutoExpand() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(
                noAutoExpandEvents = IslandEvent.entries.map { it.id }.toSet() - IslandEvent.CALL.id
            )
        )
        testDispatcher.scheduler.runCurrent()
        notifRepo.postAndDeliver(call(ringing = true), autoExpand = true)
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun portraitCallPulseOverride() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(noAutoExpandEvents = setOf(IslandEvent.CALL.id))
        )
        testDispatcher.scheduler.runCurrent()
        notifRepo.postAndDeliver(call(ringing = true), autoExpand = true)

        assertFalse(viewModel.expanded.value)
        assertEquals("call", viewModel.collapsedPulse.value.pulsing?.id)
        assertEquals(listOf("call"), viewModel.visibleNotifications.value.map { it.key })
    }

    @Test
    fun landscapeRingingSwitchOff() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(noAutoExpandEventsLandscape = setOf(IslandEvent.CALL.id))
        )
        viewModel.orientation.value = OrientationState(isLandscape = true)
        testDispatcher.scheduler.runCurrent()
        notifRepo.postAndDeliver(call(ringing = true), autoExpand = true)

        assertFalse(viewModel.expanded.value)
        assertEquals("call", viewModel.collapsedPulse.value.pulsing?.id)
        assertEquals(listOf("call"), viewModel.visibleNotifications.value.map { it.key })
    }

    @Test
    fun timerStopwatchSelectNoExpand() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(
                autoExpandOnNotification = false
            )
        )
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(
            IslandNotification(
                key = "existing",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Existing notification",
                text = "Hello",
                timeMillis = clockMs,
                mode = IslandMode.Notification
            )
        )
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(
            IslandNotification(
                key = "timer",
                packageName = "com.android.deskclock",
                appName = "Clock",
                title = "Timer",
                text = "00:10",
                timeMillis = clockMs + 10_000L,
                mode = IslandMode.Timer
            ),
            autoExpand = true
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.selectedIndex.value)
        Assert.assertFalse(viewModel.expanded.value)

        notifRepo.postNotification(
            IslandNotification(
                key = "stopwatch",
                packageName = "com.android.deskclock",
                appName = "Clock",
                title = "Stopwatch running",
                text = "",
                timeMillis = clockMs,
                mode = IslandMode.Stopwatch
            ),
            autoExpand = true
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.selectedIndex.value)
        Assert.assertFalse(viewModel.expanded.value)
    }

    // tap the pulsing pill to expand


    private fun pulsingViewModel(): Pair<IslandViewModel, SmartIslandNotificationRepository> {
        val viewModelAndRepo = newViewModel(SmartIslandSettings())
        testDispatcher.scheduler.runCurrent()
        return viewModelAndRepo
    }

    @Test
    fun pulseTapDrainsQueue() = runTest {
        val (viewModel, notifRepo) = pulsingViewModel()
        notifRepo.postNotification(batteryCard("battery_plugged_in"), autoExpand = true)
        notifRepo.postAndDeliver(batteryCard("battery_full").copy(key = "system_full"), autoExpand = true)
        assertFalse(viewModel.expanded.value)
        assertEquals("system_battery|battery_plugged_in", viewModel.collapsedPulse.value.pulsing?.id)

        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.expanded.value)
        assertEquals("system_battery", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
        assertFalse(viewModel.collapsedPulse.value.busy)
        assertEquals(2, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun dragAndSetExpandedInPulse() = runTest {
        val (viewModel, notifRepo) = pulsingViewModel()
        notifRepo.postAndDeliver(batteryCard("battery_plugged_in"), autoExpand = true)
        viewModel.setExpanded(true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        viewModel.setExpanded(false)
        testDispatcher.scheduler.runCurrent()

        val (second, secondRepo) = pulsingViewModel()
        secondRepo.postNotification(batteryCard("battery_plugged_in"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(second.dragExpand(true))
        assertTrue(second.expanded.value)
    }

    @Test
    fun tapOnRemovedQuickCardOpens() = runTest {
        val (viewModel, notifRepo) = pulsingViewModel()
        notifRepo.postNotification(batteryCard("battery_plugged_in"), autoExpand = true)
        notifRepo.postAndDeliver(batteryCard("battery_full").copy(key = "system_full"), autoExpand = true)
        // the monitor takes the pulsing card away before its turn ends
        notifRepo.removeNotification("system_battery")
        testDispatcher.scheduler.runCurrent()

        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.expanded.value)
        assertTrue(viewModel.visibleNotifications.value.any { it.key == "system_battery" })
        assertEquals("system_battery", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun tapNoCardsNoEmptyIsland() = runTest {
        val (viewModel, _) = pulsingViewModel()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
    }
}
