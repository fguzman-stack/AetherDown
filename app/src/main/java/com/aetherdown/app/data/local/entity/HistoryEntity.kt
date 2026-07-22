package com.aetherdown.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,

    @ColumnInfo(name = "platform")
    val platform: String = "",

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @ColumnInfo(name = "duration")
    val duration: Long = 0L,

    @ColumnInfo(name = "mime_type")
    val mimeType: String = "",

    @ColumnInfo(name = "is_incognito")
    val isIncognito: Boolean = false,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long = System.currentTimeMillis()
)
