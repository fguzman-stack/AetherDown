package com.aetherdown.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aetherdown.app.presentation.navigation.AetherNavGraph
import com.aetherdown.app.presentation.settings.SettingsViewModel
import com.aetherdown.app.ui.theme.AetherDownTheme
import com.aetherdown.app.ui.theme.resolveDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var sharedUrl = androidx.compose.runtime.mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            AetherDownTheme(
                darkTheme = settings.darkTheme.resolveDarkTheme(isSystemInDarkTheme()),
                dynamicColor = settings.useDynamicColors
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AetherNavGraph(sharedUrl = sharedUrl.value)
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
                    sharedUrl.value = url
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Regex("https?://[\\w./?=&%+:@#-]+")
        return urlPattern.find(text)?.value
    }
}
