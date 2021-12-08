package com.google.ar.sceneform.rendering;

import android.os.Build;
import androidx.annotation.RequiresApi;
import com.google.android.filament.Colors;
import com.google.ar.sceneform.common.TransformProvider;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.AndroidPreconditions;
import java.util.ArrayList;

/** Light property store. */
@RequiresApi(api = Build.VERSION_CODES.N)
public class Light {
  /** Type of Light Source */
  public enum Type {
    /**
     * Approximates light radiating in all directions from a single point in space, where the
     * intensity falls off with the inverse square of the distance. Point lights have a position but
     * no direction. Use {@link #setFalloffRadius} to control the falloff.
     */
    POINT,
    /** Approximates an infinitely far away, purely directional light */
    DIRECTIONAL,
    /**
     * Similar to a point light but radiating light in a cone rather than all directions. Note that
     * as you make the cone wider, the energy is spread causing the lighting to appear dimmer. A
     * spotlight has a position and a direction. Use {@link #setInnerConeAngle} and {@link
     * #setOuterConeAngle} to control the cone size.
     */
    SPOTLIGHT,
    /**
     * The same as a spotlight with the exception that the apparent lighting stays the same as the
     * cone angle changes. A spotlight has a position and a direction. Use {@link
     * #setInnerConeAngle} and {@link #setOuterConeAngle} to control the cone size.
     */
    FOCUSED_SPOTLIGHT
  };

  interface LightChangedListener {
    void onChange();
  };

  /** Minimum accepted light intensity */
  private static final float MIN_LIGHT_INTENSITY = 0.0001f;

  private final Type type;
  private final boolean enableShadows;

  private Vector3 position;
  private Vector3 direction;
  private final Color color;
  private float intensity;
  private float falloffRadius;
  private float spotlightConeInner;
  private float spotlightConeOuter;

  private final ArrayList<LightChangedListener> changedListeners = new ArrayList<>();

  /** Constructs a default light, if nothing else is set */
  public static Builder builder(Type type) {
    AndroidPreconditions.checkMinAndroidApiLevel();
    return new Builder(type);
  }

  /**
   * Sets the "RGB" color of the light. Note that intensity is a separate parameter, so you should
   * set the pure color (i.e. each channel is in the [0,1] range). However setting values outside
   * that range is valid.
   *
   * @param color "RGB" color, the default is 0xffffffff
   */
  public void setColor(Color color) {
    this.color.set(color);
    fireChangedListeners();
  }

  /**
   * Sets the "RGB" color of the light based on the desired "color temperature."
   *
   * @param temperature color temperature in Kelvin on a scale from 1,000 to 10,000K. Typical
   *     commercial and residential lighting falls somewhere in the 2000K to 6500K range.
   */
  public void setColorTemperature(float temperature) {
    final float[] rgbColor = Colors.cct(temperature);
    setColor(new Color(rgbColor[0], rgbColor[1], rgbColor[2]));
  }

  /**
   * Sets the light intensity which determines how bright the light is in Lux (lx) or Lumens (lm)
   * (depending on the light type). Larger values produce brighter lights and near zero values
   * generate very little light. A household light bulb will generally have an intensity between 800
   * - 2500 lm whereas sunlight will be around 120,000 lx. There is no absolute upper bound but
   * values larger than sunlight (120,000 lx) are generally not needed.
   *
   * @param intensity the intensity of the light, values greater than one are valid. The intensity
   *     will be clamped and cannot be zero or negative. For directional lights the default is 420
   *     lx. For other other lights the default is 2500 lm.
   */
  public void setIntensity(float intensity) {
    this.intensity = Math.max(intensity, MIN_LIGHT_INTENSITY);
    fireChangedListeners();
  }

  /**
   * Sets the range that the light intensity falls off to zero. This has no affect on the {@link
   * Type#DIRECTIONAL} type.
   *
   * @param falloffRadius the light radius in world units, default is 10.0
   */
  public void setFalloffRadius(float falloffRadius) {
    this.falloffRadius = falloffRadius;
    fireChangedListeners();
  }

