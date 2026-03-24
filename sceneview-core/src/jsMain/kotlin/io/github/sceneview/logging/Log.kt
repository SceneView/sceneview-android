package io.github.sceneview.logging

actual fun logWarning(tag: String, message: String) {
    console.warn("[$tag] $message")
}
