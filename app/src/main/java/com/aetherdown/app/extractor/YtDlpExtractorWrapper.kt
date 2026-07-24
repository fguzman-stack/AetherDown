package com.aetherdown.app.extractor

import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.domain.model.ExtractionError
import com.aetherdown.app.domain.model.StreamInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLException
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
            val request = YoutubeDLRequest(url)
            request.addOption("--no-playlist")
            request.addOption("--no-update")
            
            // Basic headers for extraction
            val referer = when {
                url.contains("twitter.com") || url.contains("x.com") -> "https://x.com/"
                url.contains("instagram.com") || url.contains("instagr.am") -> "https://www.instagram.com/"
                url.contains("tiktok.com") -> "https://www.tiktok.com/"
                url.contains("facebook.com") -> "https://www.facebook.com/"
                else -> url
            }
            request.addOption("--add-header", "Referer:$referer")
            request.addOption("--add-header", "User-Agent:$BROWSER_UA")
            request.addOption("--add-header", "Accept-Language:en-US,en;q=0.9")
            request.addOption("--extractor-args", "instagram:allow_vp9=True")

            val info = YoutubeDL.getInstance().getInfo(request)
            val detectedPlatform = detectPlatform(info.webpageUrl ?: url)
            val streams = buildStreams(info, detectedPlatform)

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

    private fun buildStreams(info: VideoInfo, platform: String): List<StreamInfo> {
        val streams = mutableListOf<StreamInfo>()
        val seenUrls = mutableSetOf<String>()
        val infoHeaders = info.httpHeaders.orEmpty()
        val platformHeaders = defaultHeadersFor(platform, info.webpageUrl)

        val allFormats = info.formats ?: emptyList()

        for (fmt in allFormats) {
            val streamUrl = fmt.url ?: continue
            if (streamUrl.isBlank() || streamUrl in seenUrls) continue

            // Only skip actual playlist/manifest URLs. Do NOT drop progressive
            // formats that merely reference a parent manifest_url (common on X/Twitter).
            val isPlaylistOnly = isPlaylistUrl(streamUrl)
            if (isPlaylistOnly) continue

            seenUrls.add(streamUrl)

            val hasVideo = !fmt.vcodec.isNullOrBlank() && fmt.vcodec != "none"
            val hasAudio = !fmt.acodec.isNullOrBlank() && fmt.acodec != "none"
            val isAudioOnly = !hasVideo && hasAudio
            // Twitter/X often omits codec fields on progressive MP4s — treat as A/V.
            val assumeAv = !hasVideo && !hasAudio && !isPlaylistUrl(streamUrl)

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

            val formatHeaders = fmt.httpHeaders.orEmpty()
            val mergedHeaders = platformHeaders + infoHeaders + formatHeaders

            streams.add(
                StreamInfo(
                    formatId = fmt.formatId.orEmpty(),
                    url = streamUrl,
                    quality = quality,
                    format = ext,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    isAudio = isAudioOnly,
                    hasVideo = hasVideo || assumeAv,
                    hasAudio = hasAudio || assumeAv || (hasVideo && !isAudioOnly),
                    bitrate = if (fmt.abr > 0) fmt.abr else fmt.tbr,
                    height = fmt.height,
                    width = fmt.width,
                    httpHeaders = mergedHeaders
                )
            )
        }

        if (streams.isEmpty()) {
            info.url?.let { directUrl ->
                if (directUrl.isNotBlank() && !isPlaylistUrl(directUrl)) {
                    val ext = info.ext ?: "mp4"
                    streams.add(
                        StreamInfo(
                            formatId = info.formatId.orEmpty(),
                            url = directUrl,
                            quality = "default",
                            format = ext,
                            mimeType = "video/$ext",
                            hasVideo = true,
                            hasAudio = true,
                            httpHeaders = platformHeaders + infoHeaders
                        )
                    )
                }
            }
        }

        // Prefer higher resolution / progressive with audio first
        return streams.sortedWith(
            compareByDescending<StreamInfo> { it.hasVideo && it.hasAudio }
                .thenByDescending { it.height }
                .thenByDescending { it.fileSize ?: 0L }
        )
    }

    private fun isPlaylistUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") ||
            lower.contains(".mpd") ||
            lower.contains("manifest") && (lower.contains("m3u8") || lower.contains("mpd"))
    }

    private fun defaultHeadersFor(platform: String, webpageUrl: String?): Map<String, String> {
        val page = webpageUrl.orEmpty()
        return when {
            platform == "X/Twitter" || page.contains("twitter.com") || page.contains("x.com") -> mapOf(
                "Referer" to "https://x.com/",
                "Origin" to "https://x.com",
                "User-Agent" to BROWSER_UA,
                "Accept" to "*/*"
            )
            platform == "Instagram" || page.contains("instagram.com") -> mapOf(
                "Referer" to "https://www.instagram.com/",
                "Origin" to "https://www.instagram.com",
                "User-Agent" to BROWSER_UA,
                "Accept" to "*/*"
            )
            platform == "TikTok" || page.contains("tiktok.com") -> mapOf(
                "Referer" to "https://www.tiktok.com/",
                "Origin" to "https://www.tiktok.com",
                "User-Agent" to BROWSER_UA,
                "Accept" to "*/*"
            )
            platform == "YouTube" || page.contains("youtube.com") || page.contains("youtu.be") -> mapOf(
                "Referer" to "https://www.youtube.com/",
                "User-Agent" to BROWSER_UA,
                "Accept" to "*/*"
            )
            platform == "Facebook" || page.contains("facebook.com") || page.contains("fb.watch") -> mapOf(
                "Referer" to "https://www.facebook.com/",
                "Origin" to "https://www.facebook.com",
                "User-Agent" to BROWSER_UA,
                "Accept" to "*/*"
            )
            else -> mapOf(
                "User-Agent" to BROWSER_UA,
                "Accept" to "*/*"
            )
        }
    }

    private fun mapYtDlpError(e: YoutubeDLException): ExtractionError {
        val msg = e.message ?: ""
        return when {
            msg.contains("age", ignoreCase = true) || msg.contains("18", ignoreCase = true) ->
                ExtractionError.AgeRestricted
            msg.contains("geo", ignoreCase = true) || msg.contains("region", ignoreCase = true) ->
                ExtractionError.RegionLocked
            msg.contains("private", ignoreCase = true) || msg.contains("deleted", ignoreCase = true) ||
                msg.contains("unavailable", ignoreCase = true) || msg.contains("not found", ignoreCase = true) ->
                ExtractionError.Unknown("Content unavailable: $msg")
            msg.contains("unsupported", ignoreCase = true) || msg.contains("not supported", ignoreCase = true) ->
                ExtractionError.Unknown("This URL is not supported by yt-dlp")
            else -> ExtractionError.Unknown(msg)
        }
    }

    private fun detectPlatform(url: String): String {
        return when {
            url.contains("youtube.com") || url.contains("youtu.be") || url.contains("googlevideo.com") -> "YouTube"
            url.contains("tiktok.com") || url.contains("tiktokcdn.com") -> "TikTok"
            url.contains("instagram.com") || url.contains("cdninstagram.com") || url.contains("instagr.am") -> "Instagram"
            url.contains("facebook.com") || url.contains("fb.com") || url.contains("fb.watch") || url.contains("fbcdn.net") -> "Facebook"
            url.contains("twitter.com") || url.contains("x.com") || url.contains("twimg.com") -> "X/Twitter"
            url.contains("reddit.com") || url.contains("redd.it") -> "Reddit"
            url.contains("soundcloud.com") || url.contains("sndcdn.com") -> "SoundCloud"
            url.contains("vimeo.com") || url.contains("vimeocdn.com") -> "Vimeo"
            url.contains("dailymotion.com") || url.contains("dmcdn.net") -> "Dailymotion"
            url.contains("twitch.tv") -> "Twitch"
            else -> "Web"
        }
    }

    companion object {
        private const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
