plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        outputModuleName.set("sceneview")
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
        // Use executable() for a single webpack-bundled JS file usable via <script>
        // The @JsExport APIs are registered on globalThis.sceneview by the Kotlin/JS runtime
        binaries.executable()
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

// Task to copy the production webpack bundle to website-static
tasks.register<Copy>("copyToWebsite") {
    dependsOn("jsBrowserProductionWebpack")
    from(layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
    into("${rootProject.projectDir}/website-static/js")
    include("*.js", "*.js.map")
}
