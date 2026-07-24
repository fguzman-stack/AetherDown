package com.aetherdown.app.extractor

import com.aetherdown.app.domain.model.ExtractResult
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractorManager @Inject constructor(
    private val ytDlpExtractor: YtDlpExtractorWrapper,
    private val directVideoExtractor: DirectVideoExtractor,
    private val videoTagExtractor: VideoTagExtractor
) {
    private val extractors: List<Extractor> = listOf(
        directVideoExtractor,
        videoTagExtractor,
        ytDlpExtractor
    )

    suspend fun extract(url: String): ExtractResult {
        val cleanUrl = sanitizeUrl(url) ?: return createDirectResult(url)

        if (directVideoExtractor.isDirectVideoUrl(cleanUrl)) {
            Timber.d("Detected direct video URL: $cleanUrl")
            return directVideoExtractor.extract(cleanUrl)
                .getOrElse { error ->
                    Timber.w(error, "Direct video extraction failed, falling back")
                    createDirectResult(cleanUrl)
                }
        }

        val extractor = extractors.firstOrNull { it.supportsUrl(cleanUrl) && it != directVideoExtractor }
        if (extractor == null) {
            Timber.d("No extractor supports URL, trying video tag extraction: $cleanUrl")
            return videoTagExtractor.extract(cleanUrl)
                .getOrElse { error ->
                    Timber.w(error, "Video tag extraction failed, falling back to direct download")
                    createDirectResult(cleanUrl)
                }
        }

        return try {
            val result = extractor.extract(cleanUrl)
            result.getOrElse { error ->
                Timber.w(error, "Extraction failed for: $cleanUrl — trying video tag extractor")
                videoTagExtractor.extract(cleanUrl)
                    .getOrElse { tagError ->
                        Timber.w(tagError, "Video tag extraction also failed, falling back to direct download")
                        createDirectResult(cleanUrl, extractor.platformName)
                    }
            }
        } catch (e: Exception) {
            Timber.w(e, "Extraction threw exception for: $cleanUrl — trying video tag extractor")
            videoTagExtractor.extract(cleanUrl)
                .getOrElse { tagError ->
                    Timber.w(tagError, "Video tag extraction also failed, falling back to direct download")
                    createDirectResult(cleanUrl)
                }
        }
    }

    fun findPlatformsInText(text: String): List<PlatformMatch> {
        val matches = mutableListOf<PlatformMatch>()
        val urlPattern = Regex("https?://[^\\s\"'<>]+")
        urlPattern.findAll(text).forEach { match ->
            val url = match.value.trimEnd('.', ',', ')', ']', '>')
            val extractor = extractorsForClipboard.firstOrNull { it.supportsUrl(url) }
            if (extractor != null) {
                matches.add(PlatformMatch(url, extractor.platformName))
            }
        }
        return matches
    }

    private val extractorsForClipboard: List<Extractor>
        get() = listOf(directVideoExtractor, ytDlpExtractor)

    private fun createDirectResult(url: String, platform: String = ""): ExtractResult {
        val fileName = url.substringAfterLast("/").substringBefore("?").ifEmpty { "download" }
        return ExtractResult(
            title = fileName,
            url = url,
            platform = platform.ifEmpty { "Direct" },
            streams = emptyList()
        )
    }

    private fun sanitizeUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null

        var processed = trimmed
        if (!processed.startsWith("http://") && !processed.startsWith("https://") && !processed.startsWith("magnet:")) {
            processed = "https://$processed"
        }

        if (processed.startsWith("magnet:")) return processed

        return try {
            val u = URL(processed)
            u.toURI().toURL().toString()
        } catch (e: Exception) {
            Timber.w(e, "Failed to sanitize URL, using as-is: $processed")
            processed
        }
    }
}

data class PlatformMatch(
    val url: String,
    val platform: String
)
