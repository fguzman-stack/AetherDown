package com.aetherdown.app.domain.model

data class ExtractResult(
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val duration: Long = 0L,
    val platform: String,
    val streams: List<StreamInfo> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistItems: List<ExtractResult> = emptyList(),
    val error: String? = null
)

data class StreamInfo(
    val url: String,
    val quality: String,
    val format: String,
    val mimeType: String,
    val fileSize: Long = 0L,
    val isAudio: Boolean = false,
    val isVideo: Boolean = true,
    val bitrate: Int = 0,
    val height: Int = 0,
    val width: Int = 0,
    val httpHeaders: Map<String, String> = emptyMap()
)
