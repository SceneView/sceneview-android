package com.google.ar.sceneform.rendering;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import com.google.android.filament.Texture;
import com.google.ar.sceneform.utilities.BufferHelper;
import com.google.ar.sceneform.utilities.Preconditions;

import java.nio.ByteBuffer;

import io.github.sceneview.texture.TextureKt;

/**
 * <pre>
 *     The DepthTexture class holds a special Texture to store
 *     information from a DepthImage or RawDepthImage to implement the occlusion of
 *     virtual objects behind real world objects.
 * </pre>
 */
public class DepthTexture {

    @Nullable
    private Texture filamentTexture;
    private final Handler handler = new Handler(Looper.myLooper());

    /**
     * <pre>
     *      A call to this constructor creates a new Filament Texture which is
     *      later used to feed in data from a DepthImage.
     * </pre>
     *
     * @param width  int
     * @param height int
     */
    public DepthTexture(Lifecycle lifecycle, int width, int height) {
        filamentTexture = TextureKt.build(new Texture.Builder()
                        .width(width)
                        .height(height)
                        .sampler(Texture.Sampler.SAMPLER_2D)
                        .format(Texture.InternalFormat.RG8)
                        .levels(1)
                , lifecycle);
    }

    public Texture getFilamentTexture() {
        return Preconditions.checkNotNull(filamentTexture);
    }

    /**
     * <pre>
     *     This is the most important function of this class.
     *     The Filament Texture is updated based on the newest
     *     DepthImage. To solve a problem with a to early
     *     released DepthImage the ByteBuffer which holds all
     *     necessary data is cloned. The cloned ByteBuffer is unaffected
     *     of a released DepthImage and therefore produces not
     *     a flickering result.
     * </pre>
     *
     * @param depthImage {@link Image}
     */
    public void updateDepthTexture(Image depthImage) {
        if (filamentTexture == null) {
            return;
        }

        Image.Plane plane = depthImage.getPlanes()[0];

        ByteBuffer buffer = plane.getBuffer();
        ByteBuffer clonedBuffer = BufferHelper.cloneByteBuffer(buffer);

        Texture.PixelBufferDescriptor pixelBufferDescriptor = new Texture.PixelBufferDescriptor(
                clonedBuffer,
                Texture.Format.RG,
                Texture.Type.UBYTE,
                1,
                0,
                0,
                0,
                handler,
                null
        );
        TextureKt.setImage(filamentTexture, 0, pixelBufferDescriptor);
    }

    public void destroy() {
        if (filamentTexture != null) {
            TextureKt.destroy(filamentTexture);
        }
        filamentTexture = null;
    }
}