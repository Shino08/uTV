package com.iptv.app

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
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
 * Implements [ImageLoaderFactory] to configure global Coil caching, preventing UI thread lag
 * and memory spikes when scrolling channel carousels.
 */
class uTVApplication : Application(), PlayerProvider, ImageLoaderFactory {

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

    // ── Coil Image Loader Optimization ────────────────────────────────────────

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    override fun onTerminate() {
        playerManager.release()
        super.onTerminate()
    }
}
