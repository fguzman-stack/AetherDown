package com.aetherdown.app.presentation.home

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherdown.app.download.SimpleDownloader
import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.domain.usecase.ExtractUrlUseCase
import com.aetherdown.app.domain.usecase.StartDownloadUseCase
import com.aetherdown.app.extractor.ExtractorManager
import com.aetherdown.app.extractor.PlatformMatch
import com.aetherdown.app.util.ClipboardHelper
import com.aetherdown.app.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val simpleDownloader: SimpleDownloader
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

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _extractedUrls = MutableStateFlow<List<PlatformMatch>>(emptyList())
    val extractedUrls: StateFlow<List<PlatformMatch>> = _extractedUrls.asStateFlow()

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

    fun startDownload(stream: com.aetherdown.app.domain.model.StreamInfo) {
        val extractResult = _uiState.value.extractResult ?: return
        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()

        viewModelScope.launch {
            val fileName = FileUtils.getSafeFileName("${extractResult.title}.${stream.format}")
            val mime = FileUtils.getMimeTypeFromExtension(stream.format)
            _uiState.value = _uiState.value.copy(downloadStarted = true, lastFileName = fileName)

            simpleDownloader.download(stream.url, fileName, mime, extractResult.url, stream.httpHeaders)
                .onSuccess { uri ->
                    Timber.d("Download completed: $uri")
                    Toast.makeText(context, "Download complete!", Toast.LENGTH_SHORT).show()
                }
                .onFailure { e ->
                    Timber.e(e, "Simple download failed")
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
        Timber.d("Starting direct download for: $url")
        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()

        val fileName = FileUtils.getSafeFileName(
            url.substringAfterLast("/").substringBefore("?").ifEmpty { "download" } + ".mp4"
        )
        _uiState.value = _uiState.value.copy(downloadStarted = true, error = null, lastFileName = fileName)

        viewModelScope.launch {
            simpleDownloader.download(url, fileName)
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
