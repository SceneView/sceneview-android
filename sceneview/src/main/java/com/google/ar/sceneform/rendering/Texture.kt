package com.google.ar.sceneform.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.filament.Engine
import com.google.android.filament.android.TextureHelper
import com.google.android.filament.proguard.UsedByNative
import com.google.ar.sceneform.resources.ResourceRegistry
import com.google.ar.sceneform.utilities.AndroidPreconditions
import com.google.ar.sceneform.utilities.LoadHelper
import io.github.sceneview.collision.Preconditions
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

/** Represents a reference to a texture. */
@Suppress("AndroidApiChecker")
@RequiresApi(api = Build.VERSION_CODES.N)
@UsedByNative("material_java_wrappers.h")
class Texture private constructor(private val textureData: TextureInternalData?) {

    /** Type of Texture usage. */
    enum class Usage {
        /** Texture contains a color map */
        COLOR_MAP,
        /** Texture contains a normal map */
        NORMAL_MAP,
        /** Texture contains arbitrary data */
        DATA
    }

    fun getSampler(): Sampler = Preconditions.checkNotNull(textureData).getSampler()

    /**
     * Get engine data required to use the texture.
     *
     * @hide
     */
    fun getFilamentTexture(): com.google.android.filament.Texture =
        Preconditions.checkNotNull(textureData).getFilamentTexture()

    /** Factory class for [Texture] */
    class Builder {
        /** The [Texture] will be constructed from the contents of this callable */
        private var inputStreamCreator: Callable<InputStream>? = null
        private var bitmap: Bitmap? = null
        private var textureInternalData: TextureInternalData? = null
        private var usage = Usage.COLOR_MAP
        /** Enables reuse through the registry */
        private var registryId: Any? = null
        private var inPremultiplied = true
        private var sampler: Sampler = Sampler.builder().build()

        /**
         * Allows a [Texture] to be constructed from [Uri]. Construction will be asynchronous.
         *
         * @param sourceUri Sets a remote Uri or android resource Uri. The texture will be added to the
         *   registry using the Uri A previously registered texture with the same Uri will be re-used.
         * @param context Sets the [Context] used to resolve sourceUri
         * @return [Builder] for chaining setup calls.
         */
        fun setSource(context: Context, sourceUri: Uri): Builder {
            Preconditions.checkNotNull(sourceUri, "Parameter \"sourceUri\" was null.")
            registryId = sourceUri
            setSource(LoadHelper.fromUri(context, sourceUri))
            return this
        }

        /**
         * Allows a [Texture] to be constructed via callable function.
         *
         * @param inputStreamCreator Supplies an [InputStream] with the [Texture] data.
         * @return [Builder] for chaining setup calls.
         */
        fun setSource(inputStreamCreator: Callable<InputStream>): Builder {
            Preconditions.checkNotNull(inputStreamCreator, "Parameter \"inputStreamCreator\" was null.")
            this.inputStreamCreator = inputStreamCreator
            bitmap = null
            return this
        }

        /**
         * Allows a [Texture] to be constructed from resource. Construction will be asynchronous.
         *
         * @param resource an android resource with raw type. A previously registered texture with the
         *   same resource id will be re-used.
         * @param context [Context] used for resolution
         * @return [Builder] for chaining setup calls.
         */
        fun setSource(context: Context, resource: Int): Builder {
            setSource(LoadHelper.fromResource(context, resource))
            registryId = context.resources.getResourceName(resource)
            return this
        }

