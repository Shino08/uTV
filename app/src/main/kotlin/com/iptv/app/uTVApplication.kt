package com.iptv.app

import android.app.Application
import androidx.room.Room
import com.iptv.app.core.database.ChannelDatabase
import com.iptv.app.core.datastore.AppPreferencesDataStore
import com.iptv.app.core.network.ChannelRepository
import com.iptv.app.core.network.M3UDownloader
import com.iptv.app.core.player.ExoPlayerManager
import com.iptv.app.core.player.PlayerProvider
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Application class — manual DI container (no Hilt/Dagger to keep the project serverless and
 * dependency-light). All singletons are lazily created and exposed as properties.
 *
 * Access pattern from ViewModels:
 *   val app = getApplication<uTVApplication>()
 *   val repository = app.channelRepository
 */
class uTVApplication : Application(), PlayerProvider {

    // ── Networking ─────────────────────────────────────────────────────────

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    val database: ChannelDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ChannelDatabase::class.java,
            ChannelDatabase.DATABASE_NAME,
        ).build()
    }

    val appPreferences: AppPreferencesDataStore by lazy {
        AppPreferencesDataStore(applicationContext)
    }

    // ── Repository ───────────────────────────────────────────────────────────

    val channelRepository: ChannelRepository by lazy {
        ChannelRepository(
            dao = database.channelDao(),
            dataStore = appPreferences,
            downloader = M3UDownloader(okHttpClient),
        )
    }

    // ── Player (PlayerProvider interface) ─────────────────────────────────────

    override val playerManager: ExoPlayerManager by lazy {
        ExoPlayerManager(applicationContext, okHttpClient)
    }

    override fun onTerminate() {
        playerManager.release()
        super.onTerminate()
    }
}
