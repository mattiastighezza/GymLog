package com.example.gymlog.ui.theme

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

// SCHEMA COLORI CHIARO
private val LightColorScheme = lightColorScheme(
    primary = BrandDarkBlue,
    onPrimary = Color.White,

    secondary = BrandForestGreen,
    onSecondary = Color.White,

    tertiary = BrandLimeGreen,

    primaryContainer = BrandLimeGreen.copy(alpha = 0.25f),
    onPrimaryContainer = BrandDarkBlue,

    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,

    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color.DarkGray
)

// SCHEMA COLORI SCURO
private val DarkColorScheme = darkColorScheme(
    primary = BrandLimeGreen,
    onPrimary = BrandDarkBlue,

    secondary = BrandForestGreen,
    onSecondary = Color.White,

    tertiary = BrandDarkBlue,

    primaryContainer = BrandDarkBlue.copy(alpha = 0.6f),
    onPrimaryContainer = BrandLimeGreen,

    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,

    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray
)

@Composable
fun GymLogTheme(
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
            val window = (view.context as Activity).window

            // 1. Colore di sfondo della barra uguale allo sfondo generale dell'app
            window.statusBarColor = colorScheme.background.toArgb()

            // 2. MAGIA DELLE ICONE: usa icone scure (!darkTheme) se siamo in modalità chiara!
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}