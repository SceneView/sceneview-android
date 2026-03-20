# SceneView Demo ProGuard Rules

# Keep Filament JNI
-keep class com.google.android.filament.** { *; }

# Keep ARCore
-keep class com.google.ar.** { *; }

# Keep Compose
-dontwarn androidx.compose.**
