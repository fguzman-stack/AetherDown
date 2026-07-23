package com.aetherdown.app.domain.model

data class DownloadRequest(
    val url: String,
    val formatId: String = "",
    val fileName: String,
    val mimeType: String = "video/mp4",
    val referer: String? = null,
    val headers: Map<String, String> = emptyMap(),
    /**
     * Original webpage URL (tweet/post page). When set, yt-dlp can re-resolve
     * and download correctly for platforms that block raw CDN fetches (X/Twitter, etc.).
     */
    val pageUrl: String? = null,
    val platform: String = ""
)
