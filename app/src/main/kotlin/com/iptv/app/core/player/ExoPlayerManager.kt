package com.iptv.app.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.iptv.app.core.datastore.AppPreferencesDataStore
import com.iptv.app.core.model.Channel
import com.iptv.app.core.model.DecoderMode
import com.iptv.app.core.model.StreamErrorState
import com.iptv.app.core.model.StreamQuality
import com.iptv.app.core.model.UserSettings
import com.iptv.app.core.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Manages a single [ExoPlayer] instance shared across the app, reacting to [UserSettings].
 *
 * Key behaviours:
 * - Injects per-channel or global User-Agent and Referer headers into the data source.
 * - Detects stream quality via [Player.Listener.onVideoSizeChanged].
 * - Dynamically bounds max resolution according to user quality settings / low data mode.
 * - Applies custom buffer sizes, volume, mute, and connection watchdog timeouts.
 * - Emits typed [StreamErrorState] without releasing the player (zapping-friendly).
 */
@OptIn(UnstableApi::class)
class ExoPlayerManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val dataStore: AppPreferencesDataStore? = null,
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var currentSettings: UserSettings = UserSettings()
        private set

    // ── Public state ──────────────────────────────────────────────────────────

    private val _quality = MutableStateFlow(StreamQuality.UNKNOWN)
    val quality: StateFlow<StreamQuality> = _quality.asStateFlow()

    private val _errorState = MutableStateFlow<StreamErrorState?>(null)
    val errorState: StateFlow<StreamErrorState?> = _errorState.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var watchdogJob: kotlinx.coroutines.Job? = null

    /** The single ExoPlayer instance. Access only from Main thread. */
    val player: ExoPlayer = buildPlayer()

    init {
        dataStore?.let { ds ->
            managerScope.launch {
                ds.userSettings.collect { settings ->
                    currentSettings = settings
                    applySettings(settings)
                }
            }
        }
    }

    private fun buildPlayer(): ExoPlayer {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

        // Live stream buffer tuning: default buffer values
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 25_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val exo = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .build()

        exo.addListener(object : Player.Listener {
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

        return exo
    }

    /**
     * Applies dynamic settings to the active ExoPlayer instance.
     */
    private fun applySettings(settings: UserSettings) {
        // Track selection (Video max resolution limit)
        val maxHeight = if (settings.lowDataMode) {
            minOf(settings.streamQuality.maxHeight, 480)
        } else {
            settings.streamQuality.maxHeight
        }

        val trackParams = player.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
            .build()
        player.trackSelectionParameters = trackParams

        // Volume & Mute
        if (settings.startMuted) {
            player.volume = 0f
        } else {
            player.volume = (settings.defaultVolume.coerceIn(0, 100)) / 100f
        }

        // Live Wi-Fi restriction update
        if (settings.onlyWifi && !NetworkUtils.isWifiOrEthernetConnected(context) && player.isPlaying) {
            player.pause()
            _errorState.value = StreamErrorState.WifiOnlyRestricted
        } else if (_errorState.value is StreamErrorState.WifiOnlyRestricted &&
            (!settings.onlyWifi || NetworkUtils.isWifiOrEthernetConnected(context))
        ) {
            _errorState.value = null
            player.play()
        }
    }

    /**
     * Loads and starts playing [channel], injecting its custom HTTP headers.
     * Resets error state and starts the configurable connection watchdog.
     */
    fun play(channel: Channel) {
        _errorState.value = null
        _quality.value = StreamQuality.UNKNOWN

        // Wi-Fi only restriction check
        if (currentSettings.onlyWifi && !NetworkUtils.isWifiOrEthernetConnected(context)) {
            _errorState.value = StreamErrorState.WifiOnlyRestricted
            player.stop()
            return
        }

        // Apply volume before playback starts
        if (currentSettings.startMuted) {
            player.volume = 0f
        } else {
            player.volume = (currentSettings.defaultVolume.coerceIn(0, 100)) / 100f
        }

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
     * Builds a [DefaultHttpDataSource.Factory] with per-channel or custom headers.
     */
    private fun buildDataSourceFactory(channel: Channel): OkHttpDataSource.Factory {
        val factory = OkHttpDataSource.Factory(okHttpClient)
        val customUa = currentSettings.customUserAgent.ifBlank { null }
        val headers = buildMap {
            (channel.userAgent ?: customUa)?.let { put("User-Agent", it) }
            channel.referrer?.let { put("Referer", it) }
        }
        if (headers.isNotEmpty()) {
            factory.setDefaultRequestProperties(headers)
        }
        return factory
    }

    /**
     * Starts a coroutine watchdog based on user-configured timeout.
     * If the player is still buffering when it fires, emits [StreamErrorState.OffAir].
     */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        val timeoutMs = (currentSettings.connectionTimeoutSeconds.coerceIn(3, 30)) * 1000L
        watchdogJob = managerScope.launch {
            delay(timeoutMs)
            if (player.playbackState == Player.STATE_BUFFERING ||
                player.playbackState == Player.STATE_IDLE) {
                _errorState.value = StreamErrorState.OffAir
            }
        }
    }

    fun stop() {
        watchdogJob?.cancel()
        player.stop()
        player.clearMediaItems()
        _isBuffering.value = false
        _errorState.value = null
        _quality.value = StreamQuality.UNKNOWN
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
