package com.iptv.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iptv.app.core.database.entity.ChannelEntity
import com.iptv.app.core.database.entity.UserChannelPrefEntity

@Database(
    entities = [ChannelEntity::class, UserChannelPrefEntity::class],
    version = 1,
    exportSchema = true,         // Schema exported to app/schemas/ for migrations
)
abstract class ChannelDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao

    companion object {
        const val DATABASE_NAME = "utv_channels.db"
    }
}
