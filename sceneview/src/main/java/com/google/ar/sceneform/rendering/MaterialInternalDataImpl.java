package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;

/**
 * Represents shared data used by {@link Material}s for rendering. The data will be released when
 * all {@link Material}s using this data are finalized.
 */
public class MaterialInternalDataImpl extends MaterialInternalData {
  @Nullable private com.google.android.filament.Material filamentMaterial;

  public MaterialInternalDataImpl(com.google.android.filament.Material filamentMaterial) {
    this.filamentMaterial = filamentMaterial;
  }

  @Override
  public com.google.android.filament.Material getFilamentMaterial() {
    if (filamentMaterial == null) {
      throw new IllegalStateException("Filament Material is null.");
    }
    return filamentMaterial;
  }
}
