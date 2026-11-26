/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.util.StatusBarReapplier.Status
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppTest {

    private val ignored: (String) -> Boolean = { it == "com.agupta07505.smartisland" || it == "com.android.systemui" || it == "com.keyboard" }

    private fun window(pkg: String?, app: Boolean = true, active: Boolean = false, focused: Boolean = false) =
        AppWindow(pkg, app, active, focused)

    @Test
    fun foregroundWindowSelection() {
        val focusedWindows = listOf(
            window("com.google.android.youtube", active = false, focused = false),
            window("com.some.dialog", active = true, focused = true)
        )
        assertEquals("com.some.dialog", ForegroundApp.pick(focusedWindows, ignored))

        val floatingWindows = listOf(
            window("com.google.android.youtube", active = true, focused = true),
            window("com.some.floating", active = false, focused = false)
        )
        assertEquals("com.google.android.youtube", ForegroundApp.pick(floatingWindows, ignored))

        val activeWindows = listOf(window("com.a", active = false), window("com.b", active = true))
        assertEquals("com.b", ForegroundApp.pick(activeWindows, ignored))

        val excludedWindows = listOf(
            window("com.agupta07505.smartisland", focused = true),
            window("com.android.systemui", focused = true),
            window("com.keyboard", focused = true)
        )
        assertNull(ForegroundApp.pick(excludedWindows, ignored))

        val missingWindows = listOf(window("com.a", app = false, focused = true), window(null, focused = true), window("", focused = true))
        assertNull(ForegroundApp.pick(missingWindows, ignored))
        assertNull(ForegroundApp.pick(emptyList(), ignored))
    }

    @Test
    fun foregroundCardVisibility() {
        val live = listOf(
            IslandMode.Music, IslandMode.Navigation, IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording,
            IslandMode.AudioRecording, IslandMode.Hotspot, IslandMode.LiveActivity, IslandMode.DownloadUpload
        )
        for (mode in live) assertTrue("$mode", ForegroundApp.hidesCard(mode, "com.app", "com.app"))

        val kept = listOf(
            IslandMode.IncomingCall, IslandMode.Notification, IslandMode.Battery, IslandMode.Bluetooth,
            IslandMode.Flashlight, IslandMode.Empty
        )
        for (mode in kept) assertFalse("$mode", ForegroundApp.hidesCard(mode, "com.app", "com.app"))

        assertFalse(ForegroundApp.hidesCard(IslandMode.Music, "com.app", "com.other"))
        assertFalse(ForegroundApp.hidesCard(IslandMode.Music, "com.app", null))
        assertFalse(ForegroundApp.hidesCard(IslandMode.Music, "com.app", ""))

        fun call(pkg: String, status: String = "") = IslandNotification(
            key = "c", packageName = pkg, appName = pkg, title = "t", text = "t", timeMillis = 0L,
            mode = IslandMode.IncomingCall, callStatusText = status
        )
        assertTrue(ForegroundApp.hidesCard(call("com.app"), "com.app"))
        // dialing still shows
        assertFalse(ForegroundApp.hidesCard(call("com.app", "Calling..."), "com.app"))
        // another app in front, or none
        assertFalse(ForegroundApp.hidesCard(call("com.app"), "com.other"))
        assertFalse(ForegroundApp.hidesCard(call("com.app"), null))
    }
}

class LmoFreeformTest {
    private val dump = """
        RootTask id=5 bounds=[0,0][1080,2400] windowingMode=fullscreen
          taskId=212: com.whatsapp/com.whatsapp.Main bounds=[0,0][1080,2400] userId=0 visible=true topActivity=ComponentInfo{com.whatsapp/com.whatsapp.Conversation}
          taskId=198: com.whatsapp/.Main bounds=[0,0][1080,2400] userId=0 visible=false topActivity=ComponentInfo{com.whatsapp/com.whatsapp.Main}
        RootTask id=1 bounds=[0,0][1080,2400] windowingMode=fullscreen
          taskId=7: com.android.launcher3/.Launcher userId=10 visible=false
    """.trimIndent()

    @Test
    fun freeformTaskSelection() {
        val parsedTasks = LmoFreeform.parseTasks(dump)
        assertEquals(listOf(212, 198, 7), parsedTasks.map { it.taskId })
        assertEquals("com.whatsapp.Conversation", parsedTasks[0].className)
        assertEquals("com.whatsapp.Main", parsedTasks[1].className)
        assertEquals("com.android.launcher3.Launcher", parsedTasks[2].className)
        assertEquals(10, parsedTasks[2].userId)

        val newTask = LmoFreeform.pickTask(LmoFreeform.parseTasks(dump), setOf(198, 7), "com.whatsapp")
        assertEquals(212, newTask?.taskId)

        val reused = LmoFreeform.pickTask(LmoFreeform.parseTasks(dump), setOf(212, 198, 7), "com.whatsapp")
        assertEquals(212, reused?.taskId)

        assertNull(LmoFreeform.pickTask(LmoFreeform.parseTasks(dump), emptySet(), "org.telegram.messenger"))
        assertEquals(emptyList<Int>(), LmoFreeform.parseTasks("").map { it.taskId })

        val summaryTasks = LmoFreeform.parseTasks(dump)
        assertEquals("[212@u0(Conversation), 198@u0(Main)]", LmoFreeform.summarize(summaryTasks, "com.whatsapp"))
        assertEquals("[]", LmoFreeform.summarize(summaryTasks, "org.telegram.messenger"))
    }

