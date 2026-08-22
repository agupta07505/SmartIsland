/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val isNewer: Boolean,
    val downloadUrl: String?
)

data class GitHubContributor(
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val contributions: Int
)

data class GitHubCommit(
    val sha: String,
    val message: String,
    val authorName: String,
    val date: String,
    val htmlUrl: String
)

data class GitHubRepoStats(
    val stars: Int,
    val forks: Int,
    val watchers: Int,
    val openIssues: Int,
    val defaultBranch: String,
    val licenseName: String
)

object GitHubApiService {

    private const val REPO_OWNER = "agupta07505"
    private const val REPO_NAME = "SmartIsland"
    private const val BASE_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME"
    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 6000
    private const val USER_AGENT = "SmartIsland-App"

    /**
     * Fetches the latest public release from GitHub without sending any user data.
     */
    suspend fun getLatestRelease(currentVersion: String = BuildConfig.VERSION_NAME): Result<GitHubRelease> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonStr = makeHttpGet("$BASE_URL/releases/latest")
            val json = JSONObject(jsonStr)
            val tagName = json.optString("tag_name", "")
            val name = json.optString("name", tagName)
            val body = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "https://github.com/$REPO_OWNER/$REPO_NAME/releases")
            val publishedAt = json.optString("published_at", "")

            var downloadUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null && assets.length() > 0) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        break
                    }
                }
            }

            val isNewer = isNewerVersion(tagName, currentVersion)

            GitHubRelease(
                tagName = tagName,
                name = name,
                body = body,
                htmlUrl = htmlUrl,
                publishedAt = publishedAt,
                isNewer = isNewer,
                downloadUrl = downloadUrl ?: htmlUrl
            )
        }
    }

    /**
     * Fetches the public contributors list from GitHub without sending any user data.
     */
    suspend fun getContributors(): Result<List<GitHubContributor>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonStr = makeHttpGet("$BASE_URL/contributors?per_page=20")
            val array = JSONArray(jsonStr)
            val list = mutableListOf<GitHubContributor>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GitHubContributor(
                        login = obj.optString("login", "contributor"),
                        avatarUrl = obj.optString("avatar_url", ""),
                        htmlUrl = obj.optString("html_url", "https://github.com/$REPO_OWNER/$REPO_NAME"),
                        contributions = obj.optInt("contributions", 1)
                    )
                )
            }
            list
        }
    }

    /**
     * Fetches the recent commits from the repository's default branch.
     */
    suspend fun getRecentCommits(limit: Int = 5): Result<List<GitHubCommit>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonStr = makeHttpGet("$BASE_URL/commits?per_page=$limit")
            val array = JSONArray(jsonStr)
            val list = mutableListOf<GitHubCommit>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val fullSha = obj.optString("sha", "")
                val shortSha = if (fullSha.length >= 7) fullSha.substring(0, 7) else fullSha
                val commitObj = obj.optJSONObject("commit")
                val message = commitObj?.optString("message", "").orEmpty().lines().firstOrNull().orEmpty()
                val authorObj = commitObj?.optJSONObject("author")
                val authorName = authorObj?.optString("name", "Contributor").orEmpty()
                val date = authorObj?.optString("date", "").orEmpty()
                val htmlUrl = obj.optString("html_url", "https://github.com/$REPO_OWNER/$REPO_NAME/commit/$fullSha")

                list.add(
                    GitHubCommit(
                        sha = shortSha,
                        message = message,
                        authorName = authorName,
                        date = date,
                        htmlUrl = htmlUrl
                    )
                )
            }
            list
        }
    }

    /**
     * Fetches repository statistics (Stars, Forks, Issues, etc.).
     */
    suspend fun getRepoStats(): Result<GitHubRepoStats> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonStr = makeHttpGet(BASE_URL)
            val json = JSONObject(jsonStr)
            val stars = json.optInt("stargazers_count", 0)
            val forks = json.optInt("forks_count", 0)
            val watchers = json.optInt("watchers_count", 0)
            val openIssues = json.optInt("open_issues_count", 0)
            val defaultBranch = json.optString("default_branch", "main")
            val licenseObj = json.optJSONObject("license")
            val licenseName = licenseObj?.optString("spdx_id", "GPL-3.0") ?: "GPL-3.0"

            GitHubRepoStats(
                stars = stars,
                forks = forks,
                watchers = watchers,
                openIssues = openIssues,
                defaultBranch = defaultBranch,
                licenseName = licenseName
            )
        }
    }

    /**
     * Determines whether the remote version tag is strictly newer than the current local version.
     */
    fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V").split("-").firstOrNull().orEmpty()
        val cleanLocal = localVersion.trim().removePrefix("v").removePrefix("V").split("-").firstOrNull().orEmpty()

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

        if (remoteParts.isEmpty() || localParts.isEmpty()) return false

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun makeHttpGet(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorStream = connection.errorStream
                val errorText = errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("GitHub API error HTTP $responseCode: $errorText")
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
