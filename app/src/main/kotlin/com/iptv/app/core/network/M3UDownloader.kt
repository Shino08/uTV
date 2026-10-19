package com.iptv.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads the raw M3U playlist content over HTTP using OkHttp.
 * Always executes on [Dispatchers.IO].
 *
 * Failures are surfaced as [Result] to let callers decide retry strategy.
 */
class M3UDownloader(
    private val okHttpClient: OkHttpClient = defaultClient(),
) {
    companion object {
        const val PLAYLIST_URL = "https://iptv-org.github.io/iptv/languages/spa.m3u"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Downloads the playlist and returns its raw UTF-8 content.
     * @return [Result.success] with the raw string, or [Result.failure] with the cause.
     */
    suspend fun download(url: String = PLAYLIST_URL): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "uTV/1.0 (Android IPTV)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: ${response.message}")
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                Result.success(body)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
