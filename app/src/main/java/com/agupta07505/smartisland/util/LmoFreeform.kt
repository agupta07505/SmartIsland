/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** LMO Freeform (LibreMobileOS, bundled in several custom ROMs). */
object LmoFreeform {
    const val PACKAGE = "com.libremobileos.freeform"
    const val ACTION_START = "com.libremobileos.freeform.START_FREEFORM"
    const val EXTRA_PACKAGE = "packageName"
    const val EXTRA_ACTIVITY = "activityName"
    const val EXTRA_USER_ID = "userId"
    const val EXTRA_TASK_ID = "taskId"

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Null if the app has no launcher activity. */
    fun launcherComponent(pm: PackageManager, packageName: String): ComponentName? =
        pm.getLaunchIntentForPackage(packageName)?.component

    /**
     * The activity an activity-type PendingIntent would open, asked of the system (Android 14+
     * `queryIntentComponents`, reached by reflection because it is not in the public SDK).
     */
    fun pendingIntentActivity(pending: PendingIntent): ComponentName? = try {
        val infos = PendingIntent::class.java.getMethod("queryIntentComponents", Int::class.javaPrimitiveType)
            .invoke(pending, 0) as? List<*>
        val info = (infos?.firstOrNull() as? android.content.pm.ResolveInfo)?.activityInfo
        info?.let { ComponentName(it.packageName, it.name) }
    } catch (_: Throwable) {
        null
    }

    fun startIntent(component: ComponentName, userId: Int = 0, taskId: Int = -1): Intent =
        Intent(ACTION_START).apply {
            setPackage(PACKAGE)
            putExtra(EXTRA_PACKAGE, component.packageName)
            putExtra(EXTRA_ACTIVITY, component.className)
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_TASK_ID, taskId)
        }

    /** A running task as listed by `am stack list`. [component] is its top activity (the real chat screen). */
    data class TaskInfo(val taskId: Int, val packageName: String, val className: String, val userId: Int) {
        /** Built on demand so parsing stays plain strings (android.content.ComponentName is a stub in JVM unit tests). */
        val component: ComponentName get() = ComponentName(packageName, className)
    }

    /** Shell command that lists tasks; `cmd activity stack list` is the same thing on builds without `am stack`. */
    const val LIST_TASKS_COMMAND = "am stack list 2>/dev/null || cmd activity stack list 2>/dev/null"

    // "  taskId=123: com.pkg/com.pkg.Act bounds=[..] userId=0 visible=true topActivity=ComponentInfo{com.pkg/com.pkg.Act}"
    private val TASK_LINE = Regex("""taskId=(\d+):\s*([\w.]+)/([\w.$]+)(.*)""")
    private val USER_ID = Regex("""userId=(\d+)""")
    private val TOP_ACTIVITY = Regex("""topActivity=ComponentInfo\{([\w.]+)/([\w.$]+)\}""")

    /** Front-most task first. Pure text parsing. */
    fun parseTasks(output: String): List<TaskInfo> = output.lineSequence().mapNotNull { line ->
        val m = TASK_LINE.find(line) ?: return@mapNotNull null
        val rest = m.groupValues[4]
        val top = TOP_ACTIVITY.find(rest)
        val pkg = top?.groupValues?.get(1) ?: m.groupValues[2]
        val cls = top?.groupValues?.get(2) ?: m.groupValues[3]
        TaskInfo(
            taskId = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null,
            packageName = pkg,
            className = if (cls.startsWith(".")) pkg + cls else cls,
            userId = USER_ID.find(rest)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        )
    }.toList()

    /**
     * The task a just-opened notification landed in: the newest task of [packageName] that was not in [before] (task
     * ids only grow); if the app reused a task it already had, its front-most one.
     */
    fun pickTask(after: List<TaskInfo>, before: Set<Int>, packageName: String): TaskInfo? {
        val mine = after.filter { it.packageName == packageName }
        return mine.filter { it.taskId !in before }.maxByOrNull { it.taskId } ?: mine.firstOrNull()
    }

    /** Lists LMO's per-window virtual displays: `lmo-freeform:<pkg>,<activity>,<user>", displayId N`. */
    const val LIST_DISPLAYS_COMMAND = "dumpsys display 2>/dev/null | grep -o 'lmo-freeform:[^\"]*\", displayId [0-9]*'"

    private val DISPLAY_LINE = Regex("""lmo-freeform:([\w.]+),[^"]*",\s*displayId\s+(\d+)""")
    private val ROOT_LINE = Regex("""RootTask id=\d+.*displayId=(\d+)""")

    fun parseDisplayIds(output: String, packageName: String): Set<Int> =
        DISPLAY_LINE.findAll(output)
            .filter { it.groupValues[1] == packageName }
            .mapNotNull { it.groupValues[2].toIntOrNull() }
            .toSet()

    /** taskId -> displayId, read from `am stack list` (task lines sit under their RootTask line). */
    fun taskDisplays(output: String): Map<Int, Int> {
        val result = HashMap<Int, Int>()
        var display = 0
        for (line in output.lineSequence()) {
            ROOT_LINE.find(line)?.let { display = it.groupValues[1].toIntOrNull() ?: display }
            TASK_LINE.find(line)?.let { m -> m.groupValues[1].toIntOrNull()?.let { result[it] = display } }
        }
        return result
    }

    fun summarize(tasks: List<TaskInfo>, packageName: String): String =
        tasks.filter { it.packageName == packageName }
            .joinToString(prefix = "[", postfix = "]") { "${it.taskId}@u${it.userId}(${it.className.substringAfterLast('.')})" }

    /**
     * Raw `am stack list` lines that mention [packageName] (trimmed). When nothing matches, the first lines of the
     * output are returned instead, which shows a format [TASK_LINE] does not understand.
     */
    fun rawLinesFor(output: String, packageName: String, maxLines: Int = 6, maxChars: Int = 260): String {
        val lines = output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val hits = lines.filter { packageName in it }.take(maxLines)
        val shown = hits.ifEmpty { lines.take(3) }
        val tag = if (hits.isEmpty()) "no line mentions $packageName; output has ${lines.size} lines, first:" else "lines:"
        return tag + shown.joinToString(prefix = " | ", separator = " | ") { it.take(maxChars) }
    }
}
