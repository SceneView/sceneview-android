plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.sceneview.sample.arpointcloud"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.sceneview.sample.arpointcloud"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    androidResources {
        noCompress.add("filamat")
        noCompress.add("ktx")
    }
}

dependencies {
    implementation(projects.samples.common)

    // ArSceneView
    releaseImplementation(libs.arsceneview)
    debugImplementation(projects.arsceneview)
}
