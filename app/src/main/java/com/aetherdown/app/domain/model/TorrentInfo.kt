package com.aetherdown.app.domain.model

data class TorrentInfo(
    val id: Long = 0,
    val magnetUri: String = "",
    val torrentPath: String = "",
    val name: String = "",
    val savePath: String = "",
    val status: TorrentStatus = TorrentStatus.STOPPED,
    val progress: Int = 0,
    val totalSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val uploadedBytes: Long = 0L,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val seeders: Int = 0,
    val leechers: Int = 0,
    val files: List<TorrentFileInfo> = emptyList(),
    val isIncognito: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class TorrentFileInfo(
    val id: Long = 0,
    val torrentId: Long = 0,
    val fileIndex: Int,
    val fileName: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val isSelected: Boolean = true
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
