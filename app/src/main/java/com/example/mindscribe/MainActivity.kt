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
import Repo.NoteRepository
import NoteViewModel.NoteViewModelFactory
import Database.NoteDatabase
import com.example.mindscribe.ui.theme.MindScribeTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // Import for splash screen
import kotlinx.coroutines.delay // Import for delay function

class MainActivity : ComponentActivity() {

    // Declare the factory as a property to be accessible by content lambda
    private lateinit var noteViewModelFactory: NoteViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        // This must be called BEFORE super.onCreate() and setContent()
        val splashScreen = installSplashScreen() // Initialize the splash screen API

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // State to control when the main app content is ready to be shown
        var contentReady by mutableStateOf(false)

        // Keep the splash screen visible until 'contentReady' becomes true
        splashScreen.setKeepOnScreenCondition { !contentReady }

        // Initialize database and repository here, once for the Activity's lifetime
        val noteDao = NoteDatabase.getDatabase(applicationContext).noteDao()
        val repository = NoteRepository(noteDao)
        noteViewModelFactory = NoteViewModelFactory(repository) // Assign to the property

        setContent {
            // Use LaunchedEffect to introduce a delay for the main content
            LaunchedEffect(Unit) {
                // Add your desired delay here in milliseconds
                delay(2000L) // For example, 2000ms = 2 seconds
                contentReady = true // Set contentReady to true after the delay
            }

            // Only render your main application UI when contentReady is true
            if (contentReady) {
                MindScribeTheme {
                    RequestAudioPermission {
                        // Pass the 'noteViewModelFactory' to your Navigation composable
                        Navigation(noteViewModelFactory = noteViewModelFactory)
                    }
                }
            }
            // If contentReady is false, the splash screen remains visible, and nothing else is rendered here.
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