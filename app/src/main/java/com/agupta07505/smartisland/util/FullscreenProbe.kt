/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.TextView
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/** Switch of the fullscreen detection test (Developer options), not saved: it is off after the app restarts. */
object FullscreenProbe {
    val enabled = MutableStateFlow(false)
}

/**
 * Developer test for spotting a fullscreen app (video, game, immersive page, status bar hidden) with only what the
 * island can read, no root or Shizuku. A small text panel floats over every app while it runs, shows the signals live
 * and each change goes to the logs:
 *
 *  - A: the status bar's visibility as seen by a plain overlay window of our own (default flags)
 *  - B: the same read from the island's real window (it uses the no-limits flags)
 *  - C: the foreground app window's bounds against the screen (the accessibility service's own window list)
 *  - S: the SystemUI windows the service can see (the bar's own window, whether it is listed and where)
 */
@SuppressLint("SetTextI18n")
class FullscreenProbeRunner(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
    private val islandView: () -> View?
) {
    private val handler = Handler(Looper.getMainLooper())
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val changes = ArrayDeque<String>()
    private val lastSeen = HashMap<String, String>()
    private var edge: View? = null
    private var panel: TextView? = null
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            refresh()
            handler.postDelayed(this, POLL_MS)
        }
    }

    fun start() {
        if (running) return
        running = true
        AppLogRecorder.i(
            TAG, "started sdk=${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL}"
        )
        runCatchingLogged(TAG, "Probe windows could not be added") {
            addEdgeWindow()
            addPanel()
        }
        handler.post(tick)
    }

    fun stop() {
        if (!running && edge == null && panel == null) return
        running = false
        handler.removeCallbacks(tick)
        listOfNotNull(edge, panel).forEach { view ->
            runCatchingLogged(TAG, "Probe window could not be removed") {
                if (view.isAttachedToWindow) windowManager.removeViewImmediate(view)
            }
        }
        edge = null
        panel = null
        changes.clear()
        lastSeen.clear()
        AppLogRecorder.i(TAG, "stopped")
    }

    /** A thin, see-through, untouchable window down the left edge with plain flags: what an ordinary overlay sees. */
    private fun addEdgeWindow() {
        val view = View(service)
        view.setOnApplyWindowInsetsListener { v, insets ->
            handler.post { if (running) refresh() }
            v.onApplyWindowInsets(insets)
        }
        val params = WindowManager.LayoutParams(
            1,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        windowManager.addView(view, params)
        edge = view
    }

    /** The readout, low on the screen so it stays visible while the status bar is hidden, and never takes a touch. */
    private fun addPanel() {
        val density = service.resources.displayMetrics.density
        val view = TextView(service).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
            setBackgroundColor(0xCC000000.toInt())
            val pad = (6 * density).toInt()
            setPadding(pad, pad, pad, pad)
            text = "Fullscreen probe starting..."
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (90 * density).toInt()
        }
        windowManager.addView(view, params)
        panel = view
    }

    private fun refresh() {
        val readout = runCatchingLogged(TAG, "Probe could not be read") {
            val a = readInsets(edge)
            val b = readInsets(islandView())
            val c = readAppWindow()
            val s = readSystemUiWindows()
            note("A", a.state())
            note("B", b.state())
            note("C", c.state)
            note("S", s)
            buildString {
                appendLine("FULLSCREEN PROBE (sdk ${Build.VERSION.SDK_INT})")
                appendLine("A edge window : ${a.describe()}")
                appendLine("B island window: ${b.describe()}")
                appendLine("C app window  : ${c.describe}")
                appendLine("S SystemUI    : $s")
                if (changes.isNotEmpty()) {
                    appendLine("-- changes --")
                    changes.forEach { appendLine(it) }
                }
            }.trimEnd()
        } ?: return
        panel?.text = readout
    }

    /** Remembers the first reading silently, then keeps the last few changes and writes each one to the logs. */
    private fun note(signal: String, state: String) {
        val before = lastSeen.put(signal, state)
        if (before == null || before == state) return
        val line = "${clock.format(Date())} $signal $before -> $state"
        AppLogRecorder.i(TAG, line)
        changes.addLast(line)
        while (changes.size > MAX_CHANGES) changes.removeFirst()
    }

    private class InsetsReading(
        val available: Boolean,
        val statusVisible: Boolean,
        val statusHeight: Int,
        val statusFullHeight: Int,
        val navVisible: Boolean
    ) {
        fun state() = if (!available) "n/a" else if (statusVisible) "status visible" else "status HIDDEN"

        fun describe() = if (!available) {
            "n/a (no window or Android below 11)"
        } else {
            "${state()}  inset=${statusHeight}px full=${statusFullHeight}px nav=" +
                if (navVisible) "visible" else "hidden"
        }
    }

    private fun readInsets(view: View?): InsetsReading {
        val none = InsetsReading(false, false, 0, 0, false)
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return none
        val raw = view.rootWindowInsets ?: return none
        return InsetsReading(
            available = true,
            statusVisible = raw.isVisible(WindowInsets.Type.statusBars()),
            statusHeight = raw.getInsets(WindowInsets.Type.statusBars()).top,
            statusFullHeight = raw.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top,
            navVisible = raw.isVisible(WindowInsets.Type.navigationBars())
        )
    }

    private class AppWindowReading(val state: String, val describe: String)

    /** The window the user is in, from the service's window list, against the whole screen. */
    @Suppress("DEPRECATION")
    private fun readAppWindow(): AppWindowReading {
        val windows = service.windows.orEmpty()
        val apps = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        val window = apps.firstOrNull { it.isFocused } ?: apps.firstOrNull { it.isActive }
            ?: return AppWindowReading("no app window", "no app window")
        val bounds = Rect()
        window.getBoundsInScreen(bounds)
        val screen = screenBoundsOf(service, windowManager)
        val root: AccessibilityNodeInfo? = window.root
        val pkg = root?.packageName?.toString() ?: "?"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) root?.recycle()
        val coversTop = bounds.top <= 0
        val coversAll = coversTop && bounds.bottom >= screen.height()
        val state = if (coversAll) "covers whole screen" else if (coversTop) "reaches top" else "starts below top"
        return AppWindowReading(
            state,
            "$pkg\n                [${bounds.left},${bounds.top}-${bounds.right},${bounds.bottom}] " +
                "screen ${screen.width()}x${screen.height()} -> $state"
        )
    }

    /** The SystemUI windows that are not tall (the status bar's own, the nav bar), with where they are. */
    private fun readSystemUiWindows(): String {
        val screenHeight = screenBoundsOf(service, windowManager).height()
        val bars = service.windows.orEmpty().filter {
            val bounds = Rect().also { rect -> it.getBoundsInScreen(rect) }
            it.type == AccessibilityWindowInfo.TYPE_SYSTEM && bounds.height() in 1 until screenHeight / 4
        }
        if (bars.isEmpty()) return "no short system windows"
        return bars.joinToString(" | ") {
            val bounds = Rect().also { rect -> it.getBoundsInScreen(rect) }
            "${it.title ?: "untitled"} [${bounds.left},${bounds.top}-${bounds.right},${bounds.bottom}]"
        }
    }

    private companion object {
        const val TAG = "FullscreenProbe"
        const val POLL_MS = 400L
        const val MAX_CHANGES = 6
    }
}
