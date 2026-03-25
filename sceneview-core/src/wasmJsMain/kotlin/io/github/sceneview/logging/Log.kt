package io.github.sceneview.logging

@JsFun("(msg) => console.warn(msg)")
private external fun consoleWarn(message: JsString)

actual fun logWarning(tag: String, message: String) {
    consoleWarn("[$tag] $message".toJsString())
}
