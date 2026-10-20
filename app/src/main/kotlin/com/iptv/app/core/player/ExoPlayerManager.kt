package com.iptv.app.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.iptv.app.core.model.Channel
import com.iptv.app.core.model.StreamErrorState
import com.iptv.app.core.model.StreamQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manages a single [ExoPlayer] instance shared across the app.
 *
 * Key behaviours:
 * - Injects per-channel User-Agent and Referer headers into the data source.
 * - Detects stream quality via [Player.Listener.onVideoSizeChanged].
 * - Emits typed [StreamErrorState] without releasing the player (zapping-friendly).
 * - 8-second connection watchdog triggers [StreamErrorState.OffAir].
 */
@OptIn(UnstableApi::class)
class ExoPlayerManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Public state ──────────────────────────────────────────────────────────

    private val _quality = MutableStateFlow(StreamQuality.UNKNOWN)
    val quality: StateFlow<StreamQuality> = _quality.asStateFlow()

    private val _errorState = MutableStateFlow<StreamErrorState?>(null)
    val errorState: StateFlow<StreamErrorState?> = _errorState.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var watchdogJob: kotlinx.coroutines.Job? = null
    private val CONNECTION_TIMEOUT_MS = 8_000L

    /** The single ExoPlayer instance. Access only from Main thread. */
    val player: ExoPlayer = buildPlayer()

    private fun buildPlayer(): ExoPlayer {
        // Default data source — headers are overridden per-channel in play()
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()

        player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _quality.value = StreamQuality.fromHeight(videoSize.height)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    // Stream is live — cancel watchdog and clear errors
                    watchdogJob?.cancel()
                    _errorState.value = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                watchdogJob?.cancel()
                val httpCode = (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode

                _errorState.value = when (httpCode) {
                    403  -> StreamErrorState.GeoBlocked
                    404  -> StreamErrorState.OffAir
                    else -> StreamErrorState.PlayerError(error.message)
                }
            }
        })

        return player
    }

    /**
     * Loads and starts playing [channel], injecting its custom HTTP headers.
     * Resets error state and starts the 8-second connection watchdog.
     */
    fun play(channel: Channel) {
        _errorState.value = null
        _quality.value = StreamQuality.UNKNOWN

        val dataSourceFactory = buildDataSourceFactory(channel)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // Rebuild the player's media source factory for this channel's headers
        player.stop()
        player.clearMediaItems()

        // Directly build HLS/DASH/progressive source
        val mediaItem = MediaItem.fromUri(channel.streamUrl)
        player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        player.prepare()
        player.playWhenReady = true

        startWatchdog()
    }

    /**
     * Builds a [DefaultHttpDataSource.Factory] with per-channel headers.
     */
    private fun buildDataSourceFactory(channel: Channel): OkHttpDataSource.Factory {
        val factory = OkHttpDataSource.Factory(okHttpClient)
        val headers = buildMap {
            channel.userAgent?.let { put("User-Agent", it) }
            channel.referrer?.let { put("Referer", it) }
        }
        if (headers.isNotEmpty()) {
            factory.setDefaultRequestProperties(headers)
        }
        return factory
    }

    /**
     * Starts an 8-second coroutine watchdog.
     * If the player is still buffering when it fires, emits [StreamErrorState.OffAir].
     */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = managerScope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            if (player.playbackState == Player.STATE_BUFFERING ||
                player.playbackState == Player.STATE_IDLE) {
                _errorState.value = StreamErrorState.OffAir
            }
        }
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    /**
     * Must be called from the Activity/Service [onDestroy] to release ExoPlayer resources.
     */
    fun release() {
        watchdogJob?.cancel()
        player.release()
    }
}
