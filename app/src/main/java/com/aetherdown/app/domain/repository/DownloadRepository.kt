package com.aetherdown.app.domain.repository

import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>
    fun getDownloadById(id: Long): Flow<DownloadEntity?>
    suspend fun getDownloadByIdOnce(id: Long): DownloadEntity?
    fun getActiveDownloads(): Flow<List<DownloadEntity>>
    suspend fun getCurrentlyDownloading(): DownloadEntity?
    fun getActiveDownloadCount(): Flow<Int>
    suspend fun insertDownload(download: DownloadEntity): Long
    suspend fun updateDownload(download: DownloadEntity)
    suspend fun updateStatus(id: Long, status: DownloadStatus)
    suspend fun updateProgress(id: Long, downloaded: Long, progress: Int, speed: Long, eta: Long)
    suspend fun deleteDownloadById(id: Long)
    suspend fun deleteByStatus(status: DownloadStatus)
    suspend fun deleteAll()
    fun getTotalDownloadedBytes(): Flow<Long>
    fun getCompletedCount(): Flow<Int>
}
