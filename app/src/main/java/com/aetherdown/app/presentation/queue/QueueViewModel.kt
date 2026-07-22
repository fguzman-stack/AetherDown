package com.aetherdown.app.presentation.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import com.aetherdown.app.domain.usecase.GetQueueUseCase
import com.aetherdown.app.domain.usecase.PauseResumeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val getQueueUseCase: GetQueueUseCase,
    private val pauseResumeUseCase: PauseResumeUseCase
) : ViewModel() {

    data class UiState(
        val downloads: List<DownloadEntity> = emptyList(),
        val activeCount: Int = 0,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getQueueUseCase.getAllDownloads().collect { downloads ->
                _uiState.value = _uiState.value.copy(
                    downloads = downloads,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            getQueueUseCase.getActiveDownloadCount().collect { count ->
                _uiState.value = _uiState.value.copy(activeCount = count)
            }
        }
    }

    fun pause(id: Long) {
        viewModelScope.launch { pauseResumeUseCase.pause(id) }
    }

    fun resume(id: Long) {
        viewModelScope.launch { pauseResumeUseCase.resume(id) }
    }

    fun cancel(id: Long) {
        viewModelScope.launch { pauseResumeUseCase.cancel(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { pauseResumeUseCase.delete(id) }
    }

    fun retry(entity: DownloadEntity) {
        viewModelScope.launch {
            pauseResumeUseCase.delete(entity.id)
            getQueueUseCase
        }
    }
}
