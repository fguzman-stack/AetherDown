package com.aetherdown.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MediaProcessingService : Service() {

    @Inject lateinit var notificationHelper: ForegroundNotification

    override fun onCreate() {
        super.onCreate()
        Timber.d("MediaProcessingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PROCESS -> {
                val notification = notificationHelper.buildNotification(isIndeterminate = true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Timber.w("MediaProcessingService timeout for startId=$startId, fgsType=$fgsType")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MediaProcessingService destroyed")
    }

    companion object {
        const val ACTION_PROCESS = "com.aetherdown.app.action.PROCESS_MEDIA"
        const val ACTION_STOP = "com.aetherdown.app.action.STOP_MEDIA_PROCESSING"
        const val NOTIFICATION_ID = 1002
    }
}
