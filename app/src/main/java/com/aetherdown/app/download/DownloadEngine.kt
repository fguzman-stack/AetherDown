package com.aetherdown.app.download

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import com.aetherdown.app.domain.repository.DownloadRepository
import com.aetherdown.app.service.DownloadService
import com.aetherdown.app.util.FileUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository,
    private val client: OkHttpClient
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeTasks = ConcurrentHashMap<Long, DownloadTask>()
    private val pendingQueue = MutableStateFlow<List<Long>>(emptyList())

    private val maxConcurrent = 3

    private val okHttpClient = client.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val activeCount = AtomicInteger(0)

    fun enqueue(downloadId: Long) {
        scope.launch {
            Timber.d("Enqueuing download $downloadId")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                enqueueUserInitiatedJob(downloadId)
            } else {
                if (activeCount.get() == 0) startForegroundService()
                if (activeCount.get() < maxConcurrent) {
                    startDownloadInternal(downloadId)
                } else {
                    addToQueue(downloadId)
                }
            }
        }
    }

    private fun enqueueUserInitiatedJob(downloadId: Long) {
        val jobInfo = JobInfo.Builder(downloadId.toInt(), ComponentName(context, com.aetherdown.app.service.DownloadJobService::class.java))
            .setUserInitiated(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setExtras(PersistableBundle().apply { putLong("DOWNLOAD_ID", downloadId) })
            .build()
        
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val result = jobScheduler.schedule(jobInfo)
        if (result == JobScheduler.RESULT_SUCCESS) {
            Timber.d("User-initiated job scheduled for download $downloadId")
        } else {
            Timber.w("JobScheduler failed ($result), falling back to foreground service for download $downloadId")
            if (activeCount.get() == 0) startForegroundService()
            scope.launch { startDownloadInternal(downloadId) }
        }
    }

    private fun startForegroundService() {
        try {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_START
            }
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start foreground service")
        }
    }

    private fun stopForegroundServiceIfIdle() {
        scope.launch {
            delay(2000)
            if (activeTasks.isEmpty() && pendingQueue.value.isEmpty()) {
                try {
                    val intent = Intent(context, DownloadService::class.java).apply {
                        action = DownloadService.ACTION_STOP
                    }
                    context.startService(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to stop foreground service")
                }
            }
        }
    }

    suspend fun startDownloadInternal(downloadId: Long) {
        if (activeTasks.containsKey(downloadId)) return
        
        val entity = downloadRepository.getDownloadByIdOnce(downloadId) ?: return
        val downloadDir = getDownloadDir(entity)

        val task = DownloadTask(downloadId, entity, okHttpClient, downloadDir)
        activeTasks[downloadId] = task
        activeCount.incrementAndGet()

        scope.launch {
            task.state.collect { updated ->
                downloadRepository.updateDownload(updated)
                when (updated.status) {
                    DownloadStatus.COMPLETED, DownloadStatus.FAILED -> {
                        if (activeTasks.remove(downloadId) != null) {
                            activeCount.decrementAndGet()
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                processQueue()
                                stopForegroundServiceIfIdle()
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        scope.launch {
            task.progress.collect { prog ->
                downloadRepository.updateProgress(
                    id = downloadId,
                    downloaded = prog.downloadedBytes,
                    progress = prog.progress,
                    speed = prog.speed,
                    eta = prog.eta
                )
            }
        }

        activeTasks[downloadId] = task
        downloadRepository.updateStatus(downloadId, DownloadStatus.DOWNLOADING)
        task.start()
    }

    fun pause(downloadId: Long) {
        activeTasks[downloadId]?.pause()
    }

    fun resumeDownload(downloadId: Long) {
        activeTasks[downloadId]?.resume()
    }

    fun cancel(downloadId: Long) {
        activeTasks[downloadId]?.cancel()
        if (activeTasks.remove(downloadId) != null) {
            activeCount.decrementAndGet()
        }
        pendingQueue.value = pendingQueue.value.filter { it != downloadId }
        stopForegroundServiceIfIdle()
    }

    fun getActiveTask(downloadId: Long): DownloadTask? = activeTasks[downloadId]

    fun updateSpeedLimit(downloadId: Long, limit: Long) {
        activeTasks[downloadId]?.updateSpeedLimit(limit)
    }

    private fun addToQueue(downloadId: Long) {
        val current = pendingQueue.value.toMutableList()
        if (!current.contains(downloadId)) {
            current.add(downloadId)
            pendingQueue.value = current
            Timber.d("Download $downloadId queued (${current.size} waiting)")
        }
    }

    private fun processQueue() {
        scope.launch {
            val queue = pendingQueue.value.toMutableList()
            while (queue.isNotEmpty() && activeTasks.size < maxConcurrent) {
                val nextId = queue.removeFirst()
                pendingQueue.value = queue.toList()
                startDownloadInternal(nextId)
            }
        }
    }

    private fun getDownloadDir(entity: DownloadEntity): File {
        return FileUtils.getDownloadDir(context, entity.folderName.ifEmpty { "AetherDown" })
    }

    fun destroy() {
        activeTasks.values.forEach { it.cancel() }
        activeTasks.clear()
        scope.cancel()
    }
}
