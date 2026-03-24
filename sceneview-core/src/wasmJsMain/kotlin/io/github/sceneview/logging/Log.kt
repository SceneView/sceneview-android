package io.github.sceneview.logging

actual fun logWarning(tag: String, message: String) {
    println("WARNING [$tag] $message")
}
