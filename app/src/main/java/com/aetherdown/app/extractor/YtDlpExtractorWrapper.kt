package com.aetherdown.app.extractor

import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.domain.model.ExtractionError
import com.aetherdown.app.domain.model.StreamInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpExtractorWrapper @Inject constructor() : Extractor {

    override val platformName: String = "yt-dlp"

    override val patterns: List<Regex> = listOf(
        Regex("https?://[^\\s]+")
    )

    override suspend fun extract(url: String): Result<ExtractResult> = withContext(Dispatchers.IO) {
        try {
            val info = YoutubeDL.getInstance().getInfo(url)
            val streams = buildStreams(info)
            val detectedPlatform = detectPlatform(info.webpageUrl ?: url)

            Result.success(
                ExtractResult(
                    title = info.title ?: info.fulltitle ?: "Unknown",
                    url = info.webpageUrl ?: url,
                    thumbnailUrl = info.thumbnail,
                    duration = info.duration.toLong(),
                    platform = detectedPlatform,
                    streams = streams
                )
            )
        } catch (e: YoutubeDLException) {
            Timber.e(e, "yt-dlp extraction failed for: $url")
            Result.failure(mapYtDlpError(e))
        } catch (e: InterruptedException) {
            Timber.e(e, "yt-dlp extraction interrupted for: $url")
            Result.failure(ExtractionError.Unknown("Extraction interrupted"))
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during extraction for: $url")
            Result.failure(ExtractionError.Unknown(e.message ?: "Unknown error"))
        }
    }

    private fun buildStreams(info: VideoInfo): List<StreamInfo> {
        val streams = mutableListOf<StreamInfo>()
        val seenUrls = mutableSetOf<String>()

        val allFormats = info.formats ?: emptyList()

        for (fmt in allFormats) {
            val streamUrl = fmt.url ?: continue
            if (streamUrl.isBlank() || streamUrl in seenUrls) continue

            val isManifestUrl = streamUrl.contains(".m3u8", ignoreCase = true) ||
                fmt.manifestUrl != null
            if (isManifestUrl) continue

            seenUrls.add(streamUrl)

            val hasVideo = !fmt.vcodec.isNullOrBlank() && fmt.vcodec != "none"
            val hasAudio = !fmt.acodec.isNullOrBlank() && fmt.acodec != "none"
            val isAudioOnly = !hasVideo && hasAudio

            val quality = when {
                !fmt.formatNote.isNullOrBlank() -> fmt.formatNote ?: ""
                fmt.height > 0 -> "${fmt.height}p"
                isAudioOnly -> when {
                    fmt.abr > 0 -> "${fmt.abr}kbps"
                    fmt.tbr > 0 -> "${fmt.tbr}kbps"
                    else -> "audio"
                }
                else -> "default"
            }

            val ext = fmt.ext ?: "mp4"
            val mimeType = if (isAudioOnly) "audio/$ext" else "video/$ext"
            val fileSize = if (fmt.fileSize > 0) fmt.fileSize else fmt.fileSizeApproximate

            streams.add(
                StreamInfo(
                    url = streamUrl,
                    quality = quality,
                    format = ext,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    isAudio = isAudioOnly,
                    isVideo = hasVideo || (!isAudioOnly && hasAudio),
                    bitrate = if (fmt.abr > 0) fmt.abr else fmt.tbr,
                    height = fmt.height,
                    width = fmt.width,
                    httpHeaders = fmt.httpHeaders ?: emptyMap()
                )
            )
        }

        if (streams.isEmpty()) {
            info.url?.let { directUrl ->
                if (directUrl.isNotBlank()) {
                    val ext = info.ext ?: "mp4"
                    streams.add(
                        StreamInfo(
                            url = directUrl,
                            quality = "default",
                            format = ext,
                            mimeType = "video/$ext",
                            isVideo = true
                        )
                    )
                }
            }
        }

        return streams
    }

    private fun mapYtDlpError(e: YoutubeDLException): ExtractionError {
        val msg = e.message ?: ""
        return when {
            msg.contains("age", ignoreCase = true) || msg.contains("18", ignoreCase = true) ->
                ExtractionError.AgeRestricted
            msg.contains("geo", ignoreCase = true) || msg.contains("region", ignoreCase = true) ->
                ExtractionError.RegionLocked
            msg.contains("private", ignoreCase = true) || msg.contains("deleted", ignoreCase = true) || msg.contains("unavailable", ignoreCase = true) || msg.contains("not found", ignoreCase = true) ->
                ExtractionError.Unknown("Content unavailable: $msg")
            msg.contains("unsupported", ignoreCase = true) || msg.contains("not supported", ignoreCase = true) ->
                ExtractionError.Unknown("This URL is not supported by yt-dlp")
            else -> ExtractionError.Unknown(msg)
        }
    }

    private fun detectPlatform(url: String): String {
        return when {
            url.contains("youtube.com") || url.contains("youtu.be") -> "YouTube"
            url.contains("tiktok.com") -> "TikTok"
            url.contains("instagram.com") -> "Instagram"
            url.contains("facebook.com") || url.contains("fb.com") || url.contains("fb.watch") -> "Facebook"
            url.contains("twitter.com") || url.contains("x.com") -> "X/Twitter"
            url.contains("reddit.com") -> "Reddit"
            url.contains("soundcloud.com") -> "SoundCloud"
            url.contains("vimeo.com") -> "Vimeo"
            url.contains("dailymotion.com") -> "Dailymotion"
            url.contains("twitch.tv") -> "Twitch"
            else -> "Web"
        }
    }
}
