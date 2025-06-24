package com.example.mindscribe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.example.mindscribe.ui.theme.MindScribeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen setup (must be called before super.onCreate())
        val splashScreen = installSplashScreen()
        var keepSplashOnScreen by mutableStateOf(true)
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
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
                    appReady = true
                }

                // Initial checks
                LaunchedEffect(Unit) {
                    // Check permission status
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    )

                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        hasPermission = true
                    }

                    // Add slight delay for smooth splash transition (optional)
                    delay(1000)

                    // Request permission if needed
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        appReady = true
                    }

                    // Update splash condition
                    keepSplashOnScreen = false
                }

                // Main content flow
                when {
                    !appReady -> { /* Keep showing splash */ }
                    hasPermission -> Navigation()
                    else -> { /* Optional: Show permission rationale UI */ }
                }
            }
        }
    }
}