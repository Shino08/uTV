package com.iptv.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

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
        val FALLBACK_URLS = listOf(
            "https://iptv-org.github.io/iptv/languages/spa.m3u",
            "http://iptv-org.github.io/iptv/languages/spa.m3u",
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/spa.m3u",
        )

        private fun defaultClient(): OkHttpClient = TolerantHttpClientFactory.create()
    }

    /**
     * Downloads the playlist and returns its raw UTF-8 content.
     * Retries mirrors automatically if SSL handshake or CDN fails.
     *
     * @return [Result.success] with the raw string, or [Result.failure] with the cause.
     */
    suspend fun download(url: String = PLAYLIST_URL): Result<String> =
        withContext(Dispatchers.IO) {
            val urlsToTry = if (url == PLAYLIST_URL) FALLBACK_URLS else listOf(url)
            var lastException: Exception? = null

            for (targetUrl in urlsToTry) {
                try {
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "uTV/1.0 (Android IPTV)")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            return@withContext Result.success(body)
                        }
                    } else {
                        lastException = IOException("HTTP ${response.code}: ${response.message} from $targetUrl")
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }

            Result.failure(lastException ?: IOException("Failed to download playlist from all mirrors"))
        }
}
