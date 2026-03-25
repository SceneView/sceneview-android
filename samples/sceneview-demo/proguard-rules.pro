# SceneView Demo ProGuard Rules

# Keep Filament JNI
-keep class com.google.android.filament.** { *; }

# Keep ARCore
-keep class com.google.ar.** { *; }

# Keep Play Core (in-app updates)
-keep class com.google.android.play.core.** { *; }

# Keep Compose
-dontwarn androidx.compose.**

# Keep SceneView AR
-keep class io.github.sceneview.ar.** { *; }
