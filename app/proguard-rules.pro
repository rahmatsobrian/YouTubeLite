# Keep WebView JavaScript interface methods (required, or JS bridge breaks silently)
-keepclassmembers class com.rolex.ytlite.util.JsBridge {
    public *;
}
-keepattributes JavascriptInterface

# Keep MediaSession callback classes
-keep class androidx.media.** { *; }
-dontwarn androidx.media.**