    @Test
    fun rawLinesShowPackageOrFirst() {
        val hit = LmoFreeform.rawLinesFor(dump, "com.whatsapp")
        assert(hit.startsWith("lines: | taskId=212"))
        val miss = LmoFreeform.rawLinesFor(dump, "org.telegram.messenger")
        assert(miss.startsWith("no line mentions org.telegram.messenger; output has 5 lines"))
    }

    @Test
    fun findsLmoWindowDisplaysOfAPackage() {
        val out = "lmo-freeform:com.google.android.gm,com.google.android.gm.ui.MailActivityGmail,0\", displayId 16\n" +
            "lmo-freeform:org.telegram.messenger,org.telegram.ui.LaunchActivity,0\", displayId 17"
        assertEquals(setOf(16), LmoFreeform.parseDisplayIds(out, "com.google.android.gm"))
        assertEquals(emptySet<Int>(), LmoFreeform.parseDisplayIds(out, "com.whatsapp"))
    }

    @Test
    fun mapsTasksToTheirDisplay() {
        val out = "RootTask id=18897 bounds=[0,0][1080,1337] displayId=16 userId=0\n" +
            "  taskId=18897: com.google.android.gm/com.google.android.gm.ui.MailActivityGmail bounds=[0,0][1080,1337] userId=0 visible=true\n" +
            "RootTask id=1 bounds=[0,0][1080,2340] displayId=0 userId=0\n" +
            "  taskId=1: com.android.launcher/com.android.launcher.Main bounds=[0,0][1080,2340] userId=0 visible=true\n"
        assertEquals(mapOf(18897 to 16, 1 to 0), LmoFreeform.taskDisplays(out))
    }
}

class OemAutostartUtilTest {

    @Test
    fun openAutostartSettingsFallback() {
        val context = mockk<Context>()
        every { context.packageName } returns "com.agupta07505.smartisland"
        every { context.startActivity(any()) } answers { }

        val result = OemAutostartUtil.openAutostartSettings(context)
        assertNotNull(result)
    }
}

class OemDeviceRulesTest {

    @Test
    fun resolveEffectiveDevice() {
        assertEquals(OemDeviceType.SAMSUNG, OemDeviceRules.resolveEffectiveDevice("SAMSUNG"))
        assertEquals(OemDeviceType.XIAOMI_REDMI_POCO, OemDeviceRules.resolveEffectiveDevice("XIAOMI_REDMI_POCO"))
        assertEquals(OemDeviceType.VIVO_IQOO, OemDeviceRules.resolveEffectiveDevice("VIVO_IQOO"))
        assertEquals(OemDeviceType.REALME_OPPO_ONEPLUS, OemDeviceRules.resolveEffectiveDevice("REALME_OPPO_ONEPLUS"))
        assertEquals(OemDeviceType.GOOGLE_PIXEL, OemDeviceRules.resolveEffectiveDevice("GOOGLE_PIXEL"))
        assertEquals(OemDeviceType.HUAWEI_HONOR, OemDeviceRules.resolveEffectiveDevice("HUAWEI_HONOR"))
        assertEquals(OemDeviceType.MOTOROLA, OemDeviceRules.resolveEffectiveDevice("MOTOROLA"))
        assertEquals(OemDeviceType.TRANSSION, OemDeviceRules.resolveEffectiveDevice("TRANSSION"))
        assertEquals(OemDeviceType.NUBIA, OemDeviceRules.resolveEffectiveDevice("NUBIA"))
    }

    @Test
    fun samsungRecordingIdentified() {
        assertTrue(OemDeviceRules.isScreenRecording("com.sec.android.app.screenrecorder", "Recording screen", OemDeviceType.SAMSUNG))
        assertTrue(OemDeviceRules.isScreenRecording("com.sec.android.app.smartcapture", "Smart Capture", OemDeviceType.SAMSUNG))
        assertTrue(OemDeviceRules.isScreenRecording("com.sec.android.app.voicenote", "Voice recording", OemDeviceType.SAMSUNG))
        assertTrue(OemDeviceRules.isScreenRecording("com.samsung.android.app.screenrecorder", "Screen recorder", OemDeviceType.SAMSUNG))
    }

    @Test
    fun xiaomiScreenRecordingIdentification() {
        assertTrue(
            OemDeviceRules.isScreenRecording(
                "com.miui.screenrecorder",
                "Screen Recorder",
                OemDeviceType.XIAOMI_REDMI_POCO
            )
        )

        assertFalse(
            OemDeviceRules.isScreenRecording(
                "com.miui.soundrecorder",
                "Sound Recorder",
                OemDeviceType.XIAOMI_REDMI_POCO
            )
        )

        assertFalse(
            OemDeviceRules.isScreenRecording(
                "com.android.soundrecorder",
                "Recorder Recording",
                OemDeviceType.XIAOMI_REDMI_POCO
            )
        )
    }

    @Test
    fun vivoScreenRecordingIdentification() {
        assertTrue(OemDeviceRules.isScreenRecording("com.vivo.screenrecorder", "S-Capture", OemDeviceType.VIVO_IQOO))
        assertTrue(OemDeviceRules.isScreenRecording("com.vivo.videocapture", "Video capture", OemDeviceType.VIVO_IQOO))
        assertTrue(OemDeviceRules.isScreenRecording("com.vivo.smartshot", "Smart shot", OemDeviceType.VIVO_IQOO))
    }

    @Test
    fun realmeOppoRecordingIdentified() {
        assertTrue(OemDeviceRules.isScreenRecording("com.oplus.screenrecorder", "Screen recording", OemDeviceType.REALME_OPPO_ONEPLUS))
        assertTrue(OemDeviceRules.isScreenRecording("com.coloros.screenrecorder", "Screen recorder", OemDeviceType.REALME_OPPO_ONEPLUS))
        assertTrue(OemDeviceRules.isScreenRecording("com.realme.screenrecorder", "Screen recording", OemDeviceType.REALME_OPPO_ONEPLUS))
    }

