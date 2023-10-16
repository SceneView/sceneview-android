package com.google.ar.sceneform.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.filament.Engine;
import com.google.android.filament.android.TextureHelper;
import com.google.android.filament.proguard.UsedByNative;
import com.google.ar.sceneform.resources.ResourceRegistry;
import com.google.ar.sceneform.utilities.AndroidPreconditions;
import com.google.ar.sceneform.utilities.LoadHelper;
import io.github.sceneview.collision.Preconditions;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/** Represents a reference to a texture. */
@SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"}) // CompletableFuture
@RequiresApi(api = Build.VERSION_CODES.N)
@UsedByNative("material_java_wrappers.h")
public class Texture {
  private static final String TAG = Texture.class.getSimpleName();

  /** Type of Texture usage. */
  public enum Usage {
    /** Texture contains a color map */
    COLOR_MAP,
    /** Assume color usage by default */
    /** Texture contains a normal map */
    NORMAL_MAP,
    /** Texture contains arbitrary data */
    DATA
  }

  // Set mipCount to the maximum number of levels, Filament will clamp it as required.
  // This will make sure that all the mip levels are filled out, down to 1x1.
  private static final int MIP_LEVELS_TO_GENERATE = 0xff;

  @Nullable private final TextureInternalData textureData;

  /** Constructs a default texture, if nothing else is set */
  public static Builder builder() {
    AndroidPreconditions.checkMinAndroidApiLevel();

    return new Builder();
  }

  @SuppressWarnings({"initialization"})
  @UsedByNative("material_java_wrappers.h")
  private Texture(TextureInternalData textureData) {
    this.textureData = textureData;
  }

  Sampler getSampler() {
    return Preconditions.checkNotNull(textureData).getSampler();
  }

  /**
   * Get engine data required to use the texture.
   *
   * @hide
   */
  com.google.android.filament.Texture getFilamentTexture() {
    return Preconditions.checkNotNull(textureData).getFilamentTexture();
  }

  private static com.google.android.filament.Texture.InternalFormat getInternalFormatForUsage(
          Usage usage) {
    com.google.android.filament.Texture.InternalFormat format;

    switch (usage) {
      case COLOR_MAP:
        format = com.google.android.filament.Texture.InternalFormat.SRGB8_A8;
        break;
      case NORMAL_MAP:
      case DATA:
      default:
        format = com.google.android.filament.Texture.InternalFormat.RGBA8;
        break;
    }
    return format;
  }

  /** Factory class for {@link Texture} */
  public static final class Builder {
    /** The {@link Texture} will be constructed from the contents of this callable */
    @Nullable private Callable<InputStream> inputStreamCreator = null;

    @Nullable private Bitmap bitmap = null;
    @Nullable private TextureInternalData textureInternalData = null;

    private Usage usage = Usage.COLOR_MAP;
    /** Enables reuse through the registry */
    @Nullable private Object registryId = null;

    private boolean inPremultiplied = true;

    private Sampler sampler = Sampler.builder().build();

    private static final int MAX_BITMAP_SIZE = 4096;

    /** Constructor for asynchronous building. The sourceBuffer will be read later. */
    private Builder() {}

    /**
     * Allows a {@link Texture} to be constructed from {@link Uri}. Construction will be
     * asynchronous.
     *
     * @param sourceUri Sets a remote Uri or android resource Uri. The texture will be added to the
     *     registry using the Uri A previously registered texture with the same Uri will be re-used.
     * @param context Sets the {@link Context} used to resolve sourceUri
     * @return {@link Builder} for chaining setup calls.
     */
    public Builder setSource(Context context, Uri sourceUri) {
      Preconditions.checkNotNull(sourceUri, "Parameter \"sourceUri\" was null.");

      registryId = sourceUri;
      setSource(LoadHelper.fromUri(context, sourceUri));
      return this;
    }

    /**
     * Allows a {@link Texture} to be constructed via callable function.
     *
     * @param inputStreamCreator Supplies an {@link InputStream} with the {@link Texture} data.
     * @return {@link Builder} for chaining setup calls.
     */
    public Builder setSource(Callable<InputStream> inputStreamCreator) {
      Preconditions.checkNotNull(inputStreamCreator, "Parameter \"inputStreamCreator\" was null.");

      this.inputStreamCreator = inputStreamCreator;
      bitmap = null;
      return this;
    }

