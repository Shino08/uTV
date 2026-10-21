package com.iptv.app.core.network

import com.iptv.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * State representing in-app GitHub update check.
 */
sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data class UpToDate(val currentVersion: String) : AppUpdateState
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val releasePageUrl: String,
    ) : AppUpdateState
    data class Error(val message: String) : AppUpdateState
}

/**
 * Checks GitHub Releases API for latest APK distribution.
 */
class AppUpdateChecker(
    private val okHttpClient: OkHttpClient,
    private val owner: String = GITHUB_OWNER,
    private val repo: String = GITHUB_REPO,
) {
    companion object {
        const val GITHUB_OWNER = "Shino08"
        const val GITHUB_REPO = "uTV"
        const val GITHUB_REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
        const val GITHUB_ISSUES_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/issues"
        const val IPTV_ORG_URL = "https://github.com/iptv-org/iptv"
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(currentVersion: String = BuildConfig.VERSION_NAME): AppUpdateState =
        withContext(Dispatchers.IO) {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "uTV/$currentVersion (Android)")
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                if (response.code == 404) {
                    return@withContext AppUpdateState.UpToDate(currentVersion)
                }
                if (!response.isSuccessful) {
                    return@withContext AppUpdateState.Error("HTTP ${response.code} al consultar GitHub")
                }

                val bodyStr = response.body?.string()
                    ?: return@withContext AppUpdateState.Error("Respuesta vacía de GitHub")

                val root = json.parseToJsonElement(bodyStr).jsonObject
                val rawTag = root["tag_name"]?.jsonPrimitive?.content ?: ""
                val latestTag = rawTag.removePrefix("v").trim()
                val htmlUrl = root["html_url"]?.jsonPrimitive?.content ?: GITHUB_REPO_URL
                val bodyNotes = root["body"]?.jsonPrimitive?.content?.take(300) ?: "Nueva versión disponible."

                // Look for an .apk asset in release assets
                var apkDownloadUrl: String? = null
                val assets = root["assets"]?.jsonArray
                if (assets != null) {
                    for (asset in assets) {
                        val assetObj = asset.jsonObject
                        val name = assetObj["name"]?.jsonPrimitive?.content ?: ""
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content
                            break
                        }
                    }
                }

                val downloadUrl = apkDownloadUrl ?: htmlUrl

                if (isNewerVersion(latestTag, currentVersion)) {
                    AppUpdateState.UpdateAvailable(
                        currentVersion = currentVersion,
                        latestVersion = latestTag,
                        releaseNotes = bodyNotes,
                        downloadUrl = downloadUrl,
                        releasePageUrl = htmlUrl,
                    )
                } else {
                    AppUpdateState.UpToDate(currentVersion)
                }
            } catch (e: Exception) {
                AppUpdateState.Error(e.localizedMessage ?: "No se pudo conectar con GitHub")
            }
        }

    /**
     * Compares semantic versions (e.g. 1.0.1 vs 1.0.0).
     * Returns true if latest > current.
     */
    fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false
        val latestParts = latest.split('.').mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val currentParts = current.split('.').mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
