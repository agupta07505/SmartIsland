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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbSunny
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
import com.agupta07505.smartisland.model.weatherLook
import com.agupta07505.smartisland.model.WeatherLook
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.util.IdleWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun WeatherWindowCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { WeatherPrefs() }
    var snapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val cached = IdleWeather.cachedFor(context, prefs)
            snapshot = cached ?: withContext(Dispatchers.IO) { IdleWeather.fetch(context, prefs) }
            if (snapshot == null) error = "Sem previsão disponível"
        } catch (_: Throwable) {
            error = "Não disponível"
        } finally {
            loading = false
        }
    }

    // Periodic refresh according to prefs.refreshMs (0 = manual)
    LaunchedEffect(prefs.refreshMs) {
        val interval = prefs.refreshMs
        if (interval <= 0L) return@LaunchedEffect
        while (true) {
            delay(interval)
            try {
                val s = withContext(Dispatchers.IO) { IdleWeather.fetch(context, prefs) }
                if (s != null) snapshot = s
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    // Respect app typography via MaterialTheme and make the card clickable to open Overcast (deep link if available)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val pkg = "com.sameerasw.overcast"
                // Try a deep link first: overcast://weather
                val deep = Intent(Intent.ACTION_VIEW, Uri.parse("overcast://weather")).setPackage(pkg)
                val pm = context.packageManager
                try {
                    if (pm.resolveActivity(deep, 0) != null) {
                        context.startActivity(deep.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        val pmIntent = pm.getLaunchIntentForPackage(pkg)
                        val intent = pmIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                        if (pmIntent == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                } catch (_: Exception) {
                    // ignore failures
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

            // Determine unit preference (best-effort): respect US locale as Fahrenheit, otherwise Celsius.
            val useFahrenheit = Locale.getDefault().country == "US"
            val temperatureText = if (weather.temperatureC.isNaN()) "--" else formatTemperature(weather.temperatureC, useFahrenheit)

            val look = weatherLook(weather.weatherCode)
            val icon = when (look) {
                WeatherLook.Clear -> Icons.Rounded.WbSunny
                WeatherLook.PartlyCloudy -> Icons.Rounded.WbSunny
                WeatherLook.Cloudy -> Icons.Rounded.Cloud
                WeatherLook.Fog -> Icons.Rounded.Cloud
                WeatherLook.Rain -> Icons.Rounded.Umbrella
                WeatherLook.Snow -> Icons.Rounded.AcUnit
                WeatherLook.Storm -> Icons.Rounded.FlashOn
            }

            Row {
                Column {
                    // Use MaterialTheme.typography so the chosen app font is respected
                    Text(
                        text = temperatureText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = prettyLabelFor(weather.weatherCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

private fun formatTemperature(celsius: Float, useFahrenheit: Boolean): String {
    return if (useFahrenheit) {
        val f = cToF(celsius)
        "${f.roundToInt()}°F"
    } else {
        "${celsius.roundToInt()}°C"
    }
}

private fun cToF(c: Float): Float = c * 9f / 5f + 32f

private fun prettyLabelFor(code: Int): String = when (code) {
    0 -> "Céu limpo"
    1, 2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Neblina"
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "Chuva"
    71, 73, 75, 77, 85, 86 -> "Neve"
    95, 96, 99 -> "Trovoada"
    else -> "Clima"
}
