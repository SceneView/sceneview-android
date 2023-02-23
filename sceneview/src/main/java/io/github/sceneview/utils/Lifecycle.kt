package io.github.sceneview.utils

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

open class DefaultLifecycle(owner: LifecycleOwner) :
    LifecycleRegistry(owner) {

    val observers = mutableListOf<LifecycleObserver>()

    override fun addObserver(observer: LifecycleObserver) {
        super.addObserver(observer)

        runCatching { observers.add(observer) }
    }

    override fun removeObserver(observer: LifecycleObserver) {
        super.removeObserver(observer)

        runCatching { observers.remove(observer) }
    }

    inline fun <reified U : LifecycleObserver> dispatchEvent(event: U.() -> Unit) {
        observers.mapNotNull { it as? U }.forEach(event)
    }
}