    @Test
    fun inCallPackages() {
        assertTrue(OemDeviceRules.isScreenRecording("cn.nubia.screenrecorder", "Screen Recorder", OemDeviceType.NUBIA))
        assertTrue(OemDeviceRules.isScreenRecording("com.zte.recorder", "Sound Recorder", OemDeviceType.NUBIA))
        assertTrue(OemDeviceRules.isInCallPackage("cn.nubia.incallui", OemDeviceType.NUBIA))
        assertTrue(OemDeviceRules.isInCallPackage("cn.nubia.dialer", OemDeviceType.NUBIA))

        assertTrue(OemDeviceRules.isInCallPackage("com.samsung.android.incallui", OemDeviceType.SAMSUNG))
        assertTrue(OemDeviceRules.isInCallPackage("com.miui.incallui", OemDeviceType.XIAOMI_REDMI_POCO))
        assertTrue(OemDeviceRules.isInCallPackage("com.vivo.incallui", OemDeviceType.VIVO_IQOO))
        assertTrue(OemDeviceRules.isInCallPackage("com.oplus.incallui", OemDeviceType.REALME_OPPO_ONEPLUS))
        assertTrue(OemDeviceRules.isInCallPackage("com.google.android.dialer", OemDeviceType.GOOGLE_PIXEL))
        assertFalse(OemDeviceRules.isInCallPackage("com.whatsapp.camera", OemDeviceType.SAMSUNG))
    }

    @Test
    fun hotspotDetection() {
        assertTrue(OemDeviceRules.isHotspot("com.samsung.android.app.mobilehotspot", "Hotspot active"))
        assertTrue(OemDeviceRules.isHotspot("com.android.settings", "Portable hotspot enabled"))
        assertTrue(OemDeviceRules.isHotspot("com.miui.securitycenter", "Tethering active"))
        assertFalse(OemDeviceRules.isHotspot("com.miui.securitycenter", "Battery saver is active"))
        assertFalse(OemDeviceRules.isHotspot("com.spotify.music", "Playing music"))
        assertFalse(OemDeviceRules.isHotspot("com.google.android.gm", "Re: [SmartIsland] Hotspot card shows up on its own"))
        assertFalse(OemDeviceRules.isHotspot("com.whatsapp", "Mobile hotspot password"))
    }

    @Test
    fun isNonNavigationContent() {
        assertTrue(OemDeviceRules.isNonNavigationContent("com.sec.android.app.screenrecorder", "Recording screen 05:20 left"))
        assertTrue(OemDeviceRules.isNonNavigationContent("com.samsung.android.incallui", "Incoming call from Mom"))
        assertTrue(OemDeviceRules.isNonNavigationContent("com.android.settings", "Mobile hotspot active"))
        assertFalse(OemDeviceRules.isNonNavigationContent("com.google.android.apps.maps", "In 300 m turn left onto Main St"))
    }

    @Test
    fun screenRecordingCompletion() {
        val completedNotif = mockk<Notification>()
        completedNotif.flags = 0 // not ongoing
        assertTrue(OemDeviceRules.isScreenRecordingComplete(completedNotif, "Recording saved • Tap to view"))

        val activeNotif = mockk<Notification>()
        activeNotif.flags = Notification.FLAG_ONGOING_EVENT
        assertFalse(OemDeviceRules.isScreenRecordingComplete(activeNotif, "Recording screen • 01:23"))
    }
}

class ShadeActionsTest {
    private fun real(title: String) = IslandNotificationAction(title, mockk<PendingIntent>())
    private fun view(title: String) = IslandNotificationAction(title, mockk<PendingIntent>())

    @Test
    fun runningTimerPauseAddMinute() {
        val actions = ShadeActions.mirror(listOf(real("Pause"), real("+1:00")), emptyList())
        assertEquals(listOf("Pause", "+1:00"), actions.map { it.title })
    }

    @Test
    fun pausedTimerShowsResumeAndStop() {
        val actions = ShadeActions.mirror(listOf(real("Resume"), real("Stop")), emptyList())
        assertEquals(listOf("Resume", "Stop"), actions.map { it.title })
        assertTrue(ShadeActions.offersResume(actions))
    }

    @Test
    fun stopwatchActions() {
        val runningActions = ShadeActions.mirror(listOf(real("Pause"), real("Lap")), emptyList())
        assertEquals(listOf("Pause", "Lap"), runningActions.map { it.title })
        assertFalse(ShadeActions.offersResume(runningActions))

        val noIntentActions = ShadeActions.mirror(
            listOf(real("Pause"), IslandNotificationAction("Stop", null)), emptyList()
        )
        assertEquals(listOf("Pause"), noIntentActions.map { it.title })

        val cappedActions = ShadeActions.mirror(
            listOf(real("Pause"), real("Lap"), real("Stop"), real("Reset")), emptyList()
        )
        assertEquals(ShadeActions.SHADE_ACTION_LIMIT, cappedActions.size)

        fun shown(kind: TimerStopwatchParser.ClockKind, vararg visible: String) =
            titles(ShadeActions.mirror(visible.map { real(it) }, emptyList(), kind))
        assertEquals(listOf("Pause", "+1:00"), shown(timer, "Pause", "Stop", "+1:00"))
        assertEquals(listOf("Resume", "Stop"), shown(timer, "Resume", "Stop"))
        assertEquals(listOf("Pause", "Lap"), shown(stopwatch, "Pause", "Lap", "Stop"))
        assertEquals(listOf("Resume", "Reset"), shown(stopwatch, "Resume", "Reset", "Lap"))

        val a = ShadeActions.mirror(listOf(real("Resume"), real("Lap"), real("Stop")), emptyList(), stopwatch)
        assertEquals(listOf("Resume", "Stop"), titles(a))
    }

