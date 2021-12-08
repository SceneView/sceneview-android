package com.google.ar.sceneform.rendering;

import android.opengl.EGLContext;
import androidx.annotation.Nullable;
import com.google.android.filament.Engine;
import com.google.android.filament.Filament;
import com.google.android.filament.gltfio.Gltfio;


import com.google.android.filament.utils.Utils;
import com.google.ar.sceneform.utilities.Preconditions;

/**
 * Store a single Filament Engine instance.
 *
 * @hide
 */
public class EngineInstance {
  @Nullable private static IEngine engine = null;
  @Nullable private static EGLContext glContext = null;
  private static boolean headlessEngine = false;
  private static boolean filamentInitialized = false;

  public static void enableHeadlessEngine() {
    headlessEngine = true;
  }

  public static void disableHeadlessEngine() {
    headlessEngine = false;
  }

  public static boolean isHeadlessMode() {
    return headlessEngine;
  }

  /**
   * Get the Filament Engine instance, creating it if necessary.
   *
   * @throws IllegalStateException
   */
  public static IEngine getEngine() {
    if (!headlessEngine) {
      createEngine();
    } else {
      createHeadlessEngine();
    }
    if (engine == null) {
      throw new IllegalStateException("Filament Engine creation has failed.");
    }
    return engine;
  }

  
  private static Engine createSharedFilamentEngine() {return null;}







  private static Engine createFilamentEngine() {
    Engine result = createSharedFilamentEngine();
    if (result == null) {
      glContext = GLHelper.makeContext();
      result = Engine.create(glContext);
    }
    return result;
  }

  
  private static boolean destroySharedFilamentEngine() {return false;}




  private static void destroyFilamentEngine() {
    if (engine != null) {
      if (headlessEngine || !destroySharedFilamentEngine()) {
        if (glContext != null) {
          GLHelper.destroyContext(glContext);
          glContext = null;
        }
        Preconditions.checkNotNull(engine).destroy();
      }
      engine = null;
    }
  }

  
  private static boolean loadUnifiedJni() {return false;}



  
  private static void gltfioInit() {
    Gltfio.init();
    filamentInitialized = true;
  }

  /**
   * Create the engine and GL Context if they have not been created yet.
   *
   * @throws IllegalStateException
   */
  private static void createEngine() {
    if (engine == null) {

      if (!filamentInitialized) {
        try {
          gltfioInit();
        } catch (UnsatisfiedLinkError err) {
          // Fallthrough and allow regular Filament to initialize.
        }
      }
      if (!filamentInitialized) {
        try {
          Filament.init();
          filamentInitialized = true;
        } catch (UnsatisfiedLinkError err) {
          // For Scene Viewer Filament's jni is included in another lib, try that before failing.
          if (loadUnifiedJni()) {
            filamentInitialized = true;
          } else {
            throw err;
          }
        }
      }

      Utils.INSTANCE.init();

      engine = new FilamentEngineWrapper(createFilamentEngine());

      // Validate that the Engine and GL Context are valid.
      if (engine == null) {
        throw new IllegalStateException("Filament Engine creation has failed.");
      }
    }
  }

  /** Create a Swiftshader engine for testing. */
  private static void createHeadlessEngine() {
    if (engine == null) {
      try {
        engine = new HeadlessEngineWrapper();
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException("Filament Engine creation failed due to reflection error", e);
      }
      if (engine == null) {
        throw new IllegalStateException("Filament Engine creation has failed.");
      }
    }
  }

  public static void destroyEngine() {
    destroyFilamentEngine();
  }

  public static boolean isEngineDestroyed() {
    return engine == null;
  }

  private static native Object nCreateEngine();

  private static native void nDestroyEngine();
}
