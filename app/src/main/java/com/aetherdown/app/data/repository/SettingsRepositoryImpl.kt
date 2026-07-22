package com.aetherdown.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aetherdown.app.domain.model.AppSettings
import com.aetherdown.app.domain.model.Language
import com.aetherdown.app.domain.model.ThemeMode
import com.aetherdown.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aetherdown_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val DOWNLOAD_DIRECTORY = stringPreferencesKey("download_directory")
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val DEFAULT_MAX_CONNECTIONS = intPreferencesKey("default_max_connections")
        val DEFAULT_SPEED_LIMIT = longPreferencesKey("default_speed_limit")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val ROAMING_ALLOWED = booleanPreferencesKey("roaming_allowed")
        val METERED_NETWORK_ALLOWED = booleanPreferencesKey("metered_network_allowed")
        val ONLY_ON_CHARGING = booleanPreferencesKey("only_on_charging")
        val ONLY_AFTER_HOUR = intPreferencesKey("only_after_hour")
        val ONLY_BEFORE_HOUR = intPreferencesKey("only_before_hour")
        val AUTO_EXTRACT_CLIPBOARD = booleanPreferencesKey("auto_extract_clipboard")
        val SHOW_FLOATING_CLIPBOARD_BUTTON = booleanPreferencesKey("show_floating_clipboard_btn")
        val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        val DARK_THEME = stringPreferencesKey("dark_theme")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val ORGANIZE_BY_PLATFORM = booleanPreferencesKey("organize_by_platform")
        val ORGANIZE_BY_TYPE = booleanPreferencesKey("organize_by_type")
        val DELETE_ORIGINAL_AFTER_CONVERSION = booleanPreferencesKey("delete_original_after_conversion")
        val AUTO_BACKUP_QUEUE = booleanPreferencesKey("auto_backup_queue")
        val BACKUP_INTERVAL_DAYS = intPreferencesKey("backup_interval_days")
        val NOTIFICATION_SPEED = booleanPreferencesKey("notification_speed")
        val NOTIFICATION_PROGRESS = booleanPreferencesKey("notification_progress")
        val COMPLETED_NOTIFICATION = booleanPreferencesKey("completed_notification")
        val LANGUAGE = stringPreferencesKey("language")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            downloadDirectory = prefs[Keys.DOWNLOAD_DIRECTORY] ?: "AetherDown",
            maxConcurrentDownloads = prefs[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 3,
            defaultMaxConnections = prefs[Keys.DEFAULT_MAX_CONNECTIONS] ?: 4,
            defaultSpeedLimit = prefs[Keys.DEFAULT_SPEED_LIMIT] ?: 0L,
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: false,
            roamingAllowed = prefs[Keys.ROAMING_ALLOWED] ?: false,
            meteredNetworkAllowed = prefs[Keys.METERED_NETWORK_ALLOWED] ?: false,
            onlyOnCharging = prefs[Keys.ONLY_ON_CHARGING] ?: false,
            onlyAfterHour = prefs[Keys.ONLY_AFTER_HOUR] ?: 0,
            onlyBeforeHour = prefs[Keys.ONLY_BEFORE_HOUR] ?: 24,
            autoExtractClipboard = prefs[Keys.AUTO_EXTRACT_CLIPBOARD] ?: true,
            showFloatingClipboardButton = prefs[Keys.SHOW_FLOATING_CLIPBOARD_BUTTON] ?: false,
            incognitoMode = prefs[Keys.INCOGNITO_MODE] ?: false,
            darkTheme = try { ThemeMode.valueOf(prefs[Keys.DARK_THEME] ?: "SYSTEM") } catch (e: Exception) { ThemeMode.SYSTEM },
            useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: true,
            organizeByPlatform = prefs[Keys.ORGANIZE_BY_PLATFORM] ?: true,
            organizeByType = prefs[Keys.ORGANIZE_BY_TYPE] ?: false,
            deleteOriginalAfterConversion = prefs[Keys.DELETE_ORIGINAL_AFTER_CONVERSION] ?: false,
            autoBackupQueue = prefs[Keys.AUTO_BACKUP_QUEUE] ?: true,
            backupIntervalDays = prefs[Keys.BACKUP_INTERVAL_DAYS] ?: 7,
            notificationSpeed = prefs[Keys.NOTIFICATION_SPEED] ?: true,
            notificationProgress = prefs[Keys.NOTIFICATION_PROGRESS] ?: true,
            completedNotification = prefs[Keys.COMPLETED_NOTIFICATION] ?: true,
            language = Language.fromCode(prefs[Keys.LANGUAGE] ?: "system")
        )
    }

    override suspend fun getSettingsOnce(): AppSettings {
        var result = AppSettings()
        context.dataStore.data.collect { prefs ->
            result = AppSettings(
                downloadDirectory = prefs[Keys.DOWNLOAD_DIRECTORY] ?: "AetherDown",
                maxConcurrentDownloads = prefs[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 3,
                defaultMaxConnections = prefs[Keys.DEFAULT_MAX_CONNECTIONS] ?: 4,
                defaultSpeedLimit = prefs[Keys.DEFAULT_SPEED_LIMIT] ?: 0L,
                wifiOnly = prefs[Keys.WIFI_ONLY] ?: false,
                roamingAllowed = prefs[Keys.ROAMING_ALLOWED] ?: false,
                meteredNetworkAllowed = prefs[Keys.METERED_NETWORK_ALLOWED] ?: false,
                onlyOnCharging = prefs[Keys.ONLY_ON_CHARGING] ?: false,
                onlyAfterHour = prefs[Keys.ONLY_AFTER_HOUR] ?: 0,
                onlyBeforeHour = prefs[Keys.ONLY_BEFORE_HOUR] ?: 24,
                autoExtractClipboard = prefs[Keys.AUTO_EXTRACT_CLIPBOARD] ?: true,
                showFloatingClipboardButton = prefs[Keys.SHOW_FLOATING_CLIPBOARD_BUTTON] ?: false,
                incognitoMode = prefs[Keys.INCOGNITO_MODE] ?: false,
                darkTheme = try { ThemeMode.valueOf(prefs[Keys.DARK_THEME] ?: "SYSTEM") } catch (e: Exception) { ThemeMode.SYSTEM },
                useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: true,
                organizeByPlatform = prefs[Keys.ORGANIZE_BY_PLATFORM] ?: true,
                organizeByType = prefs[Keys.ORGANIZE_BY_TYPE] ?: false,
                deleteOriginalAfterConversion = prefs[Keys.DELETE_ORIGINAL_AFTER_CONVERSION] ?: false,
                autoBackupQueue = prefs[Keys.AUTO_BACKUP_QUEUE] ?: true,
                backupIntervalDays = prefs[Keys.BACKUP_INTERVAL_DAYS] ?: 7,
                notificationSpeed = prefs[Keys.NOTIFICATION_SPEED] ?: true,
                notificationProgress = prefs[Keys.NOTIFICATION_PROGRESS] ?: true,
                completedNotification = prefs[Keys.COMPLETED_NOTIFICATION] ?: true,
                language = Language.fromCode(prefs[Keys.LANGUAGE] ?: "system")
            )
            return@collect
        }
        return result
    }

    override suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DOWNLOAD_DIRECTORY] = settings.downloadDirectory
            prefs[Keys.MAX_CONCURRENT_DOWNLOADS] = settings.maxConcurrentDownloads
            prefs[Keys.DEFAULT_MAX_CONNECTIONS] = settings.defaultMaxConnections
            prefs[Keys.DEFAULT_SPEED_LIMIT] = settings.defaultSpeedLimit
            prefs[Keys.WIFI_ONLY] = settings.wifiOnly
            prefs[Keys.ROAMING_ALLOWED] = settings.roamingAllowed
            prefs[Keys.METERED_NETWORK_ALLOWED] = settings.meteredNetworkAllowed
            prefs[Keys.ONLY_ON_CHARGING] = settings.onlyOnCharging
            prefs[Keys.ONLY_AFTER_HOUR] = settings.onlyAfterHour
            prefs[Keys.ONLY_BEFORE_HOUR] = settings.onlyBeforeHour
            prefs[Keys.AUTO_EXTRACT_CLIPBOARD] = settings.autoExtractClipboard
            prefs[Keys.SHOW_FLOATING_CLIPBOARD_BUTTON] = settings.showFloatingClipboardButton
            prefs[Keys.INCOGNITO_MODE] = settings.incognitoMode
            prefs[Keys.DARK_THEME] = settings.darkTheme.name
            prefs[Keys.USE_DYNAMIC_COLORS] = settings.useDynamicColors
            prefs[Keys.ORGANIZE_BY_PLATFORM] = settings.organizeByPlatform
            prefs[Keys.ORGANIZE_BY_TYPE] = settings.organizeByType
            prefs[Keys.DELETE_ORIGINAL_AFTER_CONVERSION] = settings.deleteOriginalAfterConversion
            prefs[Keys.AUTO_BACKUP_QUEUE] = settings.autoBackupQueue
            prefs[Keys.BACKUP_INTERVAL_DAYS] = settings.backupIntervalDays
            prefs[Keys.LANGUAGE] = settings.language.code
        }
        Timber.d("Settings updated")
    }

    override suspend fun updateDownloadDirectory(path: String) {
        context.dataStore.edit { it[Keys.DOWNLOAD_DIRECTORY] = path }
    }

    override suspend fun updateMaxConcurrentDownloads(max: Int) {
        context.dataStore.edit { it[Keys.MAX_CONCURRENT_DOWNLOADS] = max }
    }

    override suspend fun updateDefaultMaxConnections(max: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_MAX_CONNECTIONS] = max }
    }

    override suspend fun updateDefaultSpeedLimit(limit: Long) {
        context.dataStore.edit { it[Keys.DEFAULT_SPEED_LIMIT] = limit }
    }

    override suspend fun updateWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = wifiOnly }
    }

    override suspend fun updateRoamingAllowed(allowed: Boolean) {
        context.dataStore.edit { it[Keys.ROAMING_ALLOWED] = allowed }
    }

    override suspend fun updateMeteredNetworkAllowed(allowed: Boolean) {
        context.dataStore.edit { it[Keys.METERED_NETWORK_ALLOWED] = allowed }
    }

    override suspend fun updateOnlyOnCharging(onlyCharging: Boolean) {
        context.dataStore.edit { it[Keys.ONLY_ON_CHARGING] = onlyCharging }
    }

    override suspend fun updateAutoExtractClipboard(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_EXTRACT_CLIPBOARD] = enabled }
    }

    override suspend fun updateIncognitoMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INCOGNITO_MODE] = enabled }
    }

    override suspend fun updateDarkTheme(mode: String) {
        context.dataStore.edit { it[Keys.DARK_THEME] = mode }
    }

    override suspend fun updateUseDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLORS] = enabled }
    }

    override suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }
}
