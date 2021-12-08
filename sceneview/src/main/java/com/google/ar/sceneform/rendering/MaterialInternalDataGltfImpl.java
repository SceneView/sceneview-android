package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;
import com.google.android.filament.Material;

public class MaterialInternalDataGltfImpl extends MaterialInternalData {
  @Nullable private final Material filamentMaterial;

  MaterialInternalDataGltfImpl(Material filamentMaterial) {
    this.filamentMaterial = filamentMaterial;
  }

  @Override
  Material getFilamentMaterial() {
    if (filamentMaterial == null) {
      throw new IllegalStateException("Filament Material is null.");
    }
    return filamentMaterial;
  }

  @Override
  protected void onDispose() {
    // Resource tracked in the native-gltf loader.
  }
}
