

package NavigationMenu

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled") // Suppress lint warning for enabling JavaScript
@Composable
fun AboutScreen(navController: NavController) {
    val githubUrl = "https://github.com/AayushKrGupta/MindScribe.git"
    var isLoading by remember { mutableStateOf(true) }
    var webView: WebView? = remember { null } // Keep a reference to the WebView

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("About MindScribe") },
            navigationIcon = {
                IconButton(onClick = {
                    // If the WebView can go back, navigate within the WebView; otherwise, pop the app stack.
                    if (webView?.canGoBack() == true) {
                        webView?.goBack()
                    } else {
                        navController.popBackStack()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this // Store the WebView reference
                        settings.javaScriptEnabled = true // Enable JavaScript for full website functionality
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            // This handles redirects within the WebView itself, instead of opening external browser
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false // Return false to load the URL in the current WebView
                            }
                        }
                        loadUrl(githubUrl)
                    }
                },
                update = { webView ->
                    // This block runs on recomposition. If you need to update the WebView state based on Compose state, do it here.
                    // For simply loading a fixed URL, this might not be strictly necessary.
                }
            )

            // Show a loading indicator while the page is loading
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}