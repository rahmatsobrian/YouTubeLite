package com.rolex.ytlite

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rolex.ytlite.ui.YoutubeWebView
import com.rolex.ytlite.ui.theme.YoutubeLiteTheme

class MainActivity : ComponentActivity() {

    private var currentWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            YoutubeLiteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    YtLiteScreen()
                }
            }
        }
    }

    @Composable
    private fun YtLiteScreen() {
        var canGoBack by remember { mutableStateOf(false) }
        var canGoForward by remember { mutableStateOf(false) }
        var pageTitle by remember { mutableStateOf("YouTube Lite") }

        // System back button navigates web history first; only exits the app
        // once there's nowhere left to go back to inside the WebView.
        BackHandler(enabled = canGoBack) {
            currentWebView?.goBack()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(pageTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { currentWebView?.goBack() }, enabled = canGoBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    actions = {
                        IconButton(onClick = { currentWebView?.goForward() }, enabled = canGoForward) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Maju")
                        }
                        IconButton(onClick = { currentWebView?.loadUrl("https://m.youtube.com") }) {
                            Icon(Icons.Filled.Home, contentDescription = "Beranda")
                        }
                        IconButton(onClick = { currentWebView?.reload() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Muat ulang")
                        }
                    }
                )
            }
        ) { padding: PaddingValues ->
            Column(modifier = Modifier.fillMaxWidth().padding(padding)) {
                YoutubeWebView(
                    modifier = Modifier.fillMaxSize(),
                    onWebViewReady = { wv -> currentWebView = wv },
                    onNavigationStateChanged = { back, forward, title ->
                        canGoBack = back
                        canGoForward = forward
                        if (title.isNotBlank()) pageTitle = title
                    }
                )
            }
        }
    }
}
