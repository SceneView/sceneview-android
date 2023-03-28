package com.google.ar.sceneform.rendering;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.filament.Stream;
import com.google.android.filament.Texture;
import com.google.ar.sceneform.utilities.Preconditions;

import io.github.sceneview.texture.StreamKt;
import io.github.sceneview.texture.TextureKt;

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
        //TODO : We actually have an issue with stream being destroyed from elsewhere maybe material
        // Fix it whith kotlining here
        Stream stream = StreamKt.build(new Stream.Builder().stream(surfaceTexture));

        //TODO : We actually have an issue with stream being destroyed from elsewhere maybe material
        // Fix it when kotlining here
        initialize(stream);
    }

    @SuppressWarnings("initialization")
    private void initialize(Stream filamentStream) {
        if (filamentTexture != null) {
            throw new AssertionError("Stream was initialized twice");
        }

        // Create the filament stream.
        this.filamentStream = filamentStream;

        // Create the filament texture.
        filamentTexture = TextureKt.build(new Texture.Builder()
                .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                .format(Texture.InternalFormat.RGB8));

        TextureKt.setExternalStream(filamentTexture, filamentStream);
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

    public void destroy() {
        if (filamentTexture != null) {
            TextureKt.destroy(filamentTexture);
        }
        filamentTexture = null;

        if (filamentStream != null) {
            StreamKt.destroy(filamentStream);
        }
        filamentStream = null;
    }
}
