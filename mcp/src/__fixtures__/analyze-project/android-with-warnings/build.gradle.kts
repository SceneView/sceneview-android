// Android fixture — outdated version + multiple anti-patterns.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.warn"
    compileSdk = 34
}

dependencies {
    // Old 2.x artifact, will be flagged as outdated.
    implementation("io.github.sceneview:sceneview:2.2.1")
}
