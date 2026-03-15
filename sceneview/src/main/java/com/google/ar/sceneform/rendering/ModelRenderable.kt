package com.google.ar.sceneform.rendering

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.filament.Engine
import com.google.ar.sceneform.resources.ResourceRegistry
import com.google.ar.sceneform.utilities.AndroidPreconditions

/**
 * Renders a 3D Model by attaching it to a [com.google.ar.sceneform.Node] with
 * [com.google.ar.sceneform.Node.setRenderable].
 *
 * ```
 * future = ModelRenderable.builder().setSource(context, R.raw.renderable).build();
 * renderable = future.thenAccept(...);
 * ```
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class ModelRenderable : Renderable {

    private constructor(builder: Builder) : super(builder)

    @Suppress("initialization")
    private constructor(other: ModelRenderable) : super(other) {
        copyAnimationFrom(other)
    }

    private fun copyAnimationFrom(other: ModelRenderable) { return }

    /**
     * Creates a new instance of this ModelRenderable.
     *
     * The new renderable will have unique copy of all mutable state. All materials referenced by
     * the ModelRenderable will also be instanced. Immutable data will be shared between the
     * instances.
     */
    override fun makeCopy(): ModelRenderable = ModelRenderable(this)

    /** Factory class for [ModelRenderable]. */
    class Builder : Renderable.Builder<ModelRenderable, Builder>() {

        /** @hide */
        override fun makeRenderable(engine: Engine): ModelRenderable = ModelRenderable(this)

        /** @hide */
        override fun getRenderableClass(): Class<ModelRenderable> = ModelRenderable::class.java

        /** @hide */
        override fun getRenderableRegistry(): ResourceRegistry<ModelRenderable> =
            ResourceManager.getInstance().getModelRenderableRegistry()

        /** @hide */
        override fun getSelf(): Builder = this
    }

    companion object {
        /** Constructs a [ModelRenderable]. */
        @JvmStatic
        fun builder(): Builder {
            AndroidPreconditions.checkMinAndroidApiLevel()
            return Builder()
        }
    }
}
