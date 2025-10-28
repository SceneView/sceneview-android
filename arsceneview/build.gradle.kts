plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.publish)
    alias(libs.plugins.dokka)
    id("filament-tools-plugin")
}

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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    api(projects.sceneview)

    // ARCore
    api(libs.arcore)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

dokka {
    moduleName.set("SceneView Android")
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("example-docs") {
            url("https://example.com/docs/")
            packageListUrl("https://example.com/docs/package-list")
        }
    }
    dokkaSourceSets.main {
        includes.from("README.md")
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://example.com/src")
            remoteLineSuffix.set("#L")
        }
        reportUndocumented.set(true)
        skipEmptyPackages.set(true)
        skipDeprecated.set(true)
        jdkVersion.set(21)

        // Do not create a link to the online Android SDK documentation
        enableAndroidDocumentationLink.set(false)

        // AndroidX + Compose docs
        externalDocumentationLinks {
            register("developer.android.com/reference") {
                url("https://developer.android.com/reference/")
                packageListUrl("https://developer.android.com/reference/androidx/package-list")
            }
            register("developer.android.com/reference/kotlin") {
                url("https://developer.android.com/reference/kotlin/")
                packageListUrl("https://developer.android.com/reference/kotlin/androidx/package-list")
            }
        }

        sourceLink {
            localDirectory.set(project.file("src/main/kotlin"))
            // URL showing where the source code can be accessed through the web browser
            remoteUrl("https://github.com/sceneview/sceneview-android/blob/main/${project.name}/src/main/kotlin")
            // Suffix which is used to append the line number to the URL. Use #L for GitHub
            remoteLineSuffix.set("#L")
        }
    }
}

filamentTools {
    // Material generation: .mat -> .filamat
    materialInputDir.set(project.layout.projectDirectory.dir("src/main/materials"))
    materialOutputDir.set(project.layout.projectDirectory.dir("src/main/assets/materials"))
    iblInputDir.set(project.layout.projectDirectory.dir("src/main/environments"))
    iblOutputDir.set(project.layout.projectDirectory.dir("src/main/assets/environments"))
    iblFormat = "ktx"
}

tasks.named("clean") {
    doFirst {
        delete("src/main/assets/materials")
        delete("src/main/assets/environments")
    }
}