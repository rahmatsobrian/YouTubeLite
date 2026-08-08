package com.rolex.ytlite

import android.app.Application
import com.rolex.ytlite.util.FileLogger

class YtLiteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Keeps a small on-device crash log (no PC/adb needed) in case
        // something goes wrong - readable via any file manager at:
        // Android/data/com.rolex.ytlite/files/ytlite_debug.log
        FileLogger.installCrashLogger(this)
    }
}
