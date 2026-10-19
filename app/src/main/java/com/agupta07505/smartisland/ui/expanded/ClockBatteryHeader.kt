package com.agupta07505.smartisland.ui.expanded

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.model.WeatherLook
import com.agupta07505.smartisland.model.weatherLook
import com.agupta07505.smartisland.ui.IslandText as Text
import com.agupta07505.smartisland.util.CalendarReader
import com.agupta07505.smartisland.util.IdleWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cabecalho do menu expandido de atalhos: hora (esquerda) e bateria (direita).
 * Fica em arquivo proprio para nao conflitar com as atualizacoes do projeto original.
 */
@Composable
internal fun ClockBatteryHeader() {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current

    // Hora: atualiza no comeco de cada minuto (respeita o formato 12h/24h do sistema)
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val format = android.text.format.DateFormat.getTimeFormat(context)
        while (true) {
            val now = System.currentTimeMillis()
            timeText = format.format(java.util.Date(now))
            kotlinx.coroutines.delay(60_000L - (now % 60_000L) + 50L)
        }
    }

    // Bateria: broadcast sticky, atualiza em tempo real enquanto a ilha esta expandida
    var level by remember { mutableStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        fun apply(intent: Intent?) {
            intent ?: return
            val l = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && scale > 0) level = (l * 100) / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = apply(intent)
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        apply(sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val batteryColor = when {
        charging -> palette.success
        level in 0..15 -> palette.danger
        else -> palette.text
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeText,
            color = palette.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (level >= 0) {
            Text(
                text = "$level%",
                color = batteryColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
    Spacer(Modifier.height(10.dp))
}

private data class AgendaEvent(val title: String, val beginMs: Long, val endMs: Long, val allDay: Boolean)

/** Proximos eventos (ate 36 h) dos calendarios do aparelho, inclusive o Google Agenda sincronizado. */
private fun readAgenda(context: Context, nowMs: Long): List<AgendaEvent> {
    val uri = android.provider.CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        android.content.ContentUris.appendId(it, nowMs)
        android.content.ContentUris.appendId(it, nowMs + 36L * 60 * 60 * 1000)
    }.build()
    val projection = arrayOf(
        android.provider.CalendarContract.Instances.TITLE,
        android.provider.CalendarContract.Instances.BEGIN,
        android.provider.CalendarContract.Instances.END,
        android.provider.CalendarContract.Instances.ALL_DAY,
        android.provider.CalendarContract.Events.STATUS,
        android.provider.CalendarContract.Events.SELF_ATTENDEE_STATUS
    )
    val events = ArrayList<AgendaEvent>()
    runCatching {
        context.contentResolver.query(uri, projection, null, null, "${android.provider.CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                val cancelled = !c.isNull(4) && c.getInt(4) == android.provider.CalendarContract.Events.STATUS_CANCELED
                val declined = !c.isNull(5) && c.getInt(5) == android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                if (cancelled || declined) continue
                events += AgendaEvent(c.getString(0).orEmpty(), c.getLong(1), c.getLong(2), c.getInt(3) == 1)
            }
        }
    }
    return events.sortedWith(compareBy({ !it.allDay }, { it.beginMs })).take(3)
}

private fun weatherLabel(code: Int): String = when (weatherLook(code)) {
    WeatherLook.Clear -> "C\u00e9u limpo"
    WeatherLook.PartlyCloudy -> "Parcialmente nublado"
    WeatherLook.Cloudy -> "Nublado"
    WeatherLook.Fog -> "Neblina"
    WeatherLook.Rain -> "Chuva"
    WeatherLook.Snow -> "Neve"
    WeatherLook.Storm -> "Tempestade"
}

/**
 * Conteudo do menu expandido quando os atalhos de apps estao desativados: clima (do proprio Smart Island)
 * e os proximos compromissos da agenda do aparelho (Google Agenda, se estiver sincronizada).
 */
@Composable
internal fun StandbyAgendaPanel() {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current

    var permission by remember { mutableStateOf(CalendarReader.hasPermission(context)) }
    var events by remember { mutableStateOf<List<AgendaEvent>?>(null) }
    LaunchedEffect(Unit) {
        permission = CalendarReader.hasPermission(context)
        events = if (permission) {
            withContext(Dispatchers.IO) { readAgenda(context, System.currentTimeMillis()) }
        } else {
            emptyList()
        }
    }
    val weather = remember { IdleWeather.cached(context) }

    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormat = remember(is24) {
        java.text.SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", java.util.Locale.getDefault())
    }
    val today = remember { java.util.Calendar.getInstance() }

    fun dayLabel(ms: Long): String {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        val sameDay = c.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            c.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
        return if (sameDay) "Hoje" else "Amanh\u00e3"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (weather != null) {
            val temp = Math.round(weather.temperatureC)
            val wind = if (weather.windKmh >= 0f) " \u00b7 ${Math.round(weather.windKmh)} km/h" else ""
            Text(
                text = "$temp\u00b0C \u00b7 ${weatherLabel(weather.weatherCode)}$wind",
                color = palette.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        val list = events
        when {
            !permission -> Text(
                text = "Permitir acesso \u00e0 agenda",
                color = palette.accentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(android.net.Uri.parse("package:" + context.packageName))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
            )
            list == null -> Spacer(Modifier.height(40.dp))
            list.isEmpty() -> Text(
                text = "Nenhum compromisso nas pr\u00f3ximas horas",
                color = palette.textTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            else -> Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
            ) {
                list.forEach { e ->
                    val whenText = if (e.allDay) {
                        "Dia todo"
                    } else {
                        dayLabel(e.beginMs) + " " + timeFormat.format(java.util.Date(e.beginMs))
                    }
                    val title = if (e.title.length > 30) e.title.take(29) + "\u2026" else e.title
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = whenText, color = palette.textTertiary, fontSize = 13.sp)
                        Text(
                            text = "  $title",
                            color = palette.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
