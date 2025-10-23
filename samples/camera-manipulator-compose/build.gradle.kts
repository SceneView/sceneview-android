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

    buildTypes {
        getByName("release") {
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
    implementation(project(":samples:common"))

    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material:material:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")

    // SceneView
    "releaseImplementation"("io.github.sceneview:sceneview:2.3.0")
    "debugImplementation"(project(":sceneview"))
}
