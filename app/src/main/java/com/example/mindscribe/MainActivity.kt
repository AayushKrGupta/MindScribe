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

        val splashScreen = installSplashScreen()
        var keepSplashOnScreen by mutableStateOf(true)

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashScreenView = splashScreenViewProvider.view
            val parent = splashScreenView.parent as ViewGroup?

            val slideOutAnimation = AnimationUtils.loadAnimation(
                this,
                R.anim.slide_out_left // Using the new slide_out_left animation
            )


            slideOutAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    parent?.removeView(splashScreenView)
                    splashScreenViewProvider.remove()
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })


            splashScreenView.startAnimation(slideOutAnimation)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val isSystemInDarkTheme = isSystemInDarkTheme()
        ThemeManager.initialize(this, isSystemInDarkTheme)

        setContent {

            ThemeManagerProvider {
                MindScribeTheme {
                    val context = LocalContext.current
                    var hasPermission by remember { mutableStateOf(false) }
                    var appReady by remember { mutableStateOf(false) }

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

                    LaunchedEffect(Unit) {
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )

                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            hasPermission = true
                            appReady = true
                        }

                        delay(1000)

                        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }

                        keepSplashOnScreen = false
                    }


                    when {
                        !appReady -> { /* Keep showing splash (handled by splashScreen.setKeepOnScreenCondition) */ }
                        hasPermission -> Navigation()
                        else -> { /* Optional: Show permission rationale UI or proceed with limited functionality */ }
                    }
                }
            }
        }
    }
}


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