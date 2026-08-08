// Top-level build file
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // org.jetbrains.kotlin.plugin.compose only exists from Kotlin 2.0+ (it
    // replaces the old composeOptions{kotlinCompilerExtensionVersion} setup
    // used with Kotlin 1.9.x) - must match the Kotlin version above exactly.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