    @Test
    fun usesCustomButtonsAlongsideStandardActions() {
        val customActions = ShadeActions.mirror(
            standard = emptyList(),
            view = listOf(view("pause button"), view("Add 1 minute"))
        )
        assertEquals(listOf("pause button", "Add 1 minute"), customActions.map { it.title })

        val own = real("Pause")
        val appIntentActions = ShadeActions.mirror(listOf(own), listOf(view("Pause"), view("+1:00")))
        assertEquals(2, appIntentActions.size)
        assertSame(own.pendingIntent, appIntentActions[0].pendingIntent)

        val nonButtonsActions = ShadeActions.mirror(emptyList(), listOf(view("notification content"), view("Pause")))
        assertEquals(listOf("Pause"), nonButtonsActions.map { it.title })

        val running = ShadeActions.mirror(
            emptyList(), listOf(view("Pause"), view("+1:00"), view("Stop")), timer
        )
        assertEquals(listOf("Pause", "+1:00"), titles(running))

        val paused = ShadeActions.mirror(
            emptyList(), listOf(view("Resume"), view("Lap"), view("Stop")), timer
        )
        assertEquals(listOf("Resume", "Stop"), titles(paused))
    }

    private val timer = TimerStopwatchParser.ClockKind.Timer
    private val stopwatch = TimerStopwatchParser.ClockKind.Stopwatch

    private fun titles(a: List<IslandNotificationAction>) = a.map { it.title }

    @Test
    fun timerShadeActionsClockOnly() {
        val cases = mapOf(
            "running Google timer drops End" to
                (listOf("Pause", "End", "+1:00") to listOf("Pause", "+1:00")),
            "running timer shows Pause and +1:00 only, for any clock app" to
                (listOf("Pause", "Stop", "+1:00") to listOf("Pause", "+1:00")),
            "paused timer shows Resume and Reset only" to
                (listOf("Resume", "Stop", "Reset") to listOf("Resume", "Reset"))
        )
        for ((case, shown) in cases) {
            val (input, expected) = shown
            val a = ShadeActions.mirror(input.map { real(it) }, emptyList(), timer)
            assertEquals(case, expected, titles(a))
        }
    }

    @Test
    fun googleTimerAddMinuteFromApp() {
        val own = real("+1:00")
        val a = ShadeActions.mirror(listOf(real("Pause"), own), listOf(view("Pause")), timer)
        assertEquals(listOf("Pause", "+1:00"), titles(a))
        assertSame(own.pendingIntent, a[1].pendingIntent)
    }

    @Test
    fun stopwatchShadeActionsClockOnly() {
        // case to (what the app shows, what the island keeps)
        val cases = mapOf(
            "running Google stopwatch has Lap but no Stop" to
                (listOf("Pause", "Lap", "Stop") to listOf("Pause", "Lap")),
            "paused stopwatch shows Start and Reset only" to
                (listOf("Start", "Lap", "Reset") to listOf("Start", "Reset")),
            "running stopwatch has no Reset" to
                (listOf("Pause", "Reset", "Lap") to listOf("Pause", "Lap")),
            "paused stopwatch with Reset drops Stop" to
                (listOf("Resume", "Stop", "Reset") to listOf("Resume", "Reset"))
        )
        for ((case, shown) in cases) {
            val (input, expected) = shown
            val a = ShadeActions.mirror(input.map { real(it) }, emptyList(), stopwatch)
            assertEquals(case, expected, titles(a))
        }
    }

    @Test
    fun stateIsEnforcedBeforeTheShadeLimit() {
        val a = ShadeActions.mirror(
            listOf(real("Pause"), real("Stop"), real("Reset"), real("Lap")), emptyList(), stopwatch
        )
        assertEquals(listOf("Pause", "Lap"), titles(a))
    }

    @Test
    fun listWithNoStateIsLeftAlone() {
        val a = ShadeActions.mirror(listOf(real("Stop"), real("Lap")), emptyList(), stopwatch)
        assertEquals(listOf("Stop", "Lap"), titles(a))
    }

    @Test
    fun makerClocksNotShadeFix() {
        assertFalse(ClockPackageRules.usesShadeFixes("com.miui.deskclock"))
        assertFalse(ClockPackageRules.usesShadeFixes("com.sec.android.app.clockpackage"))
        assertTrue(ClockPackageRules.usesShadeFixes("com.google.android.deskclock"))
    }

    @Test
    fun proxyButtonsFlagged() {
        val proxy = IslandNotificationAction("Pause", mockk<PendingIntent>(), viaView = true)
        val own = real("Pause")
        assertTrue(proxy.viaView)
        assertFalse(own.viaView)
        val mirrored = ShadeActions.mirror(listOf(own), listOf(proxy), timer)
        assertFalse(mirrored.single().viaView)
    }
}

class ShadeDetectorTest {
    private val height = 2400

    private fun window(
        pkg: String? = ShadeDetector.SYSTEM_UI,
        title: String? = null,
        system: Boolean = true,
        active: Boolean = false,
        focused: Boolean = false,
        top: Int = 0,
        bottom: Int = height
    ) = ShadeWindow(pkg, title, system, active, focused, top, bottom)

    private fun open(vararg w: ShadeWindow, locked: Boolean = false, titles: ShadeTitles = ShadeTitles.ENGLISH) =
        ShadeDetector.isShadeOpen(w.toList(), height, locked, titles)

