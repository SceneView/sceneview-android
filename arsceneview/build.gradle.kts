import org.gradle.kotlin.dsl.the

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.publish)
    alias(libs.plugins.dokka)
    id("filament-tools-plugin")
}

// *************************************************************************************************
// Filament Plugin
// *************************************************************************************************
//
// Needed if you want to generate Materials, Indirect Light (IBL) and Skyboxes.
//
// 1) Copy/paste the /buildSrc dir into your project root folder
// 2) Copy/paste the plugins line above (plugins: { id 'filament-tools-plugin' }) and bellow into
// your app/module build.gradle
// 3) Download the Filament tools release archive for your development desktop:
// Filament release download: https://github.com/google/filament/releases
// (Choose the corresponding version to the filament_version bellow)
// 4) Copy/paste the Filament Plugin part from the gradle.properties file to your project
// 5) Sync Gradle and clean your project
//if (project.properties["filamentPluginEnabled"]?.toString()?.toBoolean() == true) {
//    configure<io.github.sceneview.FilamentToolsPluginExtension> {
//        // Material generation: .mat -> .filamat
//        materialInputDir.set(project.layout.projectDirectory.dir("src/main/materials"))
//        materialOutputDir.set(project.layout.projectDirectory.dir("src/main/assets/materials"))
//        // IBL and Skybox generation: .hdr -> _ibl.ktx and _skybox.ktx
//        iblInputDir.set(project.layout.projectDirectory.dir("src/main/environments"))
//        iblOutputDir.set(project.layout.projectDirectory.dir("src/main/assets/environments"))
//        iblFormat = "ktx"
//    }
//
//    tasks.named("clean") {
//        doFirst {
//            delete("src/main/assets/materials")
//            delete("src/main/assets/environments")
//        }
//    }
//}
// *************************************************************************************************

android {
    namespace = "io.github.sceneview.ar"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    // Preserve compression of filament files
    androidResources {
        noCompress.add("filamat")
        noCompress.add("ktx")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)

    // SceneView
    api(project(":sceneview"))

    // ARCore
    api(libs.arcore)
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.S01)
    signAllPublications()
}
