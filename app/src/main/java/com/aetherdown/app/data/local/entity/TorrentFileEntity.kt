package com.aetherdown.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "torrent_files",
    foreignKeys = [
        ForeignKey(
            entity = TorrentEntity::class,
            parentColumns = ["id"],
            childColumns = ["torrent_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("torrent_id")]
)
data class TorrentFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "torrent_id")
    val torrentId: Long,

    @ColumnInfo(name = "file_index")
    val fileIndex: Int,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,

    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,

    @ColumnInfo(name = "is_selected")
    val isSelected: Boolean = true
)
