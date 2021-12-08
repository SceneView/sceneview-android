package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;

import com.google.android.filament.proguard.UsedByNative;
import com.google.ar.sceneform.resources.SharedReference;
import com.google.ar.sceneform.utilities.AndroidPreconditions;

/**
 * Represents shared data used by {@link Texture}s for rendering. The data will be released when all
 * {@link Texture}s using this data are finalized.
 *
 * @hide Only for use for private features such as occlusion.
 */
@UsedByNative("material_java_wrappers.h")
public class TextureInternalData extends SharedReference {
  @Nullable private com.google.android.filament.Texture filamentTexture;

  private final Texture.Sampler sampler;

  @UsedByNative("material_java_wrappers.h")
  public TextureInternalData(
      com.google.android.filament.Texture filamentTexture, Texture.Sampler sampler) {
    this.filamentTexture = filamentTexture;
    this.sampler = sampler;
  }

  com.google.android.filament.Texture getFilamentTexture() {
    if (filamentTexture == null) {
      throw new IllegalStateException("Filament Texture is null.");
    }

    return filamentTexture;
  }

  Texture.Sampler getSampler() {
    return sampler;
  }

  @Override
  protected void onDispose() {
    AndroidPreconditions.checkUiThread();

    IEngine engine = EngineInstance.getEngine();
    com.google.android.filament.Texture filamentTexture = this.filamentTexture;
    this.filamentTexture = null;
    if (filamentTexture != null && engine != null && engine.isValid()) {
      engine.destroyTexture(filamentTexture);
    }
  }
}
