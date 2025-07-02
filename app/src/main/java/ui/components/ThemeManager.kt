package com.example.mindscribe.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import com.example.mindscribe.ui.theme.*

data class ColorPalette(
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightTertiary: Color,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkTertiary: Color
)

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val THEME_KEY = "dark_mode"
    private const val COLOR_SCHEME_KEY = "color_scheme"

    var isDarkTheme by mutableStateOf(false)
        private set

    var selectedColorScheme by mutableStateOf(0)
        private set

    val colorPalettes = listOf(
        // Purple (default)
        ColorPalette(
            lightPrimary = Purple40,
            lightSecondary = PurpleGrey40,
            lightTertiary = Pink40,
            darkPrimary = Purple80,
            darkSecondary = PurpleGrey80,
            darkTertiary = Pink80
        ),
        // Blue
        ColorPalette(
            lightPrimary = Blue40,
            lightSecondary = BlueGrey40,
            lightTertiary = LightBlue40,
            darkPrimary = Blue80,
            darkSecondary = BlueGrey80,
            darkTertiary = LightBlue80
        ),
        // Green
        ColorPalette(
            lightPrimary = Green40,
            lightSecondary = GreenGrey40,
            lightTertiary = LightGreen40,
            darkPrimary = Green80,
            darkSecondary = GreenGrey80,
            darkTertiary = LightGreen80
        )
    )

    fun initialize(context: Context, isSystemInDarkTheme: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean(THEME_KEY, isSystemInDarkTheme)
        selectedColorScheme = prefs.getInt(COLOR_SCHEME_KEY, 0)
    }

    fun toggleTheme(context: Context) {
        isDarkTheme = !isDarkTheme
        saveTheme(context)
    }

    fun selectColorScheme(index: Int, context: Context) {
        selectedColorScheme = index
        saveColorScheme(context)
    }

    private fun saveTheme(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(THEME_KEY, isDarkTheme)
        }
    }

    private fun saveColorScheme(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(COLOR_SCHEME_KEY, selectedColorScheme)
        }
    }
}

val LocalThemeManager = staticCompositionLocalOf { ThemeManager }