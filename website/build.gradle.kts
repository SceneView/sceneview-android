import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kobweb.application)
}

group = "io.github.sceneview.website"
version = "1.0.0"

kobweb {
    app {
        index {
            description.set("SceneView — 3D & AR SDK for Android, iOS, macOS, visionOS")
        }
    }
}

kotlin {
    configAsKobwebApplication("sceneview-website")

    sourceSets {
        jsMain.dependencies {
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.silk.icons.fa)
            implementation(compose.html.core)
            implementation(compose.runtime)
        }
    }
}
