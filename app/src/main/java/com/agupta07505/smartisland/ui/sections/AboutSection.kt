/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ForkRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.BuildConfig
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.components.ClickableRowItem
import com.agupta07505.smartisland.util.GitHubApiService
import com.agupta07505.smartisland.util.GitHubCommit
import com.agupta07505.smartisland.util.GitHubContributor
import com.agupta07505.smartisland.util.GitHubRelease
import com.agupta07505.smartisland.util.GitHubRepoStats
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.launch

private sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object OfflineMode : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class UpdateAvailable(val release: GitHubRelease) : UpdateState
    data class Error(val message: String) : UpdateState
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutSection(
    settings: SmartIslandSettings = SmartIslandSettings.Default,
    repository: SmartIslandSettingsRepository? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember(context) { getAppVersion(context) }

    var updateState by remember {
        mutableStateOf<UpdateState>(
            if (!settings.allowNetworkChecks) UpdateState.OfflineMode else UpdateState.Idle
        )
    }
    var repoStats by remember { mutableStateOf<GitHubRepoStats?>(null) }
    var contributors by remember { mutableStateOf<List<GitHubContributor>>(emptyList()) }
    var recentCommits by remember { mutableStateOf<List<GitHubCommit>>(emptyList()) }
    var isLoadingInsights by remember { mutableStateOf(false) }

    fun checkUpdates() {
        if (!settings.allowNetworkChecks) {
            updateState = UpdateState.OfflineMode
            return
        }
        scope.launch {
            updateState = UpdateState.Checking
            val result = GitHubApiService.getLatestRelease(currentVersion)
            result.onSuccess { release ->
                if (release.isNewer) {
                    updateState = UpdateState.UpdateAvailable(release)
                } else {
                    updateState = UpdateState.UpToDate(release.tagName.ifBlank { "v$currentVersion" })
                }
            }.onFailure { err ->
                updateState = UpdateState.Error(err.localizedMessage ?: "Failed to connect to GitHub")
            }
        }
    }

    LaunchedEffect(settings.allowNetworkChecks) {
        if (!settings.allowNetworkChecks) {
            updateState = UpdateState.OfflineMode
            isLoadingInsights = false
            return@LaunchedEffect
        }
        if (updateState is UpdateState.OfflineMode) {
            updateState = UpdateState.Idle
        }
        isLoadingInsights = true
        launch {
            GitHubApiService.getRepoStats().onSuccess { repoStats = it }
        }
        launch {
            GitHubApiService.getContributors().onSuccess { contributors = it }
        }
        launch {
            GitHubApiService.getRecentCommits(5).onSuccess { recentCommits = it }
        }
        isLoadingInsights = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. GitHub Hub & In-App Update Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = "Updates",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.github_update_center_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.current_version_label, currentVersion),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { checkUpdates() },
                        enabled = updateState !is UpdateState.Checking,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (updateState is UpdateState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = if (updateState is UpdateState.Checking) stringResource(R.string.btn_checking_updates) else stringResource(R.string.btn_check_updates),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Update Status Display
                when (val state = updateState) {
                    is UpdateState.OfflineMode -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.offline_mode_title),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.offline_mode_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (repository != null) {
                                    OutlinedButton(
                                        onClick = { scope.launch { repository.setAllowNetworkChecks(true) } },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Enable", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    is UpdateState.Idle -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Tap 'Check' to fetch the latest release directly from GitHub.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is UpdateState.Checking -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Querying GitHub public releases API...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    is UpdateState.UpToDate -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.update_up_to_date_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = "Smart Island is up to date with the latest features & optimizations.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    is UpdateState.UpdateAvailable -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.update_available_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                if (state.release.body.isNotBlank()) {
                                    Text(
                                        text = state.release.body.take(200).trim() + if (state.release.body.length > 200) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            val targetUrl = state.release.downloadUrl ?: state.release.htmlUrl
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                            runCatchingLogged("AboutSection", "Failed to open download link") {
                                                context.startActivity(intent)
                                            } ?: Toast.makeText(context, "Cannot open download link", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Download APK", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.release.htmlUrl))
                                            runCatchingLogged("AboutSection", "Failed to open release on GitHub") {
                                                context.startActivity(intent)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("View Release Notes", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                    is UpdateState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Unable to check updates: ${state.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Repository Stats & Quick Links
                Text(
                    text = stringResource(R.string.repo_insights_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepoBadge(
                        icon = Icons.Rounded.Star,
                        label = "Stars",
                        value = repoStats?.stars?.toString() ?: "—",
                        tint = Color(0xFFF59E0B),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland"))
                            context.startActivity(intent)
                        }
                    )
                    RepoBadge(
                        icon = Icons.Rounded.ForkRight,
                        label = "Forks",
                        value = repoStats?.forks?.toString() ?: "—",
                        tint = Color(0xFF38BDF8),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/fork"))
                            context.startActivity(intent)
                        }
                    )
                    RepoBadge(
                        icon = Icons.Rounded.BugReport,
                        label = "Issues",
                        value = repoStats?.openIssues?.toString() ?: "—",
                        tint = Color(0xFFEF4444),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/issues"))
                            context.startActivity(intent)
                        }
                    )
                    RepoBadge(
                        icon = Icons.Rounded.Description,
                        label = "License",
                        value = repoStats?.licenseName ?: "GPL-3.0",
                        tint = Color(0xFF10B981),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/LICENSE"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        // 2. Contributors & Community Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA855F7).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.People,
                                contentDescription = "Contributors",
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.contributors_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${contributors.size.coerceAtLeast(1)} Active Developer(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/CONTRIBUTING.md"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Contribute", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (contributors.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        contributors.forEach { contributor ->
                            ContributorChip(contributor = contributor) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contributor.htmlUrl))
                                context.startActivity(intent)
                            }
                        }
                    }
                } else {
                    ContributorChip(
                        contributor = GitHubContributor(
                            login = "agupta07505",
                            avatarUrl = "",
                            htmlUrl = "https://github.com/agupta07505",
                            contributions = 100
                        )
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505"))
                        context.startActivity(intent)
                    }
                }
            }
        }

        // 3. Recent Commits & Development Stream Card
        if (recentCommits.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Commit,
                                contentDescription = "Commits",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.recent_commits_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Latest activity on main branch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentCommits.forEach { commit ->
                            CommitRow(commit = commit) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(commit.htmlUrl))
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }

        // 4. 100% On-Device & Zero-Telemetry Privacy Shield
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "Privacy Shield",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "100% On-Device & Zero Telemetry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "Smart Island does not collect, store, or transmit any user data, identifiers, or notification details. All operations and settings remain strictly on your device. Network queries are unauthenticated, read-only requests for GitHub repository updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 5. Application Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ClickableRowItem(
                    label = "Version",
                    value = currentVersion,
                    icon = Icons.Rounded.Info,
                    iconTint = Color(0xFF38BDF8),
                    onClick = {}
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                ClickableRowItem(
                    label = "Privacy Policy",
                    subtitle = "100% On-Device & Zero Tracking",
                    icon = Icons.Rounded.Lock,
                    iconTint = Color(0xFF10B981),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/PRIVACY.md"))
                        runCatchingLogged("AboutSection", "Failed to open Privacy Policy") {
                            context.startActivity(intent)
                        } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                ClickableRowItem(
                    label = "Terms of Use",
                    subtitle = "GNU GPL v3 License Terms",
                    icon = Icons.Rounded.Description,
                    iconTint = Color(0xFFF59E0B),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/TERMS.md"))
                        runCatchingLogged("AboutSection", "Failed to open Terms of Use") {
                            context.startActivity(intent)
                        } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                ClickableRowItem(
                    label = "Open Source Repository",
                    subtitle = "GitHub • Star & Contribute",
                    icon = Icons.Rounded.Code,
                    iconTint = Color(0xFFA855F7),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland"))
                        runCatchingLogged("AboutSection", "Failed to open Open Source link") {
                            context.startActivity(intent)
                        } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 6. Developer & Contact Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Developer & Contact",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ContactButton(
                        icon = { GithubIcon(tint = MaterialTheme.colorScheme.onSurface) },
                        label = "GitHub",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505"))
                            runCatchingLogged("AboutSection", "Failed to open GitHub profile") {
                                context.startActivity(intent)
                            } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ContactButton(
                        icon = { LinkedinIcon() },
                        label = "LinkedIn",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://linkedin.com/in/agupta07505"))
                            runCatchingLogged("AboutSection", "Failed to open LinkedIn profile") {
                                context.startActivity(intent)
                            } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ContactButton(
                        icon = { InstagramIcon() },
                        label = "Instagram",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/agupta07505"))
                            runCatchingLogged("AboutSection", "Failed to open Instagram profile") {
                                context.startActivity(intent)
                            } ?: Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ContactButton(
                        icon = { EmailIcon(tint = Color(0xFFEA4335)) },
                        label = "Email",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:agupta07505@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Smart Island App Feedback")
                            }
                            runCatchingLogged("AboutSection", "Failed to send email") {
                                context.startActivity(intent)
                            } ?: Toast.makeText(context, "Cannot open email app", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepoBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ContributorChip(
    contributor: GitHubContributor,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contributor.login.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = contributor.login,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${contributor.contributions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun CommitRow(
    commit: GitHubCommit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${commit.authorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "#${commit.sha}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GithubIcon(tint: Color = Color.Black) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(12f * scaleX, 2f * scaleY)
            cubicTo(6.477f * scaleX, 2f * scaleY, 2f * scaleX, 6.477f * scaleX, 2f * scaleX, 12f * scaleY)
            cubicTo(2f * scaleX, 16.42f * scaleY, 4.865f * scaleX, 20.166f * scaleY, 8.839f * scaleX, 21.489f * scaleY)
            cubicTo(9.339f * scaleX, 21.581f * scaleY, 9.521f * scaleX, 21.272f * scaleY, 9.521f * scaleX, 21.007f * scaleY)
            cubicTo(9.521f * scaleX, 20.77f * scaleY, 9.513f * scaleX, 20.141f * scaleY, 9.508f * scaleX, 19.307f * scaleY)
            cubicTo(6.726f * scaleX, 19.91f * scaleY, 6.139f * scaleX, 17.97f * scaleY, 6.139f * scaleX, 17.97f * scaleY)
            cubicTo(5.685f * scaleX, 16.814f * scaleY, 5.029f * scaleX, 16.506f * scaleY, 5.029f * scaleX, 16.506f * scaleY)
            cubicTo(4.121f * scaleX, 15.886f * scaleY, 5.098f * scaleX, 15.898f * scaleY, 5.098f * scaleX, 15.898f * scaleY)
            cubicTo(6.101f * scaleX, 15.968f * scaleY, 6.629f * scaleX, 16.928f * scaleY, 6.629f * scaleX, 16.928f * scaleY)
            cubicTo(7.521f * scaleX, 18.457f * scaleY, 8.97f * scaleX, 18.015f * scaleY, 9.539f * scaleX, 17.759f * scaleY)
            cubicTo(9.631f * scaleX, 17.113f * scaleY, 9.889f * scaleX, 16.673f * scaleY, 10.175f * scaleX, 16.423f * scaleY)
            cubicTo(7.955f * scaleX, 16.17f * scaleY, 5.62f * scaleX, 15.313f * scaleY, 5.62f * scaleX, 11.48f * scaleY)
            cubicTo(5.62f * scaleX, 10.389f * scaleY, 6.01f * scaleX, 9.496f * scaleY, 6.649f * scaleX, 8.797f * scaleY)
            cubicTo(6.546f * scaleX, 8.544f * scaleY, 6.203f * scaleX, 7.527f * scaleY, 6.747f * scaleX, 6.15f * scaleY)
            cubicTo(6.747f * scaleX, 6.15f * scaleY, 7.587f * scaleX, 5.881f * scaleY, 9.497f * scaleX, 7.175f * scaleY)
            cubicTo(10.295f * scaleX, 6.953f * scaleY, 11.15f * scaleX, 6.842f * scaleY, 12f * scaleX, 6.838f * scaleY)
            cubicTo(12.85f * scaleX, 6.842f * scaleY, 13.705f * scaleX, 6.953f * scaleY, 14.503f * scaleX, 7.175f * scaleY)
            cubicTo(16.413f * scaleX, 5.881f * scaleY, 17.253f * scaleX, 6.15f * scaleY, 17.253f * scaleX, 6.15f * scaleY)
            cubicTo(17.797f * scaleX, 7.527f * scaleY, 17.454f * scaleX, 8.544f * scaleY, 17.351f * scaleX, 8.797f * scaleY)
            cubicTo(17.99f * scaleX, 9.496f * scaleY, 18.38f * scaleX, 10.389f * scaleY, 18.38f * scaleX, 11.48f * scaleY)
            cubicTo(18.38f * scaleX, 15.323f * scaleY, 16.041f * scaleX, 16.168f * scaleY, 13.813f * scaleX, 16.415f * scaleY)
            cubicTo(14.172f * scaleX, 16.724f * scaleY, 14.491f * scaleX, 17.334f * scaleY, 14.491f * scaleX, 18.267f * scaleY)
            cubicTo(14.491f * scaleX, 19.603f * scaleY, 14.479f * scaleX, 20.682f * scaleY, 14.479f * scaleX, 21.01f * scaleY)
            cubicTo(14.479f * scaleX, 21.277f * scaleY, 14.659f * scaleX, 21.589f * scaleY, 15.167f * scaleX, 21.489f * scaleY)
            cubicTo(19.141f * scaleX, 20.16f * scaleY, 22f * scaleX, 12f * scaleY, 22f * scaleX, 12f * scaleY)
            cubicTo(22f * scaleX, 6.477f * scaleY, 17.523f * scaleX, 2f * scaleY, 12f * scaleX, 2f * scaleY)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
private fun LinkedinIcon() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(Color(0xFF0A66C2), shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "in",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

@Composable
private fun InstagramIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color(0xFFE1306C),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFE1306C),
            radius = 4f.dp.toPx(),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFE1306C),
            radius = 1f.dp.toPx(),
            center = Offset(w * 0.72f, h * 0.28f)
        )
    }
}

@Composable
private fun EmailIcon(tint: Color = Color.Black) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(2.dp.toPx(), 4.dp.toPx())
            lineTo(w - 2.dp.toPx(), 4.dp.toPx())
            lineTo(w - 2.dp.toPx(), h - 4.dp.toPx())
            lineTo(2.dp.toPx(), h - 4.dp.toPx())
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
        val foldPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(2.dp.toPx(), 5.dp.toPx())
            lineTo(w / 2f, h / 2f)
            lineTo(w - 2.dp.toPx(), 5.dp.toPx())
        }
        drawPath(
            path = foldPath,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
        )
    }
}

private fun getAppVersion(context: Context): String {
    return runCatchingLogged("AboutSection", "Failed to get app version") {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: BuildConfig.VERSION_NAME
    } ?: BuildConfig.VERSION_NAME
}
