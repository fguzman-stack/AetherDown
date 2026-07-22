package com.aetherdown.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun getClipboardText(): String? {
        val clip = clipboardManager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()
    }

    fun hasUrl(): Boolean {
        val text = getClipboardText() ?: return false
        return URL_REGEX.containsMatchIn(text)
    }

    fun extractUrls(): List<String> {
        val text = getClipboardText() ?: return emptyList()
        return URL_REGEX.findAll(text).map { it.value }.toList()
    }

    fun copyToClipboard(text: String) {
        val clip = ClipData.newPlainText("AetherDown", text)
        clipboardManager.setPrimaryClip(clip)
    }

    companion object {
        private val URL_REGEX = Regex("https?://[^\\s]+")
    }
}
