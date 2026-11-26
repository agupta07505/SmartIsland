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
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.BuildConfig
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.util.GitHubApiService
import com.agupta07505.smartisland.util.GitHubCommit
import com.agupta07505.smartisland.util.GitHubContributor
import com.agupta07505.smartisland.util.GitHubReleaseItem
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.launch

private const val TAG = "UpdatesAndDownloads"

internal sealed interface UpdateCheckStatus {
    object Idle : UpdateCheckStatus
    object Checking : UpdateCheckStatus
    object OfflineMode : UpdateCheckStatus
    data class UpToDate(val version: String) : UpdateCheckStatus
    data class UpdateAvailable(val release: GitHubReleaseItem) : UpdateCheckStatus
    data class Error(val message: String) : UpdateCheckStatus
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpdatesAndDownloadsSection(
    settings: SmartIslandSettings = SmartIslandSettings.Default,
    repository: SmartIslandSettingsRepository? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember(context) { getAppVersion(context) }

    var releases by remember { mutableStateOf<List<GitHubReleaseItem>>(emptyList()) }
    var contributors by remember { mutableStateOf<List<GitHubContributor>>(emptyList()) }
    var recentCommits by remember { mutableStateOf<List<GitHubCommit>>(emptyList()) }

    var updateStatus by remember {
        mutableStateOf<UpdateCheckStatus>(
            if (!settings.allowNetworkChecks) UpdateCheckStatus.OfflineMode else UpdateCheckStatus.Idle
        )
    }

    var isChangelogsExpanded by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(false) }

    fun checkUpdates() {
        if (!settings.allowNetworkChecks) {
            updateStatus = UpdateCheckStatus.OfflineMode
            return
        }
        scope.launch {
            updateStatus = UpdateCheckStatus.Checking
            val result = GitHubApiService.getAllReleases(currentVersion)
            result.onSuccess { releaseList ->
                releases = releaseList

                val newestRelease = releaseList.firstOrNull()
                if (newestRelease != null && newestRelease.isNewer) {
                    updateStatus = UpdateCheckStatus.UpdateAvailable(newestRelease)
                } else {
                    val tagName = newestRelease?.tagName ?: "v$currentVersion"
                    updateStatus = UpdateCheckStatus.UpToDate(tagName)
                }
            }.onFailure { err ->
                updateStatus = UpdateCheckStatus.Error(err.localizedMessage ?: "Failed to connect to GitHub")
            }
        }
    }

    LaunchedEffect(settings.allowNetworkChecks) {
        if (!settings.allowNetworkChecks) {
            updateStatus = UpdateCheckStatus.OfflineMode
            return@LaunchedEffect
        }
        if (updateStatus is UpdateCheckStatus.OfflineMode) {
            updateStatus = UpdateCheckStatus.Idle
        }
        isLoadingData = true
        launch {
            GitHubApiService.getAllReleases(currentVersion).onSuccess { releaseList ->
                releases = releaseList
                val newest = releaseList.firstOrNull()
                if (newest != null && newest.isNewer) {
                    updateStatus = UpdateCheckStatus.UpdateAvailable(newest)
                }
            }
        }
        launch {
            GitHubApiService.getContributors().onSuccess { contributors = it }
        }
        launch {
            GitHubApiService.getRecentCommits(5).onSuccess { recentCommits = it }
        }
        isLoadingData = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppUpdateCheckerCard(
            currentVersion = currentVersion,
            updateStatus = updateStatus,
            releases = releases,
            isChangelogsExpanded = isChangelogsExpanded,
            onCheckUpdates = { checkUpdates() },
            onToggleChangelogs = { isChangelogsExpanded = !isChangelogsExpanded },
            onDownloadApk = { apkUrl ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                runCatchingLogged(TAG, "Failed to open APK URL") {
                    context.startActivity(intent)
                } ?: context.showToast("Cannot open download link")
            }
        )

        TopContributorsCard(
            contributors = contributors,
            onContributeClick = {
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/CONTRIBUTING.md"))
                runCatchingLogged(TAG, "Failed to open Contributing link") {
                    context.startActivity(intent)
                }
            },
            onContributorClick = { contributor ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contributor.htmlUrl))
                runCatchingLogged(TAG, "Failed to open Contributor profile") {
                    context.startActivity(intent)
                }
            }
        )

        if (recentCommits.isNotEmpty()) {
            RecentCommitsCard(
                commits = recentCommits,
                onCommitClick = { commit ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(commit.htmlUrl))
                    runCatchingLogged(TAG, "Failed to open Commit URL") {
                        context.startActivity(intent)
                    }
                }
            )
        }
    }
}

private fun getAppVersion(context: Context): String {
    return runCatchingLogged(TAG, "Failed to get app version") {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: BuildConfig.VERSION_NAME
    } ?: BuildConfig.VERSION_NAME
}
