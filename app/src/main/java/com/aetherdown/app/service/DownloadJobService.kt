package com.aetherdown.app.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
import com.aetherdown.app.download.DownloadEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class DownloadJobService : JobService() {

    @Inject lateinit var downloadEngine: DownloadEngine
    @Inject lateinit var notificationHelper: ForegroundNotification

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val downloadId = params.extras.getLong("DOWNLOAD_ID")
        Timber.d("DownloadJobService started for download $downloadId")

        setNotification(
            params,
            ForegroundNotification.NOTIFICATION_ID,
            notificationHelper.buildNotification("Downloading...", 0),
            JOB_END_NOTIFICATION_POLICY_DETACH
        )

        downloadJob = scope.launch {
            try {
                downloadEngine.startDownloadInternal(downloadId)
                Timber.d("DownloadJobService: download $downloadId completed")
                jobFinished(params, false)
            } catch (e: Exception) {
                Timber.e(e, "DownloadJobService: download $downloadId failed")
                jobFinished(params, true)
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val downloadId = params.extras.getLong("DOWNLOAD_ID")
        Timber.d("DownloadJobService: onStopJob for download $downloadId")
        downloadEngine.pause(downloadId)
        downloadJob?.cancel()
        scope.cancel()
        return false
    }
}