    @Test
    fun shadeDetection() {
        assertFalse(open())

        assertTrue(open(window(title = "Quick settings.", active = true)))
        assertTrue(open(window(title = "Status bar", active = true)))
        assertTrue(open(window(title = "Notification panel", focused = true)))

        assertFalse(open(window(title = "Status bar", active = true, bottom = 120)))
        assertFalse(open(window(title = "Navigation bar", active = true, top = 2250)))

        assertFalse(open(window(pkg = "com.example.app", active = true)))
        assertFalse(open(window(system = false, active = true)))
        assertFalse(open(window(pkg = null, active = true)))

        assertTrue(open(window(title = "Notification shade.", active = true), locked = true))
        assertTrue(open(window(title = "Quick settings.", active = true), locked = true))
        assertFalse(open(window(title = null, active = true), locked = true))
        assertFalse(open(window(title = "Something else", active = true), locked = true))
    }

    @Test
    fun shadeWindowTypes() {
        assertTrue(open(window(title = "Notification shade.", active = true)))
        assertTrue(open(window(focused = true)))
        assertTrue(open(window(title = "Notification shade, expanded", active = true)))
        assertTrue(open(window(title = "Status bar, expanded", focused = true)))
        assertFalse(open(window(focused = true, bottom = 120)))
        assertFalse(open(window()))
        assertFalse(open(window(title = "Notifications access dialog", active = true)))

        assertFalse(open(window(title = "Power menu", active = true, focused = true)))
        assertFalse(open(window(title = "ScreenshotAnimation", active = true)))
        assertFalse(open(window(title = "ActionsDialog", focused = true)))

        assertTrue(open(window(title = "Power menu", active = true), window(title = "Notification shade.", active = true)))

        assertFalse(open(window(title = "Notification shade.")))

        assertFalse(open(window(title = "Lock screen.", active = true), locked = true))
        assertFalse(open(window(title = "Lock screen.", active = true), locked = false))
    }

    @Test
    fun shadeWindowDimensions() {
        val german = ShadeTitles(shade = listOf("Benachrichtigungsleiste"), lockScreen = listOf("Sperrbildschirm"))
        assertTrue(open(window(title = "Benachrichtigungsleiste.", active = true), locked = true, titles = german))
        assertFalse(open(window(title = "Sperrbildschirm.", active = true), locked = true, titles = german))

        assertFalse(ShadeDetector.isShadeOpen(listOf(window(active = true)), 0, false))
    }
}

class ShizukuManagerTest {

    @Test
    fun isInstalledWhenNotPresent() {
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { pm.getPackageInfo("moe.shizuku.privileged.api", 0) } throws RuntimeException("Not installed")

        val installed = ShizukuManager.isInstalled(context)
        assertFalse(installed)
    }

    @Test
    fun findPackageNullNoDeclarer() {
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { pm.getPermissionInfo(any(), any()) } throws PackageManager.NameNotFoundException()

        Assert.assertNull(ShizukuManager.findPackageName(context))
    }

    @Test
    fun isInstalledFallsToPackage() {
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { pm.getPermissionInfo(any(), any()) } throws PackageManager.NameNotFoundException()
        every { pm.getPackageInfo("moe.shizuku.privileged.api", 0) } returns PackageInfo()

        Assert.assertTrue(ShizukuManager.isInstalled(context))
    }

    @Test
    fun isBinderAvailableWithoutCrash() {
        // without Shizuku service running, isBinderAvailable should return false safely
        val available = ShizukuManager.isBinderAvailable()
        assertFalse(available)
    }

    @Test
    fun hasPermissionWithoutCrash() {
        // without Shizuku service running, hasPermission should return false safely
        val hasPerm = ShizukuManager.hasPermission()
        assertFalse(hasPerm)
    }

    @Test
    fun colonSeparatedEntries() {
        val emptyMerged = ShizukuManager.mergeColonSeparated("", "com.agupta07505.smartisland/service")
        Assert.assertEquals("com.agupta07505.smartisland/service", emptyMerged)

        val existingExisting = "com.bitwarden.authenticator/service:com.lastpass.lpandroid/service"
        val existingNewEntry = "com.agupta07505.smartisland/service"
        val existingMerged = ShizukuManager.mergeColonSeparated(existingExisting, existingNewEntry)
        Assert.assertEquals(
            "com.bitwarden.authenticator/service:com.lastpass.lpandroid/service:com.agupta07505.smartisland/service",
            existingMerged
        )

        val duplicateExisting = "com.bitwarden.authenticator/service:com.agupta07505.smartisland/service"
        val duplicateNewEntry = "com.agupta07505.smartisland/service"
        val duplicateMerged = ShizukuManager.mergeColonSeparated(duplicateExisting, duplicateNewEntry)
        Assert.assertEquals(
            "com.bitwarden.authenticator/service:com.agupta07505.smartisland/service",
            duplicateMerged
        )
    }
}

class ShizukuMethodCheckTest {
    @Test
    fun probePassesOnMarker() {
        assertTrue(ShizukuMethodCheck.probePassed("SI_OK\n"))
        assertFalse(ShizukuMethodCheck.probePassed(""))
        assertFalse(ShizukuMethodCheck.probePassed(null))
        assertFalse(ShizukuMethodCheck.probePassed("not found"))
    }

    @Test
    fun methodProbeCoverage() {
        ShizukuMethodCheck.Method.entries.forEach { method ->
            if (method == ShizukuMethodCheck.Method.Hotspot) {
                assertNull(ShizukuMethodCheck.probeCommand(method))
            } else {
                val command = ShizukuMethodCheck.probeCommand(method)
                assertNotNull(command)
                assertTrue(command!!.contains(ShizukuMethodCheck.OK_MARKER))
            }
        }
    }

