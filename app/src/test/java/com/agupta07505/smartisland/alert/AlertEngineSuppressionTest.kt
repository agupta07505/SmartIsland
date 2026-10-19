package com.agupta07505.smartisland.alert

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Immersive suppression (Rule_Book 10.1) and the face-unlock IDLE hold (8.2). */
class AlertEngineSuppressionTest {

    private fun alert(
        key: String,
        pkg: String = "com.whatsapp",
        mode: IslandMode = IslandMode.Notification,
        category: String? = "msg",
        title: String = "Sam",
        text: String = "hi"
    ) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = title, text = text,
        timeMillis = 0L, category = category, mode = mode
    )

    private val message = alert("msg")
    private val music = alert("music", pkg = "com.spotify.music", mode = IslandMode.Music, category = "transport")
    private val navigation = alert("nav", pkg = "com.google.android.apps.maps", mode = IslandMode.Navigation, category = "navigation")
    private val call = alert("call", pkg = "com.android.dialer", mode = IslandMode.IncomingCall, category = "call")
    private val alarm = alert("alarm", pkg = "com.google.android.deskclock", mode = IslandMode.Timer, category = "alarm")
    private val lowBattery = alert("battery", pkg = "android", mode = IslandMode.Battery, category = null, title = "Low battery", text = "4% remaining")

    private fun keys(e: AlertEngine.Evaluation) = e.visible.map { it.key }

    @Test
    fun immersiveHidesEverythingBelowCriticalExceptACall() {
        val engine = AlertEngine()
        engine.immersive = true
        assertTrue(keys(engine.evaluate(listOf(message, music, navigation), 0L)).isEmpty())
    }

    @Test
    fun immersiveLetsAlarmsCriticalBatteryAndCallsThrough() {
        val engine = AlertEngine()
        engine.immersive = true
        assertEquals(listOf("alarm", "call"), keys(engine.evaluate(listOf(message, call, music, alarm), 0L)))
        assertEquals(listOf("battery"), keys(engine.evaluate(listOf(lowBattery, navigation), 0L)))
    }

    @Test
    fun leavingImmersiveRestoresTheNormalOrder() {
        val engine = AlertEngine()
        engine.immersive = true
        assertTrue(keys(engine.evaluate(listOf(navigation, music), 0L)).isEmpty())
        engine.immersive = false
        assertEquals(listOf("nav", "music"), keys(engine.evaluate(listOf(navigation, music), 10L)))
    }

    @Test
    fun aMessageKeepsItsClockWhileSuppressedAndDoesNotReturnAfterExpiry() {
        val engine = AlertEngine()
        engine.immersive = true
        assertTrue(keys(engine.evaluate(listOf(message), 0L)).isEmpty())
        engine.immersive = false
        // Still inside its seven seconds: it shows.
        assertEquals(listOf("msg"), keys(engine.evaluate(listOf(message), 3_000L)))
        engine.immersive = true
        engine.evaluate(listOf(message), 8_000L)
        engine.immersive = false
        // Timed out while hidden: it stays gone.
        assertTrue(keys(engine.evaluate(listOf(message), 9_000L)).isEmpty())
    }

    @Test
    fun biometricHoldShowsNothingEvenACriticalAlert() {
        val engine = AlertEngine()
        engine.biometricHold = true
        // `call` has no Answer action, so it is not ringing; a ringing timer is not a ringing Alarm.
        assertTrue(keys(engine.evaluate(listOf(alarm, call, music), 0L)).isEmpty())
        engine.biometricHold = false
        assertEquals(listOf("alarm", "call"), keys(engine.evaluate(listOf(alarm, call, music), 1L)))
    }

    @Test
    fun aRingingCallOrAlarmShowsThroughTheBiometricHold() {
        val ringingCall = call.copy(key = "ringing", actionIntents = listOf(IslandNotificationAction("Answer", null)))
        val ringingAlarm = alert("ring", pkg = "com.google.android.deskclock", mode = IslandMode.Alarm, category = "alarm")
        val engine = AlertEngine()
        engine.biometricHold = true
        assertEquals(listOf("ring", "ringing"), keys(engine.evaluate(listOf(message, ringingCall, music, ringingAlarm, alarm), 0L)))
        assertTrue(AlertEngine.breaksBiometricHold(ringingCall))
        assertTrue(!AlertEngine.breaksBiometricHold(call))
    }
}
