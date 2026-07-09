/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.agupta07505.smartisland.util.runCatchingLogged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.ui.PermissionCard

@Composable
fun PermissionsSection(
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    onOverlayClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PermissionCard(
            title = "Overlay permission",
            description = "Required to draw the pill above other apps.",
            granted = overlayGranted,
            buttonText = "Allow",
            onClick = onOverlayClick
        )

        PermissionCard(
            title = "Notification listener",
            description = "Lets Smart Island show incoming notification content.",
            granted = notificationGranted,
            buttonText = "Enable",
            onClick = onNotificationClick
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    tint = Color(0xFF667085)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Overlay system warning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Redirect to notification settings to hide the \"displaying over other apps\" alert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, "android")
                    }
                    runCatchingLogged("PermissionsSection", "Failed to open notification settings") {
                        context.startActivity(intent)
                    } ?: Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Hide")
                }
            }
        }
    }
}
