package com.aetherdown.app.download

import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class DownloadTask(
    val downloadId: Long,
    private val entity: DownloadEntity,
    private val client: OkHttpClient,
    private val downloadDir: File
) {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val _state = MutableStateFlow(entity)
    val state: StateFlow<DownloadEntity> = _state.asStateFlow()

    private val isCancelled = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val totalDownloaded = AtomicLong(entity.downloadedBytes)
    private val speedLimiter = SpeedLimiter(entity.speedLimit)

    private var chunkJobs = listOf<Job>()
    private var startTime = 0L

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    }

    data class Progress(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: Long,
        val progress: Int,
        val eta: Long
    )

    private val _progress = MutableStateFlow(Progress(
        downloadedBytes = entity.downloadedBytes,
        totalBytes = entity.fileSize,
        speed = 0L,
        progress = entity.progress,
        eta = 0L
    ))
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    suspend fun start() {
        Timber.d("Starting download task $downloadId for ${_state.value.url}")
        startTime = System.currentTimeMillis()
        _state.value = _state.value.copy(status = DownloadStatus.DOWNLOADING)

        try {
            val fileName = _state.value.fileName.ifEmpty { "download_${System.currentTimeMillis()}" }
            val file = File(downloadDir, fileName)
            val tempDir = File(downloadDir, ".$fileName.parts")
            tempDir.mkdirs()

            var fileSize = _state.value.fileSize
            if (fileSize == 0L) {
                Timber.d("File size unknown, probing...")
                fileSize = probeFileSize()
                Timber.d("Probe result: $fileSize bytes")
            }

            val numChunks = if (fileSize > 0) _state.value.maxConnections.coerceIn(1, 16) else 1
            
            if (numChunks > 1 && fileSize > 0) {
                val chunkSize = fileSize / numChunks
                val ranges = (0 until numChunks).map { index ->
                    val start = index * chunkSize
                    val end = if (index == numChunks - 1) fileSize - 1 else (start + chunkSize - 1)
                    Range(start, end, index)
                }

                chunkJobs = ranges.map { range ->
                    scope.launch { downloadChunk(range, tempDir) }
                }

                chunkJobs.forEach { it.join() }
                
                if (!isCancelled.get() && !isPaused.get()) {
                    Timber.d("Merging chunks for $downloadId")
                    mergeChunks(tempDir, file, ranges, fileSize)
                }
            } else {
                // Single stream download
                Timber.d("Starting single stream download for $downloadId")
                downloadSingleStream(file)
            }

            if (!isCancelled.get() && !isPaused.get()) {
                tempDir.deleteRecursively()
                val finalSize = if (fileSize > 0) fileSize else file.length()
                var savedUri: android.net.Uri? = null
                var savedPath = file.absolutePath
                
                try {
                    savedUri = com.aetherdown.app.util.FileUtils.saveToMediaStore(
                        context = com.aetherdown.app.AetherApp.instance,
                        file = file,
                        mimeType = com.aetherdown.app.util.FileUtils.getMimeTypeFromExtension(
                            com.aetherdown.app.util.FileUtils.getExtension(file.name)
                        )
                    )
                    if (savedUri != null) {
                        savedPath = savedUri.toString()
                        Timber.d("File saved to MediaStore: $savedUri")
                        file.delete()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save to MediaStore")
                }

                _state.value = _state.value.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100,
                    downloadedBytes = finalSize,
                    fileSize = finalSize,
                    filePath = savedPath,
                    completedAt = System.currentTimeMillis()
                )
                _progress.value = Progress(
                    downloadedBytes = finalSize,
                    totalBytes = finalSize,
                    speed = 0L,
                    progress = 100,
                    eta = 0L
                )
                Timber.d("Download $downloadId completed successfully at $savedPath")

                showCompletionNotification()
            }
        } catch (e: CancellationException) {
            Timber.d("Download $downloadId cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Download $downloadId failed fatally")
            fail(e.message ?: "Unknown error")
        }
    }

    private suspend fun downloadSingleStream(targetFile: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(_state.value.url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .header("Referer", _state.value.url)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val body = response.body ?: throw Exception("Empty body")
                val source = body.byteStream()
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (source.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled.get()) throw CancellationException("Cancelled")
                        while (isPaused.get()) {
                            delay(100)
                            if (isCancelled.get()) throw CancellationException("Cancelled")
                        }
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded.addAndGet(bytesRead.toLong())
                        speedLimiter.limit(bytesRead, Dispatchers.IO)
                        updateProgress()
                    }
                }
            }
        } catch (e: Exception) {
            if (e !is CancellationException) Timber.e(e, "Single stream download failed")
            throw e
        }
    }

    private suspend fun probeFileSize(): Long = withContext(Dispatchers.IO) {
        try {
            val headRequest = Request.Builder()
                .head()
                .url(entity.url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .build()
            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val length = response.header("Content-Length")?.toLongOrNull() ?: 0L
                    if (length > 0) {
                        val acceptRanges = response.header("Accept-Ranges")
                        if (acceptRanges == null || acceptRanges == "none") {
                            _state.value = _state.value.copy(maxConnections = 1)
                        }
                        _state.value = _state.value.copy(fileSize = length)
                        return@withContext length
                    }
                }
            }

            // Fallback to GET with small range
            val getRequest = Request.Builder()
                .url(entity.url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header("Range", "bytes=0-1")
                .build()
            client.newCall(getRequest).execute().use { response ->
                val contentRange = response.header("Content-Range")
                val total = contentRange?.substringAfterLast("/")?.toLongOrNull() ?: response.header("Content-Length")?.toLongOrNull() ?: 0L
                
                val acceptRanges = response.header("Accept-Ranges")
                if (contentRange == null && (acceptRanges == null || acceptRanges == "none")) {
                    _state.value = _state.value.copy(maxConnections = 1)
                }
                
                _state.value = _state.value.copy(fileSize = total)
                total
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to probe file size")
            0L
        }
    }

    private suspend fun downloadChunk(range: Range, tempDir: File) = withContext(Dispatchers.IO) {
        val partFile = File(tempDir, "part_${range.index}")
        val request = Request.Builder()
            .url(entity.url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .header("Referer", entity.url)
            .header("Range", "bytes=${range.start}-${range.end}")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val body = response.body ?: return@use
                val source = body.byteStream()
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (source.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled.get()) throw CancellationException("Cancelled")
                        while (isPaused.get()) {
                            delay(100)
                            if (isCancelled.get()) throw CancellationException("Cancelled")
                        }
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded.addAndGet(bytesRead.toLong())
                        speedLimiter.limit(bytesRead, Dispatchers.IO)
                        updateProgress()
                    }
                }
            }
        } catch (e: CancellationException) {
            Timber.d("Chunk ${range.index} cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Chunk ${range.index} failed")
            throw e
        }
    }

    private suspend fun mergeChunks(tempDir: File, targetFile: File, ranges: List<Range>, totalSize: Long) {
        RandomAccessFile(targetFile, "rw").use { raf ->
            raf.setLength(totalSize)
            for (range in ranges) {
                val partFile = File(tempDir, "part_${range.index}")
                if (partFile.exists()) {
                    partFile.inputStream().use { input ->
                        raf.seek(range.start)
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            raf.write(buf, 0, read)
                        }
                    }
                    partFile.delete()
                }
            }
        }
    }

    private fun updateProgress() {
        val fileSize = _state.value.fileSize
        if (fileSize <= 0L) return
        val downloaded = totalDownloaded.get()
        val pct = ((downloaded * 100) / fileSize).toInt().coerceIn(0, 100)
        val elapsed = System.currentTimeMillis() - startTime
        val speed = if (elapsed > 0) (downloaded * 1000) / elapsed else 0L
        val remaining = fileSize - downloaded
        val eta = if (speed > 0) remaining / speed else 0L

        _progress.value = Progress(downloaded, fileSize, speed, pct, eta)
        _state.value = _state.value.copy(
            downloadedBytes = downloaded,
            progress = pct,
            speed = speed,
            eta = eta
        )
    }

    fun pause() {
        isPaused.set(true)
        _state.value = _state.value.copy(status = DownloadStatus.PAUSED)
        Timber.d("Download $downloadId paused")
    }

    fun resume() {
        isPaused.set(false)
        _state.value = _state.value.copy(status = DownloadStatus.DOWNLOADING)
        Timber.d("Download $downloadId resumed")
    }

    fun cancel() {
        isCancelled.set(true)
        isPaused.set(false)
        job.cancel()
        Timber.d("Download $downloadId cancelled")
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(
            status = DownloadStatus.FAILED,
            errorMessage = message
        )
    }

    fun updateSpeedLimit(limit: Long) {
        speedLimiter.maxBytesPerSecond = limit
        _state.value = _state.value.copy(speedLimit = limit)
    }

    private fun showCompletionNotification() {
        try {
            val context = com.aetherdown.app.AetherApp.instance
            val notification = android.app.Notification.Builder(context, com.aetherdown.app.util.Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Download complete")
                .setContentText(_state.value.fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .build()
            val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(downloadId.toInt(), notification)
        } catch (e: Exception) {
            Timber.e(e, "Failed to show completion notification")
        }
    }
}