    /**
     * Allows a {@link Texture} to be constructed from resource. Construction will be asynchronous.
     *
     * @param resource an android resource with raw type. A previously registered texture with the
     *     same resource id will be re-used.
     * @param context {@link Context} used for resolution
     * @return {@link Builder} for chaining setup calls.
     */
    public Builder setSource(Context context, int resource) {
      setSource(LoadHelper.fromResource(context, resource));
      registryId = context.getResources().getResourceName(resource);
      return this;
    }

    /**
     * Allows a {@link Texture} to be constructed from a {@link Bitmap}. Construction will be
     * immediate.
     *
     * <p>The Bitmap must meet the following conditions to be used by Sceneform:
     *
     * <ul>
     *   <li>{@link Bitmap#getConfig()} must be {@link Bitmap.Config#ARGB_8888}.
     *   <li>{@link Bitmap#isPremultiplied()} must be true.
     *   <li>The width and height must be smaller than 4096 pixels.
     * </ul>
     *
     * @param bitmap {@link Bitmap} source of texture data
     * @throws IllegalArgumentException if the bitmap isn't valid
     */
    public Builder setSource(Bitmap bitmap) {
      Preconditions.checkNotNull(bitmap, "Parameter \"bitmap\" was null.");

      if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
        throw new IllegalArgumentException(
                "Invalid Bitmap: Bitmap's configuration must be "
                        + "ARGB_8888, but it was "
                        + bitmap.getConfig());
      }

      if (bitmap.hasAlpha() && !bitmap.isPremultiplied()) {
        throw new IllegalArgumentException("Invalid Bitmap: Bitmap must be premultiplied.");
      }

      if (bitmap.getWidth() > MAX_BITMAP_SIZE || bitmap.getHeight() > MAX_BITMAP_SIZE) {
        throw new IllegalArgumentException(
                "Invalid Bitmap: Bitmap width and height must be "
                        + "smaller than 4096. Bitmap was "
                        + bitmap.getWidth()
                        + " width and "
                        + bitmap.getHeight()
                        + " height.");
      }

      this.bitmap = bitmap;
      // TODO: don't overwrite calls to setRegistryId
      registryId = null;
      inputStreamCreator = null;
      return this;
    }

    /**
     * Sets internal data of the texture directly.
     *
     * @hide Hidden API direct from filament
     */
    public Builder setData(TextureInternalData textureInternalData) {
      this.textureInternalData = textureInternalData;
      return this;
    }

    /**
     * Indicates whether the a texture loaded via an {@link InputStream}should be loaded with
     * premultiplied alpha.
     *
     * @param inPremultiplied Whether the texture loaded via an {@link InputStream} should be loaded
     *     with premultiplied alpha. Default value is true.
     * @return {@link Builder} for chaining setup calls.
     */
    Builder setPremultiplied(boolean inPremultiplied) {
      this.inPremultiplied = inPremultiplied;
      return this;
    }

    /**
     * Allows a {@link Texture} to be reused. If registryId is non-null it will be saved in a
     * registry and the registry will be checked for this id before construction.
     *
     * @param registryId Allows the function to be skipped and a previous texture to be re-used.
     * @return {@link Builder} for chaining setup calls.
     */
    public Builder setRegistryId(Object registryId) {
      this.registryId = registryId;
      return this;
    }

    /**
     * Mark the {@link Texture} as a containing color, normal or arbitrary data. Color is the
     * default.
     *
     * @param usage Sets the kind of data in {@link Texture}
     * @return {@link Builder} for chaining setup calls.
     */
    public Builder setUsage(Usage usage) {
      this.usage = usage;
      return this;
    }

    /**
     * Sets the {@link Sampler}to control rendering parameters on the {@link Texture}.
     *
     * @param sampler Controls appearance of the {@link Texture}
     * @return {@link Builder} for chaining setup calls.
     */
    public Builder setSampler(Sampler sampler) {
      this.sampler = sampler;
      return this;
    }

