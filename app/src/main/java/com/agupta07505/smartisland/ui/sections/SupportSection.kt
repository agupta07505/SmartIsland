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
import com.agupta07505.smartisland.util.runCatchingLogged
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.ui.components.ClickableRowItem

@Composable
fun SupportSection() {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            ClickableRowItem(
                label = "Star on GitHub",
                icon = Icons.Rounded.Star,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland"))
                    runCatchingLogged("SupportSection", "Failed to open Star on GitHub link") {
                        context.startActivity(intent)
                    } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                }
            )
            ClickableRowItem(
                label = "Request a Feature",
                icon = Icons.Rounded.Feedback,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/issues/new?template=feature_request.md"))
                    runCatchingLogged("SupportSection", "Failed to open Feature Request link") {
                        context.startActivity(intent)
                    } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                }
            )
            ClickableRowItem(
                label = "Report a Bug",
                icon = Icons.Rounded.BugReport,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/issues/new?template=bug_report.md"))
                    runCatchingLogged("SupportSection", "Failed to open Bug Report link") {
                        context.startActivity(intent)
                    } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                }
            )
            ClickableRowItem(
                label = "App Review",
                icon = Icons.Rounded.RateReview,
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/agupta07505/SmartIsland/issues/new?template=app_review.md")
                    )
                    runCatchingLogged("SupportSection", "Failed to open App Review link") {
                        context.startActivity(intent)
                    } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                }
            )
            ClickableRowItem(
                label = "Licence",
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
