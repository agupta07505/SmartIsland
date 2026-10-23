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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import com.agupta07505.smartisland.model.IslandNotificationAction

/** Reads what an app put into its notification through custom [RemoteViews] instead of the standard fields. */
object NotificationViewInspector {

    data class Result(
        /** Time the view's chronometer has been running, if the view has one. */
        val chronometerElapsedMs: Long?,
        /** Clickable buttons found in the view, in layout order. */
        val buttons: List<IslandNotificationAction>,
        /** Time left on a count-down chronometer (a running clock timer), from its base; null if none or expired. */
        val countdownRemainingMs: Long? = null,
        /** Clock time the view prints ("04:59"), in seconds: the chronometer's text, else the first clock-only text. */
        val shownTimeSeconds: Long? = null,
        /** True when [shownTimeSeconds] came from the chronometer. */
        val shownFromChronometer: Boolean = false,
        /** Labels of clickable views the shade does not draw (clock apps hide buttons of the other state). Log only. */
        val hiddenButtons: List<String> = emptyList(),
        /** The drawn chronometer's base on the elapsedRealtime clock, or null. */
        val chronometerBaseMs: Long? = null,
        /** True when the drawn chronometer counts down. */
        val chronometerCountDown: Boolean = false
    )

    @Suppress("DEPRECATION")
    fun inspect(context: Context, notification: Notification, key: String): Result? {
        // Largest layout first: it has the full set of buttons.
        val candidates = listOfNotNull(
            notification.bigContentView,
            notification.contentView,
            notification.headsUpContentView
        )
        if (candidates.isEmpty()) return null

        var elapsed: Long? = null
        var countdown: Long? = null
        var shown: Long? = null
        var shownFromChrono = false
        var buttons: List<IslandNotificationAction> = emptyList()
        var buttonViews: List<View> = emptyList()
        var hidden: List<String> = emptyList()
        var chronoBase: Long? = null
        var chronoCountDown = false
        for (views in candidates) {
            val (result, kept) = runCatching { inflate(context, views, key) }
                .onFailure { android.util.Log.d(TAG, "Could not inflate notification views: ${it.message}") }
                .getOrNull() ?: continue
            if (elapsed == null) elapsed = result.chronometerElapsedMs
            if (countdown == null) countdown = result.countdownRemainingMs
            if (chronoBase == null && result.chronometerBaseMs != null) {
                chronoBase = result.chronometerBaseMs
                chronoCountDown = result.chronometerCountDown
            }
            if (shown == null && result.shownTimeSeconds != null) {
                shown = result.shownTimeSeconds
                shownFromChrono = result.shownFromChronometer
            }
            if (buttons.isEmpty()) {
                buttons = result.buttons
                buttonViews = kept
                hidden = result.hiddenButtons
            }
            if ((elapsed != null || countdown != null || shown != null) && buttons.isNotEmpty()) break
        }
        // Only the layout whose buttons the island shows is kept for pressing.
        if (buttonViews.isEmpty()) {
            liveButtons.remove(key)
            liveLabels.remove(key)
        } else {
            liveButtons[key] = buttonViews
            liveLabels[key] = buttons.map { it.title }
        }
        if (elapsed == null && countdown == null && shown == null && buttons.isEmpty()) return null
        android.util.Log.d(
            TAG,
            "Inspected custom views: elapsedMs=$elapsed countdownMs=$countdown shownSec=$shown buttons=${buttons.map { it.title }}" +
                " hiddenButtons=$hidden chronoBase=$chronoBase chronoCountDown=$chronoCountDown"
        )
        return Result(elapsed, buttons, countdown, shown, shownFromChrono, hidden, chronoBase, chronoCountDown)
    }

    private fun inflate(context: Context, views: RemoteViews, key: String): Pair<Result, List<View>> {
        // Send the app's own intent ourselves (background start allowed, fallbacks, result logged).
        val root = applyWithPressHandler(context, views) ?: views.apply(context, FrameLayout(context))

        var chronometer: Chronometer? = null
        var otherClockText: Long? = null
        val clickable = ArrayList<View>()
        walk(root) { v ->
            if (v.hasOnClickListeners()) clickable.add(v)
            // Only drawn views count; a hidden one holds the other state's value.
            if (v is Chronometer && chronometer == null && isDrawn(v)) chronometer = v
            // A plain clock-only text: what a paused timer prints.
            if (v is TextView && v !is Chronometer && otherClockText == null && isDrawn(v)) {
                otherClockText = clockOnly(v.text?.toString())
            }
        }

        // One button per label (an icon and its text share one); undrawn views are skipped.
        val kept = ArrayList<View>()
        val buttons = ArrayList<IslandNotificationAction>()
        val hidden = ArrayList<String>()
        for (v in clickable) {
            val label = buttonLabel(v)
            if (!isDrawn(v)) {
                hidden.add(label)
                continue
            }
            if (buttons.any { it.title == label }) continue
            buttons.add(
                IslandNotificationAction(title = label, pendingIntent = clickIntent(context, key, kept.size), viaView = true)
            )
            kept.add(v)
        }

        val elapsed = chronometer?.let { c ->
            if (c.isCountDown) null else (SystemClock.elapsedRealtime() - c.base).takeIf { it >= 0 }
        }
        val countdown = chronometer?.let { c ->
            if (c.isCountDown) (c.base - SystemClock.elapsedRealtime()).takeIf { it > 0 } else null
        }
        // The chronometer's own text is what the shade shows.
        val chronoText = chronometer?.let { clockOnly(it.text?.toString()) }
        val shown = chronoText ?: otherClockText
        return Result(
            elapsed, buttons, countdown, shown, chronoText != null, hidden,
            chronometerBaseMs = chronometer?.base,
            chronometerCountDown = chronometer?.isCountDown == true
        ) to kept
    }

