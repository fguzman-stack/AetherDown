package com.aetherdown.app.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object FileUtils {
    fun getDownloadDir(context: Context, subDir: String = "AetherDown"): File {
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        val dir = File(baseDir, subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSafeFileName(fileName: String): String {
        return fileName.replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .take(200)
            .ifEmpty { "download_${System.currentTimeMillis()}" }
    }

    fun getExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").ifEmpty { "unknown" }
    }

    fun getMimeTypeFromExtension(extension: String): String {
        return when (extension.lowercase()) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    fun saveToMediaStore(context: Context, file: File, mimeType: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection: Uri
            val relativePath: String

            when {
                mimeType.startsWith("video/") -> {
                    collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    relativePath = "${Environment.DIRECTORY_MOVIES}/AetherDown"
                }
                mimeType.startsWith("audio/") -> {
                    collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    relativePath = "${Environment.DIRECTORY_MUSIC}/AetherDown"
                }
                mimeType.startsWith("image/") -> {
                    collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    relativePath = "${Environment.DIRECTORY_PICTURES}/AetherDown"
                }
                else -> {
                    collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    relativePath = "${Environment.DIRECTORY_DOWNLOADS}/AetherDown"
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.SIZE, file.length())
                put(MediaStore.MediaColumns.IS_PENDING, 0)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

            try {
                val uri = context.contentResolver.insert(collection, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                            output.flush()
                        }
                    }
                    scanFile(context, file)
                }
                return uri
            } catch (e: Exception) {
                android.util.Log.e("FileUtils", "Failed to save to MediaStore", e)
                return null
            }
        }
        return null
    }

    private fun scanFile(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null,
                null
            )
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "MediaScanner scan failed", e)
        }
    }

    fun deleteFile(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    fun getFileSize(file: File): Long = file.length()

    fun exists(fileName: String, dir: File): Boolean {
        return File(dir, fileName).exists()
    }
}
