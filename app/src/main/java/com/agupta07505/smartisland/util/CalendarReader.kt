/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.model.CalendarRules
import com.agupta07505.smartisland.model.NextEvent

/** Reads the next event from the phone's visible calendars. Needs READ_CALENDAR. */
internal object CalendarReader {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** The soonest timed event that begins after [nowMs] within the next 24 hours; all-day, cancelled and declined events are skipped. */
    fun nextEvent(context: Context, nowMs: Long): NextEvent? {
        if (!hasPermission(context)) return null
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, nowMs)
            ContentUris.appendId(it, nowMs + CalendarRules.LOOK_AHEAD_MS)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Events.STATUS,
            CalendarContract.Events.SELF_ATTENDEE_STATUS
        )
        val events = ArrayList<NextEvent>()
        runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val allDay = c.getInt(3) == 1
                    val cancelled = !c.isNull(4) && c.getInt(4) == CalendarContract.Events.STATUS_CANCELED
                    val declined = !c.isNull(5) && c.getInt(5) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                    if (allDay || cancelled || declined) continue
                    events += NextEvent(title = c.getString(0).orEmpty(), beginMs = c.getLong(1), endMs = c.getLong(2))
                }
            }
        }
        return CalendarRules.pickNext(events, nowMs)
    }
}
