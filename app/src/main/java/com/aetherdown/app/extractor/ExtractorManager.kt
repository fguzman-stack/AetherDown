package com.aetherdown.app.extractor

import com.aetherdown.app.domain.model.ExtractResult
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractorManager @Inject constructor(
    private val ytDlpExtractor: YtDlpExtractorWrapper
) {
    private val extractors: List<Extractor> = listOf(ytDlpExtractor)

    suspend fun extract(url: String): ExtractResult {
        val cleanUrl = sanitizeUrl(url) ?: return createDirectResult(url)

        val extractor = extractors.firstOrNull { it.supportsUrl(cleanUrl) }
        if (extractor == null) {
            Timber.d("No extractor supports URL, downloading directly: $cleanUrl")
            return createDirectResult(cleanUrl)
        }

        return try {
            val result = extractor.extract(cleanUrl)
            result.getOrElse { error ->
                Timber.w(error, "Extraction failed for: $cleanUrl — falling back to direct download")
                createDirectResult(cleanUrl, extractor.platformName)
            }
        } catch (e: Exception) {
            Timber.w(e, "Extraction threw exception for: $cleanUrl — falling back to direct download")
            createDirectResult(cleanUrl)
        }
    }

    fun findPlatformsInText(text: String): List<PlatformMatch> {
        val matches = mutableListOf<PlatformMatch>()
        val urlPattern = Regex("https?://[^\\s\"'<>]+")
        urlPattern.findAll(text).forEach { match ->
            val url = match.value.trimEnd('.', ',', ')', ']', '>')
            val extractor = extractors.firstOrNull { it.supportsUrl(url) }
            if (extractor != null) {
                matches.add(PlatformMatch(url, extractor.platformName))
            }
        }
        return matches
    }

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
