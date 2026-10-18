/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * ADB mode for Rule_Book 10.2: hides status-bar icons that crowd the island by writing SystemUI's
 * `Settings.Secure` key [KEY] ("icon_blacklist"; the Rule_Book's "sysui_icon_blacklist" is the same thing
 * under its SystemUI tuner name). Needs WRITE_SECURE_SETTINGS, which only ADB (or Shizuku) can grant:
 * `adb shell pm grant <applicationId> android.permission.WRITE_SECURE_SETTINGS`.
 *
 * The key lists status-bar *slots*, so this hides system clutter icons ([HIDDEN_SLOTS]) and keeps
 * battery, clock, Wi-Fi and mobile signal. Stock Android has no slot for app notification icons, so those
 * stay unless the ROM offers its own switch.
 *
 * Enabling remembers the previous value; disabling removes only the slots this app added, so anything the
 * user or another tool put in the list survives.
 */
object StatusBarIconBlacklist {

    const val KEY = "icon_blacklist"

    /** Clutter slots hidden by default. AOSP slot names; unknown names are ignored by SystemUI. */
    val HIDDEN_SLOTS: List<String> = listOf(
        "alarm_clock", "bluetooth", "cast", "data_saver", "headset", "hotspot", "location",
        "managed_profile", "nfc", "rotate", "screen_record", "vpn", "volume", "zen"
    )

    private const val PREFS = "status_bar_icon_blacklist"
    private const val PREF_ENABLED = "enabled"
    private const val PREF_PREVIOUS = "previous"
    private const val PREF_HAD_PREVIOUS = "had_previous"
    private const val TAG = "StatusBarIconBlacklist"

    fun adbGrantCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    fun isPermissionGranted(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_ENABLED, false)

    /** Writes the blacklist. Returns false (and changes nothing) without the permission. */
    fun enable(context: Context): Boolean {
        if (!isPermissionGranted(context)) return false
        return runCatchingLogged(TAG, "Enabling icon blacklist failed") {
            val resolver = context.contentResolver
            val current = Settings.Secure.getString(resolver, KEY)
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(PREF_ENABLED, false)) {
                prefs.edit()
                    .putBoolean(PREF_HAD_PREVIOUS, current != null)
                    .putString(PREF_PREVIOUS, current)
                    .apply()
            }
            Settings.Secure.putString(resolver, KEY, merge(current, HIDDEN_SLOTS))
            prefs.edit().putBoolean(PREF_ENABLED, true).apply()
            true
        } ?: false
    }

    /** Takes this app's slots back out of the blacklist. */
    fun disable(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREF_ENABLED, false)) return true
        if (!isPermissionGranted(context)) return false
        return runCatchingLogged(TAG, "Restoring icon blacklist failed") {
            val resolver = context.contentResolver
            val previous = if (prefs.getBoolean(PREF_HAD_PREVIOUS, false)) prefs.getString(PREF_PREVIOUS, null) else null
            val restored = restore(Settings.Secure.getString(resolver, KEY), previous, HIDDEN_SLOTS)
            Settings.Secure.putString(resolver, KEY, restored)
            prefs.edit().clear().apply()
            true
        } ?: false
    }

    /** The slots in a stored value, trimmed, empties dropped, order kept. */
    fun parse(value: String?): List<String> =
        value.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /** [current] plus every slot in [slots] it lacks, keeping the existing order first. */
    fun merge(current: String?, slots: List<String>): String =
        (parse(current) + slots).distinct().joinToString(",")

    /**
     * [current] without the slots this app added: those in [slots] that were not in [previous]. Returns
     * null when nothing is left and there was no value before, so the key goes back to unset.
     */
    fun restore(current: String?, previous: String?, slots: List<String>): String? {
        val keep = parse(previous).toSet()
        val added = slots.filterNot { it in keep }.toSet()
        val remaining = parse(current).filterNot { it in added }
        if (remaining.isEmpty()) return if (previous == null) null else ""
        return remaining.joinToString(",")
    }
}
