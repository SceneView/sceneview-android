package io.github.sceneview.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.IdRes
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.coroutines.awaitByteArray
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

object ResourceLoader {

    private const val ASSET_FILE_PATH_ROOT = "android_asset"

    /**
     * ### Pass your own [FuelManager] and change it properties for specific http/https
     * configuration.
     */
    @JvmStatic
    var fuelManager: FuelManager = FuelManager()

    /**
     * ### Load a file content buffer from different sources
     *
     * The file location can be:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     *
     * @see okHttpClient
     */
    @JvmStatic
    suspend fun fileBuffer(context: Context, fileLocation: String): ByteBuffer? {
        val uri = Uri.parse(fileLocation)
        return withContext(Dispatchers.IO) {
            when (uri.scheme) {
                "http", "https" -> {
                    ByteBuffer.wrap(fuelManager.get(fileLocation).awaitByteArray())
                }
                else -> {
                    fileBufferLocal(context, fileLocation)
                }
            }
        }
    }

    /**
     * ### Load a file content buffer from different sources
     *
     * The file location can be:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     *
     * @see okHttpClient
     */
    @JvmStatic
    fun fileBufferLocal(context: Context, fileLocation: String): ByteBuffer? {
        val uri = Uri.parse(fileLocation)
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
 * ### Load a file content buffer from different sources
 *
 * @see ResourceLoader.fileBuffer
 */
suspend fun Context.fileBuffer(fileLocation: String): ByteBuffer? =
    ResourceLoader.fileBuffer(this, fileLocation)


/**
 * ### Load a file content buffer from different local sources
 *
 * @see ResourceLoader.fileBuffer
 */
fun Context.fileBufferLocal(fileLocation: String): ByteBuffer? =
    ResourceLoader.fileBufferLocal(this, fileLocation)

/**
 * ### Retrieve a android resource uri from a res id
 */
fun Context.getResourceUri(@IdRes resId: Int): String {
    return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$resId"
}

fun InputStream.toByteArray() = use {
    ByteArray(this.available()).apply {
        read(this)
    }
}

fun <R> InputStream.useBuffer(block: (ByteBuffer) -> R): R = use { inputStream ->
    val bytes = ByteArray(inputStream.available())
    inputStream.read(bytes)
    block(ByteBuffer.wrap(bytes))
}

fun <R> InputStream.useText(block: (String) -> R): R = use { inputStream ->
    inputStream.bufferedReader().use {
        block(it.readText())
    }
}

inline fun <T : AutoCloseable?, R> Array<T>.use(block: (Array<T>) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        forEach {
            try {
                it?.close()
            } catch (e: Exception) {
                throw exception ?: e
            }
        }
    }
}