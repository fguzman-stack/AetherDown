package com.aetherdown.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aetherdown.app.data.repository.dataStore
import com.aetherdown.app.domain.model.Language
import com.aetherdown.app.domain.model.ThemeMode
import com.aetherdown.app.presentation.navigation.AetherNavGraph
import com.aetherdown.app.ui.theme.AetherDownTheme
import com.aetherdown.app.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var sharedUrl by mutableStateOf("")

    override fun attachBaseContext(newBase: Context) {
        val languageCode = try {
            runBlocking {
                newBase.dataStore.data.first()[stringPreferencesKey("language")] ?: "system"
            }
        } catch (e: Exception) {
            "system"
        }
        val language = Language.fromCode(languageCode)
        val context = LocaleHelper.applyLanguage(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        val themeMode = try {
            runBlocking {
                val modeStr = applicationContext.dataStore.data.first()[stringPreferencesKey("dark_theme")]
                try { modeStr?.let { ThemeMode.valueOf(it) } } catch (e: Exception) { null }
            } ?: ThemeMode.SYSTEM
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        val useDynamic = try {
            runBlocking {
                applicationContext.dataStore.data.first()[booleanPreferencesKey("use_dynamic_colors")] ?: false
            }
        } catch (e: Exception) {
            false
        }

        val isDark = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        setContent {
            AetherDownTheme(
                darkTheme = isDark,
                dynamicColor = useDynamic
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AetherNavGraph(sharedUrl = sharedUrl)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null) {
                val url = extractUrl(text)
                if (url != null) {
                    sharedUrl = url
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Regex("https?://[\\w./?=&%+:@#-]+")
        return urlPattern.find(text)?.value
    }
}

