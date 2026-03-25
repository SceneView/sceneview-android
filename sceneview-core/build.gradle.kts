plugins {
    kotlin("multiplatform")
    id("com.vanniktech.maven.publish")
}

kotlin {
    // Android target (JVM-based, consumed by the Android sceneview module)
    jvm("android")

    // iOS targets
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    // Web target (JS/Browser, consumed by the sceneview-web module)
    js(IR) {
        browser()
        binaries.library()
    }

    // WASM target for Compose Multiplatform Web
    // BLOCKED: kotlin-math (dev.romainguy:kotlin-math) does not publish a wasmJs variant.
    // Uncomment when kotlin-math adds wasmJs support.
    // @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    // wasmJs {
    //     browser()
    //     binaries.library()
    // }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib")
            api("dev.romainguy:kotlin-math:1.6.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

