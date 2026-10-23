/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.glass.Card
import kotlinx.coroutines.launch

@Composable
fun FaceIdSettingsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Lock screen animation style",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FaceStyleChips(
                selected = settings.faceIdStyle,
                onSelect = { style -> scope.launch { repository.setFaceIdStyle(style) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fingerprint animation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = settings.fingerprintAnimationEnabled,
                    onCheckedChange = { value -> scope.launch { repository.setFingerprintAnimationEnabled(value) } }
                )
            }
            Text(
                text = "Plays the fingerprint scan when the phone or an app lock prompt is unlocked with a finger. " +
                    "Off: a fingerprint unlock ends the face scan without it.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Face ID",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = settings.appFaceIdEnabled,
                    onCheckedChange = { value -> scope.launch { repository.setAppFaceIdEnabled(value) } }
                )
            }
            Text(
                text = "Plays the same scan when an app's lock prompt (the phone's app lock) unlocks with your face while the phone is " +
                    "already unlocked. Read from the system log, so it needs Shizuku. It does not depend on the lock screen Face ID switch.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (settings.appFaceIdEnabled) {
                Text(
                    text = "App animation style",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                FaceStyleChips(
                    selected = settings.appFaceIdStyle,
                    onSelect = { style -> scope.launch { repository.setAppFaceIdStyle(style) } }
                )
            }
        }
    }
}
