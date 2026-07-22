package com.aetherdown.app.domain.repository

import com.aetherdown.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun getSettingsOnce(): AppSettings
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateDownloadDirectory(path: String)
    suspend fun updateMaxConcurrentDownloads(max: Int)
    suspend fun updateDefaultMaxConnections(max: Int)
    suspend fun updateDefaultSpeedLimit(limit: Long)
    suspend fun updateWifiOnly(wifiOnly: Boolean)
    suspend fun updateRoamingAllowed(allowed: Boolean)
    suspend fun updateMeteredNetworkAllowed(allowed: Boolean)
    suspend fun updateOnlyOnCharging(onlyCharging: Boolean)
    suspend fun updateAutoExtractClipboard(enabled: Boolean)
    suspend fun updateIncognitoMode(enabled: Boolean)
    suspend fun updateDarkTheme(mode: String)
    suspend fun updateUseDynamicColors(enabled: Boolean)
    suspend fun updateLanguage(language: String)
}
