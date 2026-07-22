package com.aetherdown.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherdown.app.data.local.entity.HistoryEntity
import com.aetherdown.app.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    data class UiState(
        val history: List<HistoryEntity> = emptyList(),
        val searchQuery: String = "",
        val selectedPlatform: String? = null,
        val platforms: List<String> = emptyList(),
        val historyCount: Int = 0,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            historyRepository.getAllHistory().collect { history ->
                _uiState.value = _uiState.value.copy(
                    history = history,
                    isLoading = false,
                    historyCount = history.size
                )
            }
        }
        viewModelScope.launch {
            historyRepository.getDistinctPlatforms().collect { platforms ->
                _uiState.value = _uiState.value.copy(platforms = platforms)
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, selectedPlatform = null)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (query.isBlank()) {
                historyRepository.getAllHistory().collect { history ->
                    _uiState.value = _uiState.value.copy(history = history)
                }
            } else {
                historyRepository.searchHistory(query).collect { history ->
                    _uiState.value = _uiState.value.copy(history = history)
                }
            }
        }
    }

    fun filterByPlatform(platform: String) {
        _uiState.value = _uiState.value.copy(selectedPlatform = platform, searchQuery = "")
        viewModelScope.launch {
            historyRepository.getHistoryByPlatform(platform).collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(selectedPlatform = null, searchQuery = "")
        viewModelScope.launch {
            historyRepository.getAllHistory().collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { historyRepository.deleteHistoryById(id) }
    }

    fun clearAllHistory() {
        viewModelScope.launch { historyRepository.deleteAllHistory() }
    }
}
