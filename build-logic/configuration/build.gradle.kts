import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "io.github.sceneview.buildlogic.configuration"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.publish)
}

gradlePlugin {
    plugins {
        register("filament-tools-plugin") {
            id = "filament-tools-plugin"
            implementationClass = "io.github.sceneview.FilamentToolsPlugin"
        }
        register("publishing") {
            id = "publishing"
            implementationClass = "io.github.sceneview.PublishingPlugin"
        }
    }
}
