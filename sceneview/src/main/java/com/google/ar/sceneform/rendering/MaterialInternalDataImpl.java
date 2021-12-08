package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;
import com.google.ar.sceneform.utilities.AndroidPreconditions;

/**
 * Represents shared data used by {@link Material}s for rendering. The data will be released when
 * all {@link Material}s using this data are finalized.
 */
class MaterialInternalDataImpl extends MaterialInternalData {
  @Nullable private com.google.android.filament.Material filamentMaterial;

  MaterialInternalDataImpl(com.google.android.filament.Material filamentMaterial) {
    this.filamentMaterial = filamentMaterial;
  }

  @Override
  com.google.android.filament.Material getFilamentMaterial() {
    if (filamentMaterial == null) {
      throw new IllegalStateException("Filament Material is null.");
    }
    return filamentMaterial;
  }

  @Override
  protected void onDispose() {
    AndroidPreconditions.checkUiThread();

    IEngine engine = EngineInstance.getEngine();
    com.google.android.filament.Material material = this.filamentMaterial;
    this.filamentMaterial = null;
    if (material != null && engine != null && engine.isValid()) {
      engine.destroyMaterial(material);
    }
  }
}
