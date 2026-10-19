package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.agupta07505.smartisland.ui.glass.Card
import kotlinx.coroutines.launch

/**
 * Cartão de clima usado pelo menu expandido — mostra clima atual + resumo curto.
 * A implementação abaixo é auto-contida e busca dados via WeatherProvider (esqueleto).
 * Substitua WeatherProvider.fetchCurrentWeather por integração Overcast / OpenWeatherMap.
 */

data class SimpleWeather(
    val location: String,
    val tempC: Int?,
    val desc: String?,
    val iconUrl: String? = null
)

object WeatherProvider {
    // TODO: implementar fetch real (Overcast / OpenWeatherMap). Retorna dados de exemplo por enquanto.
    suspend fun fetchCurrentWeather(): SimpleWeather {
        // placeholder: implementar chamada HTTP ao serviço (Overcast ou OWM) e mapear para SimpleWeather
        return SimpleWeather(location = "—", tempC = null, desc = "Carregando…", iconUrl = null)
    }
}

@Composable
internal fun WeatherWindowCard(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var weather by remember { mutableStateOf<SimpleWeather?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        loading = true
        scope.launch {
            try {
                val w = WeatherProvider.fetchCurrentWeather()
                weather = w
            } catch (t: Throwable) {
                weather = SimpleWeather(location = "Erro", tempC = null, desc = "Não disponível")
            } finally {
                loading = false
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Clima",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (loading && weather == null) {
                Text(text = "Carregando...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val w = weather ?: SimpleWeather("—", null, "—")
                Row {
                    // ícone (se disponível)
                    if (w.iconUrl != null) {
                        // Placeholder: Image loader (coil/Glide) pode ser usado aqui, 
                        // por enquanto deixar espaço para ícone.
                    }
                    Column {
                        Text(text = w.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = if (w.tempC != null) "${w.tempC}°C • ${w.desc ?: ""}" else (w.desc ?: "-"),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