        /**
         * Allows a [Texture] to be constructed from a [Bitmap]. Construction will be immediate.
         *
         * The Bitmap must meet the following conditions to be used by Sceneform:
         * - [Bitmap.getConfig] must be [Bitmap.Config.ARGB_8888].
         * - [Bitmap.isPremultiplied] must be true.
         * - The width and height must be smaller than 4096 pixels.
         *
         * @param bitmap [Bitmap] source of texture data
         * @throws IllegalArgumentException if the bitmap isn't valid
         */
        fun setSource(bitmap: Bitmap): Builder {
            Preconditions.checkNotNull(bitmap, "Parameter \"bitmap\" was null.")

            if (bitmap.config != Bitmap.Config.ARGB_8888) {
                throw IllegalArgumentException(
                    "Invalid Bitmap: Bitmap's configuration must be " +
                            "ARGB_8888, but it was " + bitmap.config
                )
            }

            if (bitmap.hasAlpha() && !bitmap.isPremultiplied) {
                throw IllegalArgumentException("Invalid Bitmap: Bitmap must be premultiplied.")
            }

            if (bitmap.width > MAX_BITMAP_SIZE || bitmap.height > MAX_BITMAP_SIZE) {
                throw IllegalArgumentException(
                    "Invalid Bitmap: Bitmap width and height must be " +
                            "smaller than 4096. Bitmap was " + bitmap.width +
                            " width and " + bitmap.height + " height."
                )
            }

            this.bitmap = bitmap
            // TODO: don't overwrite calls to setRegistryId
            registryId = null
            inputStreamCreator = null
            return this
        }

        /**
         * Sets internal data of the texture directly.
         *
         * @hide Hidden API direct from filament
         */
        fun setData(textureInternalData: TextureInternalData): Builder {
            this.textureInternalData = textureInternalData
            return this
        }

        /**
         * Indicates whether the a texture loaded via an [InputStream] should be loaded with
         * premultiplied alpha.
         *
         * @param inPremultiplied Whether the texture loaded via an [InputStream] should be loaded
         *   with premultiplied alpha. Default value is true.
         * @return [Builder] for chaining setup calls.
         */
        fun setPremultiplied(inPremultiplied: Boolean): Builder {
            this.inPremultiplied = inPremultiplied
            return this
        }

        /**
         * Allows a [Texture] to be reused. If registryId is non-null it will be saved in a
         * registry and the registry will be checked for this id before construction.
         *
         * @param registryId Allows the function to be skipped and a previous texture to be re-used.
         * @return [Builder] for chaining setup calls.
         */
        fun setRegistryId(registryId: Any): Builder {
            this.registryId = registryId
            return this
        }

        /**
         * Mark the [Texture] as a containing color, normal or arbitrary data. Color is the default.
         *
         * @param usage Sets the kind of data in [Texture]
         * @return [Builder] for chaining setup calls.
         */
        fun setUsage(usage: Usage): Builder {
            this.usage = usage
            return this
        }

        /**
         * Sets the [Sampler] to control rendering parameters on the [Texture].
         *
         * @param sampler Controls appearance of the [Texture]
         * @return [Builder] for chaining setup calls.
         */
        fun setSampler(sampler: Sampler): Builder {
            this.sampler = sampler
            return this
        }

        /**
         * Creates a new [Texture] based on the parameters set previously
         *
         * @throws IllegalStateException if the builder is not properly set
         */
        fun build(engine: Engine): CompletableFuture<Texture> {
            AndroidPreconditions.checkUiThread()
            val registryId = this.registryId
            if (registryId != null) {
                // See if a texture has already been registered by this id, if so re-use it.
                val registry: ResourceRegistry<Texture> = ResourceManager.getInstance().getTextureRegistry()
                val textureFuture: CompletableFuture<Texture>? = registry.get(registryId)
                if (textureFuture != null) {
                    return textureFuture
                }
            }

            if (textureInternalData != null && registryId != null) {
                throw IllegalStateException("Builder must not set both a bitmap and filament texture")
            }

            val result: CompletableFuture<Texture>
            if (this.textureInternalData != null) {
                result = CompletableFuture.completedFuture(Texture(this.textureInternalData))
            } else {
                val bitmapFuture: CompletableFuture<Bitmap>
                if (inputStreamCreator != null) {
                    bitmapFuture = makeBitmap(inputStreamCreator!!, inPremultiplied)
                } else if (bitmap != null) {
                    bitmapFuture = CompletableFuture.completedFuture(bitmap)
                } else {
                    throw IllegalStateException("Texture must have a source.")
                }

                val samplerCopy = sampler
                val usageCopy = usage
                result = bitmapFuture.thenApplyAsync(
                    { loadedBitmap ->
                        val textureData = makeTextureData(engine, loadedBitmap, samplerCopy, usageCopy, MIP_LEVELS_TO_GENERATE)
                        Texture(textureData)
                    },
                    ThreadPools.getMainExecutor()
                )
            }

            if (registryId != null) {
                val registry: ResourceRegistry<Texture> = ResourceManager.getInstance().getTextureRegistry()
                registry.register(registryId, result)
            }

            FutureHelper.logOnException(TAG, result, "Unable to load Texture registryId='$registryId'")
            return result
        }

