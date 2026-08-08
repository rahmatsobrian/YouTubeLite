package com.rolex.ytlite.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkFallbackScheme = darkColorScheme(
    primary = YtRed80,
    onPrimary = YtNeutral20,
    background = YtSurfaceDark,
    surface = YtSurfaceDark
)

private val LightFallbackScheme = lightColorScheme(
    primary = YtRed40,
    onPrimary = YtNeutral80,
    background = YtSurfaceLight,
    surface = YtSurfaceLight
)

/**
 * App-wide Material3 theme.
 *
 * - Android 12+ (API 31+): uses real Dynamic Color (Material You), taken from
 *   the user's wallpaper, both light & dark.
 * - Android 10 & 11 (API 29-30): dynamic color isn't supported by the OS, so
 *   we gracefully fall back to a static brand palette. This keeps the app
 *   crash-free and visually consistent on every supported version (10-16+).
 */
@Composable
fun YoutubeLiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkFallbackScheme
        else -> LightFallbackScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YtTypography,
        content = content
    )
}
