
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler ) apply false
    alias(libs.plugins.publish) apply false
    alias { libs.plugins.dokka } apply true
}