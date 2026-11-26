/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.util.runCatchingLogged

private const val TAG = "WelcomeDialog"

@Composable
internal fun WelcomeDialog(
    onDismiss: () -> Unit,
    onTakeTour: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = rememberAppIcon(context)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(26.dp),
            // Fully opaque: the glass theme makes `surface` translucent
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WelcomeLogo(appIcon)

                WelcomeTexts()

                Spacer(modifier = Modifier.height(4.dp))

                WelcomeLinkButtons(context)

                ElevatedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.btn_get_started), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onTakeTour, modifier = Modifier.fillMaxWidth()) {
                    Text("Take a quick tour of the gestures", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun rememberAppIcon(context: Context): ImageBitmap? = remember(context) {
    runCatchingLogged(TAG, "Failed to get app icon") {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }
}

@Composable
private fun WelcomeLogo(appIcon: ImageBitmap?) {
    if (appIcon != null) {
        Image(
            bitmap = appIcon,
            contentDescription = "Smart Island Logo",
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(18.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("SI", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun WelcomeTexts() {
    Text(
        text = stringResource(R.string.welcome_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Text(
        text = stringResource(R.string.welcome_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )
}

@Composable
private fun WelcomeLinkButtons(context: Context) {
    Button(
        onClick = {
            runCatchingLogged(TAG, "Failed to open GitHub link") {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/agupta07505/SmartIsland")))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
            Text(
                stringResource(R.string.star_on_github),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }

    OutlinedButton(
        onClick = {
            runCatchingLogged(TAG, "Failed to open Telegram link") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://telegram.me/SmartIslandApp")))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.People,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                stringResource(R.string.join_telegram),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