        companion object {
            private const val MAX_BITMAP_SIZE = 4096

            private fun makeBitmap(
                inputStreamCreator: Callable<InputStream>,
                inPremultiplied: Boolean
            ): CompletableFuture<Bitmap> {
                return CompletableFuture.supplyAsync(
                    {
                        // Read the texture file.
                        val options = BitmapFactory.Options()
                        options.inScaled = false
                        options.inPremultiplied = inPremultiplied

                        val bitmap: Bitmap?
                        // Open and read the texture file.
                        try {
                            inputStreamCreator.call().use { inputStream ->
                                bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                            }
                        } catch (e: Exception) {
                            throw IllegalStateException(e)
                        }

                        if (bitmap == null) {
                            throw IllegalStateException(
                                "Failed to decode the texture bitmap. The InputStream was not a valid bitmap."
                            )
                        }

                        if (bitmap.config != Bitmap.Config.ARGB_8888) {
                            throw IllegalStateException("Texture must use ARGB8 format.")
                        }

                        bitmap
                    },
                    ThreadPools.getThreadPoolExecutor()
                )
            }

            private fun makeTextureData(
                engine: Engine,
                bitmap: Bitmap,
                sampler: Sampler,
                usage: Usage,
                mipLevels: Int
            ): TextureInternalData {
                val textureInternalFormat = getInternalFormatForUsage(usage)
                val textureSampler = com.google.android.filament.Texture.Sampler.SAMPLER_2D

                val filamentTexture = com.google.android.filament.Texture.Builder()
                    .width(bitmap.width)
                    .height(bitmap.height)
                    .depth(1)
                    .levels(mipLevels)
                    .sampler(textureSampler)
                    .format(textureInternalFormat)
                    .build(engine)

                TextureHelper.setBitmap(engine, filamentTexture, 0, bitmap)

                if (mipLevels > 1) {
                    filamentTexture.generateMipmaps(engine)
                }

                return TextureInternalData(filamentTexture, sampler)
            }
        }
    }

    // LINT.IfChange(api)
    /** Controls what settings are used to sample Textures when rendering. */
    @UsedByNative("material_java_wrappers.h")
    class Sampler private constructor(builder: Builder) {
        /** Options for Minification Filter function. */
        @UsedByNative("material_java_wrappers.h")
        enum class MinFilter {
            @UsedByNative("material_java_wrappers.h") NEAREST,
            @UsedByNative("material_java_wrappers.h") LINEAR,
            @UsedByNative("material_java_wrappers.h") NEAREST_MIPMAP_NEAREST,
            @UsedByNative("material_java_wrappers.h") LINEAR_MIPMAP_NEAREST,
            @UsedByNative("material_java_wrappers.h") NEAREST_MIPMAP_LINEAR,
            @UsedByNative("material_java_wrappers.h") LINEAR_MIPMAP_LINEAR
        }

        /** Options for Magnification Filter function. */
        @UsedByNative("material_java_wrappers.h")
        enum class MagFilter {
            @UsedByNative("material_java_wrappers.h") NEAREST,
            @UsedByNative("material_java_wrappers.h") LINEAR
        }

        /** Options for Wrap Mode function. */
        @UsedByNative("material_java_wrappers.h")
        enum class WrapMode {
            @UsedByNative("material_java_wrappers.h") CLAMP_TO_EDGE,
            @UsedByNative("material_java_wrappers.h") REPEAT,
            @UsedByNative("material_java_wrappers.h") MIRRORED_REPEAT
        }

        private val minFilter: MinFilter = builder.minFilter
        private val magFilter: MagFilter = builder.magFilter
        private val wrapModeS: WrapMode = builder.wrapModeS
        private val wrapModeT: WrapMode = builder.wrapModeT
        private val wrapModeR: WrapMode = builder.wrapModeR

