package com.aetherdown.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.aetherdown.app.data.local.entity.DownloadStatus
import com.aetherdown.app.download.DownloadEngine
import com.aetherdown.app.domain.repository.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var downloadEngine: DownloadEngine
    @Inject lateinit var downloadRepository: DownloadRepository
    @Inject lateinit var notificationHelper: ForegroundNotification

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("DownloadService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = notificationHelper.buildNotification(isIndeterminate = true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        ForegroundNotification.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(ForegroundNotification.NOTIFICATION_ID, notification)
                }
                observeDownloads()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun observeDownloads() {
        notificationJob?.cancel()
        notificationJob = scope.launch {
            downloadRepository.getActiveDownloads().collect { downloads ->
                if (downloads.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collect
                }

                val active = downloads.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
                if (active != null) {
                    val notification = notificationHelper.buildNotification(
                        title = active.title.ifEmpty { active.fileName },
                        progress = active.progress,
                        speed = formatSpeed(active.speed)
                    )
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(ForegroundNotification.NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun formatSpeed(speed: Long): String {
        return when {
            speed > 1_000_000 -> "${speed / 1_000_000} MB/s"
            speed > 1_000 -> "${speed / 1_000} KB/s"
            else -> "$speed B/s"
        }
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        scope.cancel()
        super.onDestroy()
        Timber.d("DownloadService destroyed")
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Timber.w("DownloadService timeout for startId=$startId, fgsType=$fgsType")
        downloadEngine.pauseAndPersistActiveWork()
        stopSelf(startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.aetherdown.app.action.START"
        const val ACTION_STOP = "com.aetherdown.app.action.STOP"
    }
}
