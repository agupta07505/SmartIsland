/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context

/** App widgets on the Utilities page. */
object UtilityWidgets {
    private const val HOST_ID = 0x5349
    private val lock = Any()
    private var host: AppWidgetHost? = null
    private var listeners = 0

    private fun host(context: Context): AppWidgetHost = synchronized(lock) {
        host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }
    }

    fun manager(context: Context): AppWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    fun acquire(context: Context) = synchronized(lock) {
        if (listeners++ == 0) runCatching { host(context).startListening() }
        Unit
    }

    fun release(context: Context) = synchronized(lock) {
        if (listeners > 0 && --listeners == 0) runCatching { host(context).stopListening() }
        Unit
    }

    fun installedProviders(context: Context): List<AppWidgetProviderInfo> {
        val pm = context.packageManager
        return runCatching { manager(context).installedProviders }.getOrDefault(emptyList())
            .sortedBy { it.loadLabel(pm)?.lowercase().orEmpty() }
    }

    fun allocateId(context: Context): Int = host(context).allocateAppWidgetId()

    fun deleteId(context: Context, widgetId: Int) {
        runCatching { host(context).deleteAppWidgetId(widgetId) }
    }

    fun bindIfAllowed(context: Context, widgetId: Int, provider: ComponentName): Boolean =
        runCatching { manager(context).bindAppWidgetIdIfAllowed(widgetId, provider) }.getOrDefault(false)

    fun bindIntent(widgetId: Int, provider: ComponentName): android.content.Intent =
        android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)

    fun info(context: Context, widgetId: Int): AppWidgetProviderInfo? =
        runCatching { manager(context).getAppWidgetInfo(widgetId) }.getOrNull()

    fun createView(context: Context, widgetId: Int): AppWidgetHostView? {
        val info = info(context, widgetId) ?: return null
        return runCatching { host(context).createView(context, widgetId, info) }.getOrNull()
    }

    fun resize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        runCatching { view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp) }
    }

    private const val REQUEST_CONFIGURE = 0x5350
    private var pendingConfigure: ((Boolean) -> Unit)? = null

    /** Opens the widget's own setup screen; [onDone] gets whether the user finished it. */
    fun configure(activity: Activity, widgetId: Int, onDone: (Boolean) -> Unit) {
        pendingConfigure = onDone
        val started = runCatching {
            host(activity).startAppWidgetConfigureActivityForResult(activity, widgetId, 0, REQUEST_CONFIGURE, null)
        }.isSuccess
        if (!started) {
            pendingConfigure = null
            onDone(false)
        }
    }

    /** Called from the activity's onActivityResult; true when the result was the configure step's. */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_CONFIGURE) return false
        val done = pendingConfigure
        pendingConfigure = null
        done?.invoke(resultCode == Activity.RESULT_OK)
        return true
    }
}
