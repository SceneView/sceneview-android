package io.github.sceneview.logging

import platform.Foundation.NSLog

actual fun logWarning(tag: String, message: String) {
    NSLog("W/$tag: $message")
}
