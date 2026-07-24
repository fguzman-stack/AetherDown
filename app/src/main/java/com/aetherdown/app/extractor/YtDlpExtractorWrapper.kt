package com.aetherdown.app.extractor

import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.domain.model.ExtractionError
import com.aetherdown.app.domain.model.StreamInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpExtractorWrapper @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Extractor {

    override val platformName: String = "yt-dlp"

    override val patterns: List<Regex> = listOf(
        Regex("https?://[^\\s]+")
    )

    override suspend fun extract(url: String): Result<ExtractResult> = withContext(Dispatchers.IO) {
        val cleanUrl = sanitizePlatformUrl(url)
        try {
            val request = YoutubeDLRequest(cleanUrl)
            request.addOption("--no-playlist")
            request.addOption("--no-update")
            request.addOption("--no-warnings")
            request.addOption("--no-check-certificate")
            request.addOption("--geo-bypass")

            val referer = when {
                cleanUrl.contains("twitter.com") || cleanUrl.contains("x.com") -> "https://x.com/"
                cleanUrl.contains("instagram.com") || cleanUrl.contains("instagr.am") -> "https://www.instagram.com/"
                cleanUrl.contains("tiktok.com") -> "https://www.tiktok.com/"
                cleanUrl.contains("facebook.com") -> "https://www.facebook.com/"
                else -> cleanUrl
            }
            request.addOption("--referer", referer)
            request.addOption("--user-agent", BROWSER_UA)
            request.addOption("--add-header", "Accept-Language:en-US,en;q=0.9")

            if (cleanUrl.contains("instagram.com") || cleanUrl.contains("instagr.am")) {
                request.addOption("--extractor-args", "instagram:allow_vp9=True")
            }
            if (cleanUrl.contains("twitter.com") || cleanUrl.contains("x.com")) {
                request.addOption("--extractor-args", "twitter:api=syndication")
            }

            val info = YoutubeDL.getInstance().getInfo(request)
            val detectedPlatform = detectPlatform(info.webpageUrl ?: cleanUrl)
            val streams = buildStreams(info, detectedPlatform)

            if (streams.isEmpty()) {
                val fallbackResult = tryPlatformFallback(cleanUrl)
                if (fallbackResult != null) return@withContext Result.success(fallbackResult)
            }

            Result.success(
                ExtractResult(
                    title = info.title ?: info.fulltitle ?: "Unknown",
                    url = info.webpageUrl ?: cleanUrl,
                    thumbnailUrl = info.thumbnail,
                    duration = info.duration.toLong(),
                    platform = detectedPlatform,
                    streams = streams
                )
            )
        } catch (e: Exception) {
            Timber.w(e, "yt-dlp extraction failed for: $cleanUrl — trying fallbacks")
            val fallbackResult = tryPlatformFallback(cleanUrl)
            if (fallbackResult != null) {
                Result.success(fallbackResult)
            } else {
                Result.failure(
                    if (e is YoutubeDLException) mapYtDlpError(e) else ExtractionError.Unknown(e.message ?: "Extraction failed")
                )
            }
        }
    }

    private suspend fun tryPlatformFallback(url: String): ExtractResult? {
        val platform = detectPlatform(url)
        return when (platform) {
            "X/Twitter" -> extractTwitterFallback(url)
            "Instagram" -> extractInstagramFallback(url)
            else -> extractGenericOgVideoFallback(url, platform)
        }
    }

    private fun extractTwitterFallback(url: String): ExtractResult? {
        return try {
            val statusId = Regex("status/(\\d+)").find(url)?.groupValues?.get(1) ?: return null
            val apiUrl = "https://api.vxtwitter.com/i/status/$statusId"

            val req = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", BROWSER_UA)
                .build()

            val response = okHttpClient.newCall(req).execute()
            if (!response.isSuccessful) return null

            val jsonStr = response.body?.string() ?: return null
            val json = JSONObject(jsonStr)

            val text = json.optString("text", "Twitter Post")
            val user = json.optString("user_name", "Twitter")
            val thumbnail = json.optString("other_urls", null)
                ?: json.optJSONArray("media_urls")?.optString(0)

            val mediaArray = json.optJSONArray("media_extended")
            val streams = mutableListOf<StreamInfo>()

            if (mediaArray != null) {
                for (i in 0 until mediaArray.length()) {
                    val media = mediaArray.getJSONObject(i)
                    val type = media.optString("type")
                    if (type == "video" || type == "gif") {
                        val mediaUrl = media.optString("url")
                        if (mediaUrl.isNotBlank()) {
                            val sizeObj = media.optJSONObject("size")
                            val height = sizeObj?.optInt("height") ?: 0
                            val width = sizeObj?.optInt("width") ?: 0
                            val quality = if (height > 0) "${height}p" else "default"

                            streams.add(
                                StreamInfo(
                                    formatId = "vx_$i",
                                    url = mediaUrl,
                                    quality = quality,
                                    format = "mp4",
                                    mimeType = "video/mp4",
                                    hasVideo = true,
                                    hasAudio = type == "video",
                                    height = height,
                                    width = width,
                                    httpHeaders = mapOf("User-Agent" to BROWSER_UA, "Referer" to "https://x.com/")
                                )
                            )
                        }
                    }
                }
            }

            if (streams.isEmpty()) {
                val urlsArray = json.optJSONArray("media_urls")
                if (urlsArray != null) {
                    for (i in 0 until urlsArray.length()) {
                        val mediaUrl = urlsArray.optString(i)
                        if (mediaUrl.endsWith(".mp4") || mediaUrl.contains("video.twimg.com")) {
                            streams.add(
                                StreamInfo(
                                    formatId = "vx_direct_$i",
                                    url = mediaUrl,
                                    quality = "default",
                                    format = "mp4",
                                    mimeType = "video/mp4",
                                    hasVideo = true,
                                    hasAudio = true,
                                    httpHeaders = mapOf("User-Agent" to BROWSER_UA, "Referer" to "https://x.com/")
                                )
                            )
                        }
                    }
                }
            }

            if (streams.isNotEmpty()) {
                ExtractResult(
                    title = "$user: $text".take(100),
                    url = url,
                    thumbnailUrl = thumbnail,
                    duration = 0L,
                    platform = "X/Twitter",
                    streams = streams
                )
            } else null
        } catch (e: Exception) {
            Timber.w(e, "Twitter fallback failed for: $url")
            null
        }
    }

    private fun extractInstagramFallback(url: String): ExtractResult? {
        return try {
            val ddUrl = when {
                url.contains("instagram.com") -> url.replace("instagram.com", "ddinstagram.com")
                url.contains("instagr.am") -> url.replace("instagr.am", "ddinstagram.com")
                else -> url
            }

            val req = Request.Builder()
                .url(ddUrl)
                .header("User-Agent", "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = okHttpClient.newCall(req).execute()
            if (!response.isSuccessful) return null

            val html = response.body?.string() ?: return null

            val videoUrl = Regex("""<meta\s+property=["']og:video(?::secure_url)?["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)
                ?: Regex("""<meta\s+content=["']([^"']+)["']\s+property=["']og:video(?::secure_url)?["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)

            val title = Regex("""<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1) ?: "Instagram Reel"

            val thumbnail = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)

            if (!videoUrl.isNullOrBlank()) {
                val cleanVideoUrl = videoUrl.replace("&amp;", "&")
                ExtractResult(
                    title = title,
                    url = url,
                    thumbnailUrl = thumbnail?.replace("&amp;", "&"),
                    duration = 0L,
                    platform = "Instagram",
                    streams = listOf(
                        StreamInfo(
                            formatId = "insta_og",
                            url = cleanVideoUrl,
                            quality = "HD",
                            format = "mp4",
                            mimeType = "video/mp4",
                            hasVideo = true,
                            hasAudio = true,
                            httpHeaders = mapOf(
                                "User-Agent" to BROWSER_UA,
                                "Referer" to "https://www.instagram.com/"
                            )
                        )
                    )
                )
            } else null
        } catch (e: Exception) {
            Timber.w(e, "Instagram fallback failed for: $url")
            null
        }
    }

    private fun extractGenericOgVideoFallback(url: String, platform: String): ExtractResult? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_UA)
                .build()

            val response = okHttpClient.newCall(req).execute()
            if (!response.isSuccessful) return null

            val html = response.body?.string() ?: return null

            val videoUrl = Regex("""<meta\s+property=["']og:video(?::secure_url)?["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)
                ?: Regex("""<meta\s+content=["']([^"']+)["']\s+property=["']og:video(?::secure_url)?["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)

            val title = Regex("""<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1) ?: "Media Video"

            val thumbnail = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)

            if (!videoUrl.isNullOrBlank()) {
                val cleanVideoUrl = videoUrl.replace("&amp;", "&")
                ExtractResult(
                    title = title,
                    url = url,
                    thumbnailUrl = thumbnail?.replace("&amp;", "&"),
                    duration = 0L,
                    platform = platform,
                    streams = listOf(
                        StreamInfo(
                            formatId = "og_direct",
                            url = cleanVideoUrl,
                            quality = "default",
                            format = "mp4",
                            mimeType = "video/mp4",
                            hasVideo = true,
                            hasAudio = true,
                            httpHeaders = mapOf("User-Agent" to BROWSER_UA)
                        )
                    )
                )
            } else null
        } catch (e: Exception) {
            Timber.w(e, "Generic OG video fallback failed for: $url")
            null
        }
    }

    private fun sanitizePlatformUrl(url: String): String {
        var clean = url.trim()
        if (clean.contains("x.com")) {
            clean = clean.replace("x.com", "twitter.com")
        }
        return clean
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

            val isPlaylistOnly = isPlaylistUrl(streamUrl)
            if (isPlaylistOnly) continue

            seenUrls.add(streamUrl)

            val hasVideo = !fmt.vcodec.isNullOrBlank() && fmt.vcodec != "none"
            val hasAudio = !fmt.acodec.isNullOrBlank() && fmt.acodec != "none"
            val isAudioOnly = !hasVideo && hasAudio
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
