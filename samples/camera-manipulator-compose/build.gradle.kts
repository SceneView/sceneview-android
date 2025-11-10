plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.sceneview.sample.cameramanipulator.compose"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.sceneview.sample.cameramanipulator.compose"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        noCompress.add("filamat")
        noCompress.add("ktx")
    }
}

dependencies {
    implementation(projects.samples.common)

    // SceneView
    releaseImplementation(libs.sceneview)
    debugImplementation(projects.sceneview)
}
