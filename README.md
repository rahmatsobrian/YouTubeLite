# YouTube Lite (Kotlin DSL)

Aplikasi Android ringan pembungkus YouTube (via WebView) dengan mode
pemutaran musik/audio yang tetap berjalan di latar belakang, tema
Material3 + Dynamic Color (Material You), mendukung Android 10 s/d versi
terbaru (16+).

## Arsitektur singkat

- **MainActivity** — UI Compose (Material3), menampilkan `WebView` penuh
  layar untuk menjelajah/menonton YouTube (`YoutubeWebView.kt`).
- **MusicPlaybackService** — `LifecycleService` foreground yang menghidupkan
  `WebView` *headless* kedua, memuat URL video yang sama, lalu dijaga tetap
  hidup lewat foreground service notification + `PARTIAL_WAKE_LOCK`, dan
  diekspos ke lock screen lewat `MediaSessionCompat`.
- **JsBridge** — jembatan JavaScript (`window.YtLiteBridge`) yang memantau
  elemen `<video>` di halaman YouTube dan melaporkan judul/status
  play-pause ke kode native, serta menerima perintah play/pause dari native.
- **Theme.kt** — Dynamic Color asli (`dynamicLightColorScheme` /
  `dynamicDarkColorScheme`) untuk Android 12+, dengan fallback palet statis
  yang aman untuk Android 10 & 11 (API 29-30, sebelum Dynamic Color ada di
  OS).

## Batasan & catatan penting (harap dibaca)

1. **Tidak ada "0% bug" yang bisa dijamin siapa pun** untuk proyek software
   apa pun — termasuk saya. Kode ini ditulis mengikuti best-practice
   (lifecycle-aware service, wake lock dengan timeout, cleanup di
   `onDestroy`, permission runtime API 33+, dsb), tapi tetap butuh pengujian
   nyata di perangkat/emulator karena sandbox ini tidak punya Android SDK
   untuk mengompilasi & menjalankan build secara langsung.
2. **Pemutaran di latar belakang mengandalkan halaman web YouTube tetap
   berjalan di WebView tersembunyi.** ini bukan API resmi YouTube untuk
   audio-only background, jadi perilakunya bisa berubah kapan saja jika
   Google mengubah struktur halaman/JS mereka. Cocok untuk pemakaian
   pribadi, bukan untuk distribusi massal (perhatikan Ketentuan Layanan
   YouTube).
3. Ikon aplikasi di sini masih placeholder vector sederhana — ganti
   `ic_launcher_foreground.xml` / `ic_launcher_background.xml` sesuai brand
   kamu.

## Troubleshooting: musik & notifikasi hilang saat app diminimize

Ada 2 penyebab paling umum:

1. **Izin "Tampil di atas app lain" (overlay) belum diberikan.** Root cause:
   `WebView` yang tidak menempel ke window manapun dianggap Chromium sebagai
   halaman *hidden*, dan YouTube auto-pause video di halaman hidden — App
   ini sekarang otomatis meminta izin overlay saat pertama kali menekan
   "Putar di Latar Belakang" (lihat `MainActivity.startBackgroundPlayback()`),
   lalu `MusicPlaybackService` men-attach WebView tersebut sebagai overlay
   1x1 tak terlihat via `WindowManager` supaya halaman tetap dianggap
   visible. **Wajib di-allow**, kalau ditolak, playback tetap bisa berhenti
   sendiri saat app diminimize.
2. **Battery optimization OEM (Xiaomi/Oppo/Vivo/Samsung dll).** OS-OS ini
   sering membunuh foreground service walau sudah benar secara API. Set:
   Settings → Apps → YouTube Lite → Battery → **Unrestricted**, dan aktifkan
   izin Autostart bila ada.

## Build lokal

```bash
# generate wrapper dulu (jar sengaja tidak di-commit)
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

APK debug akan ada di `app/build/outputs/apk/debug/`.

## CI/CD (GitHub Actions)

Lihat `.github/workflows/android-build.yml`:

- **Tanpa repo secret apapun** — build yang dijalankan adalah varian
  `assembleDebug`, yang otomatis ditandatangani dengan debug keystore
  bawaan Android SDK (bukan keystore rahasia).
- **Membangkitkan `gradle-wrapper.jar` sendiri** — workflow mengunduh
  Gradle sementara di runner, lalu menjalankan `gradle wrapper` untuk
  menghasilkan `gradlew`, `gradlew.bat`, dan `gradle/wrapper/gradle-wrapper.jar`
  sebelum build dijalankan.
- **Logging lengkap** — build dijalankan dengan `--stacktrace --info
  --warning-mode all`, plus step `lintDebug` terpisah (non-blocking) dan
  upload artifact `build-logs` + APK hasil build untuk memudahkan debug.
