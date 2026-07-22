package com.aetherdown.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherdown.app.domain.model.AppSettings
import com.aetherdown.app.domain.model.Language
import com.aetherdown.app.domain.model.ThemeMode
import com.aetherdown.app.domain.usecase.GetSettingsUseCase
import com.aetherdown.app.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase.getSettings().collect { settings ->
                _uiState.value = UiState(settings = settings, isLoading = false)
            }
        }
    }

    fun updateMaxConcurrentDownloads(max: Int) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(maxConcurrentDownloads = max)
            updateSettingsUseCase(updated)
        }
    }

    fun updateDefaultMaxConnections(max: Int) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(defaultMaxConnections = max)
            updateSettingsUseCase(updated)
        }
    }

    fun updateDefaultSpeedLimit(limit: Long) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(defaultSpeedLimit = limit)
            updateSettingsUseCase(updated)
        }
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(wifiOnly = wifiOnly)
            updateSettingsUseCase(updated)
        }
    }

    fun updateRoamingAllowed(allowed: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(roamingAllowed = allowed)
            updateSettingsUseCase(updated)
        }
    }

    fun updateMeteredNetworkAllowed(allowed: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(meteredNetworkAllowed = allowed)
            updateSettingsUseCase(updated)
        }
    }

    fun updateOnlyOnCharging(onlyCharging: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(onlyOnCharging = onlyCharging)
            updateSettingsUseCase(updated)
        }
    }

    fun updateAutoExtractClipboard(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(autoExtractClipboard = enabled)
            updateSettingsUseCase(updated)
        }
    }

    fun updateIncognitoMode(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(incognitoMode = enabled)
            updateSettingsUseCase(updated)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(darkTheme = mode)
            updateSettingsUseCase(updated)
            kotlinx.coroutines.delay(100)
            _recreateApp.emit(Unit)
        }
    }

    fun updateUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(useDynamicColors = enabled)
            updateSettingsUseCase(updated)
            kotlinx.coroutines.delay(100)
            _recreateApp.emit(Unit)
        }
    }

    fun updateOrganizeByPlatform(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(organizeByPlatform = enabled)
            updateSettingsUseCase(updated)
        }
    }

    fun updateDeleteOriginalAfterConversion(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(deleteOriginalAfterConversion = enabled)
            updateSettingsUseCase(updated)
        }
    }

    fun updateNotificationProgress(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(notificationProgress = enabled)
            updateSettingsUseCase(updated)
        }
    }

    fun updateCompletedNotification(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(completedNotification = enabled)
            updateSettingsUseCase(updated)
        }
    }

    private val _recreateApp = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recreateApp: SharedFlow<Unit> = _recreateApp.asSharedFlow()

    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(language = language)
            updateSettingsUseCase(updated)
            kotlinx.coroutines.delay(200)
            _recreateApp.emit(Unit)
        }
    }
}
