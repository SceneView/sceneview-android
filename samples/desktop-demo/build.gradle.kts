import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose")
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

                // LWJGL — declared for future Filament JNI integration but currently UNUSED.
                // The current demo renders wireframes via Compose Canvas, not OpenGL.
                val lwjglVersion = "3.3.6"
                implementation("org.lwjgl:lwjgl:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")

                // LWJGL native classifiers (auto-detect platform)
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos-arm64")
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos")
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux")
                runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-macos-arm64")
                runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-macos")
                runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")
                runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-linux")
                runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-macos-arm64")
                runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-macos")
                runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-windows")
                runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-linux")
                runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos-arm64")
                runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos")
                runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-windows")
                runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-linux")

                // TODO: Filament JNI desktop libraries
                // Filament provides native C++ libraries for Windows/Linux/macOS,
                // but no published JVM/JNI bindings for desktop.
                // Options:
                // 1. Build Filament from source with JNI for desktop targets
                // 2. Use the Android JNI bindings as a starting point
                // 3. Create a thin JNI layer that loads Filament .so/.dylib/.dll
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
