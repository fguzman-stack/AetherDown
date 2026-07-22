package com.aetherdown.app.domain.usecase

import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.extractor.ExtractorManager
import timber.log.Timber
import javax.inject.Inject

class ExtractUrlUseCase @Inject constructor(
    private val extractorManager: ExtractorManager
) {
    suspend operator fun invoke(url: String): Result<ExtractResult> {
        return try {
            val result = extractorManager.extract(url)
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract URL: $url")
            Result.failure(e)
        }
    }
}