    /**
     * Creates a new {@link Texture} based on the parameters set previously
     *
     * @throws IllegalStateException if the builder is not properly set
     */
    public CompletableFuture<Texture> build(Engine engine) {
      AndroidPreconditions.checkUiThread();
      Object registryId = this.registryId;
      if (registryId != null) {
        // See if a texture has already been registered by this id, if so re-use it.
        ResourceRegistry<Texture> registry = ResourceManager.getInstance().getTextureRegistry();
        @Nullable CompletableFuture<Texture> textureFuture = registry.get(registryId);
        if (textureFuture != null) {
          return textureFuture;
        }
      }

      if (textureInternalData != null && registryId != null) {
        throw new IllegalStateException("Builder must not set both a bitmap and filament texture");
      }

      CompletableFuture<Texture> result;
      if (this.textureInternalData != null) {
        result = CompletableFuture.completedFuture(new Texture(this.textureInternalData));
      } else {
        CompletableFuture<Bitmap> bitmapFuture;
        if (inputStreamCreator != null) {
          bitmapFuture = makeBitmap(inputStreamCreator, inPremultiplied);
        } else if (bitmap != null) {
          bitmapFuture = CompletableFuture.completedFuture(bitmap);
        } else {
          throw new IllegalStateException("Texture must have a source.");
        }

        result =
                bitmapFuture.thenApplyAsync(
                        loadedBitmap -> {
                          TextureInternalData textureData =
                                  makeTextureData(engine, loadedBitmap, sampler, usage, MIP_LEVELS_TO_GENERATE);
                          return new Texture(textureData);
                        },
                        ThreadPools.getMainExecutor());
      }

      if (registryId != null) {
        ResourceRegistry<Texture> registry = ResourceManager.getInstance().getTextureRegistry();
        registry.register(registryId, result);
      }

      FutureHelper.logOnException(
              TAG, result, "Unable to load Texture registryId='" + registryId + "'");
      return result;
    }

