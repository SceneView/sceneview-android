package io.github.sceneview.utils

import kotlin.js.Date

actual fun nanoTime(): Long = (Date.now() * 1_000_000).toLong()
