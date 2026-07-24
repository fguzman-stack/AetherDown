package com.aetherdown.app.download

import android.content.Context
import android.net.Uri
import com.aetherdown.app.domain.model.DownloadRequest
import com.aetherdown.app.data.local.entity.HistoryEntity
import com.aetherdown.app.domain.repository.HistoryRepository
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
    @ApplicationContext private val context: Context,
    private val historyRepository: HistoryRepository
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
                        if (isHtmlOrSoftBlock(e)) {
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
            p.contains("facebook") || p.contains("reddit") ||
            p.contains("youtube") || p.contains("vimeo") || p.contains("twitch") ||
            p.contains("dailymotion") || p.contains("soundcloud")
    }

    private fun isSocialPage(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("twitter.com") || u.contains("x.com") || u.contains("twimg.com") ||
            u.contains("instagram.com") || u.contains("cdninstagram.com") || u.contains("instagr.am") ||
            u.contains("tiktok.com") || u.contains("tiktokcdn.com") ||
            u.contains("facebook.com") || u.contains("fb.watch") || u.contains("fb.com") || u.contains("fbcdn.net") ||
            u.contains("reddit.com") || u.contains("redd.it") ||
            u.contains("youtube.com") || u.contains("youtu.be") || u.contains("googlevideo.com") ||
            u.contains("vimeo.com") || u.contains("vimeocdn.com") ||
            u.contains("twitch.tv") ||
            u.contains("dailymotion.com") || u.contains("dmcdn.net") ||
            u.contains("soundcloud.com") || u.contains("sndcdn.com")
    }

    private fun looksLikeWebPage(url: String): Boolean {
        val u = url.lowercase().substringBefore("?")
        if (looksLikeMediaCdn(u)) return false
        
        return u.contains("twitter.com") || u.contains("x.com") ||
            u.contains("youtube.com") || u.contains("youtu.be") ||
            u.contains("instagram.com") || u.contains("instagr.am") || u.contains("tiktok.com") ||
            u.contains("facebook.com") || u.contains("fb.watch") ||
            u.contains("/status/") || u.contains("/reel/") || u.contains("/reels/") || u.contains("/watch") ||
            u.contains("/shorts/") || u.contains("/p/") || u.contains("/tv/") ||
            !u.contains(".") || u.endsWith(".html") || u.endsWith(".htm") || u.endsWith("/")
    }

    private fun looksLikeMediaCdn(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("video.twimg.com") ||
            u.contains("pbs.twimg.com") ||
            u.contains("cdninstagram") ||
            u.contains("tiktokcdn") ||
            u.contains("googlevideo.com") ||
            u.contains("fbcdn.net") ||
            u.contains(".mp4") ||
            u.contains(".m4a") ||
            u.contains(".webm") ||
            u.contains(".mp3") ||
            u.contains(".wav") ||
            u.contains(".gif")
    }

    private suspend fun downloadWithYtDlp(request: DownloadRequest): Uri {
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
        ytdlpRequest.addOption("--no-update")
        ytdlpRequest.addOption("--no-warnings")
        ytdlpRequest.addOption("--no-check-certificate")
        ytdlpRequest.addOption("--geo-bypass")
        ytdlpRequest.addOption("--downloader", "libaria2c.so")
        ytdlpRequest.addOption("--restrict-filenames")
        ytdlpRequest.addOption("--retries", "3")

        if (isSocialPage(pageUrl) || isSocialPlatform(request.platform)) {
            val referer = when {
                pageUrl.contains("twitter.com") || pageUrl.contains("x.com") -> "https://x.com/"
                pageUrl.contains("instagram.com") || pageUrl.contains("instagr.am") -> "https://www.instagram.com/"
                pageUrl.contains("tiktok.com") -> "https://www.tiktok.com/"
                pageUrl.contains("youtube.com") || pageUrl.contains("youtu.be") -> "https://www.youtube.com/"
                pageUrl.contains("facebook.com") || pageUrl.contains("fb.watch") -> "https://www.facebook.com/"
                else -> pageUrl
            }
            ytdlpRequest.addOption("--referer", referer)
            ytdlpRequest.addOption("--user-agent", BROWSER_UA)
            ytdlpRequest.addOption("--add-header", "Accept-Language:en-US,en;q=0.9")
            if (pageUrl.contains("instagram.com") || pageUrl.contains("instagr.am")) {
                ytdlpRequest.addOption("--extractor-args", "instagram:allow_vp9=True")
            }
            if (pageUrl.contains("twitter.com") || pageUrl.contains("x.com")) {
                ytdlpRequest.addOption("--extractor-args", "twitter:api=syndication")
            }
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

            val uri = FileUtils.saveToMediaStore(
                context = context,
                file = named,
                mimeType = mime
            ) ?: error("Failed to save to MediaStore")

            if (!request.isIncognito) {
                val history = HistoryEntity(
                    url = request.pageUrl ?: request.url,
                    fileName = finalName,
                    filePath = uri.toString(),
                    fileSize = named.length(),
                    platform = request.platform,
                    title = request.title,
                    thumbnailUrl = request.thumbnailUrl,
                    duration = request.duration,
                    mimeType = mime,
                    isIncognito = false
                )
                historyRepository.insertHistory(history)
            }

            return uri
        } finally {
            outDir.deleteRecursively()
        }
    }

    private suspend fun downloadWithHttp(request: DownloadRequest): Uri {
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

                val uri = FileUtils.saveToMediaStore(
                    context = context,
                    file = tempFile,
                    mimeType = request.mimeType
                ) ?: error("Failed to save to MediaStore")

                if (!request.isIncognito) {
                    val history = HistoryEntity(
                        url = request.pageUrl ?: request.url,
                        fileName = request.fileName.ifBlank { tempFile.name },
                        filePath = uri.toString(),
                        fileSize = tempFile.length(),
                        platform = request.platform,
                        title = request.title,
                        thumbnailUrl = request.thumbnailUrl,
                        duration = request.duration,
                        mimeType = request.mimeType,
                        isIncognito = false
                    )
                    historyRepository.insertHistory(history)
                }

                return uri
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
                page.contains("instagram.com") || page.contains("instagr.am") || 
                    request.url.contains("cdninstagram") -> {
                    headers["Referer"] = "https://www.instagram.com/"
                    headers["Origin"] = "https://www.instagram.com"
                }
                page.contains("tiktok.com") || request.url.contains("tiktokcdn") -> {
                    headers["Referer"] = "https://www.tiktok.com/"
                    headers["Origin"] = "https://www.tiktok.com"
                }
                page.contains("youtube.com") || page.contains("youtu.be") || 
                    request.url.contains("googlevideo.com") -> {
                    headers["Referer"] = "https://www.youtube.com/"
                }
                page.contains("facebook.com") || page.contains("fb.watch") || 
                    request.url.contains("fbcdn.net") -> {
                    headers["Referer"] = "https://www.facebook.com/"
                    headers["Origin"] = "https://www.facebook.com"
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
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
