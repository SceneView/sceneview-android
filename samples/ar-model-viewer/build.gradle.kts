plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.sceneview.sample.armodelviewer"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.sceneview.sample.armodelviewer"
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
