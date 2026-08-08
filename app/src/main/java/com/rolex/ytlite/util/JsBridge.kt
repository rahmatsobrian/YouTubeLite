package com.rolex.ytlite.util

import android.content.Context
import android.webkit.JavascriptInterface

/**
 * Bridge exposed to the page as `window.YtLiteBridge`.
 *
 * The injected JS (see [JS_WATCHER]) observes the <video> element on the
 * YouTube page and reports title/play-state changes here, so native code
 * (MediaSession, notification, Compose UI) always mirrors what's actually
 * playing inside the WebView - this is what keeps lock-screen controls and
 * the in-app UI in sync with playback.
 *
 * [loggerContext] is optional and only used to also mirror state changes to
 * [FileLogger] for on-device debugging (no context = logging skipped).
 */
class JsBridge(private val loggerContext: Context? = null) {

    @JavascriptInterface
    fun onStateChanged(title: String, isPlaying: Boolean) {
        PlaybackState.update(title = title, isPlaying = isPlaying)
        loggerContext?.let {
            FileLogger.log(it, "JsBridge", "JS reported: playing=$isPlaying title=$title")
        }
    }

    companion object {
        /** Injected via evaluateJavascript() after each page/video load. */
        const val JS_WATCHER = """
            (function() {
                if (window.__ytLiteWatcherInstalled) return;
                window.__ytLiteWatcherInstalled = true;

                function report() {
                    var v = document.querySelector('video');
                    var titleEl = document.querySelector('h1.title, .ytp-title-link, title');
                    var title = titleEl ? titleEl.textContent.trim() : document.title;
                    if (v) {
                        window.YtLiteBridge.onStateChanged(title, !v.paused && !v.ended);
                    }
                }

                document.addEventListener('play', report, true);
                document.addEventListener('pause', report, true);
                document.addEventListener('loadedmetadata', report, true);
                setInterval(report, 2000);
                report();
            })();
        """

        /** Simulates tapping play/pause on the underlying HTML5 <video>. */
        const val JS_TOGGLE_PLAY = """
            (function() {
                var v = document.querySelector('video');
                if (!v) return;
                if (v.paused) { v.play(); } else { v.pause(); }
            })();
        """

        const val JS_PLAY = "(function(){var v=document.querySelector('video'); if(v) v.play();})();"
        const val JS_PAUSE = "(function(){var v=document.querySelector('video'); if(v) v.pause();})();"
    }
}
