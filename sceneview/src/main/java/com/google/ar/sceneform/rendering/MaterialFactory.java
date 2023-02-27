package com.google.ar.sceneform.rendering;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.concurrent.CompletableFuture;

/** Utility class used to construct default {@link Material}s. */
@RequiresApi(api = Build.VERSION_CODES.N)

public final class MaterialFactory {
  /**
   * Name of material parameter for controlling the color of
   * {@link #makeOpaqueWithColor(Context, Color)} and
   * {@link #makeTransparentWithColor(Context, Color)} materials.
   *
   * @see Material#setFloat3(String, Color)
   * @see Material#setFloat4(String, Color)
   */
  public static final String MATERIAL_COLOR = "color";

  /**
   * Name of material parameter for controlling the texture of {@link
   * #makeOpaqueWithTexture(Context, Texture)} and
   * {@link #makeTransparentWithTexture(Context, Texture)} materials.
   *
   * @see Material#setTexture(String, Texture)
   */
  public static final String MATERIAL_TEXTURE = "texture";

  /**
   * Name of material parameter for controlling the metallic property of all {@link MaterialFactory}
   * materials. The metallic property defines whether the surface is a metallic (conductor) or a
   * non-metallic (dielectric) surface. This property should be used as a binary value, set to
   * either 0 or 1. Intermediate values are only truly useful to create transitions between
   * different types of surfaces when using textures. The default value is 0.
   *
   * @see Material#setFloat(String, float)
   */
  public static final String MATERIAL_METALLIC = "metallic";

  /**
   * Name of material parameter for controlling the roughness property of all {@link
   * MaterialFactory} materials. The roughness property controls the perceived smoothness of the
   * surface. When roughness is set to 0, the surface is perfectly smooth and highly glossy. The
   * rougher a surface is, the "blurrier" the reflections are. The default value is 0.4.
   *
   * @see Material#setFloat(String, float)
   */
  public static final String MATERIAL_ROUGHNESS = "roughness";

  /**
   * Name of material parameter for controlling the reflectance property of all {@link
   * MaterialFactory} materials. The reflectance property only affects non-metallic surfaces. This
   * property can be used to control the specular intensity. This value is defined between 0 and 1
   * and represents a remapping of a percentage of reflectance. The default value is 0.5.
   *
   * @see Material#setFloat(String, float)
   */
  public static final String MATERIAL_REFLECTANCE = "reflectance";

  private static final float DEFAULT_METALLIC_PROPERTY = 0.0f;
  private static final float DEFAULT_ROUGHNESS_PROPERTY = 0.4f;
  private static final float DEFAULT_REFLECTANCE_PROPERTY = 0.5f;

  /**
   * Creates an opaque {@link Material} with the {@link Color} passed in. The {@link Color} can be
   * modified by calling {@link Material#setFloat3(String, Color)} with {@link #MATERIAL_COLOR}. The
   * metallicness, roughness, and reflectance can be modified using {@link Material#setFloat(String,
   * float)}.
   *
   * @see #MATERIAL_METALLIC
   * @see #MATERIAL_ROUGHNESS
   * @see #MATERIAL_REFLECTANCE
   * @param context a context used for loading the material resource
   * @param color the color for the material to render
   * @return material that will render the given color
   */
  @SuppressWarnings("AndroidApiChecker")
  // CompletableFuture requires api level 24
  public static CompletableFuture<Material> makeOpaqueWithColor(Context context, Color color) {
    CompletableFuture<Material> materialFuture =
        Material.builder()
            .setSource(
                context, Uri.parse("sceneview/materials/opaque_colored.filamat"))
            .build();

    return materialFuture.thenApply(
        material -> {
          material.setFloat3(MATERIAL_COLOR, color);
          applyDefaultPbrParams(material);
          return material;
        });
  }

  /**
   * Creates a transparent {@link Material} with the {@link Color} passed in. The {@link Color} can
   * be modified by calling {@link Material#setFloat4(String, Color)} with {@link #MATERIAL_COLOR}.
   * The metallicness, roughness, and reflectance can be modified using {@link
   * Material#setFloat(String, float)}.
   *
   * @see #MATERIAL_METALLIC
   * @see #MATERIAL_ROUGHNESS
   * @see #MATERIAL_REFLECTANCE
   * @param context a context used for loading the material resource
   * @param color the color for the material to render
   * @return material that will render the given color
   */
  @SuppressWarnings("AndroidApiChecker")
  // CompletableFuture requires api level 24
  public static CompletableFuture<Material> makeTransparentWithColor(Context context, Color color) {
    CompletableFuture<Material> materialFuture =
        Material.builder()
            .setSource(
                context, Uri.parse("sceneview/materials/transparent_colored.filamat"))
            .build();

    return materialFuture.thenApply(
        material -> {
          material.setFloat4(MATERIAL_COLOR, color);
          applyDefaultPbrParams(material);
          return material;
        });
  }

  /**
   * Creates an opaque {@link Material} with the {@link Texture} passed in. The {@link Texture} can
   * be modified by calling {@link Material#setTexture(String, Texture)} with {@link
   * #MATERIAL_TEXTURE}. The metallicness, roughness, and reflectance can be modified using {@link
   * Material#setFloat(String, float)}.
   *
   * @see #MATERIAL_METALLIC
   * @see #MATERIAL_ROUGHNESS
   * @see #MATERIAL_REFLECTANCE
   * @param context a context used for loading the material resource
   * @param texture the texture for the material to render
   * @return material that will render the given texture
   */
  @SuppressWarnings("AndroidApiChecker")
  // CompletableFuture requires api level 24
  public static CompletableFuture<Material> makeOpaqueWithTexture(
      Context context, Texture texture) {
    CompletableFuture<Material> materialFuture =
        Material.builder()
            .setSource(
                context, Uri.parse("sceneview/materials/opaque_textured.filamat"))
            .build();

    return materialFuture.thenApply(
        material -> {
          material.setTexture(MATERIAL_TEXTURE, texture);
          applyDefaultPbrParams(material);
          return material;
        });
  }

  /**
   * Creates a transparent {@link Material} with the {@link Texture} passed in. The {@link Texture}
   * can be modified by calling {@link Material#setTexture(String, Texture)} with {@link
   * #MATERIAL_TEXTURE}. The metallicness, roughness, and reflectance can be modified using {@link
   * Material#setFloat(String, float)}.
   *
   * @see #MATERIAL_METALLIC
   * @see #MATERIAL_ROUGHNESS
   * @see #MATERIAL_REFLECTANCE
   * @param context a context used for loading the material resource
   * @param texture the texture for the material to render
   * @return material that will render the given texture
   */
  @SuppressWarnings("AndroidApiChecker")
  // CompletableFuture requires api level 24
  public static CompletableFuture<Material> makeTransparentWithTexture(
      Context context, Texture texture) {
    CompletableFuture<Material> materialFuture =
        Material.builder()
            .setSource(
                context, Uri.parse("sceneview/materials/transparent_textured.filamat"))
            .build();

    return materialFuture.thenApply(
        material -> {
          material.setTexture(MATERIAL_TEXTURE, texture);
          applyDefaultPbrParams(material);
          return material;
        });
  }

  private static void applyDefaultPbrParams(Material material) {
    material.setFloat(MATERIAL_METALLIC, DEFAULT_METALLIC_PROPERTY);
    material.setFloat(MATERIAL_ROUGHNESS, DEFAULT_ROUGHNESS_PROPERTY);
    material.setFloat(MATERIAL_REFLECTANCE, DEFAULT_REFLECTANCE_PROPERTY);
  }
}
