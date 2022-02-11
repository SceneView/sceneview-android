package com.google.ar.sceneform.rendering;

import com.google.ar.sceneform.resources.SharedReference;

public abstract class MaterialInternalData extends SharedReference {
  public abstract com.google.android.filament.Material getFilamentMaterial();
}
