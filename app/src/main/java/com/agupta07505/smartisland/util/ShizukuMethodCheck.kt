/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The "try the Shizuku methods" check offered during setup. Shell commands differ between phones and Android versions, so
 * instead of guessing, the app tries each Shizuku method once and remembers what worked. The Utilities page then uses a
 * shell method only when it passed, and falls back to the system page (or the quick settings) when it did not.
 *
 * Wi-Fi, mobile data, the screen recorder and cache trimming are only probed (nothing is switched). The hotspot is really
 * switched on and off once, because that is the only way to know the command works; the person is told first.
 */
object ShizukuMethodCheck {
    enum class Method(val id: String) {
        Shell("shell"),
        WifiData("wifi_data"),
        ScreenRecorder("screen_recorder"),
        TrimCache("trim_cache"),
        Hotspot("hotspot")
    }

    enum class Outcome { Works, Missing, NotTested }

    data class Result(val method: Method, val outcome: Outcome)

    private const val PREFS = "shizuku_method_checks"
    private const val PREF_CHECKED = "checked"
    internal const val OK_MARKER = "SI_OK"

    /** The read-only probe of a method: prints [OK_MARKER] when the tool is there. Null for the hotspot, which is tested for real. */
    internal fun probeCommand(method: Method): String? = when (method) {
        Method.Shell -> "id >/dev/null 2>&1 && echo $OK_MARKER"
        Method.WifiData -> "command -v svc >/dev/null 2>&1 && echo $OK_MARKER"
        Method.ScreenRecorder -> "command -v screenrecord >/dev/null 2>&1 && echo $OK_MARKER"
        Method.TrimCache -> "pm help 2>&1 | grep -q trim-caches && echo $OK_MARKER"
        Method.Hotspot -> null
    }

    internal fun probePassed(output: String?): Boolean = output?.contains(OK_MARKER) == true

    fun hasRun(context: Context): Boolean = prefs(context).getBoolean(PREF_CHECKED, false)

    /** The outcomes of the last check; every method is [Outcome.NotTested] before the first one. */
    fun saved(context: Context): Map<Method, Outcome> {
        val p = prefs(context)
        return Method.entries.associateWith { m ->
            when (p.getString(m.id, null)) {
                "works" -> Outcome.Works
                "missing" -> Outcome.Missing
                else -> Outcome.NotTested
            }
        }
    }

    fun works(context: Context, method: Method): Boolean = saved(context)[method] == Outcome.Works

    /** Whether a shell method may be used: Shizuku is ready and the method did not fail a check. */
    fun usable(context: Context, method: Method): Boolean {
        if (!ShizukuManager.isBinderAvailable() || !ShizukuManager.hasPermission()) return false
        return saved(context)[method] != Outcome.Missing
    }

    /**
     * Runs the check and saves the outcomes. [testHotspot] switches the hotspot on and off once; without it the
     * hotspot is left as it was (not tested).
     */
    suspend fun run(context: Context, testHotspot: Boolean): List<Result> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val results = ArrayList<Result>()
        if (!ShizukuManager.hasPermission()) {
            Method.entries.forEach { results.add(Result(it, Outcome.NotTested)) }
            return@withContext results
        }
        for (method in Method.entries) {
            val probe = probeCommand(method)
            val outcome = when {
                probe != null -> {
                    val out = ShizukuManager.runShellForOutput(probe).getOrNull()
                    if (probePassed(out)) Outcome.Works else Outcome.Missing
                }
                !testHotspot -> saved(app)[Method.Hotspot] ?: Outcome.NotTested
                else -> testHotspotToggle(app)
            }
            results.add(Result(method, outcome))
        }
        save(app, results)
        results
    }

    private suspend fun testHotspotToggle(app: Context): Outcome {
        val wasOn = HotspotUtil.isOn(app)
        if (wasOn) return Outcome.NotTested // leave a hotspot that is already in use alone
        ShizukuManager.runShellForOutput(HotspotUtil.START_COMMAND)
        val cameOn = waitFor(on = true) { HotspotUtil.isOn(app) }
        // Always try to put it back, whatever the answer.
        ShizukuManager.runShellForOutput(HotspotUtil.STOP_COMMAND)
        waitFor(on = false) { HotspotUtil.isOn(app) }
        return if (cameOn) Outcome.Works else Outcome.Missing
    }

    private suspend fun waitFor(on: Boolean, read: () -> Boolean): Boolean {
        repeat(12) {
            if (read() == on) return true
            delay(500)
        }
        return read() == on
    }

    private fun save(app: Context, results: List<Result>) {
        prefs(app).edit().apply {
            results.forEach { r ->
                when (r.outcome) {
                    Outcome.Works -> putString(r.method.id, "works")
                    Outcome.Missing -> putString(r.method.id, "missing")
                    Outcome.NotTested -> Unit // keep what an earlier check found
                }
            }
            putBoolean(PREF_CHECKED, true)
        }.apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
