package com.google.ar.sceneform.utilities

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.net.http.HttpResponseCache
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import io.github.sceneview.collision.Preconditions
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.concurrent.Callable

/**
 * Convenience class to parse Uri's.
 *
 * @hide
 */
object LoadHelper {
    private val TAG = LoadHelper::class.java.name

    // From https://developer.android.com/reference/android/content/res/Resources
    // The value 0 is an invalid identifier.
    @JvmField
    val INVALID_RESOURCE_IDENTIFIER = 0
    private const val RAW_RESOURCE_TYPE = "raw"
    private const val DRAWABLE_RESOURCE_TYPE = "drawable"
    private const val SLASH_DELIMETER = '/'
    private val ANDROID_ASSET = SLASH_DELIMETER + "android_asset" + SLASH_DELIMETER

    // Default cache size of 512MB.
    private const val DEFAULT_CACHE_SIZE_BYTES: Long = 512L shl 20

    /** True if the Uri is an Android resource, false if any other uri. */
    @JvmStatic
    fun isAndroidResource(sourceUri: Uri): Boolean {
        Preconditions.checkNotNull(sourceUri, "Parameter \"sourceUri\" was null.")
        return TextUtils.equals(ContentResolver.SCHEME_ANDROID_RESOURCE, sourceUri.scheme)
    }

    /** True if the Uri is a filename, false if it is a remote location. */
    @JvmStatic
    fun isFileAsset(sourceUri: Uri): Boolean {
        Preconditions.checkNotNull(sourceUri, "Parameter \"sourceUri\" was null.")
        val scheme: String? = sourceUri.scheme
        return TextUtils.isEmpty(scheme) || ContentResolver.SCHEME_FILE == scheme
    }

    /** True if the Uri is an Android resource, false if any other uri. */
    @JvmStatic
    fun isContentResource(sourceUri: Uri): Boolean {
        Preconditions.checkNotNull(sourceUri, "Parameter \"sourceUri\" was null.")
        return TextUtils.equals(ContentResolver.SCHEME_CONTENT, sourceUri.scheme)
    }

    /**
     * Normalizes Uri's based on a reference Uri. This function is for convenience only since the Uri
     * class can do this as well.
     */
    @JvmStatic
    fun resolveUri(unresolvedUri: Uri, parentUri: Uri?): Uri {
        return if (parentUri == null) {
            unresolvedUri
        } else {
            resolve(parentUri, unresolvedUri)
        }
    }

    /**
     * Creates an InputStream from an Android resource ID.
     *
     * @throws IllegalArgumentException for resources that can't be loaded.
     */
    @JvmStatic
    fun fromResource(context: Context, resId: Int): Callable<InputStream> {
        Preconditions.checkNotNull(context, "Parameter \"context\" was null.")

        val resourceType = context.resources.getResourceTypeName(resId)
        return if (resourceType == RAW_RESOURCE_TYPE || resourceType == DRAWABLE_RESOURCE_TYPE) {
            Callable { context.resources.openRawResource(resId) }
        } else {
            throw IllegalArgumentException(
                "Unknown resource resourceType '" +
                        resourceType +
                        "' in resId '" +
                        context.resources.getResourceName(resId) +
                        "'. Resource will not be loaded"
            )
        }
    }

    /**
     * Creates different InputStreams depending on the contents of the Uri.
     *
     * @param requestProperty Adds connection properties to created input stream.
     * @throws IllegalArgumentException for Uri's that can't be loaded.
     */
    @JvmStatic
    @JvmOverloads
    fun fromUri(
        context: Context,
        sourceUri: Uri,
        requestProperty: Map<String, String>? = null
    ): Callable<InputStream> {
        Preconditions.checkNotNull(sourceUri, "Parameter \"sourceUri\" was null.")
        Preconditions.checkNotNull(context, "Parameter \"context\" was null.")
        return if (isFileAsset(sourceUri)) {
            fileUriToInputStreamCreator(context, sourceUri)
        } else if (isAndroidResource(sourceUri)) {
            // Note: Prefer creating InputStreams directly from resources.
            // By converting to URIs first, we can't load library resources from a dynamic module.
            androidResourceUriToInputStreamCreator(context, sourceUri)
        } else if (isContentResource(sourceUri)) {
            contentUriToInputStreamCreator(context, sourceUri)
        } else if (isGltfDataUri(sourceUri)) {
            dataUriInputStreamCreator(sourceUri)
        } else {
            remoteUriToInputStreamCreator(sourceUri, requestProperty)
        }
    }