    /** True when the view and all its parents are VISIBLE and not transparent ([View.isShown] needs a window). */
    internal fun isDrawn(view: View): Boolean {
        var v: View? = view
        while (v != null) {
            if (v.visibility != View.VISIBLE || v.alpha <= 0f) return false
            v = v.parent as? View
        }
        return true
    }

    private fun buttonLabel(v: View): String {
        (v as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        if (v is ViewGroup) {
            var inner: String? = null
            walk(v) { c ->
                if (inner == null && c is TextView && c !is Chronometer && c.visibility == View.VISIBLE) {
                    inner = c.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                }
            }
            inner?.let { return it }
        }
        return v.contentDescription?.toString()?.takeIf { it.isNotBlank() } ?: idName(v)
    }

    private fun clockOnly(text: String?): Long? {
        val t = text?.trim().orEmpty()
        if (t.isEmpty() || t.length > 9 || !CLOCK_ONLY.matches(t)) return null
        return TimerStopwatchParser.parseClockTimeToSeconds(t)
    }

    private fun clickIntent(context: Context, key: String, index: Int): PendingIntent {
        val uri = Uri.Builder()
            .scheme(SCHEME)
            .authority("click")
            .appendQueryParameter("key", key)
            .appendQueryParameter("i", index.toString())
            .build()
        val intent = Intent(ACTION_CLICK, uri).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Presses the kept view for a [clickIntent] broadcast, on the main thread. True if a view was pressed. */
    fun click(uri: Uri?): Boolean {
        val key = uri?.getQueryParameter("key")
        val index = uri?.getQueryParameter("i")?.toIntOrNull()
        val view = if (key != null && index != null) liveButtons[key]?.getOrNull(index) else null
        val label = if (key != null && index != null) liveLabels[key]?.getOrNull(index) else null
        if (view == null) {
            // The clock re-posted or the listener reconnected.
            AppLogRecorder.w(TAG, "ViewPress: no kept view for key=$key index=$index, nothing pressed")
            return false
        }
        val pressed = runCatching { view.performClick() }
            .onFailure { AppLogRecorder.w(TAG, "ViewPress: could not press '$label': ${it.message}") }
            .isSuccess
        AppLogRecorder.d(TAG, "ViewPress: '$label' key=$key index=$index pressed=$pressed")
        return pressed
    }

    fun liveLabels(key: String): List<String> = liveLabels[key].orEmpty()

    @android.annotation.SuppressLint("PrivateApi", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    private fun applyWithPressHandler(context: Context, views: RemoteViews): View? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return try {
            val handlerClass = Class.forName("android.widget.RemoteViews\$InteractionHandler")
            val handler = java.lang.reflect.Proxy.newProxyInstance(
                handlerClass.classLoader,
                arrayOf(handlerClass)
            ) { proxy, method, args ->
                when (method.name) {
                    "onInteraction" -> {
                        val pendingIntent = args?.getOrNull(1) as? PendingIntent
                        val sent = pendingIntent != null &&
                            com.agupta07505.smartisland.ui.expanded.sendIntentWithOptions(context, pendingIntent)
                        AppLogRecorder.d(TAG, "ViewPress: app's own intent sent=$sent")
                        sent
                    }
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.getOrNull(0)
                    "toString" -> "SmartIslandPressHandler"
                    else -> null
                }
            }
            val apply = RemoteViews::class.java.getMethod(
                "apply", Context::class.java, ViewGroup::class.java, handlerClass
            )
            apply.invoke(views, context, FrameLayout(context), handler) as? View
        } catch (t: Throwable) {
            AppLogRecorder.w(TAG, "ViewPress: handler apply unavailable, using default (${t.javaClass.simpleName})")
            null
        }
    }

    fun forget(key: String) {
        liveButtons.remove(key)
        liveLabels.remove(key)
    }

    fun forgetAll() {
        liveButtons.clear()
        liveLabels.clear()
    }

    private fun walk(view: View, visit: (View) -> Unit) {
        visit(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), visit)
        }
    }

    private fun idName(view: View): String = runCatching {
        if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id) else ""
    }.getOrDefault("").replace('_', ' ')

    const val ACTION_CLICK = "com.agupta07505.smartisland.action.PRESS_NOTIFICATION_VIEW"
    const val SCHEME = "smartisland-view"
    private const val TAG = "NotificationViewInspector"
    private val CLOCK_ONLY = Regex("""-?(?:\d{1,2}:)?\d{1,2}:\d{2}""")

    private val liveButtons = java.util.concurrent.ConcurrentHashMap<String, List<View>>()

    private val liveLabels = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
}
