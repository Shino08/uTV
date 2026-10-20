package com.iptv.app.core.player

import android.app.Notification
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service that keeps ExoPlayer alive when the app moves to background
 * (e.g., PiP on mobile, TV screen-saver, split screen).
 *
 * Media3's [MediaSessionService] handles the service lifecycle and notification
 * automatically — no manual startForeground() call needed.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // The ExoPlayer instance is provided by the Application-scoped manager.
        // This keeps the service thin — it's just the lifecycle host.
        val player = (application as? PlayerProvider)?.playerManager?.player
            ?: return

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop service when user swipes the app away from recents
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady) {
            stopSelf()
        }
    }
}

/** Implemented by [com.iptv.app.uTVApplication] to expose the shared player manager. */
interface PlayerProvider {
    val playerManager: ExoPlayerManager
}
