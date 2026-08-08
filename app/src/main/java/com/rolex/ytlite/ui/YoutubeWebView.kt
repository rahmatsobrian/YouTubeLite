package com.rolex.ytlite.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.rolex.ytlite.util.JsBridge

private const val YOUTUBE_URL = "https://m.youtube.com"

/**
 * Foreground, visible WebView used for browsing & watching on-screen.
 * The instance is remembered/exposed via [onWebViewReady] so the caller can
 * hand the same page/video URL off to [com.rolex.ytlite.service.MusicPlaybackService]
 * for background-only audio playback.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWebView(
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {}
) {
    val bridge = remember { JsBridge() }
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.userAgentString = settings.userAgentString +
                    " YtLiteApp/1.0"

                addJavascriptInterface(bridge, "YtLiteBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(JsBridge.JS_WATCHER, null)
                    }
                }
                // Allows fullscreen <video> (e.g. rotate to landscape playback)
                webChromeClient = WebChromeClient()

                loadUrl(YOUTUBE_URL)
                webViewState.value = this
                onWebViewReady(this)
            }
        },
        update = { /* no-op: state changes handled through JsBridge */ }
    )

    DisposableEffect(Unit) {
        onDispose {
            webViewState.value?.let {
                it.loadUrl("about:blank")
                it.destroy()
            }
        }
    }
}
