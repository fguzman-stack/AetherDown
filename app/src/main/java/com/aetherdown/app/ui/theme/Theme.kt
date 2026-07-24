package com.aetherdown.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aetherdown.app.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AetherPrimary,
    onPrimary = AetherOnPrimary,
    primaryContainer = Color(0xFF3D1A9E),
    onPrimaryContainer = Color(0xFFE8D5FF),
    secondary = AetherSecondary,
    onSecondary = AetherOnSecondary,
    secondaryContainer = Color(0xFF004D59),
    onSecondaryContainer = Color(0xFFB3F0FF),
    tertiary = AetherTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF8A2A00),
    onTertiaryContainer = Color(0xFFFFD5C2),
    background = AetherBackground,
    onBackground = AetherOnSurface,
    surface = AetherCardDark,
    onSurface = AetherOnSurface,
    surfaceVariant = Color(0xFF2A2A44),
    onSurfaceVariant = Color(0xFFC4B8D0),
    outline = AetherOutline,
    outlineVariant = Color(0xFF484868),
    error = DownloadRed,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = AetherPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D5FF),
    onPrimaryContainer = Color(0xFF250062),
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB3F0FF),
    onSecondaryContainer = Color(0xFF003640),
    tertiary = AetherTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD5C2),
    onTertiaryContainer = Color(0xFF2B1500),
    background = Color(0xFFF5F0FF),
    onBackground = AetherOnSurfaceLight,
    surface = Color.White,
    onSurface = AetherOnSurfaceLight,
    surfaceVariant = Color(0xFFE8E0F0),
    onSurfaceVariant = Color(0xFF494658),
    outline = Color(0xFF7B7585),
    outlineVariant = Color(0xFFCCC4D6),
    error = DownloadRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun AetherDownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context !is Activity && context is android.content.ContextWrapper) {
                context = context.baseContext
            }
            
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

fun ThemeMode.resolveDarkTheme(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDark
}
