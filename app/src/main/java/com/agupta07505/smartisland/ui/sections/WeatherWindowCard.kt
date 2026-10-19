package com.agupta07505.smartisland.ui.sections

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.WeatherPrefs
import com.agupta07505.smartisland.model.WeatherSnapshot
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.util.IdleWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun WeatherWindowCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val prefs = WeatherPrefs()
            val cached = IdleWeather.cachedFor(context, prefs)
            snapshot = cached ?: withContext(Dispatchers.IO) { IdleWeather.fetch(context, prefs) }
            if (snapshot == null) error = "Sem previsão disponível"
        } catch (_: Throwable) {
            error = "Não disponível"
        } finally {
            loading = false
        }
    }

    // Make the whole card clickable to open the Overcast app; fallback to Play Store if not installed.
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth().clickable {
            val pkg = "com.sameerasw.overcast"
            val pmIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            val intent = pmIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            try {
                // If using application context, ensure new task flag.
                if (pmIntent == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
                // best-effort; ignore failures
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Clima",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (loading && snapshot == null) {
                Text(
                    text = "Carregando clima...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Card
            }

            val weather = snapshot
            if (weather == null) {
                Text(
                    text = error ?: "Não disponível",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Card
            }

            val temperature = if (weather.temperatureC.isNaN()) "--" else "${weather.temperatureC.roundToInt()}°C"
            val label = when (weather.weatherCode) {
                0 -> "Céu limpo"
                1, 2 -> "Parcialmente nublado"
                3 -> "Nublado"
                45, 48 -> "Neblina"
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "Chuva"
                71, 73, 75, 77, 85, 86 -> "Neve"
                95, 96, 99 -> "Trovoada"
                else -> "Clima"
            }

            Row {
                Column {
                    // Use MaterialTheme.typography so the font chosen in the app's theme is respected.
                    Text(
                        text = temperature,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Agora",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = weather.windKmh.takeIf { it >= 0f }?.let { "Vento ${it.toInt()} km/h" } ?: "Vento --",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
