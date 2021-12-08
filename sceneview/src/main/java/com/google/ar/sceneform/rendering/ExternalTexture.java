package com.google.ar.sceneform.rendering;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.filament.Stream;
import com.google.android.filament.Texture;
import com.google.ar.sceneform.utilities.AndroidPreconditions;
import com.google.ar.sceneform.utilities.Preconditions;

/**
 * Creates an Android {@link SurfaceTexture} and {@link Surface} that can be displayed by Sceneform.
 * Useful for displaying video, or anything else that can be drawn to a {@link SurfaceTexture}.
 *
 * <p>The getFilamentEngine OpenGL ES texture is automatically created by Sceneform. Also, {@link
 * SurfaceTexture#updateTexImage()} is automatically called and should not be called manually.
 *
 * <p>Call {@link Material#setExternalTexture(String, ExternalTexture)} to use an ExternalTexture.
 * The material parameter MUST be of type 'samplerExternal'.
 */
public class ExternalTexture {
    private static final String TAG = ExternalTexture.class.getSimpleName();

    @Nullable
    private final SurfaceTexture surfaceTexture;
    @Nullable
    private final Surface surface;

    @Nullable
    private Texture filamentTexture;
    @Nullable
    private Stream filamentStream;

    /**
     * Creates an ExternalTexture with a new Android {@link SurfaceTexture} and {@link Surface}.
     */
    @SuppressWarnings("initialization")
    public ExternalTexture() {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.detachFromGLContext();
        this.surfaceTexture = surfaceTexture;

        // Create the Android surface.
        surface = new Surface(surfaceTexture);

        // Create the filament stream.
        Stream stream =
                new Stream.Builder()
                        .stream(surfaceTexture).build(EngineInstance.getEngine().getFilamentEngine());

        initialize(stream);
    }

    /**
     * Creates an ExternalTexture from an OpenGL ES textureId without a SurfaceTexture. For internal
     * use only.
     */
    public ExternalTexture(int textureId, int width, int height) {
       /* this.surface = null;

        this.filamentTexture = new Texture
                .Builder()
                .importTexture(textureId)
                .width(width)
                .height(height)
                .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                .format(Texture.InternalFormat.RGB8)
                .build(EngineInstance.getEngine().getFilamentEngine());

        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.detachFromGLContext();
        this.surfaceTexture = surfaceTexture;

        filamentStream = new Stream.Builder()
                .stream(surfaceTexture)
                .build(EngineInstance.getEngine().getFilamentEngine());

        filamentTexture.setExternalStream(
                EngineInstance.getEngine().getFilamentEngine(),
                filamentStream);

        ResourceManager.getInstance()
                .getExternalTextureCleanupRegistry()
                .register(this, new CleanupCallback(filamentTexture, filamentStream));*/

        // Explicitly set the surface and surfaceTexture to null, since they are unused in this case.
        surfaceTexture = null;
        surface = null;

        // Create the filament stream.
        Stream stream =
                new Stream.Builder()
                        .stream(textureId)
                        .width(width)
                        .height(height)
                        .build(EngineInstance.getEngine().getFilamentEngine());

        initialize(stream);
    }


    @SuppressWarnings("initialization")
    private void initialize(Stream filamentStream) {
        if (filamentTexture != null) {
            throw new AssertionError("Stream was initialized twice");
        }

        // Create the filament stream.
        IEngine engine = EngineInstance.getEngine();
        this.filamentStream = filamentStream;

        // Create the filament texture.
        filamentTexture =
                new Texture.Builder()
                        .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                        .format(Texture.InternalFormat.RGB8)
                        .build(engine.getFilamentEngine());

        filamentTexture.setExternalStream(
                engine.getFilamentEngine(),
                filamentStream);
        ResourceManager.getInstance()
                .getExternalTextureCleanupRegistry()
                .register(this, new CleanupCallback(filamentTexture, filamentStream));
    }

    /**
     * Gets the surface texture created for this ExternalTexture.
     */
    public SurfaceTexture getSurfaceTexture() {
        return Preconditions.checkNotNull(surfaceTexture);
    }

    /**
     * Gets the surface created for this ExternalTexture that draws to {@link #getSurfaceTexture()}
     */
    public Surface getSurface() {
        return Preconditions.checkNotNull(surface);
    }

    public Texture getFilamentTexture() {
        return Preconditions.checkNotNull(filamentTexture);
    }

    public Stream getFilamentStream() {
        return Preconditions.checkNotNull(filamentStream);
    }

    /**
     * Cleanup filament objects after garbage collection
     */
    private static final class CleanupCallback implements Runnable {
        @Nullable
        private final Texture filamentTexture;
        @Nullable
        private final Stream filamentStream;

        CleanupCallback(Texture filamentTexture, Stream filamentStream) {
            this.filamentTexture = filamentTexture;
            this.filamentStream = filamentStream;
        }

        @Override
        public void run() {
            AndroidPreconditions.checkUiThread();

            IEngine engine = EngineInstance.getEngine();
            if (engine == null || !engine.isValid()) {
                return;
            }
            if (filamentTexture != null) {
                engine.destroyTexture(filamentTexture);
            }

            if (filamentStream != null) {
                engine.destroyStream(filamentStream);
            }
        }
    }
}
