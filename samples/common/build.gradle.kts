plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.sceneview.sample.common"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        targetSdk = 36
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Kotlin
    api(libs.kotlin.coroutines.core)
    api(libs.kotlin.coroutines.android)

    // AndroidX
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.androidx.fragment.ktx)

    // Compose
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.activity.compose)
    api(libs.androidx.compose.material)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.navigation.compose)
    debugApi(libs.androidx.compose.ui.tooling)

    // Material 3
    api(libs.androidx.compose.material3)

    // Material
    api(libs.google.material)
}
