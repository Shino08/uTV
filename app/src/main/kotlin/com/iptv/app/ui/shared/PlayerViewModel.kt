package com.iptv.app.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.app.uTVApplication
import com.iptv.app.core.model.Channel
import com.iptv.app.core.model.StreamErrorState
import com.iptv.app.core.model.StreamQuality
import com.iptv.app.core.player.ExoPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

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
                }
            }
        }
        viewModelScope.launch {
            playerManager.isBuffering.collect { buffering ->
                _uiState.update { it.copy(isBuffering = buffering) }
            }
        }
    }

    fun playChannel(channel: Channel) {
        _uiState.update {
            it.copy(
                currentChannel = channel,
                errorState = null,
                isOsdVisible = true,
            )
        }
        playerManager.play(channel)
        viewModelScope.launch { repository.recordWatch(channel.tvgId) }
    }

    fun toggleOsd() {
        _uiState.update { it.copy(isOsdVisible = !it.isOsdVisible) }
    }

    fun showOsd() {
        _uiState.update { it.copy(isOsdVisible = true) }
    }

    fun toggleMiniGuide() {
        _uiState.update { it.copy(isMiniGuideVisible = !it.isMiniGuideVisible) }
    }

    fun toggleFavorite() {
        val channel = _uiState.value.currentChannel ?: return
        viewModelScope.launch {
            repository.toggleFavorite(channel.tvgId, channel.isFavorite)
        }
    }
}
