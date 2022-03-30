package io.github.sceneview.utils

inline val <reified T> T.TAG get() = T::class.java.simpleName
