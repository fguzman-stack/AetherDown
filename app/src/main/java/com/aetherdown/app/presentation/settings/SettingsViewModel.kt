package com.aetherdown.app.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherdown.app.domain.model.AppSettings
import com.aetherdown.app.domain.model.Language
import com.aetherdown.app.domain.model.ThemeMode
import com.aetherdown.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppSettings()
        )

    fun updateMaxConcurrentDownloads(max: Int) {
        viewModelScope.launch {
            settingsRepository.updateMaxConcurrentDownloads(max)
        }
    }

    fun updateDefaultMaxConnections(max: Int) {
        viewModelScope.launch {
            settingsRepository.updateDefaultMaxConnections(max)
        }
    }

    fun updateDefaultSpeedLimit(limit: Long) {
        viewModelScope.launch {
            settingsRepository.updateDefaultSpeedLimit(limit)
        }
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWifiOnly(wifiOnly)
        }
    }

    fun updateRoamingAllowed(allowed: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRoamingAllowed(allowed)
        }
    }

    fun updateMeteredNetworkAllowed(allowed: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMeteredNetworkAllowed(allowed)
        }
    }

    fun updateOnlyOnCharging(onlyCharging: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateOnlyOnCharging(onlyCharging)
        }
    }

    fun updateAutoExtractClipboard(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoExtractClipboard(enabled)
        }
    }

    fun updateIncognitoMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateIncognitoMode(enabled)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateDarkTheme(mode.name)
        }
    }

    fun updateUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseDynamicColors(enabled)
        }
    }

    fun updateOrganizeByPlatform(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateOrganizeByPlatform(enabled)
        }
    }

    fun updateDeleteOriginalAfterConversion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDeleteOriginalAfterConversion(enabled)
        }
    }

    fun updateNotificationProgress(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationProgress(enabled)
        }
    }

    fun updateCompletedNotification(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateCompletedNotification(enabled)
        }
    }

    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(language.code)

            val locales = when (language) {
                Language.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                else -> LocaleListCompat.forLanguageTags(language.code)
            }

            withContext(Dispatchers.Main.immediate) {
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }
}
