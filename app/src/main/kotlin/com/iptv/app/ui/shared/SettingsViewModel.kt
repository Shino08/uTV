package com.iptv.app.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.app.uTVApplication
import com.iptv.app.core.model.AspectRatioPreference
import com.iptv.app.core.model.BufferDurationMode
import com.iptv.app.core.model.DecoderMode
import com.iptv.app.core.model.FontSizePreference
import com.iptv.app.core.model.StreamQualityPreference
import com.iptv.app.core.model.ThemePreference
import com.iptv.app.core.model.UserSettings
import com.iptv.app.core.model.ZappingSpeedPreference
import com.iptv.app.core.network.AppUpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared ViewModel handling configuration read/write operations and maintenance tasks
 * for both Android TV and Mobile platforms.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val dataStore = (app as uTVApplication).appPreferences
    private val repository = (app as uTVApplication).channelRepository
    private val updateChecker = (app as uTVApplication).appUpdateChecker

    val settings: StateFlow<UserSettings> = dataStore.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    fun dismissFeedback() {
        _feedbackMessage.value = null
    }

    fun checkForUpdates() {
        if (_updateState.value is AppUpdateState.Checking) return
        viewModelScope.launch {
            _updateState.value = AppUpdateState.Checking
            val result = updateChecker.check()
            _updateState.value = result
            when (result) {
                is AppUpdateState.UpToDate -> {
                    _feedbackMessage.value = "uTV está actualizado (v${result.currentVersion})"
                }
                is AppUpdateState.UpdateAvailable -> {
                    _feedbackMessage.value = "Nueva versión v${result.latestVersion} disponible"
                }
                is AppUpdateState.Error -> {
                    _feedbackMessage.value = result.message
                }
                else -> Unit
            }
        }
    }

    fun dismissUpdateState() {
        _updateState.value = AppUpdateState.Idle
    }

    // ── Video & Playback ─────────────────────────────────────────────────────

    fun setStreamQuality(quality: StreamQualityPreference) {
        viewModelScope.launch { dataStore.setStreamQuality(quality) }
    }

    fun setBufferMode(mode: BufferDurationMode) {
        viewModelScope.launch { dataStore.setBufferMode(mode) }
    }

    fun setDecoderMode(mode: DecoderMode) {
        viewModelScope.launch { dataStore.setDecoderMode(mode) }
    }

    fun setAspectRatio(aspectRatio: AspectRatioPreference) {
        viewModelScope.launch { dataStore.setAspectRatio(aspectRatio) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { dataStore.setAutoPlayNext(enabled) }
    }

    fun setStartMuted(muted: Boolean) {
        viewModelScope.launch { dataStore.setStartMuted(muted) }
    }

    fun setDefaultVolume(volume: Int) {
        viewModelScope.launch { dataStore.setDefaultVolume(volume) }
    }

    // ── Network & Data ───────────────────────────────────────────────────────

    fun setOnlyWifi(onlyWifi: Boolean) {
        viewModelScope.launch { dataStore.setOnlyWifi(onlyWifi) }
    }

    fun setSyncOnlyWifi(syncOnlyWifi: Boolean) {
        viewModelScope.launch { dataStore.setSyncOnlyWifi(syncOnlyWifi) }
    }

    fun setCustomUserAgent(ua: String) {
        viewModelScope.launch {
            dataStore.setCustomUserAgent(ua)
            _feedbackMessage.value = "User-Agent actualizado"
        }
    }

    fun setConnectionTimeoutSeconds(seconds: Int) {
        viewModelScope.launch { dataStore.setConnectionTimeoutSeconds(seconds) }
    }

    fun setLowDataMode(lowData: Boolean) {
        viewModelScope.launch { dataStore.setLowDataMode(lowData) }
    }

    // ── Privacy ──────────────────────────────────────────────────────────────

    fun setPrivateMode(privateMode: Boolean) {
        viewModelScope.launch { dataStore.setPrivateMode(privateMode) }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearWatchHistory()
            _feedbackMessage.value = "Historial de reproducción eliminado"
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            repository.clearFavorites()
            _feedbackMessage.value = "Canales favoritos eliminados"
        }
    }

    fun forceSync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.forceSync()
            _isSyncing.value = false
            _feedbackMessage.value = if (result.isSuccess) {
                "Canales resincronizados correctamente"
            } else {
                "Error al sincronizar canales"
            }
        }
    }

    // ── Appearance & Controls ────────────────────────────────────────────────

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { dataStore.setThemePreference(theme) }
    }

    fun setFontSizePreference(size: FontSizePreference) {
        viewModelScope.launch { dataStore.setFontSizePreference(size) }
    }

    fun setReduceAnimations(reduce: Boolean) {
        viewModelScope.launch { dataStore.setReduceAnimations(reduce) }
    }

    fun setZappingSpeed(speed: ZappingSpeedPreference) {
        viewModelScope.launch { dataStore.setZappingSpeed(speed) }
    }
}
