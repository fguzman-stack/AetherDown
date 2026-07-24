package com.aetherdown.app.extractor

import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.domain.model.StreamInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoTagExtractor @Inject constructor(
    private val client: OkHttpClient
) : Extractor {

    override val platformName: String = "Web Video"

    override val patterns: List<Regex> = listOf(
        Regex("https?://[^\\s\"'<>]+")
    )

    override suspend fun extract(url: String): Result<ExtractResult> {
        val cleanUrl = sanitizeWebUrl(url) ?: return Result.failure(
            IllegalArgumentException("Invalid URL")
        )

        try {
            val (title, videoUrl, thumbnailUrl) = fetchPageAndExtract(cleanUrl)
                ?: return Result.failure(IllegalArgumentException("No video found on page"))

            val extension = videoUrl.substringAfterLast('.').substringBefore('?').ifEmpty { "mp4" }
            val mimeType = getMimeTypeForExtension(extension)

            val streams = listOf(
                StreamInfo(
                    formatId = "web_${extension}",
                    url = videoUrl,
                    quality = "Original",
                    format = extension,
                    mimeType = mimeType,
                    hasVideo = true,
                    hasAudio = true,
                    httpHeaders = mapOf("Referer" to cleanUrl, "User-Agent" to BROWSER_UA)
                )
            )

            return Result.success(
                ExtractResult(
                    title = title,
                    url = cleanUrl,
                    thumbnailUrl = thumbnailUrl,
                    platform = "Web Video",
                    streams = streams
                )
            )
        } catch (e: Exception) {
            Timber.w(e, "Web video extraction failed for: $url")
            return Result.failure(e)
        }
    }

    private suspend fun fetchPageAndExtract(url: String): Triple<String, String, String?>? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_UA)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val html = client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string() ?: return null
        }

        val title = extractTitle(html) ?: url.substringAfterLast("/").substringBefore("?").ifEmpty { "Web Video" }

        val videoUrl = extractVideoSource(html) ?: extractOgVideo(html)
        if (videoUrl.isNullOrBlank()) return null

        val cleanVideoUrl = videoUrl.replace("&amp;", "&")
        val thumbnailUrl = extractOgImage(html)

        return Triple(title, cleanVideoUrl, thumbnailUrl)
    }

    private fun extractTitle(html: String): String? {
        return Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()
            ?: Regex("""<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
    }

    private fun extractVideoSource(html: String): String? {
        val sourceMatches = Regex("""<source[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
            .findAll(html)
        for (match in sourceMatches) {
            val src = match.groupValues.getOrNull(1) ?: continue
            if (isVideoUrl(src)) return src
        }

        val videoMatches = Regex("""<video[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
        if (videoMatches != null && isVideoUrl(videoMatches)) return videoMatches

        val videoSrcMatches = Regex("""<video[^>]*>[\s\S]*?<source[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
        if (videoSrcMatches != null && isVideoUrl(videoSrcMatches)) return videoSrcMatches

        return null
    }

    private fun extractOgVideo(html: String): String? {
        return Regex("""<meta\s+property=["']og:video(?::secure_url)?["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
            ?: Regex("""<meta\s+content=["']([^"']+)["']\s+property=["']og:video(?::secure_url)?["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
    }

    private fun extractOgImage(html: String): String? {
        return Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
    }

    private fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return DIRECT_VIDEO_EXTENSIONS.any { ext -> lower.endsWith(".$ext") || lower.contains(".$ext?") || lower.contains(".$ext&") }
    }

    private fun getMimeTypeForExtension(ext: String): String {
        return when (ext.lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "flv" -> "video/x-flv"
            "avi" -> "video/x-msvideo"
            "m4v" -> "video/mp4"
            "3gp" -> "video/3gpp"
            "ts" -> "video/mp2t"
            "ogg" -> "video/ogg"
            else -> "video/mp4"
        }
    }

    private fun sanitizeWebUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        return try {
            val u = URL(trimmed)
            u.toURI().toURL().toString()
        } catch (e: Exception) {
            Timber.w(e, "Failed to sanitize web URL: $trimmed")
            trimmed
        }
    }

    companion object {
        private val DIRECT_VIDEO_EXTENSIONS = listOf(
            "mp4", "webm", "mkv", "mov", "flv", "avi", "m4v",
            "3gp", "ts", "ogg", "ogv", "wmv", "mpg", "mpeg", "m4s"
        )
        private const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}