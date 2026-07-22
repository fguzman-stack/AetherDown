package com.aetherdown.app.domain.usecase

import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import com.aetherdown.app.download.DownloadEngine
import com.aetherdown.app.domain.repository.DownloadRepository
import com.aetherdown.app.domain.repository.SettingsRepository
import com.aetherdown.app.util.FileUtils
import timber.log.Timber
import javax.inject.Inject

class StartDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val downloadEngine: DownloadEngine,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        url: String,
        fileName: String? = null,
        platform: String = "",
        title: String = "",
        thumbnailUrl: String? = null,
        duration: Long = 0L,
        maxConnections: Int? = null,
        speedLimit: Long? = null,
        folderName: String? = null,
        isIncognito: Boolean = false
    ): Result<Long> {
        return try {
            val settings = settingsRepository.getSettingsOnce()
            
            val finalFileName = if (fileName != null) {
                FileUtils.getSafeFileName(fileName)
            } else {
                val nameFromUrl = url.substringAfterLast("/").substringBefore("?")
                FileUtils.getSafeFileName(nameFromUrl.ifEmpty { "download_${System.currentTimeMillis()}" })
            }

            val download = DownloadEntity(
                url = url,
                fileName = finalFileName,
                status = DownloadStatus.PENDING,
                platform = platform,
                title = title,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                maxConnections = maxConnections ?: settings.defaultMaxConnections,
                speedLimit = speedLimit ?: settings.defaultSpeedLimit,
                folderName = folderName ?: (if (settings.organizeByPlatform && platform.isNotEmpty()) platform else settings.downloadDirectory),
                isIncognito = isIncognito || settings.incognitoMode,
                priority = com.aetherdown.app.data.local.entity.DownloadPriority.NORMAL
            )
            val id = downloadRepository.insertDownload(download)
            downloadEngine.enqueue(id)
            Timber.d("Download queued: $id -> $url")
            Result.success(id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start download")
            Result.failure(e)
        }
    }
}
