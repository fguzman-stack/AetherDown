package com.aetherdown.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "torrents")
data class TorrentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "magnet_uri")
    val magnetUri: String = "",

    @ColumnInfo(name = "torrent_path")
    val torrentPath: String = "",

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "save_path")
    val savePath: String = "",

    @ColumnInfo(name = "status")
    val status: TorrentStatus = TorrentStatus.STOPPED,

    @ColumnInfo(name = "progress")
    val progress: Int = 0,

    @ColumnInfo(name = "total_size")
    val totalSize: Long = 0L,

    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,

    @ColumnInfo(name = "uploaded_bytes")
    val uploadedBytes: Long = 0L,

    @ColumnInfo(name = "download_speed")
    val downloadSpeed: Long = 0L,

    @ColumnInfo(name = "upload_speed")
    val uploadSpeed: Long = 0L,

    @ColumnInfo(name = "seeders")
    val seeders: Int = 0,

    @ColumnInfo(name = "leechers")
    val leechers: Int = 0,

    @ColumnInfo(name = "is_incognito")
    val isIncognito: Boolean = false,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)

enum class TorrentStatus {
    CHECKING,
    DOWNLOADING,
    SEEDING,
    STOPPED,
    COMPLETED,
    FAILED,
    PAUSED
}