    private static CompletableFuture<Bitmap> makeBitmap(
            Callable<InputStream> inputStreamCreator, boolean inPremultiplied) {
      return CompletableFuture.supplyAsync(
              () -> {
                // Read the texture file.
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;
                options.inPremultiplied = inPremultiplied;
                Bitmap bitmap;

                // Open and read the texture file.
                try (InputStream inputStream = inputStreamCreator.call()) {
                  bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }

                if (bitmap == null) {
                  throw new IllegalStateException(
                          "Failed to decode the texture bitmap. The InputStream was not a valid bitmap.");
                }

                if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                  throw new IllegalStateException("Texture must use ARGB8 format.");
                }

                return bitmap;
              },
              ThreadPools.getThreadPoolExecutor());
    }

    private static TextureInternalData makeTextureData(Engine engine, Bitmap bitmap, Sampler sampler, Usage usage, int mipLevels) {
      // Due to fun ambiguities between Texture (RenderCore) and Texture (Filament)
      // Texture references must be fully qualified giving rise to the following monstrosity
      // of verbosity.
      final com.google.android.filament.Texture.InternalFormat textureInternalFormat =
              getInternalFormatForUsage(usage);
      final com.google.android.filament.Texture.Sampler textureSampler =
              com.google.android.filament.Texture.Sampler.SAMPLER_2D;

      com.google.android.filament.Texture filamentTexture =
              new com.google.android.filament.Texture.Builder()
                              .width(bitmap.getWidth())
                              .height(bitmap.getHeight())
                              .depth(1)
                              .levels(mipLevels)
                              .sampler(textureSampler)
                              .format(textureInternalFormat)
              .build(engine);

      TextureHelper.setBitmap(engine, filamentTexture, 0, bitmap);

      if (mipLevels > 1) {
        filamentTexture.generateMipmaps(engine);
      }

      return new TextureInternalData(filamentTexture, sampler);
    }
  }

  // LINT.IfChange(api)
  /** Controls what settings are used to sample Textures when rendering. */
  @UsedByNative("material_java_wrappers.h")
  public static class Sampler {
    /** Options for Minification Filter function. */
    @UsedByNative("material_java_wrappers.h")
    public enum MinFilter {
      @UsedByNative("material_java_wrappers.h")
      NEAREST,
      @UsedByNative("material_java_wrappers.h")
      LINEAR,
      @UsedByNative("material_java_wrappers.h")
      NEAREST_MIPMAP_NEAREST,
      @UsedByNative("material_java_wrappers.h")
      LINEAR_MIPMAP_NEAREST,
      @UsedByNative("material_java_wrappers.h")
      NEAREST_MIPMAP_LINEAR,
      @UsedByNative("material_java_wrappers.h")
      LINEAR_MIPMAP_LINEAR
    }

    /** Options for Magnification Filter function. */
    @UsedByNative("material_java_wrappers.h")
    public enum MagFilter {
      @UsedByNative("material_java_wrappers.h")
      NEAREST,
      @UsedByNative("material_java_wrappers.h")
      LINEAR
    }

    /** Options for Wrap Mode function. */
    @UsedByNative("material_java_wrappers.h")
    public enum WrapMode {
      @UsedByNative("material_java_wrappers.h")
      CLAMP_TO_EDGE,
      @UsedByNative("material_java_wrappers.h")
      REPEAT,
      @UsedByNative("material_java_wrappers.h")
      MIRRORED_REPEAT
    }

    private final MinFilter minFilter;
    private final MagFilter magFilter;
    private final WrapMode wrapModeS;
    private final WrapMode wrapModeT;
    private final WrapMode wrapModeR;











    private Sampler(Builder builder) {
      this.minFilter = builder.minFilter;
      this.magFilter = builder.magFilter;
      this.wrapModeS = builder.wrapModeS;
      this.wrapModeT = builder.wrapModeT;
      this.wrapModeR = builder.wrapModeR;
    }

    /**
     * Get the minifying function used whenever the level-of-detail function determines that the
     * texture should be minified.
     */
    public MinFilter getMinFilter() {
      return minFilter;
    }

    /**
     * Get the magnification function used whenever the level-of-detail function determines that the
     * texture should be magnified.
     */
    public MagFilter getMagFilter() {
      return magFilter;
    }

    /**
     * Get the wrap mode for texture coordinate S. The wrap mode determines how a texture is
     * rendered for uv coordinates outside the range of [0, 1].
     */
    public WrapMode getWrapModeS() {
      return wrapModeS;
    }

    /**
     * Get the wrap mode for texture coordinate T. The wrap mode determines how a texture is
     * rendered for uv coordinates outside the range of [0, 1].
     */
    public WrapMode getWrapModeT() {
      return wrapModeT;
    }

    /**
     * Get the wrap mode for texture coordinate R. The wrap mode determines how a texture is
     * rendered for uv coordinates outside the range of [0, 1].
     */
    public WrapMode getWrapModeR() {
      return wrapModeR;
    }

    public static Builder builder() {
      return new Builder()
              .setMinFilter(MinFilter.LINEAR_MIPMAP_LINEAR)
              .setMagFilter(MagFilter.LINEAR)
              .setWrapMode(WrapMode.CLAMP_TO_EDGE);
    }

    /** Builder for constructing Sampler objects. */
    public static class Builder {
      private MinFilter minFilter;
      private MagFilter magFilter;
      private WrapMode wrapModeS;
      private WrapMode wrapModeT;
      private WrapMode wrapModeR;

      /** Set both the texture minifying function and magnification function. */
      public Builder setMinMagFilter(MagFilter minMagFilter) {
        return setMinFilter(MinFilter.values()[minMagFilter.ordinal()]).setMagFilter(minMagFilter);
      }

      /**
       * Set the minifying function used whenever the level-of-detail function determines that the
       * texture should be minified.
       */
      public Builder setMinFilter(MinFilter minFilter) {
        this.minFilter = minFilter;
        return this;
      }

      /**
       * Set the magnification function used whenever the level-of-detail function determines that
       * the texture should be magnified.
       */
      public Builder setMagFilter(MagFilter magFilter) {
        this.magFilter = magFilter;
        return this;
      }

      /**
       * Set the wrap mode for all texture coordinates. The wrap mode determines how a texture is
       * rendered for uv coordinates outside the range of [0, 1].
       */
      public Builder setWrapMode(WrapMode wrapMode) {
        return setWrapModeS(wrapMode).setWrapModeT(wrapMode).setWrapModeR(wrapMode);
      }

      /**
       * Set the wrap mode for texture coordinate S. The wrap mode determines how a texture is
       * rendered for uv coordinates outside the range of [0, 1].
       */
      public Builder setWrapModeS(WrapMode wrapMode) {
        wrapModeS = wrapMode;
        return this;
      }

      /**
       * Set the wrap mode for texture coordinate T. The wrap mode determines how a texture is
       * rendered for uv coordinates outside the range of [0, 1].
       */
      public Builder setWrapModeT(WrapMode wrapMode) {
        wrapModeT = wrapMode;
        return this;
      }

      /**
       * Set the wrap mode for texture coordinate R. The wrap mode determines how a texture is
       * rendered for uv coordinates outside the range of [0, 1].
       */
      public Builder setWrapModeR(WrapMode wrapMode) {
        wrapModeR = wrapMode;
        return this;
      }

      /** Construct a Sampler from the properties of the Builder. */
      public Sampler build() {
        return new Sampler(this);
      }
    }
  }
}
