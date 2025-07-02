package com.example.mindscribe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View // Import View
import android.view.ViewGroup // Import ViewGroup
import android.view.animation.AnimationUtils // Import AnimationUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.mindscribe.ui.components.LocalThemeManager
import com.example.mindscribe.ui.components.ThemeManager
import com.example.mindscribe.ui.theme.MindScribeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen setup (must be called before super.onCreate())
        val splashScreen = installSplashScreen()
        var keepSplashOnScreen by mutableStateOf(true) // State to control splash screen visibility

        // Set up the exit animation for the splash screen
        // This must be called BEFORE super.onCreate()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            // Get the splash screen's content view
            val splashScreenView = splashScreenViewProvider.view
            val parent = splashScreenView.parent as ViewGroup?

            // Load your desired exit animation (e.g., slide_out_left)
            val slideOutAnimation = AnimationUtils.loadAnimation(
                this,
                R.anim.slide_out_left // Using the new slide_out_left animation
            )

            // Set a listener for when the animation finishes
            slideOutAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // Once the animation ends, remove the splash screen view
                    // This is crucial for completing the splash screen dismissal
                    parent?.removeView(splashScreenView)
                    splashScreenViewProvider.remove() // Important: Call this to officially remove the splash screen
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })

            // Start the animation on the splash screen view
            splashScreenView.startAnimation(slideOutAnimation)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Keep this as it allows drawing under system bars

        // Initialize theme manager with system dark mode setting
        val isSystemInDarkTheme = isSystemInDarkTheme()
        ThemeManager.initialize(this, isSystemInDarkTheme)

        setContent {
            // Wrap with ThemeManagerProvider
            ThemeManagerProvider {
                MindScribeTheme {
                    // Combined permission and splash screen state
                    val context = LocalContext.current
                    var hasPermission by remember { mutableStateOf(false) }
                    var appReady by remember { mutableStateOf(false) }

                    // Permission launcher
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        hasPermission = isGranted
                        if (!isGranted) {
                            Toast.makeText(
                                context,
                                "Microphone access required for voice notes",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        appReady = true // Set appReady after permission response
                    }

                    // Initial checks and splash screen duration control
                    LaunchedEffect(Unit) {
                        // Check permission status initially
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )

                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            hasPermission = true
                            appReady = true // App is ready if permission is already granted
                        }

                        // Add slight delay for minimum splash screen display duration
                        delay(1000) // Changed to 1 second for noticeable effect

                        // Request permission if needed and not already granted
                        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }

                        // Signal that the splash screen can be dismissed
                        // The setOnExitAnimationListener will then trigger the animation
                        keepSplashOnScreen = false // This will trigger the exit animation
                    }

                    // Main content flow
                    when {
                        !appReady -> { /* Keep showing splash (handled by splashScreen.setKeepOnScreenCondition) */ }
                        hasPermission -> Navigation() // Your main app navigation
                        else -> { /* Optional: Show permission rationale UI or proceed with limited functionality */ }
                    }
                }
            }
        }
    }
}

// Helper function to check system dark mode
private fun Context.isSystemInDarkTheme(): Boolean {
    return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        else -> false
    }
}

@Composable
private fun ThemeManagerProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThemeManager provides ThemeManager) {
        content()
    }
}