package io.github.sceneview.utils

import android.content.Context
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

open class DefaultLifecycle(val context: Context, owner: LifecycleOwner) :
    LifecycleRegistry(owner) {

    val observers = mutableListOf<LifecycleObserver>()

    override fun addObserver(observer: LifecycleObserver) {
        super.addObserver(observer)

        observers.add(observer)
    }

    override fun removeObserver(observer: LifecycleObserver) {
        super.removeObserver(observer)

        observers.remove(observer)
    }

    inline fun <reified U : LifecycleObserver> dispatchEvent(event: U.() -> Unit) {
        observers.mapNotNull { it as? U }.forEach(event)
    }
}
