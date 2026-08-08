package com.rolex.ytlite.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes readable diagnostic lines to a plain-text file the user can find
 * with any file manager app, WITHOUT needing adb/PC - useful for debugging
 * on real devices where the failure only reproduces outside a debugger.
 *
 * File path: Android/data/com.rolex.ytlite/files/ytlite_debug.log
 * (standard external-files-dir, no extra storage permission required).
 */
object FileLogger {
    private const val FILE_NAME = "ytlite_debug.log"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun log(context: Context, tag: String, message: String) {
        Log.d(tag, message)
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, FILE_NAME)
            // Cap file size so it never grows unbounded on long sessions.
            if (file.exists() && file.length() > 512 * 1024) {
                file.delete()
            }
            file.appendText("${dateFormat.format(Date())}  [$tag]  $message\n")
        } catch (e: Exception) {
            Log.e("FileLogger", "Failed to write log file", e)
        }
    }

    fun logFilePath(context: Context): String {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, FILE_NAME).absolutePath
    }

    /** Installs a crash handler that saves the full stack trace to the log
     * file before letting the app crash normally - this is essential because
     * once a Service crashes, its own Log.d/onDestroy code often never runs.
     */
    fun installCrashLogger(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                log(appContext, "FATAL", "Uncaught exception on ${thread.name}: ${Log.getStackTraceString(throwable)}")
            } catch (_: Exception) {
                // Never let logging itself block the crash handoff.
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
