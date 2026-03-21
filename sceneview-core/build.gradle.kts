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

// Expose the JVM "android" variant with Android build-type attributes so that
// Android Gradle Plugin consumers (e.g. :sceneview, :samples:sceneview-demo) can
// resolve the correct variant when they request the "debug" or "release" build type.
val buildTypeAttr = Attribute.of("com.android.build.api.attributes.BuildTypeAttr", String::class.java)

configurations.named("androidApiElements") {
    attributes {
        attribute(buildTypeAttr, "debug")
    }
}

configurations.named("androidRuntimeElements") {
    attributes {
        attribute(buildTypeAttr, "debug")
    }
}
