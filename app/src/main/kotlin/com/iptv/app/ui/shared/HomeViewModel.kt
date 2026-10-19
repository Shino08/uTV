package com.iptv.app.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.app.uTVApplication
import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the Home screen. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val syncError: String? = null,
    val allChannels: List<Channel> = emptyList(),
    val recentlyWatched: List<Channel> = emptyList(),
    val favorites: List<Channel> = emptyList(),
    val activeCategory: Category? = null,
    val activeCountry: String? = null,
)

/**
 * Shared ViewModel powering both MobileHomeScreen and TvHomeScreen.
 * Uses [AndroidViewModel] to access the Application-scoped repository.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as uTVApplication).channelRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val allChannels: StateFlow<List<Channel>> = repository.observeAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<Channel>> = repository.observeFavoriteChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyWatched: StateFlow<List<Channel>> = repository.observeRecentlyWatched()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        syncChannels()
    }

    private fun syncChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, syncError = null) }
            val result = repository.syncIfNeeded()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    syncError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun setActiveCategory(category: Category?) {
        _uiState.update { it.copy(activeCategory = category, activeCountry = null) }
    }

    fun setActiveCountry(countryCode: String?) {
        _uiState.update { it.copy(activeCountry = countryCode, activeCategory = null) }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.tvgId, channel.isFavorite)
        }
    }

    fun onChannelSelected(channel: Channel) {
        viewModelScope.launch {
            repository.recordWatch(channel.tvgId)
            (getApplication<uTVApplication>()).appPreferences.saveLastWatchedChannelId(channel.id)
        }
    }

    fun retrySync() = syncChannels()
}
