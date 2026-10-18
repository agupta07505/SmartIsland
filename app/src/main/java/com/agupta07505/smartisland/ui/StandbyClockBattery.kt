package com.agupta07505.smartisland.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/**
 * Hora (slot esquerdo) e bateria (slot direito) da pilula em repouso, quando nenhum widget ocioso esta na pilula.
 * Fica em arquivo proprio para nao conflitar com as atualizacoes do projeto original.
 */
@Composable
internal fun StandbyTimeSlot() {
    val context = LocalContext.current
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        val format = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        while (true) {
            val now = System.currentTimeMillis()
            timeText = format.format(java.util.Date(now))
            kotlinx.coroutines.delay(60_000L - (now % 60_000L) + 50L)
        }
    }
    IslandText(
        text = timeText,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun StandbyBatterySlot() {
    val context = LocalContext.current
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
    if (level < 0) return
    val color = when {
        charging -> Color(0xFF4ADE80)
        level <= 15 -> Color(0xFFF87171)
        else -> Color.White
    }
    IslandText(
        text = "$level%",
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Cartao fixo de hora e bateria: fica sempre no fim da lista de eventos da ilha. Sem notificacao ele ocupa a pilula;
 * com notificacao, deslizar para o lado troca entre ela e este cartao.
 */
internal object StandbyCard {
    val card = IslandNotification(
        key = "smartisland_standby_clock",
        packageName = "smartisland.standby",
        appName = "Smart Island",
        title = "",
        text = "",
        timeMillis = 0L,
        mode = IslandMode.Empty
    )
}
