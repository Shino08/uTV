package com.iptv.app.core.model

/**
 * Represents a stream quality label derived from ExoPlayer's
 * [Player.Listener.onVideoSizeChanged] callback.
 */
enum class StreamQuality(val label: String) {
    FHD("FHD 1080p"),
    HD("HD 720p"),
    SD("SD"),
    UNKNOWN(""),
    ;

    companion object {
        fun fromHeight(height: Int): StreamQuality = when {
            height >= 1080 -> FHD
            height >= 720  -> HD
            height > 0     -> SD
            else           -> UNKNOWN
        }
    }
}

/**
 * Sealed hierarchy of player error states — rendered as overlays without
 * stopping playback or releasing the ExoPlayer instance.
 */
sealed interface StreamErrorState {
    /** HTTP 403 received — stream is likely geo-restricted. */
    data object GeoBlocked : StreamErrorState

    /** HTTP 404, stream unreachable, or 8-second connection timeout. */
    data object OffAir : StreamErrorState

    /** Unrecoverable generic player error with an optional message. */
    data class PlayerError(val message: String?) : StreamErrorState
}
