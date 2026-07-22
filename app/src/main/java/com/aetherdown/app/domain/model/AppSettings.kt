package com.aetherdown.app.domain.model

data class AppSettings(
    val downloadDirectory: String = "AetherDown",
    val maxConcurrentDownloads: Int = 3,
    val defaultMaxConnections: Int = 4,
    val defaultSpeedLimit: Long = 0L,
    val wifiOnly: Boolean = false,
    val roamingAllowed: Boolean = false,
    val meteredNetworkAllowed: Boolean = false,
    val onlyOnCharging: Boolean = false,
    val onlyAfterHour: Int = 0,
    val onlyBeforeHour: Int = 24,
    val autoExtractClipboard: Boolean = true,
    val showFloatingClipboardButton: Boolean = false,
    val incognitoMode: Boolean = false,
    val darkTheme: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true,
    val organizeByPlatform: Boolean = true,
    val organizeByType: Boolean = false,
    val deleteOriginalAfterConversion: Boolean = false,
    val autoBackupQueue: Boolean = true,
    val backupIntervalDays: Int = 7,
    val notificationSpeed: Boolean = true,
    val notificationProgress: Boolean = true,
    val completedNotification: Boolean = true,
    val language: Language = Language.SYSTEM
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class Language(val code: String, val displayName: String) {
    SYSTEM("system", "System default"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    ITALIAN("it", "Italiano"),
    PORTUGUESE("pt", "Português");

    companion object {
        fun fromCode(code: String): Language {
            return entries.firstOrNull { it.code == code } ?: SYSTEM
        }
    }
}