        /**
         * Get the minifying function used whenever the level-of-detail function determines that the
         * texture should be minified.
         */
        fun getMinFilter(): MinFilter = minFilter

        /**
         * Get the magnification function used whenever the level-of-detail function determines that the
         * texture should be magnified.
         */
        fun getMagFilter(): MagFilter = magFilter

        /**
         * Get the wrap mode for texture coordinate S. The wrap mode determines how a texture is
         * rendered for uv coordinates outside the range of [0, 1].
         */
        fun getWrapModeS(): WrapMode = wrapModeS

        /**
         * Get the wrap mode for texture coordinate T. The wrap mode determines how a texture is
         * rendered for uv coordinates outside the range of [0, 1].
         */
        fun getWrapModeT(): WrapMode = wrapModeT

        /**
         * Get the wrap mode for texture coordinate R. The wrap mode determines how a texture is
         * rendered for uv coordinates outside the range of [0, 1].
         */
        fun getWrapModeR(): WrapMode = wrapModeR

        /** Builder for constructing Sampler objects. */
        class Builder {
            lateinit var minFilter: MinFilter
            lateinit var magFilter: MagFilter
            lateinit var wrapModeS: WrapMode
            lateinit var wrapModeT: WrapMode
            lateinit var wrapModeR: WrapMode

            /** Set both the texture minifying function and magnification function. */
            fun setMinMagFilter(minMagFilter: MagFilter): Builder {
                return setMinFilter(MinFilter.values()[minMagFilter.ordinal]).setMagFilter(minMagFilter)
            }

            /**
             * Set the minifying function used whenever the level-of-detail function determines that the
             * texture should be minified.
             */
            fun setMinFilter(minFilter: MinFilter): Builder {
                this.minFilter = minFilter
                return this
            }

            /**
             * Set the magnification function used whenever the level-of-detail function determines that
             * the texture should be magnified.
             */
            fun setMagFilter(magFilter: MagFilter): Builder {
                this.magFilter = magFilter
                return this
            }

            /**
             * Set the wrap mode for all texture coordinates. The wrap mode determines how a texture is
             * rendered for uv coordinates outside the range of [0, 1].
             */
            fun setWrapMode(wrapMode: WrapMode): Builder {
                return setWrapModeS(wrapMode).setWrapModeT(wrapMode).setWrapModeR(wrapMode)
            }

            /**
             * Set the wrap mode for texture coordinate S.
             */
            fun setWrapModeS(wrapMode: WrapMode): Builder {
                wrapModeS = wrapMode
                return this
            }

            /**
             * Set the wrap mode for texture coordinate T.
             */
            fun setWrapModeT(wrapMode: WrapMode): Builder {
                wrapModeT = wrapMode
                return this
            }

            /**
             * Set the wrap mode for texture coordinate R.
             */
            fun setWrapModeR(wrapMode: WrapMode): Builder {
                wrapModeR = wrapMode
                return this
            }

            /** Construct a Sampler from the properties of the Builder. */
            fun build(): Sampler = Sampler(this)
        }

        companion object {
            @JvmStatic
            fun builder(): Builder = Builder()
                .setMinFilter(MinFilter.LINEAR_MIPMAP_LINEAR)
                .setMagFilter(MagFilter.LINEAR)
                .setWrapMode(WrapMode.CLAMP_TO_EDGE)
        }
    }

    companion object {
        private val TAG = Texture::class.java.simpleName

        // Set mipCount to the maximum number of levels, Filament will clamp it as required.
        // This will make sure that all the mip levels are filled out, down to 1x1.
        private const val MIP_LEVELS_TO_GENERATE = 0xff

        /** Constructs a default texture, if nothing else is set */
        @JvmStatic
        fun builder(): Builder {
            AndroidPreconditions.checkMinAndroidApiLevel()
            return Builder()
        }

        private fun getInternalFormatForUsage(usage: Usage): com.google.android.filament.Texture.InternalFormat {
            return when (usage) {
                Usage.COLOR_MAP -> com.google.android.filament.Texture.InternalFormat.SRGB8_A8
                Usage.NORMAL_MAP, Usage.DATA -> com.google.android.filament.Texture.InternalFormat.RGBA8
            }
        }
    }
}
