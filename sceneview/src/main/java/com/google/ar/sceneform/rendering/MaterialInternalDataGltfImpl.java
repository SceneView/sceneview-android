package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;

import com.google.android.filament.Material;

public class MaterialInternalDataGltfImpl extends MaterialInternalData {
    @Nullable
    private final Material filamentMaterial;

    public MaterialInternalDataGltfImpl(Material filamentMaterial) {
        this.filamentMaterial = filamentMaterial;
    }

    @Override
    public Material getFilamentMaterial() {
        if (filamentMaterial == null) {
            throw new IllegalStateException("Filament Material is null.");
        }
        return filamentMaterial;
    }
}