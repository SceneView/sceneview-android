package io.github.sceneview.node

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ExternalTexture
import io.github.sceneview.SceneView
import io.github.sceneview.material.MaterialLoader
import io.github.sceneview.material.destroy
import io.github.sceneview.material.setExternalTexture
import io.github.sceneview.material.setParameter
import io.github.sceneview.math.Position
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toVector3
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.texture.*
import io.github.sceneview.utils.FrameTime
import io.github.sceneview.utils.colorOf

/**
 * Node that can show a video by passing a [MediaPlayer] instance
 *
 * Create a new VideoNode for showing a video from a MediaPlayer instance inside a node on an
 * adjusted plane renderable with video transparency color set to chromaKeyColor.
 *
 * Note that VideoNode does not manage video playback by itself (e.g. starting the video).
 *
 * Filtering out a specific color in the video is also supported by
 * defining a chroma key color.
 *
 * Optionally an [ExternalTexture] can be passed if multiple VideoNode instances
 * need to render the exact same instance of a video. This will also improve performance
 * dramatically instead of rendering each instance separately.
 *
 * @param context Resources context
 * @param lifecycle Provide your lifecycle in order to load your model instantly and to destroy
 * it (and its resources) when the lifecycle goes to destroy state.
 * Otherwise the model loading is done when the parent [SceneView] is attached because it needs
 * a [kotlinx.coroutines.CoroutineScope] to load and resources will be destroyed when the
 * [SceneView] is.
 * You are responsible of manually destroy this [Node] only if you don't provide lifecycle and
 * the node is never attached to a [SceneView]
 * @param player The video media player to render on the plane node. You must handle the
 * playback state by yourself.
 * @param chromaKeyColor Chroma Key color to made the video transparent from
 * @param scaleToVideoRatio Automatically change the node [modelScale] to fit the video aspect
 * ratio. e.g. If the video ratio is (width=16,height=9) then the model will scale to be (1, 9/16)
 * If false, the video will stretch itself to fit the model
 * @param glbFileLocation the model glb file location:
 * ```
 * - A relative asset file location *models/mymodel.glb*
 * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
 * - A File path *Uri.fromFile(myModelFile).path*
 * - An http or https url *https://mydomain.com/mymodel.glb*
 * ```
 * @param centerOrigin Center point model origin of the model origin.
 * In unit (-1.0, 1.0) cube position (percent from model size)
 * ```
 * - `null` = Keep the original model center point
 * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
 * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
 * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
 * - ...
 * ```
 * @param onError Thrown exception during model loading
 * @param onLoaded When the resource renderable (RenderableInstance) is available (stop loading
 * display, change material, texture,...
 */
open class VideoNode(
    val player: MediaPlayer,
    val chromaKeyColor: Int? = null,
    scaleToVideoRatio: Boolean = true,
    val glbFileLocation: String = "sceneview/models/plane.glb",
    materialFileLocation: String = if (chromaKeyColor != null) {
        "sceneview/materials/video_stream_chroma_key.filamat"
    } else {
        "sceneview/materials/video_stream_plain.filamat"
    },
    autoAnimate: Boolean = true,
    scaleToUnits: Float? = null,
    val centerOrigin: Position? = null,
    val onError: ((error: Exception) -> Unit)? = null,
    val onLoaded: ((modelInstance: ModelInstance, materialInstance: MaterialInstance) -> Unit)? = null
) : ModelNode() {

    /**
     * ### Keep the video aspect ratio.
     * - `true` to fit within max width or height
     * - `false` the video will be stretched to the node scale
     */
    var isKeepAspectRatio = true

    /**
     * ### If this Flag is set to true, the VideoNode will always point to the Camera (You).
     */
    var isAlwaysLookAtCamera = false

    /**
     * ### Images drawn to the Surface will be made available to the Filament Stream
     */
    val surfaceTexture: SurfaceTexture = SurfaceTexture(0).apply {
        detachFromGLContext()
    }

    /**
     * ### The Android surface.
     */
    val surface: Surface = Surface(surfaceTexture)

    lateinit var material: Material
        private set

    lateinit var materialInstance: MaterialInstance
        private set

    /**
     * ### The Filament Stream
     */
    val stream: Stream = Stream.Builder()
        .stream(surfaceTexture)
        .build()

    /**
     * ### The Filament Texture diffusing the stream
     */
    val texture: Texture =
        Texture.Builder()
            .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
            .format(Texture.InternalFormat.RGB8)
            .build()
            .apply {
                setExternalStream(stream)
            }

    init {
        player.setSurface(surface)
        if (scaleToVideoRatio) {
            player.doOnVideoSized { player, width, height ->
                if (player == this.player) {
                    if (isKeepAspectRatio) {
                        modelScale = modelScale.apply {
                            x = if (width >= height) {
                                1.0f
                            } else {
                                width.toFloat() / height.toFloat()
                            }
                            y = if (width >= height) {
                                height.toFloat() / width.toFloat()
                            } else {
                                1.0f
                            }
                        }
                    }
                }
            }
        }
        doOnAttachedToScene { sceneView ->
            sceneView.lifecycle.coroutineScope.launchWhenCreated {
                try {
                    loadModelGlb(
                        context = sceneView.context,
                        glbFileLocation = glbFileLocation,
                        autoAnimate = autoAnimate,
                        scaleToUnits = scaleToUnits,
                        centerOrigin = centerOrigin
                    )
                    material = MaterialLoader.loadMaterial(
                        context = sceneView.context,
                        filamatFileLocation = materialFileLocation
                    )!!
                    materialInstance = material.defaultInstance.apply {
                        if (chromaKeyColor != null) {
                            setParameter("keyColor", colorOf(chromaKeyColor))
                        }
                        setExternalTexture("videoTexture", texture)
                    }
                    setMaterialInstance(materialInstance)
                    onLoaded?.invoke(modelInstance!!, materialInstance)
                } catch (error: Exception) {
                    onError(error)
                }
            }
        }
    }

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        //TODO: Move to kotlin-math
        if (isAlwaysLookAtCamera && sceneView != null) {
            val cameraPosition = sceneView!!.cameraNode.worldPosition.toVector3()
            val cardPosition = worldPosition.toVector3()
            val direction = Vector3.subtract(cameraPosition, cardPosition)
            val lookRotation = Quaternion.lookRotation(direction, Vector3.up())
            quaternion = lookRotation.toNewQuaternion()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        material.destroy()
        materialInstances.forEach { it.destroy() }
    }

    open fun onError(exception: Exception) {
        onError?.invoke(exception)
    }
}


fun MediaPlayer.doOnVideoSized(block: (player: MediaPlayer, videoWidth: Int, videoHeight: Int) -> Unit) {
    if (videoWidth > 0 && videoHeight > 0) {
        block(this, videoWidth, videoHeight)
    } else {
        setOnVideoSizeChangedListener { _, width: Int, height: Int ->
            if (width > 0 && height > 0) {
                block(this, width, height)
            }
        }
    }
}