    @Test
    fun methodIdsAreUnique() {
        val ids = ShizukuMethodCheck.Method.entries.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
    }
}

class ShizukuRuntimePermissionsTest {
    @Test
    fun permissionGrants() {
        val commands = ShizukuManager.runtimePermissionCommands("com.example")
        assertTrue("pm grant com.example android.permission.BLUETOOTH_CONNECT" in commands)
        assertTrue("pm grant com.example android.permission.POST_NOTIFICATIONS" in commands)

        val all = ShizukuManager.runtimePermissionCommands("com.example").joinToString(" ")
        ShizukuManager.OPTIONAL_PERMISSIONS.forEach { assertFalse(all.contains(it)) }
        assertFalse(all.contains("VPN", ignoreCase = true))
    }
}

class ShizukuSetupScopeTest {
    private val pkg = "com.example"

    private fun commands(scope: ShizukuManager.SetupScope) = ShizukuManager.setupCommands(
        scope = scope,
        pkg = pkg,
        accessibilityClass = "$pkg/A",
        notificationClass = "$pkg/N",
        mergedAccessibilityServices = "$pkg/A",
        mergedNotificationListeners = "$pkg/N"
    )

    @Test
    fun shizukuCommandSets() {
        val requiredAll = commands(ShizukuManager.SetupScope.Necessary).joinToString("\n")
        listOf(
            "ACCESS_RESTRICTED_SETTINGS", "SYSTEM_ALERT_WINDOW", "WRITE_SETTINGS", "BIND_ACCESSIBILITY_SERVICE",
            "enabled_accessibility_services", "allow_listener", "RECEIVE_SENSITIVE_NOTIFICATIONS",
            "POST_NOTIFICATIONS", "BLUETOOTH_CONNECT", "deviceidle whitelist"
        ).forEach { assertTrue("missing $it", requiredAll.contains(it)) }

        assertTrue(commands(ShizukuManager.SetupScope.Necessary).first().contains("ACCESS_RESTRICTED_SETTINGS"))

        val optionalAll = commands(ShizukuManager.SetupScope.Necessary).joinToString("\n")
        listOf("LOCATION", "allow_dnd", "GET_USAGE_STATS").forEach {
            assertFalse("unexpected $it", optionalAll.contains(it))
        }

        val necessary = commands(ShizukuManager.SetupScope.Necessary)
        val everything = commands(ShizukuManager.SetupScope.All)
        assertEquals(necessary, everything.take(necessary.size))
        assertEquals(ShizukuManager.optionalCommands(pkg), everything.drop(necessary.size))
    }

    @Test
    fun optionalCoversUsageDndAndLocation() {
        val optional = ShizukuManager.optionalCommands(pkg).joinToString("\n")
        listOf("GET_USAGE_STATS", "allow_dnd", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION").forEach {
            assertTrue("missing $it", optional.contains(it))
        }
    }
}

class StatusBarTileModelTest {
    @Test
    fun statusTileState() {
        assertEquals(StatusBarTileModel(false, StatusBarTileLabel.Off), statusBarTileModel(false, true, Status.Applied))
        assertEquals(StatusBarTileModel(false, StatusBarTileLabel.NeedsShizuku), statusBarTileModel(false, false, Status.Off))
        assertEquals(StatusBarTileLabel.Hidden, statusBarTileModel(true, true, Status.Applied).label)
        assertEquals(StatusBarTileLabel.Failed, statusBarTileModel(true, true, Status.Failed).label)
        assertEquals(StatusBarTileLabel.Applying, statusBarTileModel(true, true, Status.Off).label)

        val expected = StatusBarTileModel(true, StatusBarTileLabel.NeedsShizuku)
        assertEquals(expected, statusBarTileModel(true, false, Status.Applied))
        assertEquals(expected, statusBarTileModel(true, true, Status.ShizukuLost))
    }


}

class StatusBarPlanTest {
    @Test
    fun defaultPlanHidesAllImmersive() {
        val plan = StatusBarPlan.Everything
        assertEquals(listOf("system-icons", "clock", "notification-icons"), plan.disableFlags)
        assertTrue(plan.immersive)
        assertTrue(plan.iconBlacklist.containsAll(listOf("clock", "time", "battery", "wifi", "managed_profile")))
    }

    @Test
    fun clockAloneUsesClockFlag() {
        val plan = StatusBarPlan(notificationIcons = false, clock = true, allSystemIcons = false)
        assertEquals(listOf("clock"), plan.disableFlags)
        assertEquals(listOf("clock", "time"), plan.iconBlacklist)
        assertFalse(plan.immersive)
    }

    @Test
    fun notifIconsAloneUsesOwnFlag() {
        val plan = StatusBarPlan(notificationIcons = true, clock = false, allSystemIcons = false)
        assertEquals(listOf("notification-icons"), plan.disableFlags)
        assertTrue(plan.iconBlacklist.isEmpty())
        assertFalse(plan.isEmpty)
    }

    @Test
    fun singleSystemIconsHideOnlyTheirSlots() {
        val plan = StatusBarPlan(false, false, false, setOf("battery", "mobile_signal", "unknown"))
        assertTrue(plan.disableFlags.isEmpty())
        assertEquals(listOf("battery", "mobile", "phone_signal"), plan.iconBlacklist)
    }

    @Test
    fun visibleClockSlots() {
        val plan = StatusBarPlan(notificationIcons = true, clock = false, allSystemIcons = true)
        assertTrue("system-icons" in plan.disableFlags)
        assertFalse("clock" in plan.disableFlags)
        assertFalse("clock" in plan.iconBlacklist)
        assertFalse("time" in plan.iconBlacklist)
        assertFalse(plan.immersive)
    }

