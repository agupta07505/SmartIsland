/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LmoFreeformTest {
    private val dump = """
        RootTask id=5 bounds=[0,0][1080,2400] windowingMode=fullscreen
          taskId=212: com.whatsapp/com.whatsapp.Main bounds=[0,0][1080,2400] userId=0 visible=true topActivity=ComponentInfo{com.whatsapp/com.whatsapp.Conversation}
          taskId=198: com.whatsapp/.Main bounds=[0,0][1080,2400] userId=0 visible=false topActivity=ComponentInfo{com.whatsapp/com.whatsapp.Main}
        RootTask id=1 bounds=[0,0][1080,2400] windowingMode=fullscreen
          taskId=7: com.android.launcher3/.Launcher userId=10 visible=false
    """.trimIndent()

    @Test
    fun parsesTasksWithTopActivityAndUser() {
        val tasks = LmoFreeform.parseTasks(dump)
        assertEquals(listOf(212, 198, 7), tasks.map { it.taskId })
        assertEquals("com.whatsapp.Conversation", tasks[0].className)
        assertEquals("com.whatsapp.Main", tasks[1].className)
        assertEquals("com.android.launcher3.Launcher", tasks[2].className)
        assertEquals(10, tasks[2].userId)
    }

    @Test
    fun picksTheNewTaskOfThePackage() {
        val t = LmoFreeform.pickTask(LmoFreeform.parseTasks(dump), setOf(198, 7), "com.whatsapp")
        assertEquals(212, t?.taskId)
    }

    @Test
    fun reusedTaskFallsBackToFrontMostOfThePackage() {
        val t = LmoFreeform.pickTask(LmoFreeform.parseTasks(dump), setOf(212, 198, 7), "com.whatsapp")
        assertEquals(212, t?.taskId)
    }

    @Test
    fun noTaskForPackageIsNull() {
        assertNull(LmoFreeform.pickTask(LmoFreeform.parseTasks(dump), emptySet(), "org.telegram.messenger"))
        assertEquals(emptyList<Int>(), LmoFreeform.parseTasks("").map { it.taskId })
    }

    @Test
    fun summarizeListsOnlyThePackagesTasks() {
        val tasks = LmoFreeform.parseTasks(dump)
        assertEquals("[212@u0(Conversation), 198@u0(Main)]", LmoFreeform.summarize(tasks, "com.whatsapp"))
        assertEquals("[]", LmoFreeform.summarize(tasks, "org.telegram.messenger"))
    }

    @Test
    fun rawLinesForShowsPackageLinesOrTheFirstLines() {
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
