package com.aetherdown.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aetherdown.app.data.local.dao.DownloadDao
import com.aetherdown.app.data.local.dao.HistoryDao
import com.aetherdown.app.data.local.dao.TorrentDao
import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.HistoryEntity
import com.aetherdown.app.data.local.entity.TorrentEntity
import com.aetherdown.app.data.local.entity.TorrentFileEntity

@Database(
    entities = [
        DownloadEntity::class,
        HistoryEntity::class,
        TorrentEntity::class,
        TorrentFileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AetherDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao
    abstract fun torrentDao(): TorrentDao
}
