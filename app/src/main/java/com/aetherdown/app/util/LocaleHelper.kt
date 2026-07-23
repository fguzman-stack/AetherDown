package com.aetherdown.app.util

object LocaleHelper {
    fun getSystemLanguageCode(): String {
        return java.util.Locale.getDefault().language.ifEmpty { "en" }
    }
}
