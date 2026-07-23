package com.aetherdown.app.domain.model

enum class DownloadMode {
    VIDEO,
    AUDIO,
    GIF
}

data class DownloadOption(
    val mode: DownloadMode,
    val label: String,
    val subtitle: String,
    val enabled: Boolean = true
)
