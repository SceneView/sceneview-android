plugins {
    kotlin("multiplatform")
}

kotlin {
    // Android target (JVM-based, consumed by the Android sceneview module)
    jvm("android")

    // iOS targets
    iosArm64()
    iosSimulatorArm64()
    iosX64()

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

