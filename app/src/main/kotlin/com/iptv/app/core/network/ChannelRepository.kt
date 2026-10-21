package com.iptv.app.core.network

import com.iptv.app.core.database.ChannelDao
import com.iptv.app.core.database.entity.ChannelEntity
import com.iptv.app.core.database.entity.UserChannelPrefEntity
import com.iptv.app.core.datastore.AppPreferencesDataStore
import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import com.iptv.app.core.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Single repository that owns all channel data operations.
 *
 * Responsibilities:
 * - TTL-guarded M3U sync (delegates to [M3UDownloader] + [M3UParser] + [DialNumberAssigner]).
 * - Joins [ChannelEntity] with [UserChannelPrefEntity] into [Channel] domain objects.
 * - Exposes reactive [Flow] streams consumed by ViewModels.
 * - Manages user interactions (favorites, watch tracking, isWorking flag).
 */
class ChannelRepository(
    private val dao: ChannelDao,
    private val dataStore: AppPreferencesDataStore,
    private val downloader: M3UDownloader,
) {
    val userSettings: Flow<UserSettings> = dataStore.userSettings

    // ── Sync ─────────────────────────────────────────────────────────────────

    /**
     * Checks TTL and syncs the M3U playlist only when the cache is stale.
     * Always runs on [Dispatchers.IO].
     *
     * @return [Result.success] if data is available (from cache or network),
     *         [Result.failure] on network error when cache is empty.
     */
    suspend fun syncIfNeeded(): Result<Unit> = withContext(Dispatchers.IO) {
        val lastSync = dataStore.lastSyncTimestamp.first()

        if (dataStore.isCacheFresh(lastSync) && dao.countChannels() > 0) {
            return@withContext Result.success(Unit) // Cache is fresh
        }

        val downloadResult = downloader.download()
        if (downloadResult.isFailure) {
            // If we have stale data, surface it rather than failing
            if (dao.countChannels() > 0) return@withContext Result.success(Unit)
            return@withContext Result.failure(downloadResult.exceptionOrNull()!!)
        }

        val rawContent = downloadResult.getOrThrow()
        val parsed = M3UParser.parse(rawContent)
        val withDials = DialNumberAssigner.assign(parsed)
        val entities = withDials.map { it.toEntity() }

        // Atomic replace: channels table is cleared and repopulated.
        // user_channel_prefs is NOT touched.
        dao.replaceAllChannels(entities)

        // Seed prefs rows for new channels (IGNORE conflict = no overwrite for existing)
        entities.forEach { entity ->
            dao.insertPrefIfAbsent(UserChannelPrefEntity(tvgId = entity.tvgId))
        }

        dataStore.markSyncCompleted()
        Result.success(Unit)
    }

    // ── Reactive queries ──────────────────────────────────────────────────────

    fun observeAllChannels(): Flow<List<Channel>> =
        combine(
            dao.observeAllChannels(),
            dao.observeFavoritePrefs(),
        ) { channels, favoritePrefs ->
            val favoriteIds = favoritePrefs.filter { it.isFavorite }.map { it.tvgId }.toSet()
            channels.map { entity ->
                entity.toDomain(isFavorite = entity.tvgId in favoriteIds)
            }
        }

    fun observeFavoriteChannels(): Flow<List<Channel>> =
        dao.observeFavoriteChannels().combine(dao.observeFavoritePrefs()) { channels, prefs ->
            val prefMap = prefs.associateBy { it.tvgId }
            channels.map { entity ->
                entity.toDomain(
                    isFavorite = true,
                    pref = prefMap[entity.tvgId],
                )
            }
        }

    fun observeRecentlyWatched(limit: Int = 20): Flow<List<Channel>> =
        dao.observeRecentlyWatched(limit).combine(dao.observeFavoritePrefs()) { channels, prefs ->
            val favoriteIds = prefs.filter { it.isFavorite }.map { it.tvgId }.toSet()
            channels.map { it.toDomain(isFavorite = it.tvgId in favoriteIds) }
        }

    fun observeChannelsByCategory(category: Category): Flow<List<Channel>> =
        dao.observeChannelsByCategory(category.name)
            .combine(dao.observeFavoritePrefs()) { channels, prefs ->
                val favoriteIds = prefs.filter { it.isFavorite }.map { it.tvgId }.toSet()
                channels.map { it.toDomain(isFavorite = it.tvgId in favoriteIds) }
            }

    suspend fun getChannelById(id: String): Channel? = withContext(Dispatchers.IO) {
        val entity = dao.getChannelById(id) ?: return@withContext null
        val pref = dao.getPrefByTvgId(entity.tvgId)
        entity.toDomain(isFavorite = pref?.isFavorite == true, pref = pref)
    }

    // ── User actions ──────────────────────────────────────────────────────────

    suspend fun toggleFavorite(tvgId: String, currentValue: Boolean) {
        dao.setFavorite(tvgId, !currentValue)
    }

    suspend fun recordWatch(tvgId: String) {
        val isPrivate = dataStore.userSettings.first().privateMode
        if (!isPrivate) {
            dao.recordWatch(tvgId, System.currentTimeMillis())
        }
    }

    suspend fun clearWatchHistory() = withContext(Dispatchers.IO) {
        dao.clearWatchHistory()
    }

    suspend fun clearFavorites() = withContext(Dispatchers.IO) {
        dao.clearFavorites()
    }

    suspend fun forceSync(): Result<Unit> = withContext(Dispatchers.IO) {
        dataStore.resetSyncTimestamp()
        syncIfNeeded()
    }

    suspend fun markChannelWorking(tvgId: String, isWorking: Boolean) {
        dao.setIsWorking(tvgId, isWorking)
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Channel.toEntity() = ChannelEntity(
        id = id,
        tvgId = tvgId,
        dialNumber = dialNumber,
        name = name,
        logoUrl = logoUrl,
        categoryKey = category.name,
        categoryDisplay = category.displayName,
        countryCode = countryCode,
        streamUrl = streamUrl,
        userAgent = userAgent,
        referrer = referrer,
    )

    private fun ChannelEntity.toDomain(
        isFavorite: Boolean = false,
        pref: UserChannelPrefEntity? = null,
    ) = Channel(
        id = id,
        tvgId = tvgId,
        dialNumber = dialNumber,
        name = name,
        logoUrl = logoUrl,
        category = Category.entries.firstOrNull { it.name == categoryKey } ?: Category.UNDEFINED,
        countryCode = countryCode,
        streamUrl = streamUrl,
        userAgent = userAgent,
        referrer = referrer,
        isFavorite = isFavorite,
        lastWatchedTimestamp = pref?.lastWatchedTimestamp,
        watchCount = pref?.watchCount ?: 0,
        isWorking = pref?.isWorking ?: true,
    )
}
