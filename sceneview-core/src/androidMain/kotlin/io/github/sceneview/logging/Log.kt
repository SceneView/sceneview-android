package io.github.sceneview.logging

import java.util.logging.Level
import java.util.logging.Logger

actual fun logWarning(tag: String, message: String) {
    Logger.getLogger(tag).log(Level.WARNING, message)
}
