package com.google.ar.sceneform.rendering;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;

import com.google.android.filament.Engine;
import com.google.android.filament.MaterialInstance;
import io.github.sceneview.collision.Vector3;
import com.google.ar.sceneform.resources.ResourceRegistry;
import com.google.ar.sceneform.utilities.AndroidPreconditions;
import com.google.ar.sceneform.utilities.LoadHelper;
import io.github.sceneview.collision.Preconditions;
import com.google.ar.sceneform.utilities.SceneformBufferUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Represents a reference to a material.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class Material {
    private static final String TAG = Material.class.getSimpleName();

    private final MaterialParameters materialParameters = new MaterialParameters();

    @Nullable
    protected Lifecycle lifecycle;

    @Nullable
    private final com.google.android.filament.Material filamentMaterial;
    public final MaterialInstance filamentMaterialInstance;

    /**
     * Creates a new instance of this Material.
     *
     * <p>The new material will have a unique copy of the material parameters that can be changed
     * independently. The getFilamentEngine material resource is immutable and will be shared between
     * instances.
     */
    public Material makeCopy() {
        return new Material(this);
    }

    public void setBoolean(String name, boolean x) {
        materialParameters.setBoolean(name, x);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setBoolean2(String name, boolean x, boolean y) {
        materialParameters.setBoolean2(name, x, y);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setBoolean3(String name, boolean x, boolean y, boolean z) {
        materialParameters.setBoolean3(name, x, y, z);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setBoolean4(String name, boolean x, boolean y, boolean z, boolean w) {
        materialParameters.setBoolean4(name, x, y, z, w);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setFloat(String name, float x) {
        materialParameters.setFloat(name, x);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setFloat2(String name, float x, float y) {
        materialParameters.setFloat2(name, x, y);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setFloat3(String name, float x, float y, float z) {
        materialParameters.setFloat3(name, x, y, z);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setFloat3(String name, Vector3 value) {
        materialParameters.setFloat3(name, value);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setFloat3(String name, Color color) {
        materialParameters.setFloat3(name, color.r, color.g, color.b);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setFloat4(String name, float x, float y, float z, float w) {
        materialParameters.setFloat4(name, x, y, z, w);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setFloat4(String name, Color color) {
        materialParameters.setFloat4(name, color.r, color.g, color.b, color.a);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setInt(String name, int x) {
        materialParameters.setInt(name, x);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setInt2(String name, int x, int y) {
        materialParameters.setInt2(name, x, y);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setInt3(String name, int x, int y, int z) {
        materialParameters.setInt3(name, x, y, z);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setInt4(String name, int x, int y, int z, int w) {
        materialParameters.setInt4(name, x, y, z, w);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setTexture(String name, Texture texture) {
        materialParameters.setTexture(name, texture);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public void setBaseColorTexture(Texture texture) {
        //Set the baseColorIndex to 0 if no existing texture was set
        setInt("baseColorIndex", 0);
        setTexture("baseColorMap", texture);
    }

    /**
     * <pre>
     *     Sets a {@link Texture} to a parameter of the type 'sampler2d' on this material.
     * </pre>
     *
     * @param name         the name of the parameter in the material
     * @param depthTexture the texture to set
     */
    public void setDepthTexture(String name, com.google.android.filament.Texture depthTexture) {
        materialParameters.setDepthTexture(name, depthTexture);
        materialParameters.applyParameterTo(filamentMaterialInstance, name);
    }

    /**
     * Sets an {@link ExternalTexture} to a parameter of type 'samplerExternal' on this material.
     *
     * @param name            the name of the parameter in the material
     * @param externalTexture the texture to set
     */
    public void setExternalTexture(String name, ExternalTexture externalTexture) {
        materialParameters.setExternalTexture(name, externalTexture);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    @Nullable
    public ExternalTexture getExternalTexture(String name) {
        return materialParameters.getExternalTexture(name);
    }

    /**
     * Constructs a {@link Material}
     *
     * @hide We do not support custom materials in version 1.0 and use a Material Factory to create
     * new materials, so there is no need to expose a builder.
     */
    public static Builder builder() {
        AndroidPreconditions.checkMinAndroidApiLevel();

        return new Builder();
    }

    void copyMaterialParameters(MaterialParameters materialParameters) {
        materialParameters.copyFrom(materialParameters);
        materialParameters.applyTo(filamentMaterialInstance);
    }

    public com.google.android.filament.Material getFilamentMaterial() {
        return filamentMaterial != null ? filamentMaterial : filamentMaterialInstance.getMaterial();
    }

    public MaterialInstance getFilamentMaterialInstance() {
        // Filament Material Instance is only set to null when it is disposed or destroyed, so any
        // usage after that point is an internal error.
        if (filamentMaterialInstance == null) {
            throw new AssertionError("Filament Material Instance is null.");
        }
        return filamentMaterialInstance;
    }

    public Material(com.google.android.filament.Material filamentMaterial) {
        this(filamentMaterial, false);
    }

    @SuppressWarnings("initialization")
    public Material(com.google.android.filament.Material filamentMaterial, boolean useDefaultInstance) {
        this.lifecycle = lifecycle;
        this.filamentMaterial = filamentMaterial;
        this.filamentMaterialInstance = useDefaultInstance ? filamentMaterial.getDefaultInstance() : filamentMaterial.createInstance();
    }

    @SuppressWarnings("initialization")
    public Material(@Nullable Lifecycle lifecycle, com.google.android.filament.MaterialInstance materialInstance) {
        this.lifecycle = lifecycle;
        this.filamentMaterial = materialInstance.getMaterial();
        this.filamentMaterialInstance = materialInstance;
    }

    @SuppressWarnings("initialization")
    private Material(Material other) {
        this(other.filamentMaterial);
        copyMaterialParameters(other.materialParameters);
    }

    /**
     * Builder for constructing a {@link Material}
     *
     * @hide We do not support custom materials in version 1.0 and use a Material Factory to create
     * new materials, so there is no need to expose a builder.
     */
    public static final class Builder {
        /**
         * The {@link Material} will be constructed from the contents of this buffer
         */
        @Nullable
        ByteBuffer sourceBuffer;
        /**
         * The {@link Material} will be constructed from the contents of this callable
         */
        @Nullable
        private Callable<InputStream> inputStreamCreator;
        /**
         * The {@link Material} will be constructed from an existing filament material.
         */
        com.google.android.filament.Material existingMaterial;

        @Nullable
        private Object registryId;

        /**
         * Constructor for asynchronous building. The sourceBuffer will be read later.
         */
        private Builder() {
        }

        /**
         * Allows a {@link Material} to be created with data.
         *
         * <p>Construction will be immediate. Please use {@link #setRegistryId(Object)} to register this
         * material for reuse.
         *
         * @param materialBuffer Sets the material data.
         * @return {@link Builder} for chaining setup calls
         */
        public Builder setSource(ByteBuffer materialBuffer) {
            // TODO: Determine if this should be added to the registry?
            Preconditions.checkNotNull(materialBuffer, "Parameter \"materialBuffer\" was null.");

            inputStreamCreator = null;
            sourceBuffer = materialBuffer;
            return this;
        }

        /**
         * Allows a {@link Material} to be constructed from {@link Uri}. Construction will be
         * asynchronous.
         *
         * @param context   Sets the {@link Context} used for loading the resource
         * @param sourceUri Sets a remote Uri or android resource Uri. The material will be added to the
         *                  registry using the Uri. A previously registered material with the same Uri will be
         *                  re-used.
         * @return {@link Builder} for chaining setup calls
         */
        public Builder setSource(Context context, Uri sourceUri) {
            Preconditions.checkNotNull(sourceUri, "Parameter \"sourceUri\" was null.");

            registryId = sourceUri;
            inputStreamCreator = LoadHelper.fromUri(context, sourceUri);
            sourceBuffer = null;
            return this;
        }

        /**
         * Allows a {@link Material} to be constructed from resource.
         *
         * <p>Construction will be asynchronous.
         *
         * @param context  Sets the {@link Context} used for loading the resource
         * @param resource an android resource with raw type. A previously registered material with the
         *                 same resource id will be re-used.
         * @return {@link Builder} for chaining setup calls
         */
        public Builder setSource(Context context, int resource) {
            registryId = context.getResources().getResourceName(resource);
            inputStreamCreator = LoadHelper.fromResource(context, resource);
            sourceBuffer = null;
            return this;
        }

        /**
         * Allows a {@link Material} to be constructed via callable function.
         *
         * @param inputStreamCreator Supplies an {@link InputStream} with the {@link Material} data
         * @return {@link Builder} for chaining setup calls
         */
        public Builder setSource(Callable<InputStream> inputStreamCreator) {
            Preconditions.checkNotNull(
                    inputStreamCreator, "Parameter \"sourceInputStreamCallable\" was null.");

            this.inputStreamCreator = inputStreamCreator;
            sourceBuffer = null;
            return this;
        }

        /**
         * Allows a {@link Material} to be reused. If registryId is non-null it will be saved in a
         * registry and the registry will be checked for this id before construction.
         *
         * @param registryId allows the function to be skipped and a previous material to be re-used
         * @return {@link Builder} for chaining setup calls
         */
        public Builder setRegistryId(Object registryId) {
            this.registryId = registryId;
            return this;
        }

        /**
         * Creates a new {@link Material} based on the parameters set previously. A source must be
         * specified.
         */
        @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
        public CompletableFuture<Material> build(Engine engine) {
            try {
                checkPreconditions();
            } catch (Throwable failedPrecondition) {
                CompletableFuture<Material> result = new CompletableFuture<>();
                result.completeExceptionally(failedPrecondition);
                FutureHelper.logOnException(
                        TAG, result, "Unable to load Material registryId='" + registryId + "'");
                return result;
            }

            // For static-analysis check.
            Object registryId = this.registryId;
            if (registryId != null) {
                // See if a material has already been registered by this id, if so re-use it.
                ResourceRegistry<Material> registry = ResourceManager.getInstance().getMaterialRegistry();
                @Nullable CompletableFuture<Material> materialFuture = registry.get(registryId);
                if (materialFuture != null) {
                    return materialFuture.thenApply(material -> material.makeCopy());
                }
            }

            if (sourceBuffer != null) {
                com.google.android.filament.Material filamentMaterial = createFilamentMaterial(engine, sourceBuffer);
                Material material = new Material(filamentMaterial);

                // Register the new material in the registry.
                if (registryId != null) {
                    ResourceRegistry<Material> registry = ResourceManager.getInstance().getMaterialRegistry();
                    registry.register(registryId, CompletableFuture.completedFuture(material));
                }

                CompletableFuture<Material> result = CompletableFuture.completedFuture(material.makeCopy());
                FutureHelper.logOnException(
                        TAG, result, "Unable to load Material registryId='" + registryId + "'");
                return result;
            } else if (existingMaterial != null) {
                Material material = new Material(existingMaterial);

                // Register the new material in the registry.
                if (registryId != null) {
                    ResourceRegistry<Material> registry = ResourceManager.getInstance().getMaterialRegistry();
                    // In this case register a copy of the material.
                    registry.register(registryId, CompletableFuture.completedFuture(material.makeCopy()));
                }

                // The current existing (in use) material is returned.
                CompletableFuture<Material> result = CompletableFuture.completedFuture(material);
                FutureHelper.logOnException(
                        TAG, result, "Unable to load Material registryId='" + registryId + "'");
                return result;
            }

            // For static-analysis check. Must be final for the lambda to accept the parameter.
            final Callable<InputStream> inputStreamCallable = this.inputStreamCreator;
            if (inputStreamCallable == null) {
                CompletableFuture<Material> result = new CompletableFuture<>();
                result.completeExceptionally(new AssertionError("Input Stream Creator is null."));
                return result;
            }

            CompletableFuture<Material> result =
                    CompletableFuture.supplyAsync(
                                    () -> {
                                        @Nullable ByteBuffer byteBuffer;
                                        // Open and read the material file.
                                        try (InputStream inputStream = inputStreamCallable.call()) {
                                            byteBuffer = SceneformBufferUtils.readStream(inputStream);
                                        } catch (Exception e) {
                                            throw new CompletionException(e);
                                        }

                                        if (byteBuffer == null) {
                                            throw new IllegalStateException("Unable to read data from input stream.");
                                        }

                                        return byteBuffer;
                                    },
                                    ThreadPools.getThreadPoolExecutor())
                            .thenApplyAsync(
                                    byteBuffer -> {
                                        return new Material(createFilamentMaterial(engine, byteBuffer));
                                    },
                                    ThreadPools.getMainExecutor());

            if (registryId != null) {
                ResourceRegistry<Material> registry = ResourceManager.getInstance().getMaterialRegistry();
                registry.register(registryId, result);
            }

            return result.thenApply(material -> material.makeCopy());
        }

        private void checkPreconditions() {
            AndroidPreconditions.checkUiThread();

            if (!hasSource()) {
                throw new AssertionError("Material must have a source.");
            }
        }

        private Boolean hasSource() {
            return inputStreamCreator != null || sourceBuffer != null || existingMaterial != null;
        }

        private com.google.android.filament.Material createFilamentMaterial(Engine engine, ByteBuffer sourceBuffer) {
            try {
                return new com.google.android.filament.Material.Builder()
                        .payload(sourceBuffer, sourceBuffer.limit())
                        .build(engine);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to create material from source byte buffer.", e);
            }
        }
    }
}