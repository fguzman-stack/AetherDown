package com.aetherdown.app.download

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.aetherdown.app.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleDownloader @Inject constructor(
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    private val httpClient = client.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun download(
        url: String,
        fileName: String,
        mimeType: String = "video/mp4",
        referer: String? = null,
        headers: Map<String, String> = emptyMap()
    ): Result<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("SimpleDownloader starting: $url")

                val tempFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}_$fileName")

                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "video/*, audio/*, image/*, */*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                if (referer != null) {
                    requestBuilder.header("Referer", referer)
                }
                for ((key, value) in headers) {
                    if (!key.equals("User-Agent", ignoreCase = true) &&
                        !key.equals("Accept", ignoreCase = true) &&
                        !key.equals("Accept-Language", ignoreCase = true) &&
                        !key.equals("Referer", ignoreCase = true)) {
                        requestBuilder.header(key, value)
                    }
                }

                httpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                    }
                    val body = response.body ?: return@withContext Result.failure(Exception("Empty body"))

                    tempFile.outputStream().use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output, bufferSize = 128 * 1024)
                            output.flush()
                        }
                    }

                    val fileLen = tempFile.length()
                    if (fileLen < 100) {
                        val snippet = if (fileLen > 0) tempFile.readText().take(100) else "empty"
                        tempFile.delete()
                        return@withContext Result.failure(Exception("File too small ($fileLen bytes): $snippet"))
                    }

                    val magic = tempFile.inputStream().use { it.readNBytes(512) }
                    val head = String(magic).trimStart().take(50)
                    if (head.startsWith("<!DOCTYPE") || head.startsWith("<html") || head.startsWith("{")) {
                        val snippet = String(magic).take(200)
                        tempFile.delete()
                        return@withContext Result.failure(Exception("Server returned error page: ${snippet.take(100)}"))
                    }
                }

                Timber.d("Downloaded: ${tempFile.absolutePath} (${tempFile.length()} bytes)")

                val uri = FileUtils.saveToMediaStore(context, tempFile, mimeType)
                tempFile.delete()

                if (uri != null) {
                    Timber.d("Saved to MediaStore: $uri")
                    Result.success(uri)
                } else {
                    Result.failure(Exception("Failed to save to MediaStore"))
                }
            } catch (e: Exception) {
                Timber.e(e, "SimpleDownloader failed")
                Result.failure(e)
            }
        }
    }
}
