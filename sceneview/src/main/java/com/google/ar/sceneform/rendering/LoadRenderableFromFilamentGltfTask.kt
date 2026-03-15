package com.google.ar.sceneform.rendering

import android.content.Context
import android.net.Uri
import androidx.annotation.NonNull
import com.google.ar.sceneform.utilities.SceneformBufferUtils
import io.github.sceneview.collision.Preconditions
import java.io.InputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.function.Function

/** Task for initializing a renderable with glTF data loaded with gltfio. */
@Suppress("AndroidApiChecker")
class LoadRenderableFromFilamentGltfTask<T : Renderable>(
    private val renderable: T,
    context: Context,
    sourceUri: Uri,
    urlResolver: Function<String, Uri>?
) {
    private val renderableData: RenderableInternalFilamentAssetData

    init {
        val data = renderable.getRenderableData()
        if (data is RenderableInternalFilamentAssetData) {
            renderableData = data
        } else {
            throw IllegalStateException("Expected task type $TAG")
        }
        renderableData.urlResolver = Function { missingPath ->
            getUriFromMissingResource(sourceUri, missingPath, urlResolver)
        }
        renderableData.context = context.applicationContext
        renderable.getId().update()
    }

    /** Returns [CompletableFuture] for a new [Renderable]. */
    fun downloadAndProcessRenderable(inputStreamCreator: Callable<InputStream>): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(
            // Download byte buffer via thread pool
            {
                try {
                    SceneformBufferUtils.inputStreamCallableToByteArray(inputStreamCreator)
                } catch (e: Exception) {
                    throw CompletionException(e)
                }
            },
            ThreadPools.getThreadPoolExecutor()
        ).thenApplyAsync(
            { gltfByteBuffer ->
                // Check for glb header
                renderableData.isGltfBinary = gltfByteBuffer[0] == 0x67.toByte()
                        && gltfByteBuffer[1] == 0x6C.toByte()
                        && gltfByteBuffer[2] == 0x54.toByte()
                        && gltfByteBuffer[3] == 0x46.toByte()
                renderableData.gltfByteBuffer = ByteBuffer.wrap(gltfByteBuffer)
                renderable
            },
            ThreadPools.getMainExecutor()
        )
    }

    companion object {
        private val TAG = LoadRenderableFromFilamentGltfTask::class.java.simpleName

        @JvmStatic
        fun getUriFromMissingResource(
            @NonNull parentUri: Uri,
            @NonNull missingResource: String,
            urlResolver: Function<String, Uri>?
        ): Uri {
            if (urlResolver != null) {
                return urlResolver.apply(missingResource)
            }

            var resource = missingResource
            if (resource.startsWith("/")) {
                resource = resource.substring(1)
            }

            // Ensure encoding.
            val decodedMissingResUri = Uri.parse(Uri.decode(resource))

            if (decodedMissingResUri.scheme != null) {
                throw AssertionError(
                    String.format(
                        "Resource path contains a scheme but should be relative, uri: (%s)",
                        decodedMissingResUri
                    )
                )
            }

            // Build uri to missing resource.
            val decodedMissingResPath = Preconditions.checkNotNull(decodedMissingResUri.path)
            val decodedParentUri = Uri.parse(Uri.decode(parentUri.toString()))
            val uri = decodedParentUri.buildUpon().appendPath("..").appendPath(decodedMissingResPath).build()
            // Normalize and return Uri.
            return Uri.parse(Uri.decode(URI.create(uri.toString()).normalize().toString()))
        }
    }
}
