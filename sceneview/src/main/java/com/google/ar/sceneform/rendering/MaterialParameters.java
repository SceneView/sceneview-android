package com.google.ar.sceneform.rendering;


import androidx.annotation.Nullable;

import com.google.android.filament.MaterialInstance;
import com.google.android.filament.TextureSampler;
import com.google.android.filament.proguard.UsedByNative;
import io.github.sceneview.collision.Vector3;

import java.util.HashMap;
import java.util.Optional;

/**
 * Material property store.
 */
@UsedByNative("material_java_wrappers.h")
final class MaterialParameters {
    private final HashMap<String, Parameter> namedParameters = new HashMap<>();


    @UsedByNative("material_java_wrappers.h")
    void setBoolean(String name, boolean x) {
        namedParameters.put(name, new BooleanParameter(name, x));
    }

    boolean getBoolean(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof BooleanParameter) {
            BooleanParameter booleanParam = (BooleanParameter) param;
            return booleanParam.x;
        }

        return false;
    }

    @UsedByNative("material_java_wrappers.h")
    void setBoolean2(String name, boolean x, boolean y) {
        namedParameters.put(name, new Boolean2Parameter(name, x, y));
    }

    @Nullable
    boolean[] getBoolean2(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Boolean2Parameter) {
            Boolean2Parameter boolean2 = (Boolean2Parameter) param;
            return new boolean[]{boolean2.x, boolean2.y};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setBoolean3(String name, boolean x, boolean y, boolean z) {
        namedParameters.put(name, new Boolean3Parameter(name, x, y, z));
    }

    @Nullable
    boolean[] getBoolean3(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Boolean3Parameter) {
            Boolean3Parameter boolean3 = (Boolean3Parameter) param;
            return new boolean[]{boolean3.x, boolean3.y, boolean3.z};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setBoolean4(String name, boolean x, boolean y, boolean z, boolean w) {
        namedParameters.put(name, new Boolean4Parameter(name, x, y, z, w));
    }

    @Nullable
    boolean[] getBoolean4(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Boolean4Parameter) {
            Boolean4Parameter boolean4 = (Boolean4Parameter) param;
            return new boolean[]{boolean4.x, boolean4.y, boolean4.z, boolean4.w};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setFloat(String name, float x) {
        namedParameters.put(name, new FloatParameter(name, x));
    }

    float getFloat(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof FloatParameter) {
            FloatParameter floatParam = (FloatParameter) param;
            return floatParam.x;
        }

        return 0.0f;
    }

    @UsedByNative("material_java_wrappers.h")
    void setFloat2(String name, float x, float y) {
        namedParameters.put(name, new Float2Parameter(name, x, y));
    }

    @Nullable
    float[] getFloat2(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Float2Parameter) {
            Float2Parameter float2 = (Float2Parameter) param;
            return new float[]{float2.x, float2.y};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setFloat3(String name, float x, float y, float z) {
        namedParameters.put(name, new Float3Parameter(name, x, y, z));
    }

    void setFloat3(String name, Vector3 value) {
        namedParameters.put(
                name, new Float3Parameter(name, value.x, value.y, value.z));
    }

    @Nullable
    float[] getFloat3(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Float3Parameter) {
            Float3Parameter float3 = (Float3Parameter) param;
            return new float[]{float3.x, float3.y, float3.z};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setFloat4(String name, float x, float y, float z, float w) {
        namedParameters.put(name, new Float4Parameter(name, x, y, z, w));
    }

    @Nullable
    float[] getFloat4(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Float4Parameter) {
            Float4Parameter float4 = (Float4Parameter) param;
            return new float[]{float4.x, float4.y, float4.z, float4.w};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setInt(String name, int x) {
        namedParameters.put(name, new IntParameter(name, x));
    }

    int getInt(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof IntParameter) {
            IntParameter intParam = (IntParameter) param;
            return intParam.x;
        }

        return 0;
    }

    @UsedByNative("material_java_wrappers.h")
    void setInt2(String name, int x, int y) {
        namedParameters.put(name, new Int2Parameter(name, x, y));
    }

    @Nullable
    int[] getInt2(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Int2Parameter) {
            Int2Parameter int2 = (Int2Parameter) param;
            return new int[]{int2.x, int2.y};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setInt3(String name, int x, int y, int z) {
        namedParameters.put(name, new Int3Parameter(name, x, y, z));
    }

    @Nullable
    int[] getInt3(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Int3Parameter) {
            Int3Parameter int3 = (Int3Parameter) param;
            return new int[]{int3.x, int3.y, int3.z};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setInt4(String name, int x, int y, int z, int w) {
        namedParameters.put(name, new Int4Parameter(name, x, y, z, w));
    }

    @Nullable
    int[] getInt4(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof Int4Parameter) {
            Int4Parameter int4 = (Int4Parameter) param;
            return new int[]{int4.x, int4.y, int4.z, int4.w};
        }

        return null;
    }

    @UsedByNative("material_java_wrappers.h")
    void setTexture(String name, Texture texture) {
        namedParameters.put(name, new TextureParameter(name, texture));
    }

    @Nullable
    Texture getTexture(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof TextureParameter) {
            return ((TextureParameter) param).texture;
        }

        return null;
    }

    void setDepthTexture(String name, com.google.android.filament.Texture depthTexture) {
        namedParameters.put(name, new DepthTextureParameter(name, depthTexture));
    }

//  @Nullable
//  DepthTexture getDepthTexture(String name) {
//    Parameter param = namedParameters.get(name);
//    if(param instanceof DepthTextureParameter) {
//      return ((DepthTextureParameter) param).depthTexture;
//    }
//    return null;
//  }

    void setExternalTexture(String name, ExternalTexture externalTexture) {
        namedParameters.put(
                name, new ExternalTextureParameter(name, externalTexture));
    }

    @Nullable
    ExternalTexture getExternalTexture(String name) {
        Parameter param = namedParameters.get(name);
        if (param instanceof ExternalTextureParameter) {
            return ((ExternalTextureParameter) param).externalTexture;
        }

        return null;
    }

    void applyTo(MaterialInstance materialInstance) {
        com.google.android.filament.Material material = materialInstance.getMaterial();

        for (Parameter value : namedParameters.values()) {
            if (material.hasParameter(value.name)) {
                value.applyTo(materialInstance);
            }
        }
    }

    void applyParameterTo(MaterialInstance materialInstance, String name) {
        com.google.android.filament.Material material = materialInstance.getMaterial();

        if (material.hasParameter(name)) {
            Optional
                    .ofNullable(namedParameters.get(name))
                    .ifPresent(parameter -> parameter.applyTo(materialInstance));
        }
    }

    void copyFrom(MaterialParameters other) {
        namedParameters.clear();
        merge(other);
    }

    void merge(MaterialParameters other) {
        for (Parameter value : other.namedParameters.values()) {
            Parameter clonedValue = value.clone();
            namedParameters.put(clonedValue.name, clonedValue);
        }
    }

    void mergeIfAbsent(MaterialParameters other) {
        for (Parameter value : other.namedParameters.values()) {
            if (!namedParameters.containsKey(value.name)) {
                Parameter clonedValue = value.clone();
                namedParameters.put(clonedValue.name, clonedValue);
            }
        }
    }


    abstract static class Parameter implements Cloneable {
        String name;

        abstract void applyTo(MaterialInstance materialInstance);

        @Override
        public Parameter clone() {
            try {
                return (Parameter) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }


    static class BooleanParameter extends Parameter {
        boolean x;

        BooleanParameter(String name, boolean x) {
            this.name = name;
            this.x = x;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x);
        }
    }


    static class Boolean2Parameter extends Parameter {
        boolean x;
        boolean y;

        Boolean2Parameter(String name, boolean x, boolean y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y);
        }
    }


    static class Boolean3Parameter extends Parameter {
        boolean x;
        boolean y;
        boolean z;

        Boolean3Parameter(String name, boolean x, boolean y, boolean z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y, z);
        }
    }


    static class Boolean4Parameter extends Parameter {
        boolean x;
        boolean y;
        boolean z;
        boolean w;

        Boolean4Parameter(String name, boolean x, boolean y, boolean z, boolean w) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y, z, w);
        }
    }


    static class FloatParameter extends Parameter {
        float x;

        FloatParameter(String name, float x) {
            this.name = name;
            this.x = x;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x);
        }
    }


    static class Float2Parameter extends Parameter {
        float x;
        float y;

        Float2Parameter(String name, float x, float y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y);
        }
    }


    static class Float3Parameter extends Parameter {
        float x;
        float y;
        float z;

        Float3Parameter(String name, float x, float y, float z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y, z);
        }
    }


    static class Float4Parameter extends Parameter {
        float x;
        float y;
        float z;
        float w;

        Float4Parameter(String name, float x, float y, float z, float w) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y, z, w);
        }
    }


    static class IntParameter extends Parameter {
        int x;

        IntParameter(String name, int x) {
            this.name = name;
            this.x = x;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x);
        }
    }


    static class Int2Parameter extends Parameter {
        int x;
        int y;

        Int2Parameter(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y);
        }
    }


    static class Int3Parameter extends Parameter {
        int x;
        int y;
        int z;

        Int3Parameter(String name, int x, int y, int z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y, z);
        }
    }


    static class Int4Parameter extends Parameter {
        int x;
        int y;
        int z;
        int w;

        Int4Parameter(String name, int x, int y, int z, int w) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(name, x, y, z, w);
        }
    }


    static class TextureParameter extends Parameter {
        final Texture texture;

        TextureParameter(String name, Texture texture) {
            this.name = name;
            this.texture = texture;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            materialInstance.setParameter(
                    name, texture.getFilamentTexture(), convertTextureSampler(texture.getSampler()));
        }

        @Override
        public Parameter clone() {
            return new TextureParameter(name, texture);
        }
    }


    static class DepthTextureParameter extends Parameter {
        private final com.google.android.filament.Texture depthTexture;

        DepthTextureParameter(String name, com.google.android.filament.Texture depthTexture) {
            this.name = name;
            this.depthTexture = depthTexture;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            TextureSampler depthTextureSampler = new TextureSampler(
                    TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
                    TextureSampler.MagFilter.LINEAR,
                    TextureSampler.WrapMode.REPEAT);

            materialInstance.setParameter(name, depthTexture, depthTextureSampler);
        }
    }


    static class ExternalTextureParameter extends Parameter {
        private final ExternalTexture externalTexture;

        ExternalTextureParameter(String name, ExternalTexture externalTexture) {
            this.name = name;
            this.externalTexture = externalTexture;
        }

        @Override
        void applyTo(MaterialInstance materialInstance) {
            TextureSampler filamentSampler = getExternalFilamentSampler();

            materialInstance.setParameter(name, externalTexture.getFilamentTexture(), filamentSampler);
        }

        private TextureSampler getExternalFilamentSampler() {
            TextureSampler filamentSampler =
                    new TextureSampler();
            filamentSampler.setMinFilter(TextureSampler.MinFilter.LINEAR);
            filamentSampler.setMagFilter(TextureSampler.MagFilter.LINEAR);
            filamentSampler.setWrapModeS(TextureSampler.WrapMode.CLAMP_TO_EDGE);
            filamentSampler.setWrapModeT(TextureSampler.WrapMode.CLAMP_TO_EDGE);
            filamentSampler.setWrapModeR(TextureSampler.WrapMode.CLAMP_TO_EDGE);
            return filamentSampler;
        }

        @Override
        public Parameter clone() {
            return new ExternalTextureParameter(name, externalTexture);
        }
    }


    private static TextureSampler convertTextureSampler(
            Texture.Sampler sampler) {
        TextureSampler convertedSampler =
                new TextureSampler();

        switch (sampler.getMinFilter()) {
            case NEAREST:
                convertedSampler.setMinFilter(TextureSampler.MinFilter.NEAREST);
                break;
            case LINEAR:
                convertedSampler.setMinFilter(TextureSampler.MinFilter.LINEAR);
                break;
            case NEAREST_MIPMAP_NEAREST:
                convertedSampler.setMinFilter(
                        TextureSampler.MinFilter.NEAREST_MIPMAP_NEAREST);
                break;
            case LINEAR_MIPMAP_NEAREST:
                convertedSampler.setMinFilter(
                        TextureSampler.MinFilter.LINEAR_MIPMAP_NEAREST);
                break;
            case NEAREST_MIPMAP_LINEAR:
                convertedSampler.setMinFilter(
                        TextureSampler.MinFilter.NEAREST_MIPMAP_LINEAR);
                break;
            case LINEAR_MIPMAP_LINEAR:
                convertedSampler.setMinFilter(
                        TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR);
                break;
            default:
                throw new IllegalArgumentException("Invalid MinFilter");
        }

        switch (sampler.getMagFilter()) {
            case NEAREST:
                convertedSampler.setMagFilter(TextureSampler.MagFilter.NEAREST);
                break;
            case LINEAR:
                convertedSampler.setMagFilter(TextureSampler.MagFilter.LINEAR);
                break;
            default:
                throw new IllegalArgumentException("Invalid MagFilter");
        }

        convertedSampler.setWrapModeS(convertWrapMode(sampler.getWrapModeS()));
        convertedSampler.setWrapModeT(convertWrapMode(sampler.getWrapModeT()));
        convertedSampler.setWrapModeR(convertWrapMode(sampler.getWrapModeR()));

        return convertedSampler;
    }

    private static TextureSampler.WrapMode convertWrapMode(
            Texture.Sampler.WrapMode wrapMode) {
        switch (wrapMode) {
            case CLAMP_TO_EDGE:
                return TextureSampler.WrapMode.CLAMP_TO_EDGE;
            case REPEAT:
                return TextureSampler.WrapMode.REPEAT;
            case MIRRORED_REPEAT:
                return TextureSampler.WrapMode.MIRRORED_REPEAT;
            default:
                throw new IllegalArgumentException("Invalid WrapMode");
        }
    }
}
