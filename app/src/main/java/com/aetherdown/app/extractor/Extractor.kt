package com.aetherdown.app.extractor

import com.aetherdown.app.domain.model.ExtractResult

interface Extractor {
    val platformName: String
    val patterns: List<Regex>
    suspend fun extract(url: String): Result<ExtractResult>
    fun supportsUrl(url: String): Boolean = patterns.any { it.containsMatchIn(url) }
}
