package com.rolex.ytlite.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media.session.MediaButtonReceiver
import com.rolex.ytlite.MainActivity
import com.rolex.ytlite.R
import com.rolex.ytlite.YtLiteApp
import com.rolex.ytlite.util.FileLogger
import com.rolex.ytlite.util.JsBridge
import com.rolex.ytlite.util.PlaybackState
import kotlinx.coroutines.launch

/**
 * Keeps a video playing (audio only, in practice) after the user leaves the
 * app / turns the screen off, using a *headless* WebView held alive by a
 * foreground service + MediaSession (so it survives Android's background
 * execution limits) plus a partial wake lock (so JS timers inside the page
 * aren't throttled).
 *
 * Note: this relies on the YouTube web player continuing to run inside a
 * background WebView, which is not an officially supported/guaranteed
 * behaviour of the YouTube website - Google can change page behaviour at
 * any time. Treat this as best-effort, for personal use.
 */
class MusicPlaybackService : LifecycleService() {

    companion object {
        private const val TAG = "MusicPlaybackService"
        private const val NOTIF_ID = 1001

        const val ACTION_START = "com.rolex.ytlite.action.START"
        const val ACTION_TOGGLE = "com.rolex.ytlite.action.TOGGLE"
        const val ACTION_STOP = "com.rolex.ytlite.action.STOP"
        const val EXTRA_URL = "extra_url"

        fun start(context: Context, url: String) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, url)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    private var webView: WebView? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var mediaSession: MediaSessionCompat
    private var windowManager: WindowManager? = null
    private var attachedToWindow = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        FileLogger.log(this, TAG, "onCreate - service process started")

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = runJs(JsBridge.JS_PLAY)
                override fun onPause() = runJs(JsBridge.JS_PAUSE)
                override fun onStop() = stopSelfAndCleanup()
            })
            isActive = true
        }

        lifecycleScope.launch {
            PlaybackState.info.collect { info ->
                FileLogger.log(this@MusicPlaybackService, TAG, "PlaybackState changed: playing=${info.isPlaying} title=${info.title}")
                updatePlaybackState(info.isPlaying)
                updateNotification(info.title, info.isPlaying)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        FileLogger.log(this, TAG, "onStartCommand action=${intent?.action} startId=$startId")

        MediaButtonReceiver.handleIntent(mediaSession, intent)

        when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL)
                startForeground(NOTIF_ID, buildNotification("", false))
                acquireWakeLock()
                ensureWebView(url)
            }
            ACTION_TOGGLE -> runJs(JsBridge.JS_TOGGLE_PLAY)
            ACTION_STOP -> stopSelfAndCleanup()
        }
        return START_STICKY
    }

    @Suppress("DEPRECATION")
    private fun ensureWebView(url: String?) {
        if (webView == null) {
            webView = WebView(applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                addJavascriptInterface(JsBridge(applicationContext), "YtLiteBridge")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String?) {
                        super.onPageFinished(view, finishedUrl)
                        view.evaluateJavascript(JsBridge.JS_WATCHER, null)
                        view.evaluateJavascript(JsBridge.JS_PLAY, null)
                    }
                }
                webChromeClient = WebChromeClient()
            }
            attachOverlayIfPossible()
        }
        if (!url.isNullOrBlank()) {
            webView?.loadUrl(url)
        }
    }

    /**
     * Root cause fix: a WebView that is never attached to any real window is
     * treated by Chromium as a hidden/background page (Page Visibility API),
     * and the YouTube web player auto-pauses video on such pages - which is
     * why audio previously died the moment the app left the foreground.
     *
     * Here we attach the (1x1, invisible, non-touchable) WebView to a real
     * system window via WindowManager, so the page is considered visible and
     * keeps playing even while MainActivity itself is backgrounded.
     *
     * Requires "Display over other apps" (SYSTEM_ALERT_WINDOW) permission.
     * If not granted, we fall back silently to the old (less reliable)
     * behaviour instead of crashing.
     */
    private fun attachOverlayIfPossible() {
        if (attachedToWindow) return
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted - falling back to detached WebView (audio may pause in background)")
            return
        }
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val params = WindowManager.LayoutParams(
                1, 1,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            windowManager?.addView(webView, params)
            attachedToWindow = true
            Log.d(TAG, "WebView attached to overlay window - page will be treated as visible")
            FileLogger.log(this, TAG, "Overlay attach SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay window, continuing without it", e)
            FileLogger.log(this, TAG, "Overlay attach FAILED: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun detachOverlay() {
        if (!attachedToWindow) return
        try {
            webView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing overlay window", e)
        }
        attachedToWindow = false
    }

    private fun runJs(js: String) {
        webView?.evaluateJavascript(js, null)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "YtLite:PlaybackWakeLock"
        ).apply { acquire(6 * 60 * 60 * 1000L /* 6h safety timeout */) }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    private fun updateNotification(title: String, isPlaying: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIF_ID, buildNotification(title, isPlaying))
    }

    private fun buildNotification(title: String, isPlaying: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseLabel = if (isPlaying) getString(R.string.action_pause) else getString(R.string.action_play)

        return NotificationCompat.Builder(this, YtLiteApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifBlank { getString(R.string.app_name) })
            .setContentText(getString(R.string.notif_channel_desc))
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .addAction(playPauseIcon, playPauseLabel, toggleIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), stopIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun stopSelfAndCleanup() {
        Log.d(TAG, "stopSelfAndCleanup")
        releaseWakeLock()
        detachOverlay()
        webView?.destroy()
        webView = null
        mediaSession.isActive = false
        mediaSession.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        FileLogger.log(this, TAG, "onDestroy called - service is being torn down")
        releaseWakeLock()
        detachOverlay()
        webView?.destroy()
        mediaSession.release()
        super.onDestroy()
    }

    /**
     * Called by the system when the user removes the app's task (e.g. swipes
     * it away from Recents). Logged explicitly so we can distinguish "user
     * swiped the app away" from "OS silently killed the process" when
     * reading the log file - by default we deliberately do NOT stop the
     * service here, since background playback should survive task removal.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        FileLogger.log(this, TAG, "onTaskRemoved - task removed from Recents (playback keeps running)")
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
