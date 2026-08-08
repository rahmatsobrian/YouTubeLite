package com.rolex.ytlite

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rolex.ytlite.service.MusicPlaybackService
import com.rolex.ytlite.ui.YoutubeWebView
import com.rolex.ytlite.ui.theme.YoutubeLiteTheme

class MainActivity : ComponentActivity() {

    private var currentWebView: WebView? = null

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: background playback still works, just without a visible ongoing badge on some OEMs */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        maybeRequestNotificationPermission()

        setContent {
            YoutubeLiteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    YtLiteApp()
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @Composable
    private fun YtLiteApp() {
        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Filled.Home, contentDescription = stringRes(R.string.btn_home)) },
                        label = { Text(stringRes(R.string.btn_home)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            startBackgroundPlayback()
                        },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = stringRes(R.string.btn_background_play)) },
                        label = { Text(stringRes(R.string.btn_background_play)) }
                    )
                }
            }
        ) { padding: PaddingValues ->
            Column(modifier = Modifier.fillMaxWidth().padding(padding)) {
                YoutubeWebView(
                    modifier = Modifier.fillMaxSize(),
                    onWebViewReady = { wv -> currentWebView = wv }
                )
            }
        }
    }

    private fun startBackgroundPlayback() {
        val url = currentWebView?.url ?: return
        MusicPlaybackService.start(this, url)
    }

    @Composable
    private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

    override fun onDestroy() {
        // Note: we deliberately do NOT stop MusicPlaybackService here - that's
        // the whole point of background playback surviving Activity destruction.
        super.onDestroy()
    }
}
