package com.aetherdown.app.presentation.home

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherdown.app.domain.model.DownloadRequest
import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.domain.model.StreamInfo
import com.aetherdown.app.domain.repository.MediaDownloadGateway
import com.aetherdown.app.domain.repository.SettingsRepository
import com.aetherdown.app.domain.usecase.ExtractUrlUseCase
import com.aetherdown.app.domain.usecase.StartDownloadUseCase
import com.aetherdown.app.extractor.ExtractorManager
import com.aetherdown.app.extractor.PlatformMatch
import com.aetherdown.app.util.ClipboardHelper
import com.aetherdown.app.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val extractUrlUseCase: ExtractUrlUseCase,
    private val extractorManager: ExtractorManager,
    private val clipboardHelper: ClipboardHelper,
    private val downloadGateway: MediaDownloadGateway,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    data class UiState(
        val urlInput: String = "",
        val isExtracting: Boolean = false,
        val extractResult: ExtractResult? = null,
        val error: String? = null,
        val downloadStarted: Boolean = false,
        val lastFileName: String = "",
        val clipboardDetectedUrls: List<PlatformMatch> = emptyList(),
        val showClipboardBanner: Boolean = false
    )

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _extractedUrls = kotlinx.coroutines.flow.MutableStateFlow<List<PlatformMatch>>(emptyList())
    val extractedUrls: StateFlow<List<PlatformMatch>> = _extractedUrls.asStateFlow()

    val showSmartModeOnboarding: StateFlow<Boolean> = settingsRepository.smartModeOnboardingSeen
        .map { seen -> !seen }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun dismissSmartModeOnboarding() {
        viewModelScope.launch {
            settingsRepository.markSmartModeOnboardingSeen()
        }
    }

    fun handleSharedUrl(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url, error = null)
        extractAndDownload(url)
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            urlInput = url,
            error = null,
            downloadStarted = false,
            extractResult = null
        )
    }

    fun checkClipboard() {
        viewModelScope.launch {
            try {
                val text = clipboardHelper.getClipboardText() ?: return@launch
                val matches = extractorManager.findPlatformsInText(text)
                if (matches.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        clipboardDetectedUrls = matches,
                        showClipboardBanner = true
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Clipboard check failed")
            }
        }
    }

    fun dismissClipboardBanner() {
        _uiState.value = _uiState.value.copy(showClipboardBanner = false)
    }

    fun acceptClipboardUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            urlInput = url,
            showClipboardBanner = false
        )
    }

    fun extractAndDownload(url: String = _uiState.value.urlInput) {
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExtracting = true, error = null, downloadStarted = false)

            val result = extractUrlUseCase(url)
            result.onSuccess { extractResult ->
                _uiState.value = _uiState.value.copy(
                    extractResult = extractResult,
                    isExtracting = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isExtracting = false,
                    error = "Extraction failed: ${e.message}"
                )
            }
        }
    }

    fun startDownload(stream: StreamInfo) {
        val extractResult = _uiState.value.extractResult ?: return
        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()

        viewModelScope.launch {
            val settings = settingsRepository.getSettingsOnce()
            val fileName = FileUtils.getSafeFileName("${extractResult.title}.${stream.format}")
            _uiState.value = _uiState.value.copy(downloadStarted = true, lastFileName = fileName)

            val request = DownloadRequest(
                url = stream.url,
                formatId = stream.formatId,
                fileName = fileName,
                mimeType = stream.mimeType ?: "video/mp4",
                referer = extractResult.url,
                headers = stream.httpHeaders,
                pageUrl = extractResult.url,
                platform = extractResult.platform,
                title = extractResult.title,
                thumbnailUrl = extractResult.thumbnailUrl,
                duration = extractResult.duration,
                isIncognito = settings.incognitoMode
            )

            downloadGateway.download(request)
                .onSuccess { uri ->
                    Timber.d("Download completed: $uri")
                    Toast.makeText(context, "Download complete!", Toast.LENGTH_SHORT).show()
                }
                .onFailure { e ->
                    Timber.e(e, "Download failed")
                    _uiState.value = _uiState.value.copy(error = "Download failed: ${e.message}")
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    fun startDownloadDirectly(url: String = _uiState.value.urlInput) {
        if (url.isBlank()) {
            Toast.makeText(context, "URL is empty", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()

        val extractResult = _uiState.value.extractResult
        val pageUrl = extractResult?.url ?: url
        val platform = extractResult?.platform.orEmpty()
        val title = extractResult?.title
            ?.takeIf { it.isNotBlank() && it != "Unknown" }
            ?: url.substringAfterLast("/").substringBefore("?").ifEmpty { "download" }

        val fileName = FileUtils.getSafeFileName("$title.mp4")
        _uiState.value = _uiState.value.copy(downloadStarted = true, error = null, lastFileName = fileName)

        viewModelScope.launch {
            val settings = settingsRepository.getSettingsOnce()
            val request = DownloadRequest(
                url = url,
                fileName = fileName,
                mimeType = "video/mp4",
                referer = pageUrl,
                pageUrl = pageUrl,
                platform = platform,
                title = extractResult?.title ?: title,
                thumbnailUrl = extractResult?.thumbnailUrl,
                duration = extractResult?.duration ?: 0L,
                isIncognito = settings.incognitoMode
            )
            downloadGateway.download(request)
                .onSuccess { uri ->
                    Timber.d("Direct download completed: $uri")
                    Toast.makeText(context, "Download complete!", Toast.LENGTH_SHORT).show()
                }
                .onFailure { e ->
                    Timber.e(e, "Direct download failed")
                    _uiState.value = _uiState.value.copy(error = "Download failed: ${e.message}")
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetDownloadStarted() {
        _uiState.value = _uiState.value.copy(downloadStarted = false)
    }
}
