package com.aetherdown.app.util

import android.content.Intent
import android.net.Uri

fun String.isValidUrl(): Boolean {
    return try {
        val uri = Uri.parse(this)
        uri.scheme != null && (uri.scheme == "http" || uri.scheme == "https" || uri.scheme == "magnet")
    } catch (e: Exception) {
        false
    }
}

fun String.isMagnetLink(): Boolean = startsWith("magnet:?")

fun String.isTorrentFile(): Boolean = endsWith(".torrent", ignoreCase = true)

fun Long.toHumanReadableSize(): String {
    return when {
        this >= 1_073_741_824 -> String.format("%.2f GB", this.toDouble() / 1_073_741_824.0)
        this >= 1_048_576 -> String.format("%.2f MB", this.toDouble() / 1_048_576.0)
        this >= 1_024 -> String.format("%.2f KB", this.toDouble() / 1_024.0)
        else -> "$this B"
    }
}

fun Intent.shareText(text: String): Intent {
    putExtra(Intent.EXTRA_TEXT, text)
    type = "text/plain"
    return this
}
