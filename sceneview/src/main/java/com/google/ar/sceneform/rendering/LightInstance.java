package com.google.ar.sceneform.rendering;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import com.google.android.filament.Entity;
import com.google.android.filament.LightManager;
import com.google.ar.sceneform.common.TransformProvider;
import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.AndroidPreconditions;

import io.github.sceneview.Filament;
import io.github.sceneview.light.LightKt;

/**
 * Wraps a Filament Light.
 *
 * @hide This class is not part of the user facing API.
 */
public class LightInstance {
  private static final String TAG = LightInstance.class.getSimpleName();

  private class LightInstanceChangeListener implements Light.LightChangedListener {
    @Override
    public void onChange() {
      dirty = true;
    }
  }

  private @Entity int entity;
  private final Light light;
  @Nullable private Renderer renderer;

  @Nullable private TransformProvider transformProvider = null;

  private Vector3 localPosition;
  private Vector3 localDirection;
  private boolean dirty;

  private LightInstanceChangeListener changeListener = new LightInstanceChangeListener();

  LightInstance(Lifecycle lifecycle, Light light, TransformProvider transformProvider) {
    this.light = light;
    this.transformProvider = transformProvider;
    this.localPosition = light.getLocalPosition();
    this.localDirection = light.getLocalDirection();
    this.dirty = false;

    // Add a listener so the light instance knows when the light changes.
    light.addChangedListener(changeListener);

    // Filament will crash if you call functions on the builder that are not appropriate for the
    // light type.
    if (light.getType() == Light.Type.POINT) {
      entity = LightKt.build(new LightManager.Builder(LightManager.Type.POINT)
          .position(
              light.getLocalPosition().x, light.getLocalPosition().y, light.getLocalPosition().z)
          .color(light.getColor().r, light.getColor().g, light.getColor().b)
          .intensity(light.getIntensity())
          .falloff(light.getFalloffRadius())
          .castShadows(light.isShadowCastingEnabled())
              , lifecycle);
    } else if (light.getType() == Light.Type.DIRECTIONAL) {
      entity = LightKt.build(new LightManager.Builder(LightManager.Type.DIRECTIONAL)
          .direction(
              light.getLocalDirection().x, light.getLocalDirection().y, light.getLocalDirection().z)
          .color(light.getColor().r, light.getColor().g, light.getColor().b)
          .intensity(light.getIntensity())
          .castShadows(light.isShadowCastingEnabled())
              , lifecycle);
    } else if (light.getType() == Light.Type.SPOTLIGHT) {
      entity = LightKt.build(new LightManager.Builder(LightManager.Type.SPOT)
          .position(
              light.getLocalPosition().x, light.getLocalPosition().y, light.getLocalPosition().z)
          .direction(
              light.getLocalDirection().x, light.getLocalDirection().y, light.getLocalDirection().z)
          .color(light.getColor().r, light.getColor().g, light.getColor().b)
          .intensity(light.getIntensity())
          .spotLightCone(
              Math.min(light.getInnerConeAngle(), light.getOuterConeAngle()),
              light.getOuterConeAngle())
          .castShadows(light.isShadowCastingEnabled())
              , lifecycle);
    } else if (light.getType() == Light.Type.FOCUSED_SPOTLIGHT) {
      entity = LightKt.build(new LightManager.Builder(LightManager.Type.FOCUSED_SPOT)
          .position(
              light.getLocalPosition().x, light.getLocalPosition().y, light.getLocalPosition().z)
          .direction(
              light.getLocalDirection().x, light.getLocalDirection().y, light.getLocalDirection().z)
          .color(light.getColor().r, light.getColor().g, light.getColor().b)
          .intensity(light.getIntensity())
          .spotLightCone(
              Math.min(light.getInnerConeAngle(), light.getOuterConeAngle()),
              light.getOuterConeAngle())
          .castShadows(light.isShadowCastingEnabled())
              , lifecycle);
    } else {
      throw new UnsupportedOperationException("Unsupported light type.");
    }
  }