    /**
     * Generates a Uri from an Android resource.
     *
     * @throws android.content.res.Resources.NotFoundException
     */
    @JvmStatic
    fun resourceToUri(context: Context, resID: Int): Uri {
        val resources = context.resources
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resID))
            .appendPath(resources.getResourceTypeName(resID))
            .appendPath(resources.getResourceEntryName(resID))
            .build()
    }

    /** Return the integer resource id for the specified resource name. */
    @JvmStatic
    fun rawResourceNameToIdentifier(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, RAW_RESOURCE_TYPE, context.packageName)
    }

    /** Return the integer resource id for the specified resource name. */
    @JvmStatic
    fun drawableResourceNameToIdentifier(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, DRAWABLE_RESOURCE_TYPE, context.packageName)
    }

    /**
     * Enables HTTP caching with default settings, remote Uri requests responses are cached to
     * cacheBaseDir/cacheFolderName
     */
    @JvmStatic
    fun enableCaching(context: Context) {
        enableCaching(DEFAULT_CACHE_SIZE_BYTES, context.cacheDir, "http_cache")
    }

    /**
     * Enables HTTP caching, remote Uri requests responses are cached to cacheBaseDir/cacheFolderName
     */
    @JvmStatic
    fun enableCaching(cacheByteSize: Long, cacheBaseDir: File, cacheFolderName: String) {
        // Define the default response cache if it has been previously defined.
        if (HttpResponseCache.getInstalled() == null) {
            try {
                val httpCacheDir = File(cacheBaseDir, cacheFolderName)
                if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
                    HttpResponseCache.install(httpCacheDir, cacheByteSize)
                }
            } catch (e: IOException) {
                Log.i(TAG, "HTTP response cache installation failed:$e")
            }
        }
    }

    @JvmStatic
    fun flushHttpCache() {
        val cache = HttpResponseCache.getInstalled()
        cache?.flush()
    }

    /** Creates an inputStream to read from asset file */
    // TODO: Fix nullness violation: dereference of possibly-null reference sourceUri.getPath()
    @Suppress("nullness:dereference.of.nullable")
    private fun fileUriToInputStreamCreator(context: Context, sourceUri: Uri): Callable<InputStream> {
        val assetManager = context.assets
        val filename: String = if (sourceUri.authority == null) {
            sourceUri.path!!
        } else if (sourceUri.path!!.isEmpty()) {
            sourceUri.authority!!
        } else {
            sourceUri.authority + sourceUri.path
        }

        // Remove "android_asset/" from URI paths like "file:///android_asset/...".
        @Suppress("nullness:argument.type.incompatible")
        val scrubbedFilename = removeAndroidAssetPath(filename)

        return Callable {
            if (assetExists(assetManager, scrubbedFilename)) {
                // Open Android Asset if an Asset was found
                assetManager.open(scrubbedFilename)
            } else {
                // Open file from storage or other non asset location.
                FileInputStream(File(filename))
            }
        }
    }

    private fun removeAndroidAssetPath(filename: String): String {
        // Remove "android_asset/" from URI paths like "file:///android_asset/...".
        return if (filename.startsWith(ANDROID_ASSET)) {
            filename.substring(ANDROID_ASSET.length)
        } else {
            filename
        }
    }

    /**
     * Creates an inputStream to read from android resource
     *
     * @throws IllegalArgumentException for resources that can't be loaded.
     */
    // TODO: incompatible types in return.
    @Suppress("nullness:return.type.incompatible")
    private fun androidResourceUriToInputStreamCreator(
        context: Context,
        sourceUri: Uri
    ): Callable<InputStream> {
        val sourceUriPath = sourceUri.path
        // TODO: Fix nullness violation: dereference of possibly-null reference sourceUriPath
        @Suppress("nullness:dereference.of.nullable")
        val lastSlashIndex = sourceUriPath!!.lastIndexOf(SLASH_DELIMETER)
        val resourceType = sourceUriPath.substring(1, lastSlashIndex)

        return if (resourceType == RAW_RESOURCE_TYPE || resourceType == DRAWABLE_RESOURCE_TYPE) {
            Callable { context.contentResolver.openInputStream(sourceUri) }
        } else {
            throw IllegalArgumentException(
                "Unknown resource resourceType '" +
                        resourceType +
                        "' in uri '" +
                        sourceUri +
                        "'. Resource will not be loaded"
            )
        }
    }

    /**
     * Creates an inputStream to read from android content uri
     *
     * @throws IllegalArgumentException for content that can't be loaded.
     */
    // TODO: incompatible types in return.
    @Suppress("nullness:return.type.incompatible")
    private fun contentUriToInputStreamCreator(
        context: Context,
        sourceUri: Uri
    ): Callable<InputStream> {
        return Callable { context.contentResolver.openInputStream(sourceUri) }
    }

    /**
     * Creates an inputStream to read from remote URL
     *
     * @throws IllegalArgumentException for URL's that can't be loaded.
     */
    private fun remoteUriToInputStreamCreator(
        sourceUri: Uri,
        requestProperty: Map<String, String>?
    ): Callable<InputStream> {
        try {
            val sourceURL = URL(sourceUri.toString())
            val conn = sourceURL.openConnection()
            // Apply properties to the connection if they are available.
            if (requestProperty != null) {
                for ((key, value) in requestProperty) {
                    conn.addRequestProperty(key, value)
                }
            }
            return Callable { conn.getInputStream() }
        } catch (ex: MalformedURLException) {
            // This is rare. Most bad URL's get filtered out when the URL class is constructed.
            throw IllegalArgumentException("Unable to parse url: '$sourceUri'", ex)
        } catch (e: IOException) {
            throw AssertionError("Error opening url connection: '$sourceUri'", e)
        }
    }

    private fun resolve(parent: Uri, child: Uri): Uri {
        try {
            val javaParentUri = URI(parent.toString())
            val javaChildUri = URI(child.toString())
            val resolvedUri = javaParentUri.resolve(javaChildUri)
            return Uri.parse(resolvedUri.toString())
        } catch (ex: URISyntaxException) {
            throw IllegalArgumentException("Unable to parse Uri.", ex)
        }
    }

    @Throws(IOException::class)
    private fun assetExists(assetManager: AssetManager, assetRelativePath: String): Boolean {
        val targetAssetName: String
        val assetsInSameDirectory: Array<String>?
        val lastSlashIndex = assetRelativePath.lastIndexOf(SLASH_DELIMETER)

        if (lastSlashIndex != -1) {
            targetAssetName = assetRelativePath.substring(lastSlashIndex + 1)
            assetsInSameDirectory = assetManager.list(assetRelativePath.substring(0, lastSlashIndex))
        } else {
            targetAssetName = assetRelativePath
            assetsInSameDirectory = assetManager.list("")
        }

        if (assetsInSameDirectory != null) {
            // Search for Android Asset in given directory.
            for (assetName in assetsInSameDirectory) {
                if (targetAssetName == assetName) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun isDataUri(uri: Uri): Boolean {
        val scheme = uri.scheme
        return scheme != null && scheme == "data"
    }

    @JvmStatic
    fun isGltfDataUri(uri: Uri): Boolean {
        return if (!isDataUri(uri)) {
            false
        } else {
            getGltfExtensionFromSchemeSpecificPart(uri.schemeSpecificPart) != null
        }
    }

    private fun getGltfExtensionFromSchemeSpecificPart(schemeSpecificPart: String): String? {
        if (schemeSpecificPart.startsWith("model/gltf-binary")) {
            return "glb"
        }
        if (schemeSpecificPart.startsWith("model/gltf+json")) {
            return "gltf"
        }
        return null
    }

    /**
     * Creates an inputStream to read from a data URI.
     *
     * @throws IllegalArgumentException for URL's that can't be loaded.
     */
    private fun dataUriInputStreamCreator(uri: Uri): Callable<InputStream> {
        val data = uri.schemeSpecificPart
        val commaIndex = data.indexOf(',')
        if (commaIndex < 0) {
            throw IllegalArgumentException("Malformed data uri - does not contain a ','")
        }
        val prefix = data.substring(0, commaIndex)
        val isBase64 = prefix.contains(";base64")
        val dataString = data.substring(commaIndex + 1)
        return Callable {
            ByteArrayInputStream(
                if (isBase64) Base64.decode(dataString, Base64.DEFAULT) else dataString.toByteArray()
            )
        }
    }

    @JvmStatic
    fun getLastPathSegment(uri: Uri): String {
        return if (isGltfDataUri(uri)) {
            "file." + getGltfExtensionFromSchemeSpecificPart(uri.schemeSpecificPart)
        } else {
            var lastPathSegment = uri.lastPathSegment
            if (lastPathSegment == null) {
                // This could be a file:// uri, e.g. if it's loaded out of assets.
                val uriString = uri.toString()
                lastPathSegment = uriString.substring(uriString.lastIndexOf('/') + 1)
            }
            lastPathSegment
        }
    }
}