    @Test
    fun nothingSelectedIsEmpty() {
        assertTrue(StatusBarPlan(false, false, false, emptySet()).isEmpty)
    }

    @Test
    fun iconSlotsUniqueInCatalog() {
        val all = StatusBarSystemIcon.entries.flatMap { it.slots }
        assertEquals(all.size, all.toSet().size)
        assertEquals(StatusBarSystemIcon.entries.map { it.key }.size, StatusBarSystemIcon.entries.map { it.key }.toSet().size)
    }

    @Test
    fun hideCommandSequence() {
        val fullCommands = StatusBarHider.hideCommands(StatusBarPlan.Everything)
        assertEquals("am broadcast -a com.android.systemui.demo -e command exit", fullCommands[0])
        assertEquals("cmd statusbar send-disable-flag none", fullCommands[1])
        assertEquals("cmd statusbar send-disable-flag system-icons clock notification-icons", fullCommands[2])
        assertEquals("settings put global policy_control immersive.status=*", fullCommands[3])
        assertEquals(3, fullCommands.count { it.startsWith("settings put ") && "icon_blacklist" in it })

        val partialCommands = StatusBarHider.hideCommands(StatusBarPlan(true, false, false))
        assertTrue("settings delete global policy_control" in partialCommands)
        assertFalse(partialCommands.any { "immersive.status" in it })
        assertTrue("cmd statusbar send-disable-flag notification-icons" in partialCommands)

        val emptyCommands = StatusBarHider.hideCommands(StatusBarPlan(true, false, false))
        assertTrue("settings delete secure icon_blacklist" in emptyCommands)
        assertTrue("settings delete system icon_blacklist" in emptyCommands)
        assertTrue("settings delete global icon_blacklist" in emptyCommands)
    }

    @Test
    fun phoneSwitchesHideAlone() {
        val plan = StatusBarPlan(
            notificationIcons = false, clock = false, allSystemIcons = false,
            systemIcons = setOf("rotate", "mute", "data_saver")
        )
        assertTrue(plan.iconBlacklist.containsAll(listOf("rotate", "mute", "data_saver")))
        assertFalse("volume" in plan.iconBlacklist)
    }

