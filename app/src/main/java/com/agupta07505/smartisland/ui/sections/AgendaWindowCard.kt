package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.util.AgendaPrefs

/**
 * Agenda do menu expandido: ate que distancia no futuro os compromissos aparecem na ilha.
 * Fica em arquivo proprio para nao conflitar com as atualizacoes do projeto original.
 */
@Composable
internal fun AgendaWindowCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hours by remember { mutableStateOf(AgendaPrefs.windowHours(context)) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Agenda no menu da ilha",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Quando os atalhos de apps estão desativados, o menu expandido mostra os próximos compromissos. " +
                    "Escolha até que distância no futuro eles aparecem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgendaPrefs.OPTIONS.forEach { option ->
                    if (option == hours) {
                        FilledTonalButton(onClick = {}) { Text(AgendaPrefs.label(option)) }
                    } else {
                        OutlinedButton(onClick = {
                            hours = option
                            AgendaPrefs.setWindowHours(context, option)
                        }) { Text(AgendaPrefs.label(option)) }
                    }
                }
            }
        }
    }
}
