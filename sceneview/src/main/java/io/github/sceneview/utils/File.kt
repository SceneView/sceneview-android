package io.github.sceneview.utils

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import androidx.annotation.RawRes
import androidx.core.net.toUri
import com.google.ar.sceneform.utilities.SceneformBufferUtils
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object FileLoader {
    private val TAG: String = SceneformBufferUtils::class.java.getSimpleName()
    private const val ASSET_FILE_PATH_ROOT = "android_asset"

    /**
     * Fuel 3.0 uses a singleton object.
     * For custom HTTP/HTTPS configuration, you can use FuelBuilder:
     * var fuelClient: HttpLoader = FuelBuilder().build()
     *
     * Load a file content buffer from different sources.
     *
     * The file location can be:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     */
    suspend fun loadFileBuffer(context: Context, fileLocation: String): ByteBuffer? {
        val uri = fileLocation.toUri()
        return withContext(Dispatchers.IO) {
            when (uri.scheme) {
                "http", "https" -> {
                    try {
                        val bytes = Fuel.get(fileLocation).source.readByteArray()
                        ByteBuffer.wrap(bytes)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading file from URL: $fileLocation", e)
                        null
                    }
                }

                else -> {
                    readFileBuffer(context, fileLocation)
                }
            }
        }
    }

    /**
     * Load a file content buffer from a local source.
     *
     * The file location can be:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     */
    fun readFileBuffer(context: Context, fileLocation: String): ByteBuffer? {
        val uri = fileLocation.toUri()
        return when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> if (uri.firstPathSegment == ASSET_FILE_PATH_ROOT) {
                context.assets.open(
                    uri.pathSegments.drop(1).joinToString("/")
                ).toByteArray()
            } else {
                File(uri.path!!).inputStream().toByteArray()
            }

            ContentResolver.SCHEME_CONTENT -> {
                context.contentResolver.openInputStream(uri)?.toByteArray()
            }

            ContentResolver.SCHEME_ANDROID_RESOURCE -> {
                // Expected format: android.resource://example.package.name/12345678
                context.resources.openRawResource(uri.pathSegments.lastOrNull()?.toInt()!!)
                    .toByteArray()
            }

            else -> context.assets.open(fileLocation).toByteArray()
        }?.let { byteArray ->
            ByteBuffer.wrap(byteArray)
        }
    }

    @JvmStatic
    private val Uri.firstPathSegment: String?
        get() = pathSegments.firstOrNull()
}


/**
 * @see FileLoader.loadFileBuffer
 */
suspend fun Context.loadFileBuffer(fileLocation: String): ByteBuffer? =
    FileLoader.loadFileBuffer(this, fileLocation)

fun AssetManager.readBuffer(fileLocation: String): ByteBuffer = open(fileLocation).readBuffer()
fun Resources.readBuffer(@RawRes rawResId: Int): ByteBuffer = openRawResource(rawResId).readBuffer()
fun File.readBuffer(): ByteBuffer = inputStream().use { it.readBuffer() }

fun InputStream.readBuffer(): ByteBuffer = toByteArray().let {
    ByteBuffer.wrap(it)
}

/**
 * Retrieve a android resource uri from a res id.
 */
fun Context.getResourceUri(resId: Int): String {
    return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$resId"
}

fun InputStream.toByteArray() = use {
    ByteArray(this.available()).apply {
        read(this)
    }
}

fun ByteBuffer.clone() = ByteBuffer.allocate(this@clone.capacity()).apply {
    order(ByteOrder.nativeOrder())
    // Copy from the beginning
    this@clone.rewind()
    put(this@clone)
    this@clone.rewind()
    flip()
}

fun FloatBuffer.clone() = FloatBuffer.allocate(this@clone.capacity()).apply {
    // Copy from the beginning
    this@clone.rewind()
    put(this@clone)
    this@clone.rewind()
    flip()
}
