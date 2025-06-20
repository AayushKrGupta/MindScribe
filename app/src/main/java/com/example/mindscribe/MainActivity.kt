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
import com.example.mindscribe.ui.theme.MindScribeTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint  // <-- THIS IS THE CRUCIAL ADDITION
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var contentReady by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !contentReady }

        setContent {
            MindScribeTheme {
                LaunchedEffect(Unit) {
                    delay(2000L)
                    contentReady = true
                }

                if (contentReady) {
                    RequestAudioPermission {
                        Navigation()
                    }
                }
            }
        }
    }
}

@Composable
fun RequestAudioPermission(onPermissionGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            hasPermission = true
        }
    }

    if (hasPermission) {
        onPermissionGranted()
    }
}