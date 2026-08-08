# YouTube Lite (Kotlin DSL)

Aplikasi Android ringan pembungkus YouTube (via WebView), tema Material3 +
Dynamic Color (Material You), mendukung Android 10 s/d versi terbaru (16+).

**Versi ini sengaja disederhanakan** (atas permintaan): tidak ada lagi mode
pemutaran latar belakang/notifikasi/foreground service — murni WebView
browser untuk YouTube dengan navigasi Back/Forward/Reload/Home, login
persisten, dan adblock domain-based ringan.

## Fitur

- **WebView YouTube penuh** (m.youtube.com) — nonton, cari, login akun Google.
- **Login tersimpan** — cookies (termasuk third-party, untuk alur login
  Google) diaktifkan & di-flush ke disk tiap halaman selesai dimuat, jadi
  sesi login tidak hilang saat app ditutup lalu dibuka lagi.
- **Navigasi lengkap** — tombol Back/Forward/Home/Reload di top bar, dan
  tombol Back sistem Android akan mundur ke halaman sebelumnya di dalam
  WebView (bukan langsung keluar app) selama masih ada riwayat.
- **Adblock ringan (best-effort)** — `AdBlocker.kt` memblokir request ke
  domain iklan/tracker pihak ketiga yang dikenal (DoubleClick, Google
  Syndication, Google Analytics, dll) lewat `shouldInterceptRequest`.
  **Catatan jujur:** ini TIDAK bisa diandalkan memblokir iklan in-stream
  YouTube sendiri, karena makin sering disajikan dari domain/CDN yang sama
  dengan video-nya — adblock sempurna di dalam WebView polos itu di luar
  jangkauan tanpa engine filtering penuh seperti uBlock.
- **Dynamic Color** asli untuk Android 12+, fallback palet statis aman
  untuk Android 10 & 11.
- **Crash logger ringan** (`FileLogger.kt`) — kalau app crash, detailnya
  otomatis tersimpan di `Android/data/com.rolex.ytlite/files/ytlite_debug.log`
  (bisa dibuka file manager mana saja) untuk memudahkan debug.

## Build lokal

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

APK debug ada di `app/build/outputs/apk/debug/`.

## CI/CD (GitHub Actions)

Lihat `.github/workflows/android-build.yml`:

- Tanpa repo secret apapun — build varian `assembleDebug`, otomatis
  ditandatangani debug keystore bawaan Android SDK.
- Membangkitkan `gradle-wrapper.jar` sendiri di runner (tidak di-commit).
- Logging lengkap (`--stacktrace --info --warning-mode all`) + upload
  artifact log & APK.
