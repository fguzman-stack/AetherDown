package com.aetherdown.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_path")
    val filePath: String = "",

    @ColumnInfo(name = "mime_type")
    val mimeType: String = "",

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,

    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,

    @ColumnInfo(name = "status")
    val status: DownloadStatus = DownloadStatus.PENDING,

    @ColumnInfo(name = "progress")
    val progress: Int = 0,

    @ColumnInfo(name = "speed")
    val speed: Long = 0L,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "platform")
    val platform: String = "",

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @ColumnInfo(name = "duration")
    val duration: Long = 0L,

    @ColumnInfo(name = "priority")
    val priority: DownloadPriority = DownloadPriority.NORMAL,

    @ColumnInfo(name = "max_connections")
    val maxConnections: Int = 4,

    @ColumnInfo(name = "speed_limit")
    val speedLimit: Long = 0L,

    @ColumnInfo(name = "is_incognito")
    val isIncognito: Boolean = false,

    @ColumnInfo(name = "folder_name")
    val folderName: String = "AetherDown",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "eta")
    val eta: Long = 0L
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    QUEUED,
    VERIFYING
}

enum class DownloadPriority {
    LOW,
    NORMAL,
    HIGH
}
