plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.sceneview.sample.arcloudanchor"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.sceneview.sample.arcloudanchor"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        noCompress.add("filamat")
        noCompress.add("ktx")
    }
}

dependencies {
    implementation(project(":samples:common"))

    // ArSceneView
    "releaseImplementation"(libs.arsceneview)
    "debugImplementation"(project(":arsceneview"))
}
