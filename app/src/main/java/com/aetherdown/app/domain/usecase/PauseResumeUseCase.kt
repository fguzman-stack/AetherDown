package com.aetherdown.app.domain.usecase

import com.aetherdown.app.data.local.entity.DownloadStatus
import com.aetherdown.app.download.DownloadEngine
import com.aetherdown.app.domain.repository.DownloadRepository
import timber.log.Timber
import javax.inject.Inject

class PauseResumeUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val downloadEngine: DownloadEngine
) {
    suspend fun pause(id: Long) {
        downloadEngine.pause(id)
        downloadRepository.updateStatus(id, DownloadStatus.PAUSED)
        Timber.d("Download paused: $id")
    }

    suspend fun resume(id: Long) {
        downloadRepository.updateStatus(id, DownloadStatus.PENDING)
        downloadEngine.enqueue(id)
        Timber.d("Download resumed: $id")
    }

    suspend fun cancel(id: Long) {
        downloadEngine.cancel(id)
        downloadRepository.updateStatus(id, DownloadStatus.FAILED)
        Timber.d("Download cancelled: $id")
    }

    suspend fun delete(id: Long) {
        downloadEngine.cancel(id)
        downloadRepository.deleteDownloadById(id)
        Timber.d("Download deleted: $id")
    }
}
