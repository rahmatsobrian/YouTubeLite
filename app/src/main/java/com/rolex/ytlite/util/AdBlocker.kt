package com.rolex.ytlite.util

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Best-effort, domain-based ad/tracker blocker for the in-app WebView.
 *
 * How it works: every network request the WebView makes passes through
 * [shouldBlock]; if the request's host matches a known ad/tracking domain,
 * we return an empty 200 response instead of letting it load, via
 * `WebViewClient.shouldInterceptRequest`.
 *
 * Limitations (please manage expectations):
 * - This blocks third-party ad *network* domains (DoubleClick, Google
 *   Syndication, ad trackers, etc.) - it does NOT reliably block YouTube's
 *   own in-stream video ads, which are increasingly served from the same
 *   domains/CDNs as the actual video content. A perfect in-video adblock
 *   inside a plain WebView isn't realistically achievable without a full
 *   content-filtering engine (like uBlock's), which is out of scope here.
 */
object AdBlocker {

    private val blockedHosts = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "static.doubleclick.net",
        "ad.doubleclick.net",
        "stats.g.doubleclick.net",
        "amazon-adsystem.com",
        "adsafeprotected.com",
        "moatads.com",
        "scorecardresearch.com",
        "imasdk.googleapis.com" // Google's IMA SDK - used for video-ad requests
    )

    private val emptyResponse: WebResourceResponse
        get() = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

    fun shouldBlock(request: WebResourceRequest): Boolean {
        val host = request.url.host ?: return false
        return blockedHosts.any { blocked -> host == blocked || host.endsWith(".$blocked") }
    }

    fun blockedResponse(): WebResourceResponse = emptyResponse
}
