package com.google.ar.sceneform.rendering;

import com.google.ar.sceneform.resources.SharedReference;

abstract class MaterialInternalData extends SharedReference {
  abstract com.google.android.filament.Material getFilamentMaterial();
}
