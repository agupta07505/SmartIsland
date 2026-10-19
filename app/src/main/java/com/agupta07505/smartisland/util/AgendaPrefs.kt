package com.agupta07505.smartisland.util

import android.content.Context

/** Ate que distancia no futuro o menu expandido da ilha mostra compromissos da agenda (em horas). */
internal object AgendaPrefs {
    private const val PREFS = "standby_agenda"
    private const val KEY_HOURS = "window_hours"
    const val DEFAULT_HOURS = 36
    val OPTIONS = listOf(3, 6, 12, 24, 36, 72, 168)

    fun windowHours(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_HOURS, DEFAULT_HOURS)

    fun setWindowHours(context: Context, hours: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_HOURS, hours).apply()
    }

    fun label(hours: Int): String = if (hours < 48) "$hours h" else "${hours / 24} dias"
}
