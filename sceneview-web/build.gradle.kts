plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "sceneview-web.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.library()
    }

    sourceSets {
        jsMain.dependencies {
            // SceneView KMP core (collision, math, geometry, animation, physics)
            api(project(":sceneview-core"))

            // Filament.js WASM renderer (same engine as Android)
            implementation(npm("filament", "1.52.3"))
        }

        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
