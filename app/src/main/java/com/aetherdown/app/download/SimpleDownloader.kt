package com.aetherdown.app.download

import android.content.Context
import android.net.Uri
import com.aetherdown.app.domain.model.DownloadRequest
import com.aetherdown.app.domain.repository.MediaDownloadGateway
import com.aetherdown.app.util.FileUtils
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
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
) : MediaDownloadGateway {

    private val httpClient = client.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override suspend fun download(request: DownloadRequest): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (shouldUseYtDlp(request)) {
                    Timber.d("Using yt-dlp download for platform=${request.platform} url=${request.pageUrl ?: request.url}")
                    downloadWithYtDlp(request)
                } else {
                    try {
                        downloadWithHttp(request)
                    } catch (e: Exception) {
                        // CDN often returns HTML soft-blocks without proper session; retry via yt-dlp
                        if (isHtmlOrSoftBlock(e) && hasPageContext(request)) {
                            Timber.w(e, "HTTP download returned non-media; falling back to yt-dlp")
                            downloadWithYtDlp(request)
                        } else {
                            throw e
                        }
                    }
                }
            }.onFailure { Timber.e(it, "SimpleDownloader failed") }
        }

    private fun shouldUseYtDlp(request: DownloadRequest): Boolean {
        val page = request.pageUrl ?: request.referer ?: request.url
        val platform = request.platform
        if (isSocialPage(page) || isSocialPlatform(platform)) return true
        if (request.url.contains(".m3u8", ignoreCase = true)) return true
        // Direct download of a page URL (not a media CDN link)
        if (looksLikeWebPage(request.url) && !looksLikeMediaCdn(request.url)) return true
        return false
    }

    private fun hasPageContext(request: DownloadRequest): Boolean {
        val page = request.pageUrl ?: request.referer
        return !page.isNullOrBlank() || isSocialPage(request.url)
    }

    private fun isHtmlOrSoftBlock(e: Exception): Boolean {
        val msg = e.message.orEmpty()
        return msg.contains("text/html", ignoreCase = true) ||
            msg.contains("application/json", ignoreCase = true) ||
            msg.contains("error document", ignoreCase = true) ||
            msg.contains("too small", ignoreCase = true)
    }

    private fun isSocialPlatform(platform: String): Boolean {
        val p = platform.lowercase()
        return p.contains("twitter") || p.contains("x/twitter") || p == "x" ||
            p.contains("instagram") || p.contains("tiktok") ||
            p.contains("facebook") || p.contains("reddit")
    }

    private fun isSocialPage(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("twitter.com") || u.contains("x.com/") || u.contains("://x.com") ||
            u.contains("instagram.com") || u.contains("tiktok.com") ||
            u.contains("facebook.com") || u.contains("fb.watch") ||
            u.contains("reddit.com")
    }

    private fun looksLikeWebPage(url: String): Boolean {
        val u = url.lowercase()
        if (looksLikeMediaCdn(u)) return false
        return u.contains("twitter.com") || u.contains("x.com") ||
            u.contains("/status/") || u.contains("/reel/") || u.contains("/watch")
    }

    private fun looksLikeMediaCdn(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("video.twimg.com") ||
            u.contains("pbs.twimg.com") ||
            u.contains("cdninstagram") ||
            u.contains("tiktokcdn") ||
            u.contains("googlevideo.com") ||
            u.contains(".mp4") ||
            u.contains(".m4a") ||
            u.contains(".webm")
    }

    private fun downloadWithYtDlp(request: DownloadRequest): Uri {
        val pageUrl = listOf(request.pageUrl, request.referer, request.url)
            .firstOrNull { !it.isNullOrBlank() }
            ?: error("No URL available for yt-dlp download")

        val outDir = File(context.cacheDir, "ytdlp_dl").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
        val outTemplate = File(outDir, "aether_%(id)s.%(ext)s").absolutePath

        val ytdlpRequest = YoutubeDLRequest(pageUrl)

        if (request.formatId.isNotBlank()) {
            // Prefer the chosen format; fall back to a single-file progressive stream
            // so we don't require ffmpeg for merging.
            ytdlpRequest.addOption(
                "-f",
                "${request.formatId}/best[ext=mp4]/best[protocol^=http]/best[protocol^=https]/best"
            )
        } else {
            ytdlpRequest.addOption(
                "-f",
                "best[ext=mp4]/best[protocol^=http]/best[protocol^=https]/best"
            )
        }

        ytdlpRequest.addOption("-o", outTemplate)
        ytdlpRequest.addOption("--no-mtime")
        ytdlpRequest.addOption("--no-playlist")
        ytdlpRequest.addOption("--no-part")
        ytdlpRequest.addOption("--restrict-filenames")
        ytdlpRequest.addOption("--retries", "3")

        if (isSocialPage(pageUrl) || isSocialPlatform(request.platform)) {
            val referer = when {
                pageUrl.contains("twitter.com") || pageUrl.contains("x.com") -> "https://x.com/"
                pageUrl.contains("instagram.com") -> "https://www.instagram.com/"
                pageUrl.contains("tiktok.com") -> "https://www.tiktok.com/"
                else -> pageUrl
            }
            ytdlpRequest.addOption("--add-header", "Referer:$referer")
            ytdlpRequest.addOption("--add-header", "User-Agent:$BROWSER_UA")
        }

        Timber.d("yt-dlp execute: $pageUrl format=${request.formatId}")
        YoutubeDL.getInstance().execute(ytdlpRequest)

        val downloaded = outDir.listFiles()
            ?.filter { it.isFile && it.length() >= 1_024L }
            ?.maxByOrNull { it.lastModified() }
            ?: error("yt-dlp finished but produced no media file")

        try {
            val ext = FileUtils.getExtension(downloaded.name)
            val mime = request.mimeType.takeIf { it.isNotBlank() && it != "video/mp4" }
                ?: FileUtils.getMimeTypeFromExtension(ext)

            // Prefer the user-facing file name when possible
            val finalName = request.fileName.ifBlank { downloaded.name }
            val named = if (finalName != downloaded.name) {
                val target = File(outDir, FileUtils.getSafeFileName(finalName).let {
                    if (it.contains('.')) it else "$it.$ext"
                })
                if (downloaded.renameTo(target)) target else downloaded
            } else {
                downloaded
            }

            return FileUtils.saveToMediaStore(
                context = context,
                file = named,
                mimeType = mime
            ) ?: error("Failed to save to MediaStore")
        } finally {
            outDir.deleteRecursively()
        }
    }

    private fun downloadWithHttp(request: DownloadRequest): Uri {
        val headers = buildHeaders(request)

        val reqBuilder = Request.Builder()
            .url(request.url)
            .apply {
                headers.forEach { (name, value) -> header(name, value) }
                header("Accept-Encoding", "identity")
            }
            .build()

        httpClient.newCall(reqBuilder).execute().use { response ->
            if (!response.isSuccessful) {
                val bodyPreview = response.body?.string()?.take(300).orEmpty()
                error("HTTP ${response.code}: $bodyPreview")
            }

            val body = response.body ?: error("Empty response body")
            val contentType = body.contentType()?.toString().orEmpty()

            if (contentType.contains("text/html", ignoreCase = true) ||
                contentType.contains("application/json", ignoreCase = true)
            ) {
                error("Unexpected content type: $contentType")
            }

            val tempFile = File.createTempFile("aether_", ".part", context.cacheDir)

            try {
                body.byteStream().use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }

                validateDownloadedMedia(tempFile, contentType)

                return FileUtils.saveToMediaStore(
                    context = context,
                    file = tempFile,
                    mimeType = request.mimeType
                ) ?: error("Failed to save to MediaStore")
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun buildHeaders(request: DownloadRequest): Map<String, String> {
        val headers = linkedMapOf<String, String>()

        // Platform defaults first, request headers override
        val page = request.pageUrl ?: request.referer ?: request.url
        if (isSocialPage(page) || isSocialPlatform(request.platform) || looksLikeMediaCdn(request.url)) {
            when {
                page.contains("twitter.com") || page.contains("x.com") ||
                    request.url.contains("twimg.com") -> {
                    headers["Referer"] = "https://x.com/"
                    headers["Origin"] = "https://x.com"
                }
                page.contains("instagram.com") -> {
                    headers["Referer"] = "https://www.instagram.com/"
                    headers["Origin"] = "https://www.instagram.com"
                }
                page.contains("tiktok.com") -> {
                    headers["Referer"] = "https://www.tiktok.com/"
                    headers["Origin"] = "https://www.tiktok.com"
                }
            }
        }

        headers["User-Agent"] = BROWSER_UA
        headers["Accept"] = "*/*"

        if (!request.referer.isNullOrBlank()) {
            headers["Referer"] = request.referer
        }
        if (!request.pageUrl.isNullOrBlank() && headers["Referer"].isNullOrBlank()) {
            headers["Referer"] = request.pageUrl
        }

        request.headers.forEach { (name, value) ->
            headers[name] = value
        }

        return headers
    }

    private fun validateDownloadedMedia(file: File, contentType: String) {
        require(file.length() >= 1_024L) { "Downloaded file is too small" }

        val firstBytes = file.inputStream().use { it.readNBytes(512) }
        val text = firstBytes.toString(Charsets.UTF_8).trimStart().lowercase()

        require(
            !text.startsWith("<!doctype") &&
                !text.startsWith("<html") &&
                !text.startsWith("{\"") &&
                !contentType.contains("text/html", true)
        ) {
            "Server returned an error document instead of media"
        }
    }

    companion object {
        private const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    }
}
