package com.aetherdown.app.data.repository

import com.aetherdown.app.data.local.dao.DownloadDao
import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import com.aetherdown.app.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao
) : DownloadRepository {

    override fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    override fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>> =
        downloadDao.getDownloadsByStatus(status)

    override fun getDownloadById(id: Long): Flow<DownloadEntity?> =
        downloadDao.getDownloadById(id)

    override suspend fun getDownloadByIdOnce(id: Long): DownloadEntity? =
        downloadDao.getDownloadByIdOnce(id)

    override fun getActiveDownloads(): Flow<List<DownloadEntity>> =
        downloadDao.getActiveDownloads()

    override suspend fun getCurrentlyDownloading(): DownloadEntity? =
        downloadDao.getCurrentlyDownloading()

    override fun getActiveDownloadCount(): Flow<Int> =
        downloadDao.getActiveDownloadCount()

    override suspend fun insertDownload(download: DownloadEntity): Long =
        downloadDao.insertDownload(download)

    override suspend fun updateDownload(download: DownloadEntity) =
        downloadDao.updateDownload(download)

    override suspend fun updateStatus(id: Long, status: DownloadStatus) =
        downloadDao.updateStatus(id, status)

    override suspend fun updateProgress(id: Long, downloaded: Long, progress: Int, speed: Long, eta: Long) =
        downloadDao.updateProgress(id, downloaded, progress, speed, eta)

    override suspend fun deleteDownloadById(id: Long) =
        downloadDao.deleteDownloadById(id)

    override suspend fun deleteByStatus(status: DownloadStatus) =
        downloadDao.deleteByStatus(status)

    override suspend fun deleteAll() = downloadDao.deleteAll()

    override fun getTotalDownloadedBytes(): Flow<Long> =
        downloadDao.getTotalDownloadedBytes()

    override fun getCompletedCount(): Flow<Int> =
        downloadDao.getCompletedCount()
}
