package com.aetherdown.app.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.aetherdown.app.domain.model.Language
import java.util.Locale

object LocaleHelper {

    fun applyLanguage(context: Context, language: Language): Context {
        return when (language) {
            Language.SYSTEM -> context
            else -> setLocale(context, language.code)
        }
    }

    private fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        return updateLocale(context, locale)
    }

    private fun updateLocale(context: Context, locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getSystemLanguageCode(): String {
        val locale = Resources.getSystem().configuration.locales[0] ?: Locale.getDefault()
        return locale.language.ifEmpty { "en" }
    }
}
