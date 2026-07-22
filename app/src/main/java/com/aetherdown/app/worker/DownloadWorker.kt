package com.aetherdown.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aetherdown.app.download.DownloadEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadEngine: DownloadEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong("DOWNLOAD_ID", -1L)
        if (downloadId == -1L) return Result.failure()

        downloadEngine.enqueue(downloadId)
        return Result.success()
    }
}
