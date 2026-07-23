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
    val formatId: String = "",
    val url: String,
    val quality: String,
    val format: String,
    val mimeType: String? = null,
    val fileSize: Long? = null,
    val isAudio: Boolean = false,
    val hasVideo: Boolean = true,
    val hasAudio: Boolean = false,
    val bitrate: Int = 0,
    val height: Int = 0,
    val width: Int = 0,
    val httpHeaders: Map<String, String> = emptyMap()
) {
    val isVideo get() = hasVideo
}
