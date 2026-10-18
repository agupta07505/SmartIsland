package com.agupta07505.smartisland.alert

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {

    private fun alert(
        key: String,
        pkg: String = "com.whatsapp",
        mode: IslandMode = IslandMode.Notification,
        category: String? = "msg",
        title: String = "Sam",
        text: String = "hi",
        time: Long = 0L,
        groupId: String? = null,
        reply: Boolean = false
    ) = IslandNotification(
        key = key,
        packageName = pkg,
        appName = pkg,
        title = title,
        text = text,
        timeMillis = time,
        category = category,
        mode = mode,
        groupId = groupId,
        actionIntents = if (reply) listOf(IslandNotificationAction("Reply", null, isQuickReply = true)) else emptyList()
    )

    @Test
    fun passiveAlertsNeverReachTheIsland() {
        val engine = AlertEngine()
        val result = engine.evaluate(
            listOf(
                alert("promo", category = "promo"),
                alert("likes", pkg = "com.instagram.android", category = "social"),
                alert("update", pkg = "com.android.vending", category = null)
            ),
            nowMs = 0
        )
        assertTrue(result.visible.isEmpty())
        assertNull(result.nextExpiryAtMs)
    }

    @Test
    fun aSocialAlertWithAReplyBoxIsStillAMessage() {
        val engine = AlertEngine()
        val result = engine.evaluate(listOf(alert("dm", pkg = "com.instagram.android", category = "social", reply = true)), 0)
        assertEquals(listOf("dm"), result.visible.map { it.key })
    }

    @Test
    fun uncategorisedAlertsAreShownRatherThanHidden() {
        val engine = AlertEngine()
        assertEquals(1, engine.evaluate(listOf(alert("x", pkg = "com.some.app", category = null)), 0).visible.size)
    }

    @Test
    fun aCallKeepsThePrimaryIslandWhenAMessageArrivesDuringIt() {
        val engine = AlertEngine()
        val call = alert("call", pkg = "com.android.dialer", mode = IslandMode.IncomingCall, category = "call")
        val message = alert("msg", time = 10)
        val result = engine.evaluate(listOf(message, call), 0)
        assertEquals(listOf("call", "msg"), result.visible.map { it.key })
    }

    @Test
    fun criticalAlertsOutrankCallsAndMessages() {
        val engine = AlertEngine()
        val alarm = alert("alarm", pkg = "com.clock", category = "alarm")
        val call = alert("call", pkg = "com.android.dialer", mode = IslandMode.IncomingCall, category = "call")
        val result = engine.evaluate(listOf(alert("msg"), call, alarm), 0)
        assertEquals(listOf("alarm", "call", "msg"), result.visible.map { it.key })
    }

    @Test
    fun batteryIsCriticalOnlyAtFivePercentOrLessAndNotWhileCharging() {
        fun battery(title: String, text: String) =
            alert("b", pkg = "android", mode = IslandMode.Battery, category = "battery_low", title = title, text = text)
        assertEquals(AlertPriority.Critical, AlertClassifier.classify(battery("Low Battery", "5%")))
        assertEquals(AlertPriority.Critical, AlertClassifier.classify(battery("Low Battery", "3%")))
        assertEquals(AlertPriority.Live, AlertClassifier.classify(battery("Low Battery", "6%")))
        assertEquals(AlertPriority.Live, AlertClassifier.classify(battery("Charging", "4%")))
    }

    @Test
    fun messagesFromTheSameAppAreFoldedIntoOneCountedEntry() {
        val engine = AlertEngine()
        val result = engine.evaluate(
            listOf(alert("a", time = 1), alert("b", time = 2), alert("c", time = 3, text = "latest")),
            0
        )
        assertEquals(1, result.visible.size)
        val group = result.visible.single()
        assertEquals(3, group.groupCount)
        assertEquals("c", group.key)
        assertEquals("latest", group.text)
        assertEquals(listOf("a", "b", "c"), group.memberKeys)
    }

    @Test
    fun differentAppsOrGroupsAreNotFolded() {
        val engine = AlertEngine()
        val result = engine.evaluate(
            listOf(
                alert("w1", pkg = "com.whatsapp", time = 1),
                alert("s1", pkg = "com.Slack", time = 2),
                alert("w2", pkg = "com.whatsapp", time = 3, groupId = "work")
            ),
            0
        )
        assertEquals(3, result.visible.size)
        assertTrue(result.visible.all { it.groupCount == 1 })
    }

    @Test
    fun aMessageLeavesTheIslandAfterExactlySevenSeconds() {
        val engine = AlertEngine()
        val raw = listOf(alert("m"))
        val first = engine.evaluate(raw, 1_000)
        assertEquals(1, first.visible.size)
        assertEquals(8_000L, first.nextExpiryAtMs)
        assertEquals(1, engine.evaluate(raw, 7_999).visible.size)
        assertTrue(engine.evaluate(raw, 8_000).visible.isEmpty())
        // Once gone it stays gone, even though the notification is still in the shade.
        assertTrue(engine.evaluate(raw, 20_000).visible.isEmpty())
    }

    @Test
    fun touchingTheIslandRestartsTheSevenSeconds() {
        val engine = AlertEngine()
        val raw = listOf(alert("m"))
        engine.evaluate(raw, 0)
        engine.touch(5_000)
        assertEquals(1, engine.evaluate(raw, 11_999).visible.size)
        assertEquals(12_000L, engine.evaluate(raw, 11_999).nextExpiryAtMs)
        assertTrue(engine.evaluate(raw, 12_000).visible.isEmpty())
    }

    @Test
    fun aNewMessageInAGroupRestartsTheGroupsClock() {
        val engine = AlertEngine()
        engine.evaluate(listOf(alert("a", time = 1)), 0)
        val raw = listOf(alert("a", time = 1), alert("b", time = 2))
        val atFive = engine.evaluate(raw, 5_000)
        assertEquals(2, atFive.visible.single().groupCount)
        assertEquals(12_000L, atFive.nextExpiryAtMs)
        assertEquals(1, engine.evaluate(raw, 11_999).visible.size)
        assertTrue(engine.evaluate(raw, 12_000).visible.isEmpty())
    }

    @Test
    fun liveTasksAndAlarmsPersistUntilTheyAreRemoved() {
        val engine = AlertEngine()
        val raw = listOf(
            alert("call", pkg = "com.android.dialer", mode = IslandMode.IncomingCall, category = "call"),
            alert("alarm", pkg = "com.clock", category = "alarm")
        )
        engine.evaluate(raw, 0)
        val later = engine.evaluate(raw, 3_600_000)
        assertEquals(listOf("alarm", "call"), later.visible.map { it.key })
        assertNull(later.nextExpiryAtMs)
    }

    @Test
    fun anExpiredMessageStopsBeingRememberedOnceItLeavesTheShade() {
        val engine = AlertEngine()
        engine.evaluate(listOf(alert("m")), 0)
        assertTrue(engine.evaluate(listOf(alert("m")), 8_000).visible.isEmpty())
        engine.evaluate(emptyList(), 9_000)
        // The same key posted again later is a fresh alert.
        assertEquals(1, engine.evaluate(listOf(alert("m")), 10_000).visible.size)
    }
}
