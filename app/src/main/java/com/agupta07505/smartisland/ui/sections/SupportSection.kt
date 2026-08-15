/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.ui.components.ClickableRowItem
import com.agupta07505.smartisland.util.runCatchingLogged

@Composable
fun SupportSection() {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Support & Community",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ClickableRowItem(
                    label = "Star on GitHub",
                    subtitle = "Support open-source development",
                    icon = Icons.Rounded.Star,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland"))
                        runCatchingLogged("SupportSection", "Failed to open Star on GitHub link") {
                            context.startActivity(intent)
                        } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                ClickableRowItem(
                    label = "Join Telegram Community",
                    subtitle = "Chat with the developer & beta testers",
                    icon = Icons.Rounded.People,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://telegram.me/SmartIslandApp"))
                        runCatchingLogged("SupportSection", "Failed to open Telegram Community link") {
                            context.startActivity(intent)
                        } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                ClickableRowItem(
                    label = "Request a Feature",
                    subtitle = "Propose new island widgets or animation ideas",
                    icon = Icons.Rounded.Feedback,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/issues/new?template=feature_request.md"))
                        runCatchingLogged("SupportSection", "Failed to open Feature Request link") {
                            context.startActivity(intent)
                        } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                ClickableRowItem(
                    label = "Report a Bug",
                    subtitle = "File an issue with device details on GitHub",
                    icon = Icons.Rounded.BugReport,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/issues/new?template=bug_report.md"))
                        runCatchingLogged("SupportSection", "Failed to open Bug Report link") {
                            context.startActivity(intent)
                        } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                ClickableRowItem(
                    label = "License (GNU GPL v3)",
                    subtitle = "Free & Open Source Software",
                    icon = Icons.Rounded.Gavel,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/LICENSE"))
                        runCatchingLogged("SupportSection", "Failed to open License link") {
                            context.startActivity(intent)
                        } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
