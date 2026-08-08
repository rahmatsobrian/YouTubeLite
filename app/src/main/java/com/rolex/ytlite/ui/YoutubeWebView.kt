package com.rolex.ytlite.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.rolex.ytlite.util.AdBlocker

private const val YOUTUBE_URL = "https://m.youtube.com"

/**
 * Full-screen WebView for browsing/watching YouTube. Cookies & local/DOM
 * storage are enabled and persisted so signing in stays remembered between
 * launches. Ad/tracker domains are filtered via [AdBlocker] (best-effort -
 * see its doc comment for limitations).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWebView(
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {},
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean, title: String) -> Unit = { _, _, _ -> }
) {
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)

            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.userAgentString = settings.userAgentString + " YtLiteApp/1.0"

                // Needed for Google Sign-In flows that hop across
                // accounts.google.com <-> youtube.com.
                cookieManager.setAcceptThirdPartyCookies(this, true)

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return if (AdBlocker.shouldBlock(request)) {
                            AdBlocker.blockedResponse()
                        } else {
                            super.shouldInterceptRequest(view, request)
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        cookieManager.flush() // persist session/login cookies to disk
                        onNavigationStateChanged(view.canGoBack(), view.canGoForward(), view.title ?: "")
                    }
                }
                webChromeClient = WebChromeClient()

                loadUrl(YOUTUBE_URL)
                webViewState.value = this
                onWebViewReady(this)
            }
        },
        update = { /* no-op */ }
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