  /**
   * Spotlights shine light in a cone, this value determines the size of the inner part of the cone.
   * The intensity is interpolated between the inner and outer cone angles - meaning if they are the
   * same than the cone is perfectly sharp. Generally you will want the inner cone to be smaller
   * than the outer cone to avoid aliasing.
   *
   * @param coneInner inner cone angle in radians, default 0.5
   */
  public void setInnerConeAngle(float coneInner) {
    this.spotlightConeInner = coneInner;
    fireChangedListeners();
  }

  /**
   * Spotlights shine light in a cone, this value determines the size of the outer part of the cone.
   * The intensity is interpolated between the inner and outer cone angles - meaning if they are the
   * same than the cone is perfectly sharp. Generally you will want the inner cone to be smaller
   * than the outer cone to avoid aliasing.
   *
   * @param coneOuter outer cone angle in radians, default is 0.6
   */
  public void setOuterConeAngle(float coneOuter) {
    this.spotlightConeOuter = coneOuter;
    fireChangedListeners();
  }

  /** Get the light {@link Type}. */
  public Type getType() {
    return this.type;
  }

  /** Returns true if the light has shadow casting enabled. */
  public boolean isShadowCastingEnabled() {
    return this.enableShadows;
  }

  /** @hide This is no longer a user facing API. */
  public Vector3 getLocalPosition() {
    return new Vector3(this.position);
  }

  /** @hide This is no longer a user facing API. */
  public Vector3 getLocalDirection() {
    return new Vector3(this.direction);
  }

  /** Get the RGB {@link Color} of the light. */
  public Color getColor() {
    return new Color(this.color);
  }

  /** Get the intensity of the light. */
  public float getIntensity() {
    return this.intensity;
  }

  /** Get the falloff radius of the light. */
  public float getFalloffRadius() {
    return this.falloffRadius;
  }

  /** Get the inner cone angle for spotlights. */
  public float getInnerConeAngle() {
    return this.spotlightConeInner;
  }

  /** Get the outer cone angle for spotlights. */
  public float getOuterConeAngle() {
    return this.spotlightConeOuter;
  }

  /** @hide this functionality is not part of the end-user API */
  public LightInstance createInstance(TransformProvider transformProvider) {
    LightInstance instance = new LightInstance(this, transformProvider);
    if (instance == null) {
      throw new AssertionError("Failed to create light instance, result is null.");
    }
    return instance;
  }

  /** Factory class for {@link Light} */
  public static final class Builder {
    // LINT.IfChange
    private static final float DEFAULT_DIRECTIONAL_INTENSITY = 420.0f;
    // LINT.ThenChange(//depot/google3/third_party/arcore/ar/sceneform/viewer/viewer.cc)

    private final Type type;

    private boolean enableShadows = false;
    private Vector3 position = new Vector3(0.0f, 0.0f, 0.0f);
    private Vector3 direction = new Vector3(0.0f, 0.0f, -1.0f);
    private Color color = new Color(1.0f, 1.0f, 1.0f);
    private float intensity = 2500.0f;
    private float falloffRadius = 10.0f;
    private float spotlightConeInner = 0.5f;
    private float spotlightConeOuter = 0.6f;

    /** Constructor for building. */
    private Builder(Type type) {
      this.type = type;
      // Directional lights should have a different default intensity
      if (type == Type.DIRECTIONAL) {
        intensity = DEFAULT_DIRECTIONAL_INTENSITY;
      }
    }

    /**
     * Determines whether the light casts shadows, or whether synthetic objects can block the light.
     *
     * @param enableShadows true to enable to shadows, false to disable; default is false.
     */
    public Builder setShadowCastingEnabled(boolean enableShadows) {
      this.enableShadows = enableShadows;
      return this;
    }

