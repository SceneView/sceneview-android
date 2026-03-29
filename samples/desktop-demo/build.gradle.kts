import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                // SceneView KMP core — declared as dependency but NOT used for rendering.
                // This demo is a Compose Canvas wireframe renderer, not a SceneView integration.
                api(project(":sceneview-core"))

                // Compose Desktop
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.foundation)

                // TODO: Filament JNI desktop libraries
                // Filament provides native C++ libraries for Windows/Linux/macOS,
                // but no published JVM/JNI bindings for desktop.
                // When available, add LWJGL + Filament JNI dependencies here.
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.sceneview.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sceneview-desktop"
            packageVersion = "1.0.0"

            macOS {
                bundleID = "io.github.sceneview.desktop"
            }
            windows {
                menuGroup = "SceneView"
            }
            linux {
                packageName = "sceneview-desktop"
            }
        }
    }
}