  public void updateTransform() {
    // Update the light instance based on changes to the source light.
    updateProperties();

    // Handle lights that do not have transform providers such as default global sunlight.
    if (transformProvider == null) {
      return;
    }

    LightManager lightManager = Filament.getLightManager();

    final int instance = lightManager.getInstance(entity);
    final Matrix transform = transformProvider.getTransformationMatrix();

    if (lightTypeRequiresPosition(light.getType())) {
      final Vector3 position = transform.transformPoint(localPosition);
      lightManager.setPosition(instance, position.x, position.y, position.z);
    }
    if (lightTypeRequiresDirection(light.getType())) {
      final Vector3 direction = transform.transformDirection(localDirection);
      lightManager.setDirection(instance, direction.x, direction.y, direction.z);
    }
  }

  public Renderer getRenderer() {
    return renderer;
  }

  public void setRenderer(Renderer renderer) {
    if (this.renderer != renderer) {
      if (this.renderer != null) {
        this.renderer.removeLight(this);
      }
      if (renderer != null) {
        renderer.addLight(this);
      }
      this.renderer = renderer;
    }
  }

  public Light getLight() {
    return light;
  }

  @Entity
  int getEntity() {
    return entity;
  }

  public void destroy() {
    AndroidPreconditions.checkUiThread();

    // Remove the changed listener from the light so the light instance's memory can be freed.
    if (light != null) {
      light.removeChangedListener(changeListener);
      changeListener = null;
    }

    Filament.getLightManager().destroy(entity);
    Filament.getEntityManager().destroy(entity);
  }

  /** @hide */
  @Override
  protected void finalize() throws Throwable {
    try {
      ThreadPools.getMainExecutor().execute(() -> destroy());
    } catch (Exception e) {
      Log.e(TAG, "Error while Finalizing Light Instance.", e);
    } finally {
      super.finalize();
    }
  }

  /*
   * Copy updated light properites from the light data
   * This just updates a light rather than creating a new one.
   */
  private void updateProperties() {
    // Only update the properties if the light is marked as dirty.
    if (!dirty) {
      return;
    }
    dirty = false;

    LightManager lightManager = Filament.getLightManager();
    final int instance = lightManager.getInstance(entity);

    localPosition = light.getLocalPosition();
    localDirection = light.getLocalDirection();

    // Handle lights that are not attached to nodes, treat local direction/position as world space.
    if (transformProvider == null) {
      if (lightTypeRequiresPosition(light.getType())) {
        lightManager.setPosition(instance, localPosition.x, localPosition.y, localPosition.z);
      }
      if (lightTypeRequiresDirection(light.getType())) {
        lightManager.setDirection(instance, localDirection.x, localDirection.y, localDirection.z);
      }
    }

    lightManager.setColor(instance, light.getColor().r, light.getColor().g, light.getColor().b);
    lightManager.setIntensity(instance, light.getIntensity());
    if (light.getType() == Light.Type.POINT) {
      lightManager.setFalloff(instance, light.getFalloffRadius());
    } else if (light.getType() == Light.Type.SPOTLIGHT
        || light.getType() == Light.Type.FOCUSED_SPOTLIGHT) {
      lightManager.setSpotLightCone(
          instance,
          Math.min(light.getInnerConeAngle(), light.getOuterConeAngle()),
          light.getOuterConeAngle());
    }
  }

  private static boolean lightTypeRequiresPosition(Light.Type type) {
    return type == Light.Type.POINT
        || type == Light.Type.SPOTLIGHT
        || type == Light.Type.FOCUSED_SPOTLIGHT;
  }

  private static boolean lightTypeRequiresDirection(Light.Type type) {
    return type == Light.Type.SPOTLIGHT
        || type == Light.Type.FOCUSED_SPOTLIGHT
        || type == Light.Type.DIRECTIONAL;
  }
}
