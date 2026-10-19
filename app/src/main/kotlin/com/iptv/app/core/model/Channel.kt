package com.iptv.app.core.model

/**
 * Immutable domain model representing a single IPTV channel.
 * This is the object passed between layers; UI never touches Room entities directly.
 */
data class Channel(
    /** Stable unique key: SHA-256 of tvgId + streamUrl (truncated to 16 chars). */
    val id: String,
    /** Raw tvg-id from the M3U, e.g. "EnerGeek.cl" */
    val tvgId: String,
    /** Assigned dial number (100–899) based on category. */
    val dialNumber: Int,
    val name: String,
    val logoUrl: String?,
    val category: Category,
    /** ISO 3166-1 alpha-2, derived from tvgId suffix, e.g. "CL". */
    val countryCode: String,
    val streamUrl: String,
    /** Optional HTTP header injected into ExoPlayer's data source factory. */
    val userAgent: String? = null,
    /** Optional HTTP header injected into ExoPlayer's data source factory. */
    val referrer: String? = null,
    // ── User-specific state (joined from user_channel_prefs) ──────────────
    val isFavorite: Boolean = false,
    val lastWatchedTimestamp: Long? = null,
    val watchCount: Int = 0,
    val isWorking: Boolean = true,
)
