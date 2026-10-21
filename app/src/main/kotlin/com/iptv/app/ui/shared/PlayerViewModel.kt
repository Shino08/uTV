package com.iptv.app.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.app.uTVApplication
import com.iptv.app.core.model.Channel
import com.iptv.app.core.model.StreamErrorState
import com.iptv.app.core.model.StreamQuality
import com.iptv.app.core.model.UserSettings
import com.iptv.app.core.player.ExoPlayerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the Player screen. */
data class PlayerUiState(
    val currentChannel: Channel? = null,
    val quality: StreamQuality = StreamQuality.UNKNOWN,
    val errorState: StreamErrorState? = null,
    val isBuffering: Boolean = false,
    val isOsdVisible: Boolean = false,
    val isMiniGuideVisible: Boolean = false,
)

/**
 * Shared ViewModel powering both MobilePlayerScreen and TvPlayerScreen.
 * Delegates all ExoPlayer interactions to [ExoPlayerManager].
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as uTVApplication).channelRepository
    private val playerManager: ExoPlayerManager = (app as uTVApplication).playerManager

    val userSettings: StateFlow<UserSettings> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** Full channel list ordered by dialNumber, consumed for zapping and MiniGuide. */
    val allChannels: StateFlow<List<Channel>> = repository.observeAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Expose the ExoPlayer instance for the Compose PlayerView integration. */
    val player get() = playerManager.player

    init {
        // Forward player state into UI state
        viewModelScope.launch {
            playerManager.quality.collect { quality ->
                _uiState.update { it.copy(quality = quality) }
            }
        }
        viewModelScope.launch {
            playerManager.errorState.collect { error ->
                _uiState.update { it.copy(errorState = error) }
                // Mark channel as not working on error
                if (error != null) {
                    _uiState.value.currentChannel?.let { ch ->
                        repository.markChannelWorking(ch.tvgId, false)
                    }
                    if (userSettings.value.autoPlayNext) {
                        viewModelScope.launch {
                            delay(3_000)
                            if (_uiState.value.errorState != null) {
                                zapNext()
                            }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            playerManager.isBuffering.collect { buffering ->
                _uiState.update { it.copy(isBuffering = buffering) }
            }
        }
    }

    fun loadChannel(channelId: String) {
        viewModelScope.launch {
            val channel = repository.getChannelById(channelId)
            if (channel != null) {
                playChannel(channel)
            }
        }
    }

    fun playChannel(channel: Channel) {
        _uiState.update {
            it.copy(
                currentChannel = channel,
                errorState = null,
                isOsdVisible = true,
                isMiniGuideVisible = false,
            )
        }
        playerManager.play(channel)
        viewModelScope.launch { repository.recordWatch(channel.tvgId) }
    }

    private var zapDebounceJob: kotlinx.coroutines.Job? = null

    /** Zaps to the next channel in the sorted channel list (dial + 1 / next index). */
    fun zapNext() {
        val channels = allChannels.value
        if (channels.isEmpty()) return
        val current = _uiState.value.currentChannel
        val currentIndex = channels.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex in 0 until channels.lastIndex) currentIndex + 1 else 0
        val targetChannel = channels[nextIndex]

        val debounceMs = userSettings.value.zappingSpeed.debounceMs
        if (debounceMs <= 0L) {
            playChannel(targetChannel)
        } else {
            // Update OSD preview immediately so the UI responds smoothly
            _uiState.update { it.copy(currentChannel = targetChannel, isOsdVisible = true) }
            zapDebounceJob?.cancel()
            zapDebounceJob = viewModelScope.launch {
                delay(debounceMs)
                playerManager.play(targetChannel)
                repository.recordWatch(targetChannel.tvgId)
            }
        }
    }

    /** Zaps to the previous channel in the sorted channel list (dial - 1 / previous index). */
    fun zapPrevious() {
        val channels = allChannels.value
        if (channels.isEmpty()) return
        val current = _uiState.value.currentChannel
        val currentIndex = channels.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else channels.lastIndex
        val targetChannel = channels[prevIndex]

        val debounceMs = userSettings.value.zappingSpeed.debounceMs
        if (debounceMs <= 0L) {
            playChannel(targetChannel)
        } else {
            // Update OSD preview immediately so the UI responds smoothly
            _uiState.update { it.copy(currentChannel = targetChannel, isOsdVisible = true) }
            zapDebounceJob?.cancel()
            zapDebounceJob = viewModelScope.launch {
                delay(debounceMs)
                playerManager.play(targetChannel)
                repository.recordWatch(targetChannel.tvgId)
            }
        }
    }

    fun toggleOsd() {
        _uiState.update { it.copy(isOsdVisible = !it.isOsdVisible) }
    }

    fun showOsd() {
        _uiState.update { it.copy(isOsdVisible = true) }
    }

    fun dismissOsd() {
        _uiState.update { it.copy(isOsdVisible = false) }
    }

    fun toggleMiniGuide() {
        _uiState.update { it.copy(isMiniGuideVisible = !it.isMiniGuideVisible) }
    }

    fun closeMiniGuide() {
        _uiState.update { it.copy(isMiniGuideVisible = false) }
    }

    fun toggleFavorite() {
        val channel = _uiState.value.currentChannel ?: return
        val newFav = !channel.isFavorite
        _uiState.update { it.copy(currentChannel = channel.copy(isFavorite = newFav)) }
        viewModelScope.launch {
            repository.toggleFavorite(channel.tvgId, channel.isFavorite)
        }
    }

    fun retryCurrentChannel() {
        val channel = _uiState.value.currentChannel ?: return
        playChannel(channel)
    }

    /** Fully stops stream, clears media items, and resets playback state. */
    fun stopPlayback() {
        playerManager.stop()
        _uiState.update {
            it.copy(
                currentChannel = null,
                isOsdVisible = false,
                isMiniGuideVisible = false,
                errorState = null,
                isBuffering = false,
            )
        }
    }

    fun pausePlayback() {
        playerManager.pause()
    }

    fun resumePlayback() {
        playerManager.resume()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
    }
}
