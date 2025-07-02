package com.example.mindscribe.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.mindscribe.ui.components.ThemeManager
import com.example.mindscribe.ui.components.ColorPalette

private fun darkColorSchemeFromPalette(palette: ColorPalette) = darkColorScheme(
    primary = palette.darkPrimary,
    secondary = palette.darkSecondary,
    tertiary = palette.darkTertiary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    inversePrimary = palette.lightPrimary
)

private fun lightColorSchemeFromPalette(palette: ColorPalette) = lightColorScheme(
    primary = palette.lightPrimary,
    secondary = palette.lightSecondary,
    tertiary = palette.lightTertiary,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    inversePrimary = palette.darkPrimary
)

@Composable
fun MindScribeTheme(
    darkTheme: Boolean = ThemeManager.isDarkTheme,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorPalette = ThemeManager.colorPalettes.getOrNull(ThemeManager.selectedColorScheme)
        ?: ThemeManager.colorPalettes.first() // fallback to first palette if index is invalid

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorSchemeFromPalette(colorPalette)
        else -> lightColorSchemeFromPalette(colorPalette)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
