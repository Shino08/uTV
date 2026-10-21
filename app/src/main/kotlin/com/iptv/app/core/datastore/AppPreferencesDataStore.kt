package com.iptv.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iptv.app.core.model.AspectRatioPreference
import com.iptv.app.core.model.BufferDurationMode
import com.iptv.app.core.model.DecoderMode
import com.iptv.app.core.model.FontSizePreference
import com.iptv.app.core.model.StreamQualityPreference
import com.iptv.app.core.model.ThemePreference
import com.iptv.app.core.model.UserSettings
import com.iptv.app.core.model.ZappingSpeedPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Top-level extension property — one DataStore instance per process. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Single-responsibility wrapper around [DataStore] for app-level preferences and user settings.
 */
class AppPreferencesDataStore(private val context: Context) {

    private object Keys {
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp_v2_ms")
        val LAST_WATCHED_CHANNEL_ID = stringPreferencesKey("last_watched_channel_id")

        // ── Settings Keys ────────────────────────────────────────────────────
        val STREAM_QUALITY = stringPreferencesKey("stream_quality")
        val BUFFER_MODE = stringPreferencesKey("buffer_mode")
        val DECODER_MODE = stringPreferencesKey("decoder_mode")
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val START_MUTED = booleanPreferencesKey("start_muted")
        val DEFAULT_VOLUME = intPreferencesKey("default_volume")

        val ONLY_WIFI = booleanPreferencesKey("only_wifi")
        val SYNC_ONLY_WIFI = booleanPreferencesKey("sync_only_wifi")
        val CUSTOM_USER_AGENT = stringPreferencesKey("custom_user_agent")
        val CONNECTION_TIMEOUT = intPreferencesKey("connection_timeout_sec")
        val LOW_DATA_MODE = booleanPreferencesKey("low_data_mode")

        val PRIVATE_MODE = booleanPreferencesKey("private_mode")

        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val FONT_SIZE_PREFERENCE = stringPreferencesKey("font_size_preference")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val ZAPPING_SPEED = stringPreferencesKey("zapping_speed")
    }

    /** TTL in milliseconds: 24 hours. */
    private val syncTtlMs: Long = 24L * 60 * 60 * 1000

    // ── Read flows ───────────────────────────────────────────────────────────

    val lastSyncTimestamp: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L }

    val lastWatchedChannelId: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[Keys.LAST_WATCHED_CHANNEL_ID] }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            streamQuality = prefs[Keys.STREAM_QUALITY]?.let {
                runCatching { StreamQualityPreference.valueOf(it) }.getOrNull()
            } ?: StreamQualityPreference.AUTO,
            bufferMode = prefs[Keys.BUFFER_MODE]?.let {
                runCatching { BufferDurationMode.valueOf(it) }.getOrNull()
            } ?: BufferDurationMode.MEDIUM,
            decoderMode = prefs[Keys.DECODER_MODE]?.let {
                runCatching { DecoderMode.valueOf(it) }.getOrNull()
            } ?: DecoderMode.HARDWARE,
            aspectRatio = prefs[Keys.ASPECT_RATIO]?.let {
                runCatching { AspectRatioPreference.valueOf(it) }.getOrNull()
            } ?: AspectRatioPreference.FIT,
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: false,
            startMuted = prefs[Keys.START_MUTED] ?: false,
            defaultVolume = prefs[Keys.DEFAULT_VOLUME] ?: 100,

            onlyWifi = prefs[Keys.ONLY_WIFI] ?: false,
            syncOnlyWifi = prefs[Keys.SYNC_ONLY_WIFI] ?: false,
            customUserAgent = prefs[Keys.CUSTOM_USER_AGENT] ?: "",
            connectionTimeoutSeconds = prefs[Keys.CONNECTION_TIMEOUT] ?: 8,
            lowDataMode = prefs[Keys.LOW_DATA_MODE] ?: false,

            privateMode = prefs[Keys.PRIVATE_MODE] ?: false,

            themePreference = prefs[Keys.THEME_PREFERENCE]?.let {
                runCatching { ThemePreference.valueOf(it) }.getOrNull()
            } ?: ThemePreference.DARK,
            fontSizePreference = prefs[Keys.FONT_SIZE_PREFERENCE]?.let {
                runCatching { FontSizePreference.valueOf(it) }.getOrNull()
            } ?: FontSizePreference.NORMAL,
            reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: false,
            zappingSpeed = prefs[Keys.ZAPPING_SPEED]?.let {
                runCatching { ZappingSpeedPreference.valueOf(it) }.getOrNull()
            } ?: ZappingSpeedPreference.NORMAL,
        )
    }

    // ── Write operations ─────────────────────────────────────────────────────

    suspend fun markSyncCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun resetSyncTimestamp() {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = 0L
        }
    }

    suspend fun saveLastWatchedChannelId(channelId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_WATCHED_CHANNEL_ID] = channelId
        }
    }

    suspend fun setStreamQuality(quality: StreamQualityPreference) {
        context.dataStore.edit { it[Keys.STREAM_QUALITY] = quality.name }
    }

    suspend fun setBufferMode(mode: BufferDurationMode) {
        context.dataStore.edit { it[Keys.BUFFER_MODE] = mode.name }
    }

    suspend fun setDecoderMode(mode: DecoderMode) {
        context.dataStore.edit { it[Keys.DECODER_MODE] = mode.name }
    }

    suspend fun setAspectRatio(aspectRatio: AspectRatioPreference) {
        context.dataStore.edit { it[Keys.ASPECT_RATIO] = aspectRatio.name }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setStartMuted(muted: Boolean) {
        context.dataStore.edit { it[Keys.START_MUTED] = muted }
    }

    suspend fun setDefaultVolume(volume: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_VOLUME] = volume.coerceIn(0, 100) }
    }

    suspend fun setOnlyWifi(onlyWifi: Boolean) {
        context.dataStore.edit { it[Keys.ONLY_WIFI] = onlyWifi }
    }

    suspend fun setSyncOnlyWifi(syncOnlyWifi: Boolean) {
        context.dataStore.edit { it[Keys.SYNC_ONLY_WIFI] = syncOnlyWifi }
    }

    suspend fun setCustomUserAgent(ua: String) {
        context.dataStore.edit { it[Keys.CUSTOM_USER_AGENT] = ua.trim() }
    }

    suspend fun setConnectionTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.CONNECTION_TIMEOUT] = seconds }
    }

    suspend fun setLowDataMode(lowData: Boolean) {
        context.dataStore.edit { it[Keys.LOW_DATA_MODE] = lowData }
    }

    suspend fun setPrivateMode(privateMode: Boolean) {
        context.dataStore.edit { it[Keys.PRIVATE_MODE] = privateMode }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME_PREFERENCE] = theme.name }
    }

    suspend fun setFontSizePreference(size: FontSizePreference) {
        context.dataStore.edit { it[Keys.FONT_SIZE_PREFERENCE] = size.name }
    }

    suspend fun setReduceAnimations(reduce: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_ANIMATIONS] = reduce }
    }

    suspend fun setZappingSpeed(speed: ZappingSpeedPreference) {
        context.dataStore.edit { it[Keys.ZAPPING_SPEED] = speed.name }
    }

    // ── TTL check ────────────────────────────────────────────────────────────

    /**
     * Returns true if the cached channel list is fresh enough (< 24h old)
     * and does not need a network refresh.
     */
    suspend fun isCacheFresh(lastTimestamp: Long): Boolean {
        if (lastTimestamp == 0L) return false
        return (System.currentTimeMillis() - lastTimestamp) < syncTtlMs
    }
}
