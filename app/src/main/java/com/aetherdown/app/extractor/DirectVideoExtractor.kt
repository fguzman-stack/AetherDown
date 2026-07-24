package com.aetherdown.app.extractor

import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.domain.model.StreamInfo
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectVideoExtractor @Inject constructor() : Extractor {

    override val platformName: String = "Direct Video"

    override val patterns: List<Regex> = VIDEO_EXTENSIONS.map { ext ->
        Regex("https?://[^\\s\"'<>]+\\.$ext$", RegexOption.IGNORE_CASE)
    } + listOf(
        Regex("https?://[^\\s\"'<>]+\\?(?:[^&]*&)*v=([^&\\s]+)", RegexOption.IGNORE_CASE),
        Regex("https?://[^\\s\"'<>]+\\.(?:mp4|webm|mkv|mov|flv|avi|m4v|3gp|ts|mmst|mms|rtsp)[^\\s\"'<>]*", RegexOption.IGNORE_CASE)
    )

    override suspend fun extract(url: String): Result<ExtractResult> {
        val cleanUrl = sanitizeDirectUrl(url) ?: return Result.failure(
            IllegalArgumentException("Invalid direct video URL")
        )

        try {
            val fileName = cleanUrl.substringAfterLast("/").substringBefore("?").ifEmpty { "video" }
            val extension = fileName.substringAfterLast('.').lowercase().ifEmpty { "mp4" }
            val mimeType = getMimeTypeForExtension(extension)

            val streams = listOf(
                StreamInfo(
                    formatId = "direct_${extension}",
                    url = cleanUrl,
                    quality = "Original",
                    format = extension,
                    mimeType = mimeType,
                    hasVideo = true,
                    hasAudio = true
                )
            )

            return Result.success(
                ExtractResult(
                    title = fileName.substringBeforeLast('.').ifEmpty { "direct_video" },
                    url = cleanUrl,
                    platform = "Direct Video",
                    streams = streams
                )
            )
        } catch (e: Exception) {
            Timber.w(e, "Direct video extraction failed for: $url")
            return Result.failure(e)
        }
    }

    fun isDirectVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return VIDEO_EXTENSIONS.any { ext -> lower.endsWith(".$ext") || lower.contains(".$ext?") || lower.contains(".$ext&") || lower.contains(".$ext#") }
    }

    private fun sanitizeDirectUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        return try {
            val u = URL(trimmed)
            u.toURI().toURL().toString()
        } catch (e: Exception) {
            Timber.w(e, "Failed to sanitize direct video URL: $trimmed")
            trimmed
        }
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

    companion object {
        private val VIDEO_EXTENSIONS = listOf(
            "mp4", "webm", "mkv", "mov", "flv", "avi", "m4v",
            "3gp", "ts", "ogg", "ogv", "wmv", "mpg", "mpeg", "m4s"
        )
    }
}