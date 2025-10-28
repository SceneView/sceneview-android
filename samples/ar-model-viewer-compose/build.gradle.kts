plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.sceneview.sample.armodelviewer.compose"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.sceneview.sample.armodelviewer.compose"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        noCompress.add("filamat")
        noCompress.add("ktx")
    }
}

dependencies {
    implementation(projects.samples.common)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ArSceneView
    releaseImplementation(libs.arsceneview)
    debugImplementation(projects.arsceneview)
}