    /**
     * Sets the "RGB" color of the light. Note that intensity if is a separate parameter, so you
     * should set the pure color (i.e. each channel is in the [0,1] range). However setting values
     * outside that range is valid.
     *
     * @param color "RGB" color, default is (1, 1, 1)
     */
    public Builder setColor(Color color) {
      this.color = color;
      return this;
    }

    /**
     * Sets the "RGB" color of the light based on the desired "color temperature."
     *
     * @param temperature color temperature in Kelvin on a scale from 1,000 to 10,000K. Typical
     *     commercial and residential lighting falls somewhere in the 2000K to 6500K range.
     */
    public Builder setColorTemperature(float temperature) {
      final float[] rgbColor = Colors.cct(temperature);
      setColor(new Color(rgbColor[0], rgbColor[1], rgbColor[2]));
      return this;
    }

    /**
     * Sets the light intensity which determines how bright the light is in Lux (lx) or Lumens (lm)
     * (depending on the light type). Larger values produce brighter lights and near zero values
     * generate very little light. A household light bulb will generally have an intensity between
     * 800 - 2500 lm whereas sunlight will be around 120,000 lx. There is no absolute upper bound
     * but values larger than sunlight (120,000 lx) are generally not needed.
     *
     * @param intensity the intensity of the light, values greater than one are valid. The intensity
     *     will be clamped and cannot be zero or negative. For directional lights the default is 420
     *     lx. For other other lights the default is 2500 lm.
     */
    public Builder setIntensity(float intensity) {
      this.intensity = intensity;
      return this;
    }

    /**
     * Sets the range that the light intensity falls off to zero. This has no affect on infinite
     * light types - the Directional types.
     *
     * @param falloffRadius the light radius in world units, default is 10.0f.
     */
    public Builder setFalloffRadius(float falloffRadius) {
      this.falloffRadius = falloffRadius;
      return this;
    }

    /**
     * Spotlights shine light in a cone, this value determines the size of the inner part of the
     * cone. The intensity is interpolated between the inner and outer cone angles - meaning if they
     * are the same than the cone is perfectly sharp. Generally you will want the inner cone to be
     * smaller than the outer cone to avoid aliasing.
     *
     * @param coneInner inner cone angle in radians, default is 0.5
     */
    public Builder setInnerConeAngle(float coneInner) {
      this.spotlightConeInner = coneInner;
      return this;
    }

    /**
     * Spotlights shine light in a cone, this value determines the size of the outer part of the
     * cone. The intensity is interpolated between the inner and outer cone angles - meaning if they
     * are the same than the cone is perfectly sharp. Generally you will want the inner cone to be
     * smaller than the outer cone to avoid aliasing.
     *
     * @param coneOuter outer cone angle in radians, default is 0.6
     */
    public Builder setOuterConeAngle(float coneOuter) {
      this.spotlightConeOuter = coneOuter;
      return this;
    }

    /** Creates a new {@link Light} based on the parameters set previously */
    public Light build() {
      Light light = new Light(this);
      if (light == null) {
        throw new AssertionError("Allocating a new light failed.");
      }
      return light;
    }
  }

  /**
   * package-private function to add listeners so that light instances can be updated when light
   * parameters change.
   */
  void addChangedListener(LightChangedListener listener) {
    changedListeners.add(listener);
  }

  /** package-private function to remove change listeners. */
  void removeChangedListener(LightChangedListener listener) {
    changedListeners.remove(listener);
  }

  private Light(Builder builder) {
    this.type = builder.type;
    this.enableShadows = builder.enableShadows;
    this.position = builder.position;
    this.direction = builder.direction;
    this.color = builder.color;
    this.intensity = builder.intensity;
    this.falloffRadius = builder.falloffRadius;
    this.spotlightConeInner = builder.spotlightConeInner;
    this.spotlightConeOuter = builder.spotlightConeOuter;
  }

  private void fireChangedListeners() {
    for (LightChangedListener listener : changedListeners) {
      listener.onChange();
    }
  }
}
