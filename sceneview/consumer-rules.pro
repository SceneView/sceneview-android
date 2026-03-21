# Filament — JNI bridge classes must not be renamed or stripped
-keep class com.google.android.filament.** { *; }

# Kotlin-Math — used reflectively for transform operations
-keep class dev.romainguy.kotlin.math.** { *; }

# SceneView collision system — uses reflection for shape intersection
-keep class io.github.sceneview.collision.** { *; }
