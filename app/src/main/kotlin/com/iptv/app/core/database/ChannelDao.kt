package com.iptv.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.iptv.app.core.database.entity.ChannelEntity
import com.iptv.app.core.database.entity.UserChannelPrefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    // ── Channel table ────────────────────────────────────────────────────────

    /**
     * Atomic sync: replaces all channels in a single transaction.
     * user_channel_prefs is untouched.
     */
    @Transaction
    suspend fun replaceAllChannels(channels: List<ChannelEntity>) {
        deleteAllChannels()
        insertChannels(channels)
    }

    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    /** Emits whenever the channel list changes — perfect for StateFlow collection. */
    @Query("SELECT * FROM channels ORDER BY dialNumber ASC")
    fun observeAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE categoryKey = :categoryKey ORDER BY dialNumber ASC")
    fun observeChannelsByCategory(categoryKey: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE countryCode = :countryCode ORDER BY dialNumber ASC")
    fun observeChannelsByCountry(countryCode: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun countChannels(): Int

    // ── UserChannelPrefs table ────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPrefIfAbsent(pref: UserChannelPrefEntity)

    @Update
    suspend fun updatePref(pref: UserChannelPrefEntity)

    @Query("SELECT * FROM user_channel_prefs WHERE tvgId = :tvgId LIMIT 1")
    suspend fun getPrefByTvgId(tvgId: String): UserChannelPrefEntity?

    @Query("SELECT * FROM user_channel_prefs WHERE isFavorite = 1")
    fun observeFavoritePrefs(): Flow<List<UserChannelPrefEntity>>

    /** Increments watchCount and sets lastWatchedTimestamp atomically. */
    @Query("""
        UPDATE user_channel_prefs
        SET watchCount = watchCount + 1,
            lastWatchedTimestamp = :timestamp
        WHERE tvgId = :tvgId
    """)
    suspend fun recordWatch(tvgId: String, timestamp: Long)

    @Query("UPDATE user_channel_prefs SET isFavorite = :isFavorite WHERE tvgId = :tvgId")
    suspend fun setFavorite(tvgId: String, isFavorite: Boolean)

    @Query("UPDATE user_channel_prefs SET isWorking = :isWorking WHERE tvgId = :tvgId")
    suspend fun setIsWorking(tvgId: String, isWorking: Boolean)

    /** Returns all favorite channels joined with their channel data, ordered by dial. */
    @Query("""
        SELECT c.* FROM channels c
        INNER JOIN user_channel_prefs p ON c.tvgId = p.tvgId
        WHERE p.isFavorite = 1
        ORDER BY c.dialNumber ASC
    """)
    fun observeFavoriteChannels(): Flow<List<ChannelEntity>>

    /** Returns most-watched channels (watchCount DESC) for the "Recently Watched" row. */
    @Query("""
        SELECT c.* FROM channels c
        INNER JOIN user_channel_prefs p ON c.tvgId = p.tvgId
        WHERE p.watchCount > 0
        ORDER BY p.lastWatchedTimestamp DESC
        LIMIT :limit
    """)
    fun observeRecentlyWatched(limit: Int = 20): Flow<List<ChannelEntity>>
}
