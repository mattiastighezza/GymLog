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
    primary = BrandDarkBlue, // Colore principale (Barre, Bottoni ok)
    onPrimary = Color.White, // Testo sopra il colore primario

    secondary = BrandForestGreen, // Colore secondario (Tasto Storico)
    onSecondary = Color.White,

    tertiary = BrandLimeGreen, // Colore terziario per dettagli

    // Il colore di sfondo della riga quando completi una serie! Usiamo il tuo Lime ma leggermente trasparente
    primaryContainer = BrandLimeGreen.copy(alpha = 0.25f),
    onPrimaryContainer = BrandDarkBlue,

    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,

    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color.DarkGray
)

// SCHEMA COLORI SCURO (Per chi usa il telefono in Dark Mode)
private val DarkColorScheme = darkColorScheme(
    primary = BrandLimeGreen, // Al buio, il lime "spara" e si legge meglio
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
    // IMPORTANTE: Messo su FALSE così Android non ti ruba i colori dal wallpaper!
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assicurati di non cancellare il file Type.kt che gestisce questo!
        content = content
    )
}