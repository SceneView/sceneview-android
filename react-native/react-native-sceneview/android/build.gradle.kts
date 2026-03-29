plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.sceneview.reactnative"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // SceneView — latest on Maven Central
    implementation("io.github.sceneview:sceneview:3.5.1")
    implementation("io.github.sceneview:arsceneview:3.5.1")

    // React Native
    implementation("com.facebook.react:react-android")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
}
