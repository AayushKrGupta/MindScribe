package com.example.mindscribe.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // Keep this import, as Color(0xFF...) still uses it
import androidx.compose.ui.platform.LocalContext

// Assuming Purple80, PurpleGrey80, Pink80, Purple40, PurpleGrey40, Pink40
// are defined in a separate file like Color.kt in the same package
// For example, in Color.kt:
// val Purple80 = Color(0xFFD0BCFF)
// val PurpleGrey80 = Color(0xFFCCC2DC)
// val Pink80 = Color(0xFFEFB8C8)
// val Purple40 = Color(0xFF6650a4)
// val PurpleGrey40 = Color(0xFF625b71)
// val Pink40 = Color(0xFF7D5260)


private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFFFF), // Changed from Color.White to hex code for pure white
    surface = Color(0xFFFFFFFF),    // Changed from Color.White to hex code for pure white
    onPrimary = Color(0xFFFFFFFF),  // Changed from Color.White to hex code for pure white
    onSecondary = Color(0xFFFFFFFF),// Changed from Color.White to hex code for pure white
    onTertiary = Color(0xFFFFFFFF), // Changed from Color.White to hex code for pure white
    onBackground = Color(0xFF1C1B1F), // Keep this as a dark color for text on white background
    onSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFFFF0000)

)


@Composable
fun MindScribeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, // Keep this as true if you want dynamic colors on Android 12+
    // Set to 'false' in your composable where you use MindScribeTheme
    // if you want to force your defined LightColorScheme regardless of Android 12+
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming Typography is defined in Typography.kt
        content = content
    )
}