    @Test
    fun hidePlanMatching() {
        val fullPlan = StatusBarPlan.Everything
        assertTrue(StatusBarHider.matches(fullPlan, "immersive.status=*", null))
        assertTrue(StatusBarHider.matches(fullPlan, null, "battery,clock,time,wifi"))
        assertFalse(StatusBarHider.matches(fullPlan, "null", "null"))

        val partialPlan = StatusBarPlan(false, true, false, setOf("battery"))
        assertTrue(StatusBarHider.matches(partialPlan, null, "clock,time,battery"))
        assertFalse(StatusBarHider.matches(partialPlan, null, "clock,time"))
        // a slot name inside another name does not count
        assertFalse(StatusBarHider.matches(StatusBarPlan(false, false, false, setOf("mobile_data")), null, "data_connection_x"))

        assertTrue(StatusBarHider.matches(StatusBarPlan(true, false, false), null, "null"))
        assertTrue(StatusBarHider.matches(StatusBarPlan(false, false, false, emptySet()), null, null))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatusBarReapplierTest {

    private class Fake {
        var shizuku = true
        var interactive = true
        var hiddenOnDevice: Boolean? = true
        var hideResult: Result<Unit> = Result.success(Unit)
        val hideTimes = mutableListOf<Long>()
        var restores = 0
    }

    private fun TestScope.engine(fake: Fake) = StatusBarReapplier(
        scope = backgroundScope,
        interactive = { fake.interactive },
        hasShizuku = { fake.shizuku },
        hide = { fake.hideTimes += currentTime; fake.hideResult },
        isHidden = { fake.hiddenOnDevice },
        restore = { fake.restores++; Result.success(Unit) }
    )

    @Test
    fun passGapsLandAt0_350_750_1400() {
        var at = 0L
        val times = StatusBarReapplier.PASS_GAPS_MS.map { at += it; at }
        assertEquals(listOf(0L, 350L, 750L, 1400L), times)
    }

    @Test
    fun switchingOnAppliesFourPasses() = runTest {
        val fake = Fake()
        val engine = engine(fake)
        engine.onSettings(true)
        advanceTimeBy(1500)
        assertEquals(listOf(0L, 350L, 750L, 1400L), fake.hideTimes)
        assertEquals(StatusBarReapplier.Status.Applied, engine.status.value)
    }

    @Test
    fun newTriggerReplacesWaitingPasses() = runTest {
        val fake = Fake()
        val engine = engine(fake)
        engine.onSettings(true)
        advanceTimeBy(400)
        engine.trigger("screen on")
        runCurrent()
        advanceTimeBy(1500)
        assertEquals(listOf(0L, 350L, 400L, 750L, 1150L, 1800L), fake.hideTimes)
    }

    @Test
    fun hideSwitchedOffNeverAppliesAnything() = runTest {
        val fake = Fake()
        val engine = engine(fake)
        engine.onSettings(false)
        engine.trigger("screen on")
        advanceTimeBy(40_000)
        assertTrue(fake.hideTimes.isEmpty())
    }

    @Test
    fun firstOffNoRestoreSwitchOffDoes() = runTest {
        val fake = Fake()
        val engine = engine(fake)
        engine.onSettings(false)
        runCurrent()
        assertEquals(0, fake.restores)
        engine.onSettings(true)
        runCurrent()
        engine.onSettings(false)
        runCurrent()
        assertEquals(1, fake.restores)
        assertEquals(StatusBarReapplier.Status.Off, engine.status.value)
    }

    @Test
    fun switchOffStopsWaitingPasses() = runTest {
        val fake = Fake()
        val engine = engine(fake)
        engine.onSettings(true)
        advanceTimeBy(400)
        engine.onSettings(false)
        advanceTimeBy(40_000)
        assertEquals(listOf(0L, 350L), fake.hideTimes)
    }

    @Test
    fun shizukuReconnect() = runTest {
        val fake = Fake().also { it.shizuku = false }
        val engine = engine(fake)
        engine.onSettings(true)
        advanceTimeBy(1500)
        assertTrue(fake.hideTimes.isEmpty())
        assertEquals(StatusBarReapplier.Status.ShizukuLost, engine.status.value)

        fake.shizuku = true
        advanceTimeBy(StatusBarReapplier.WATCH_INTERVAL_MS)
        assertEquals(1, fake.hideTimes.size)
        assertEquals(StatusBarReapplier.Status.Applied, engine.status.value)
    }

    @Test
    fun watcherLeavesAHealthyHideAlone() = runTest {
        val fake = Fake()
        val engine = engine(fake)
        engine.onSettings(true)
        advanceTimeBy(1500)
        val after = fake.hideTimes.size
        advanceTimeBy(StatusBarReapplier.WATCH_INTERVAL_MS * 3)
        assertEquals(after, fake.hideTimes.size)
    }

    @Test
    fun watcherReappliesLostHide() = runTest {
        val fake = Fake()
        val engine = engine(fake)
        engine.onSettings(true)
        advanceTimeBy(1500)
        val after = fake.hideTimes.size
        fake.hiddenOnDevice = false
        advanceTimeBy(StatusBarReapplier.WATCH_INTERVAL_MS)
        assertEquals(after + 1, fake.hideTimes.size)
    }

    @Test
    fun watcherSkipsWhileTheScreenIsOff() = runTest {
        val fake = Fake()
        val engine = engine(fake)
        engine.onSettings(true)
        advanceTimeBy(1500)
        val after = fake.hideTimes.size
        fake.interactive = false
        fake.hiddenOnDevice = false
        advanceTimeBy(StatusBarReapplier.WATCH_INTERVAL_MS * 2)
        assertEquals(after, fake.hideTimes.size)
    }

    @Test
    fun failedHideIsRetriedByTheWatcher() = runTest {
        val fake = Fake().also { it.hideResult = Result.failure(IllegalStateException("boom")) }
        val engine = engine(fake)
        engine.onSettings(true)
        advanceTimeBy(1500)
        assertEquals(StatusBarReapplier.Status.Failed, engine.status.value)
        val after = fake.hideTimes.size
        fake.hideResult = Result.success(Unit)
        advanceTimeBy(StatusBarReapplier.WATCH_INTERVAL_MS)
        assertEquals(after + 1, fake.hideTimes.size)
        assertEquals(StatusBarReapplier.Status.Applied, engine.status.value)
    }

    @Test
    fun missedRestoreRunsNextTrigger() = runTest {
        val fake = Fake().also { it.shizuku = false }
        val engine = engine(fake)
        engine.onSettings(true)
        engine.onSettings(false)
        runCurrent()
        assertEquals(0, fake.restores)

        fake.shizuku = true
        engine.trigger("screen on")
        runCurrent()
        assertEquals(1, fake.restores)
        // done once: a later trigger does not restore again
        engine.trigger("unlock")
        runCurrent()
        assertEquals(1, fake.restores)
    }

    @Test
    fun awaitShizukuReturnsWhenReady() = runTest {
        val up = StatusBarReapplier.awaitShizuku(timeoutMs = 2_000, hasShizuku = { currentTime >= 1_200 })
        assertTrue(up)
        assertTrue(currentTime in 1_200..1_700)
    }

    @Test
    fun awaitShizukuGivesUpAtTheTimeout() = runTest {
        val up = StatusBarReapplier.awaitShizuku(timeoutMs = 1_000, hasShizuku = { false })
        assertFalse(up)
        assertEquals(1_000L, currentTime)
    }

    @Test
    fun changedElementsApplyNewPlan() = runTest {
        val plans = mutableListOf<StatusBarPlan>()
        val engine = StatusBarReapplier(
            scope = backgroundScope,
            interactive = { true },
            hasShizuku = { true },
            hide = { plan -> plans += plan; Result.success(Unit) },
            isHidden = { true },
            restore = { Result.success(Unit) }
        )
        engine.onSettings(true, StatusBarPlan.Everything)
        advanceTimeBy(1500)
        val clockOnly = StatusBarPlan(notificationIcons = false, clock = true, allSystemIcons = false)
        engine.onSettings(true, clockOnly)
        advanceTimeBy(1500)
        assertEquals(StatusBarPlan.Everything, plans.first())
        assertEquals(clockOnly, plans.last())
        assertEquals(8, plans.size)
    }

    @Test
    fun samePlanDoesNotApplyAgain() = runTest {
        var hides = 0
        val engine = StatusBarReapplier(
            scope = backgroundScope,
            interactive = { true },
            hasShizuku = { true },
            hide = { hides++; Result.success(Unit) },
            isHidden = { true },
            restore = { Result.success(Unit) }
        )
        engine.onSettings(true, StatusBarPlan.Everything)
        advanceTimeBy(1500)
        val after = hides
        engine.onSettings(true, StatusBarPlan.Everything)
        advanceTimeBy(1500)
        assertEquals(after, hides)
    }
}
