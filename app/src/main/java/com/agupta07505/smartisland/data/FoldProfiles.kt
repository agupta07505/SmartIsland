/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.agupta07505.smartisland.model.FoldPosture

object FoldProfiles {
    const val SEPARATOR = "@"

    /**
     * Where the camera is on this screen. A landscape mode derives its camera from the portrait position of the same
     * screen, so these follow that portrait mode's own values
     */
    val CameraKeys: Set<String> = setOf(
        "camera_cutout_style", "camera_cutout_manual",
        "camera_cutout_x", "camera_cutout_y", "camera_cutout_width", "camera_cutout_height"
    )

    /** What the Portrait profile (Home and Layout tabs, portrait) holds. */
    val PortraitKeys: Set<String> = CameraKeys + setOf(
        "width", "height", "x_offset", "y_offset", "corner_radius", "opacity",
        "enable_shadow", "shadow_elevation", "circle_position",
        "enable_notch_mode", "saved_compact_layout", "saved_notch_layout",
        "disabled_events", "no_auto_expand_events",
        "content_pill_same_as_idle", "content_pill_width_percent",
        "anim_speed_portrait", "merge_style",
        "idle_widgets_enabled", "idle_off_after_tap_close", "idle_off_after_long_press", "idle_off_when_stored"
    )

    val LandscapeKeys: Set<String> = setOf(
        "show_in_landscape",
        "landscape_width", "landscape_height", "landscape_x_offset", "landscape_y_offset",
        "landscape_corner_radius", "landscape_opacity",
        "landscape_circle_size", "landscape_circle_opacity", "landscape_circle_edge_inset",
        "landscape_circle_y_offset", "landscape_circle_x_adjust", "landscape_circle_y_adjust",
        "landscape_pill_position", "landscape_layout_style",
        "edge_pill_tuning", "saved_edge_flat_look", "saved_edge_round_look",
        "disabled_events_landscape", "no_auto_expand_events_landscape",
        "content_pill_same_as_idle_landscape", "content_pill_width_percent_landscape",
        "anim_speed_landscape", "merge_style_landscape",
        "idle_widgets_enabled_landscape", "idle_off_after_tap_close_landscape",
        "idle_off_after_long_press_landscape", "idle_off_when_stored_landscape"
    )

    fun scopedKeys(posture: FoldPosture): Set<String> = if (posture.landscape) LandscapeKeys else PortraitKeys

    fun profileName(base: String, posture: FoldPosture): String = base + SEPARATOR + posture.id

    private fun initName(posture: FoldPosture): String = "fold_profile_init" + SEPARATOR + posture.id

    fun isProfileName(name: String): Boolean = SEPARATOR in name

    /** True once [posture] has been edited, i.e. has its own values. */
    fun hasOwnValues(raw: Preferences, posture: FoldPosture): Boolean =
        raw[booleanPreferencesKey(initName(posture))] == true

    /** The mode whose own values hold [name] for [posture]; null when the key is not per mode. */
    private fun ownerOf(posture: FoldPosture, name: String): FoldPosture? = when {
        name in scopedKeys(posture) -> posture
        posture.landscape && name in CameraKeys -> posture.portraitTwin
        else -> null
    }

    private fun suffixOf(posture: FoldPosture): String = SEPARATOR + posture.id

    private fun ensureOwnValues(raw: MutablePreferences, posture: FoldPosture) {
        if (hasOwnValues(raw, posture)) return
        val scoped = scopedKeys(posture)
        for ((key, value) in raw.asMap()) {
            if (key.name in scoped) put(raw, key.name + suffixOf(posture), value)
        }
        raw[booleanPreferencesKey(initName(posture))] = true
    }

    /** Preferences as [posture] sees them */
    fun view(raw: Preferences, posture: FoldPosture): MutablePreferences {
        val out = mutablePreferencesOf()
        val all = raw.asMap()
        for ((key, value) in all) {
            if (isProfileName(key.name)) continue
            val owner = ownerOf(posture, key.name)
            if (owner != null && hasOwnValues(raw, owner)) continue
            put(out, key.name, value)
        }
        for ((key, value) in all) {
            val at = key.name.indexOf(SEPARATOR)
            if (at <= 0) continue
            val base = key.name.substring(0, at)
            val owner = ownerOf(posture, base) ?: continue
            if (key.name.substring(at) == suffixOf(owner) && hasOwnValues(raw, owner)) put(out, base, value)
        }
        return out
    }

    /** Runs [transform] on the view of [posture] and writes what it changed back. */
    suspend fun edit(raw: MutablePreferences, posture: FoldPosture, transform: suspend (MutablePreferences) -> Unit) {
        ensureOwnValues(raw, posture)
        val view = view(raw, posture)
        val before = view.asMap().mapKeys { it.key.name }
        transform(view)
        val after = view.asMap().mapKeys { it.key.name }
        for (name in before.keys + after.keys) {
            val b = before[name]
            val a = after[name]
            if (a == b) continue
            val owner = ownerOf(posture, name)
            if (owner != null) ensureOwnValues(raw, owner)
            val target = if (owner != null) name + suffixOf(owner) else name
            if (a == null) remove(raw, target, b!!) else put(raw, target, a)
        }
    }

    /** Drops every fold mode's own values (a factory reset). */
    fun clearAll(raw: MutablePreferences) {
        for ((key, value) in raw.asMap()) {
            if (isProfileName(key.name)) remove(raw, key.name, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun keyFor(name: String, value: Any): Preferences.Key<Any>? = when (value) {
        is Boolean -> booleanPreferencesKey(name)
        is Int -> intPreferencesKey(name)
        is Long -> longPreferencesKey(name)
        is Float -> floatPreferencesKey(name)
        is Double -> doublePreferencesKey(name)
        is String -> stringPreferencesKey(name)
        is Set<*> -> stringSetPreferencesKey(name)
        else -> null
    } as Preferences.Key<Any>?

    private fun put(prefs: MutablePreferences, name: String, value: Any) {
        keyFor(name, value)?.let { prefs[it] = value }
    }

    private fun remove(prefs: MutablePreferences, name: String, typedLike: Any) {
        keyFor(name, typedLike)?.let { prefs.remove(it) }
    }
}
