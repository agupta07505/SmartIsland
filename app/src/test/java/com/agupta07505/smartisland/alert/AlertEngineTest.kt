package com.agupta07505.smartisland.alert

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.LockscreenVisibility
import org.junit.Assert.assertFalse
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
        reply: Boolean = false,
        visibility: Int = LockscreenVisibility.PUBLIC
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
        lockscreenVisibility = visibility,
        actionIntents = if (reply) listOf(IslandNotificationAction("Reply", null, isQuickReply = true)) else emptyList()
    )

    private fun call(key: String = "call") =
        alert(key, pkg = "com.android.dialer", mode = IslandMode.IncomingCall, category = "call")

    private fun music(key: String = "music") =
        alert(key, pkg = "com.spotify.music", mode = IslandMode.Music, category = "transport")

    private fun timer(key: String = "timer") =
        alert(key, pkg = "com.clock", mode = IslandMode.Timer, category = null)

    private fun keys(e: AlertEngine.Evaluation) = e.visible.map { it.key }

    // ---- Tiers ----

    @Test
    fun tiersFollowTheRulebook() {
        fun tier(mode: IslandMode, category: String? = null) = AlertClassifier.classify(alert("k", mode = mode, category = category))
        listOf(
            IslandMode.IncomingCall, IslandMode.Navigation, IslandMode.ScreenRecording, IslandMode.Timer, IslandMode.Stopwatch,
            IslandMode.VoiceRecording, IslandMode.PrivacyIndicator
        ).forEach { assertEquals(it.name, AlertTier.Persistent, tier(it)) }
        assertEquals(AlertTier.Critical, tier(IslandMode.Alarm, "alarm"))
        listOf(IslandMode.Music, IslandMode.LiveActivity, IslandMode.Hotspot, IslandMode.DownloadUpload)
            .forEach { assertEquals(it.name, AlertTier.Background, tier(it)) }
        listOf(IslandMode.Battery, IslandMode.Bluetooth, IslandMode.Flashlight)
            .forEach { assertEquals(it.name, AlertTier.Ephemeral, tier(it, "battery_low")) }
        assertEquals(AlertTier.Ephemeral, tier(IslandMode.Notification, "msg"))
        assertEquals(AlertTier.Critical, tier(IslandMode.Timer, "alarm"))
    }

    @Test
    fun aTimerThatReachesZeroIsPromotedToCritical() {
        fun timerTier(title: String, text: String) =
            AlertClassifier.classify(alert("t", pkg = "com.clock", mode = IslandMode.Timer, category = null, title = title, text = text))
        assertEquals(AlertTier.Persistent, timerTier("Timer", "4:59"))
        assertEquals(AlertTier.Persistent, timerTier("Timer", "1:00:00"))
        assertEquals(AlertTier.Persistent, timerTier("Timer", "10:00"))
        assertEquals(AlertTier.Critical, timerTier("Timer", "00:00"))
        assertEquals(AlertTier.Critical, timerTier("Timer", "0:00:00"))
        assertEquals(AlertTier.Critical, timerTier("Time's up", "Pasta"))
        // Once Critical it outranks a call.
        val ringing = alert("ringing", pkg = "com.clock", mode = IslandMode.Timer, category = null, title = "Timer", text = "0:00")
        assertEquals(listOf("ringing", "call"), keys(AlertEngine().evaluate(listOf(call(), ringing), 0)))
    }

    @Test
    fun batteryIsCriticalOnlyAtFivePercentOrLessAndNotWhileCharging() {
        fun battery(title: String, text: String) =
            alert("b", pkg = "android", mode = IslandMode.Battery, category = "battery_low", title = title, text = text)
        assertEquals(AlertTier.Critical, AlertClassifier.classify(battery("Low Battery", "5%")))
        assertEquals(AlertTier.Critical, AlertClassifier.classify(battery("Low Battery", "3%")))
        assertEquals(AlertTier.Ephemeral, AlertClassifier.classify(battery("Low Battery", "6%")))
        assertEquals(AlertTier.Ephemeral, AlertClassifier.classify(battery("Charging", "4%")))
    }

    @Test
    fun passiveAlertsNeverReachTheIsland() {
        val result = AlertEngine().evaluate(
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
        val result = AlertEngine().evaluate(listOf(alert("dm", pkg = "com.instagram.android", category = "social", reply = true)), 0)
        assertEquals(listOf("dm"), keys(result))
    }

    @Test
    fun uncategorisedAlertsAreShownRatherThanHidden() {
        assertEquals(1, AlertEngine().evaluate(listOf(alert("x", pkg = "com.some.app", category = null)), 0).visible.size)
    }

    // ---- Priority and the two slots ----

    @Test
    fun aPersistentTaskKeepsTheMainPillWhenAMessageArrives() {
        val result = AlertEngine().evaluate(listOf(alert("msg", time = 10), call()), 0)
        assertEquals(listOf("call", "msg"), keys(result))
    }

    @Test
    fun criticalOutranksPersistentWhichOutranksMessages() {
        val alarm = alert("alarm", pkg = "com.clock", category = "alarm")
        val result = AlertEngine().evaluate(listOf(alert("msg"), call(), alarm), 0)
        assertEquals(listOf("alarm", "call"), keys(result))
    }

    @Test
    fun aMessageHijacksMusicForSevenSecondsThenMusicReturns() {
        val engine = AlertEngine()
        val raw = listOf(music(), alert("msg"))
        engine.evaluate(listOf(music()), 0)
        val during = engine.evaluate(raw, 1_000)
        assertEquals(listOf("msg", "music"), keys(during))
        assertEquals(8_000L, during.nextExpiryAtMs)
        assertEquals(listOf("msg", "music"), keys(engine.evaluate(raw, 7_999)))
        assertEquals(listOf("music"), keys(engine.evaluate(raw, 8_000)))
    }

    @Test
    fun onlyTwoStatesAreShownAndTheLowestPriorityIsHidden() {
        val engine = AlertEngine()
        val raw = listOf(music(), timer(), call())
        engine.evaluate(raw, 0)
        // call (main), timer (companion); music is hidden but still alive in the shade.
        assertEquals(listOf("call", "timer"), keys(engine.evaluate(raw, 100)))
        // When the call ends, the hidden music takes the freed slot.
        assertEquals(listOf("timer", "music"), keys(engine.evaluate(listOf(music(), timer()), 200)))
    }

    @Test
    fun theMostRecentlyStartedPersistentTaskTakesTheMainPill() {
        val engine = AlertEngine()
        engine.evaluate(listOf(timer("timer")), 0)
        val rec = alert("rec", pkg = "com.rec", mode = IslandMode.ScreenRecording, category = null)
        assertEquals(listOf("rec", "timer"), keys(engine.evaluate(listOf(timer("timer"), rec), 5_000)))
    }

    @Test
    fun anIncomingCallBeatsOlderAndNewerPersistentTasks() {
        val engine = AlertEngine()
        engine.evaluate(listOf(call("call")), 0)
        val timerLater = engine.evaluate(listOf(call("call"), timer("timer")), 5_000)
        assertEquals("call", timerLater.visible.first().key)
    }

    @Test
    fun backgroundTasksStayOrderedNewestFirst() {
        val engine = AlertEngine()
        val hotspot = alert("hot", pkg = "x", mode = IslandMode.Hotspot, category = null)
        engine.evaluate(listOf(music()), 0)
        assertEquals(listOf("hot", "music"), keys(engine.evaluate(listOf(music(), hotspot), 3_000)))
    }

    // ---- Ephemeral timing ----

    @Test
    fun aMessageLeavesTheIslandAfterExactlySevenSeconds() {
        val engine = AlertEngine()
        val raw = listOf(alert("m"))
        val first = engine.evaluate(raw, 1_000)
        assertEquals(8_000L, first.nextExpiryAtMs)
        assertEquals(1, engine.evaluate(raw, 7_999).visible.size)
        assertTrue(engine.evaluate(raw, 8_000).visible.isEmpty())
        // Once gone it stays gone, even though the notification is still in the shade.
        assertTrue(engine.evaluate(raw, 20_000).visible.isEmpty())
    }

    @Test
    fun touchingTheIslandRestartsTheClock() {
        val engine = AlertEngine()
        val raw = listOf(alert("m"))
        engine.evaluate(raw, 0)
        engine.touch(5_000)
        assertEquals(12_000L, engine.evaluate(raw, 11_999).nextExpiryAtMs)
        assertEquals(1, engine.evaluate(raw, 11_999).visible.size)
        assertTrue(engine.evaluate(raw, 12_000).visible.isEmpty())
    }

    @Test
    fun batteryBluetoothAndFlashlightInterruptForFiveSeconds() {
        listOf(IslandMode.Battery, IslandMode.Bluetooth, IslandMode.Flashlight).forEach { mode ->
            val engine = AlertEngine()
            val raw = listOf(alert("x", pkg = "android", mode = mode, category = "battery_low", title = "State", text = "50%"))
            assertEquals(mode.name, 1, engine.evaluate(raw, 0).visible.size)
            assertEquals(mode.name, 1, engine.evaluate(raw, 4_999).visible.size)
            assertTrue(mode.name, engine.evaluate(raw, 5_000).visible.isEmpty())
        }
    }

    @Test
    fun aNewBatteryStateInterruptsAgainButAPercentTickDoesNot() {
        val engine = AlertEngine()
        fun battery(title: String, text: String) =
            listOf(alert("b", pkg = "android", mode = IslandMode.Battery, category = "battery_charging", title = title, text = text))
        engine.evaluate(battery("Charging", "80%"), 0)
        assertTrue(engine.evaluate(battery("Charging", "81%"), 6_000).visible.isEmpty())
        assertEquals(1, engine.evaluate(battery("Low Battery", "15%"), 7_000).visible.size)
    }

    @Test
    fun criticalBatteryPersistsAndOutranksACall() {
        val engine = AlertEngine()
        val low = alert("b", pkg = "android", mode = IslandMode.Battery, category = "battery_low", title = "Low Battery", text = "5%")
        engine.evaluate(listOf(low, call()), 0)
        val later = engine.evaluate(listOf(low, call()), 3_600_000)
        assertEquals(listOf("b", "call"), keys(later))
        assertNull(later.nextExpiryAtMs)
    }

    // ---- Grouping ----

    @Test
    fun messagesFromTheSameAppAreFoldedIntoOneCountedEntry() {
        val result = AlertEngine().evaluate(
            listOf(alert("a", time = 1), alert("b", time = 2), alert("c", time = 3, text = "latest")),
            0
        )
        val group = result.visible.single()
        assertEquals(3, group.groupCount)
        assertEquals("c", group.key)
        assertEquals("latest", group.text)
        assertEquals(listOf("a", "b", "c"), group.memberKeys)
    }

    @Test
    fun differentAppsOrGroupsAreNotFolded() {
        val result = AlertEngine().evaluate(
            listOf(
                alert("w1", pkg = "com.whatsapp", time = 1),
                alert("s1", pkg = "com.Slack", time = 2),
                alert("w2", pkg = "com.whatsapp", time = 3, groupId = "work")
            ),
            0
        )
        // Three separate entries, but the island shows at most two.
        assertEquals(2, result.visible.size)
        assertTrue(result.visible.all { it.groupCount == 1 })
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
    fun anExpiredMessageStopsBeingRememberedOnceItLeavesTheShade() {
        val engine = AlertEngine()
        engine.evaluate(listOf(alert("m")), 0)
        assertTrue(engine.evaluate(listOf(alert("m")), 8_000).visible.isEmpty())
        engine.evaluate(emptyList(), 9_000)
        assertEquals(1, engine.evaluate(listOf(alert("m")), 10_000).visible.size)
    }

    // ---- Lockscreen privacy ----

    @Test
    fun aSecretMessageHoldsNoSlotWhileLocked() {
        val engine = AlertEngine()
        val secret = alert("s", visibility = LockscreenVisibility.SECRET)
        val locked = engine.evaluate(listOf(secret, music()), 0, isLocked = true)
        assertEquals(listOf("music"), keys(locked))
        // Unlocked, it takes its slot (and its seven seconds) from then on.
        val unlocked = engine.evaluate(listOf(secret, music()), 1_000, isLocked = false)
        assertEquals(listOf("s", "music"), keys(unlocked))
        assertEquals(8_000L, unlocked.nextExpiryAtMs)
    }

    @Test
    fun aPrivateMessageIsStrippedToTheAppAndAFallbackWhileLocked() {
        val private = alert("p", title = "Sam", text = "meet at 6", visibility = LockscreenVisibility.PRIVATE)
        val masked = AlertEngine().evaluate(listOf(private), 0, isLocked = true).visible.single()
        assertTrue(masked.isMasked)
        assertEquals("1 New Message", masked.title)
        assertEquals("", masked.text)
        assertNull(masked.largeIcon)
        assertEquals("com.whatsapp", masked.appName)
        // Unlocked, the same alert shows in full.
        val shown = AlertEngine().evaluate(listOf(private), 0, isLocked = false).visible.single()
        assertFalse(shown.isMasked)
        assertEquals("meet at 6", shown.text)
    }

    @Test
    fun aMaskedGroupCountsItsMessages() {
        val raw = listOf(
            alert("a", time = 1, visibility = LockscreenVisibility.PUBLIC),
            alert("b", time = 2, visibility = LockscreenVisibility.PRIVATE),
            alert("c", time = 3, text = "latest", visibility = LockscreenVisibility.PUBLIC)
        )
        val group = AlertEngine().evaluate(raw, 0, isLocked = true).visible.single()
        assertEquals(3, group.groupCount)
        assertTrue(group.isMasked)
        assertEquals("3 New Messages", group.title)
        assertEquals("", group.text)
    }

    @Test
    fun mediaToolsAndPublicMessagesAreNeverMasked() {
        val tools = listOf(
            music().copy(lockscreenVisibility = LockscreenVisibility.SECRET),
            timer().copy(lockscreenVisibility = LockscreenVisibility.PRIVATE)
        )
        val result = AlertEngine().evaluate(tools, 0, isLocked = true)
        assertEquals(setOf("music", "timer"), keys(result).toSet())
        assertTrue(result.visible.none { it.isMasked })
        val public = AlertEngine().evaluate(listOf(alert("m", text = "hi")), 0, isLocked = true).visible.single()
        assertFalse(public.isMasked)
        assertEquals("hi", public.text)
    }

    @Test
    fun effectiveVisibilityOnlyEverTightens() {
        val pub = LockscreenVisibility.PUBLIC
        val priv = LockscreenVisibility.PRIVATE
        val secret = LockscreenVisibility.SECRET
        assertEquals(pub, LockscreenPrivacy.effectiveVisibility(pub, null, osShowsNotifications = true))
        assertEquals(priv, LockscreenPrivacy.effectiveVisibility(pub, priv, osShowsNotifications = true))
        assertEquals(priv, LockscreenPrivacy.effectiveVisibility(priv, pub, osShowsNotifications = true))
        assertEquals(secret, LockscreenPrivacy.effectiveVisibility(pub, null, osShowsNotifications = false))
    }

    // ---- Alarm, voice recorder and privacy indicator (Rule_Book 4.10, 4.3, 4.15/4.16) ----

    @Test
    fun aRingingAlarmTakesTheMainPillFromACallAndARecording() {
        val ringing = alert("ring", pkg = "com.google.android.deskclock", mode = IslandMode.Alarm, category = "alarm", title = "Alarm", text = "7:00")
        val voice = alert("voice", pkg = "com.sec.android.app.voicenote", mode = IslandMode.VoiceRecording, category = null)
        assertEquals(listOf("ring", "call"), keys(AlertEngine().evaluate(listOf(voice, call(), ringing), 0)))
    }

    @Test
    fun anAlarmThatIsNotRingingIsOnlyAPassingNote() {
        fun alarmNote(title: String, text: String) =
            AlertClassifier.classify(alert("a", pkg = "com.google.android.deskclock", category = "alarm", title = title, text = text))
        assertEquals(AlertTier.Critical, alarmNote("Alarm", "Wake up"))
        assertEquals(AlertTier.Ephemeral, alarmNote("Upcoming alarm", "Tue 7:00 AM"))
        assertEquals(AlertTier.Ephemeral, alarmNote("Alarm", "Snoozed until 7:10 AM"))
    }

    @Test
    fun aVoiceRecordingAndThePrivacyIndicatorArePersistentTasks() {
        val engine = AlertEngine()
        val voice = alert("voice", pkg = "com.google.android.apps.recorder", mode = IslandMode.VoiceRecording, category = null)
        val privacy = alert("system_privacy_indicator", pkg = "com.agupta07505.smartisland", mode = IslandMode.PrivacyIndicator, category = "privacy_camera_microphone")
        engine.evaluate(listOf(voice), 0)
        // Newest persistent first; a message waits behind both and they never time out.
        val result = engine.evaluate(listOf(voice, privacy, alert("msg")), 10)
        assertEquals(listOf("system_privacy_indicator", "voice"), keys(result))
        assertEquals(listOf("system_privacy_indicator", "voice"), keys(engine.evaluate(listOf(voice, privacy), 60_000)))
        // Camera and microphone are one merged alert: they never take both slots.
        assertEquals(1, keys(engine.evaluate(listOf(privacy), 60_001)).size)
    }
}
