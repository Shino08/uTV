package com.iptv.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Top-level extension property — one DataStore instance per process. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Single-responsibility wrapper around [DataStore] for app-level preferences.
 *
 * Responsibilities:
 * - TTL tracking for M3U sync (last successful sync timestamp).
 * - Last-watched channel ID (restored on cold start).
 */
class AppPreferencesDataStore(private val context: Context) {

    private object Keys {
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp_ms")
        val LAST_WATCHED_CHANNEL_ID = stringPreferencesKey("last_watched_channel_id")
    }

    /** TTL in milliseconds: 24 hours. */
    private val syncTtlMs: Long = 24L * 60 * 60 * 1000

    // ── Read flows ───────────────────────────────────────────────────────────

    val lastSyncTimestamp: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L }

    val lastWatchedChannelId: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[Keys.LAST_WATCHED_CHANNEL_ID] }

    // ── Write operations ─────────────────────────────────────────────────────

    suspend fun markSyncCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun saveLastWatchedChannelId(channelId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_WATCHED_CHANNEL_ID] = channelId
        }
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
