package com.agupta07505.smartisland.ui.expanded

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
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
import com.agupta07505.smartisland.ui.IslandText as Text

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
                text = (if (charging) "\u26A1 " else "") + "$level%",
                color = batteryColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
    Spacer(Modifier.height(10.dp))
}
