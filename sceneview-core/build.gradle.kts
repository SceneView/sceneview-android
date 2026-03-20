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

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosX64Main by getting

        // Shared iOS source set
        val iosMain by creating {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            iosX64Main.dependsOn(this)
        }
    }
}
