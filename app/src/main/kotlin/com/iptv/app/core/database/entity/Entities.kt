package com.iptv.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted channel data — rebuilt on every M3U sync.
 * User preferences live in a SEPARATE table ([UserChannelPrefEntity])
 * and are NEVER touched during a sync refresh.
 */
@Entity(tableName = "channels")
data class ChannelEntity(
    /** Stable unique key: hex-truncated SHA-256 of (tvgId + streamUrl). */
    @PrimaryKey val id: String,
    val tvgId: String,
    val dialNumber: Int,
    val name: String,
    val logoUrl: String?,
    /** Language-agnostic key matching [com.iptv.app.core.model.Category.name]. */
    val categoryKey: String,
    /** Human-readable Spanish display name, e.g. "Anime y Animación". */
    val categoryDisplay: String,
    /** ISO 3166-1 alpha-2 derived from tvgId suffix, e.g. "CL". */
    val countryCode: String,
    val streamUrl: String,
    val userAgent: String?,
    val referrer: String?,
)

/**
 * User preferences keyed by [tvgId].
 * This table SURVIVES a channel list refresh — never DELETE * on sync.
 */
@Entity(tableName = "user_channel_prefs")
data class UserChannelPrefEntity(
    @PrimaryKey val tvgId: String,
    val isFavorite: Boolean = false,
    val lastWatchedTimestamp: Long? = null,
    /** Tracks total play initiations — used for "Most Watched" sorting. */
    val watchCount: Int = 0,
    /** Set to false after a failed stream attempt; reset to true on success. */
    val isWorking: Boolean = true,
)
