package com.iptv.app.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.app.uTVApplication
import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.mobile.MobileTab
import com.iptv.app.ui.tv.TvSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the Home screen with persistent navigation caching. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val syncError: String? = null,
    val allChannels: List<Channel> = emptyList(),
    val recentlyWatched: List<Channel> = emptyList(),
    val favorites: List<Channel> = emptyList(),
    val activeCategory: Category? = null,
    val activeCountry: String? = null,
    val selectedTvSection: TvSection = TvSection.HOME,
    val selectedMobileTab: MobileTab = MobileTab.HOME,
    val tvSearchQuery: String = "",
    val tvSelectedFilterLabel: String? = null,
    val tvSelectedHubId: String? = null,
    val mobileSelectedHubId: String? = null,
    val mobileGuideSearchQuery: String = "",
    val mobileGuideCategoryFilter: Category? = null,
)

/**
 * Shared ViewModel powering both MobileHomeScreen and TvHomeScreen.
 * Uses [AndroidViewModel] to access the Application-scoped repository.
 * Retains active section, search, and hub states across navigation to player.
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

    fun setTvSection(section: TvSection) {
        _uiState.update { it.copy(selectedTvSection = section) }
    }

    fun setMobileTab(tab: MobileTab) {
        _uiState.update { it.copy(selectedMobileTab = tab) }
    }

    fun setTvSearchState(query: String, filterLabel: String?) {
        _uiState.update { it.copy(tvSearchQuery = query, tvSelectedFilterLabel = filterLabel) }
    }

    fun setTvSelectedHubId(hubId: String?) {
        _uiState.update { it.copy(tvSelectedHubId = hubId) }
    }

    fun setMobileSelectedHubId(hubId: String?) {
        _uiState.update { it.copy(mobileSelectedHubId = hubId) }
    }

    fun setMobileGuideSearchState(query: String, category: Category?) {
        _uiState.update { it.copy(mobileGuideSearchQuery = query, mobileGuideCategoryFilter = category) }
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
