package com.aetherdown.app.domain.model

data class DownloadInfo(
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val speed: Long = 0L,
    val errorMessage: String? = null,
    val platform: String = "",
    val title: String = "",
    val thumbnailUrl: String? = null,
    val duration: Long = 0L,
    val priority: DownloadPriority = DownloadPriority.NORMAL,
    val maxConnections: Int = 4,
    val speedLimit: Long = 0L,
    val isIncognito: Boolean = false,
    val folderName: String = "AetherDown",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
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
