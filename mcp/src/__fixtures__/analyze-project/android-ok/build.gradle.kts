// Minimal Android fixture — latest SceneView, no anti-patterns.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ok"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.ok"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("io.github.sceneview:sceneview:4.0.0-rc.1")
}
