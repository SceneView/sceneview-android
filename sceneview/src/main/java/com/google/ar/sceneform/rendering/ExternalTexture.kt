package com.google.ar.sceneform.rendering

import android.graphics.SurfaceTexture
import android.view.Surface
import com.google.android.filament.Engine
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import io.github.sceneview.collision.Preconditions

/**
 * Creates an Android [SurfaceTexture] and [Surface] that can be displayed by Sceneform.
 * Useful for displaying video, or anything else that can be drawn to a [SurfaceTexture].
 *
 * The getFilamentEngine OpenGL ES texture is automatically created by Sceneform. Also,
 * [SurfaceTexture.updateTexImage] is automatically called and should not be called manually.
 *
 * Call [Material.setExternalTexture] to use an ExternalTexture.
 * The material parameter MUST be of type 'samplerExternal'.
 */
class ExternalTexture(private val engine: Engine) {

    private val surfaceTexture: SurfaceTexture?
    private val surface: Surface?

    private var filamentTexture: Texture? = null
    private var filamentStream: Stream? = null

    init {
        val st = SurfaceTexture(0)
        st.detachFromGLContext()
        surfaceTexture = st

        // Create the Android surface.
        surface = Surface(st)

        // Create the filament stream.
        //TODO : We actually have an issue with stream being destroyed from elsewhere maybe material
        // Fix it with kotlining here
        val stream = Stream.Builder().stream(st).build(engine)

        //TODO : We actually have an issue with stream being destroyed from elsewhere maybe material
        // Fix it when kotlining here
        initialize(stream)
    }

    private fun initialize(filamentStream: Stream) {
        if (filamentTexture != null) {
            throw AssertionError("Stream was initialized twice")
        }

        // Create the filament stream.
        this.filamentStream = filamentStream

        // Create the filament texture.
        filamentTexture = Texture.Builder()
            .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
            .format(Texture.InternalFormat.RGB8)
            .build(engine)

        filamentTexture!!.setExternalStream(engine, filamentStream)
    }

    /** Gets the surface texture created for this ExternalTexture. */
    fun getSurfaceTexture(): SurfaceTexture = Preconditions.checkNotNull(surfaceTexture)

    /** Gets the surface created for this ExternalTexture that draws to [getSurfaceTexture] */
    fun getSurface(): Surface = Preconditions.checkNotNull(surface)

    fun getFilamentTexture(): Texture = Preconditions.checkNotNull(filamentTexture)

    fun getFilamentStream(): Stream = Preconditions.checkNotNull(filamentStream)

    fun destroy() {
        filamentTexture?.let { engine.destroyTexture(it) }
        filamentTexture = null

        filamentStream?.let { engine.destroyStream(it) }
        filamentStream = null
    }

    companion object {
        private val TAG = ExternalTexture::class.java.simpleName
    }
}
