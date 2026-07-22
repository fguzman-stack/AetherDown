package com.aetherdown.app.domain.usecase

import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import com.aetherdown.app.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQueueUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    fun getActiveDownloads(): Flow<List<DownloadEntity>> {
        return downloadRepository.getActiveDownloads()
    }

    fun getAllDownloads(): Flow<List<DownloadEntity>> {
        return downloadRepository.getAllDownloads()
    }

    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>> {
        return downloadRepository.getDownloadsByStatus(status)
    }

    fun getActiveDownloadCount(): Flow<Int> {
        return downloadRepository.getActiveDownloadCount()
    }
}
