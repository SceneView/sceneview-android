# SceneView Demo ProGuard Rules

# ── Filament JNI ──────────────────────────────────────────────────────────────
-keep class com.google.android.filament.** { *; }
-keepclassmembers class com.google.android.filament.** { *; }

# ── ARCore ────────────────────────────────────────────────────────────────────
-keep class com.google.ar.** { *; }
-keepclassmembers class com.google.ar.** { *; }

# ── Play Core (in-app updates) ────────────────────────────────────────────────
-keep class com.google.android.play.core.** { *; }
-keep interface com.google.android.play.core.** { *; }

# ── SceneView ─────────────────────────────────────────────────────────────────
-keep class io.github.sceneview.** { *; }
-keepclassmembers class io.github.sceneview.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Jetpack Compose ───────────────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ── AndroidX ──────────────────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }

# ── Suppress known harmless warnings ─────────────────────────────────────────
-dontwarn com.google.android.filament.**
-dontwarn com.google.ar.**
