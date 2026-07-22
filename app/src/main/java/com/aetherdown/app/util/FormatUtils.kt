package com.aetherdown.app.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val sizeFormat = DecimalFormat("#.##")
    private val speedFormat = DecimalFormat("#.#")
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "${sizeFormat.format(bytes.toDouble() / 1_073_741_824.0)} GB"
            bytes >= 1_048_576 -> "${sizeFormat.format(bytes.toDouble() / 1_048_576.0)} MB"
            bytes >= 1_024 -> "${sizeFormat.format(bytes.toDouble() / 1_024.0)} KB"
            else -> "$bytes B"
        }
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1_000_000 -> "${speedFormat.format(bytesPerSecond / 1_000_000.0)} MB/s"
            bytesPerSecond >= 1_000 -> "${speedFormat.format(bytesPerSecond / 1_000.0)} KB/s"
            bytesPerSecond > 0 -> "$bytesPerSecond B/s"
            else -> "0 B/s"
        }
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0) return "—"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${secs}s"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSecs = millis / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return when {
            hours > 0 -> "%d:%02d:%02d".format(hours, minutes, secs)
            else -> "%d:%02d".format(minutes, secs)
        }
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatProgress(progress: Int): String = "$progress%"
}
