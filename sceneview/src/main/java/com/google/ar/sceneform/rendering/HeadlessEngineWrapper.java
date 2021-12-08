package com.google.ar.sceneform.rendering;

import androidx.annotation.NonNull;
import com.google.android.filament.Engine;
import com.google.android.filament.NativeSurface;
import com.google.android.filament.SwapChain;
import com.google.ar.sceneform.utilities.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Interface for the swiftshader backed version of the Filament engine. */
public class HeadlessEngineWrapper extends FilamentEngineWrapper {
  public static final String TAG = HeadlessEngineWrapper.class.getName();

  long nativeHandle;
  private static final Constructor<SwapChain> swapChainInit;
  private static final Constructor<Engine> engineInit;
  private static final Method getNativeEngineMethod;
  private static final Method getNativeSwapChainMethod;

  static {
    try {
      getNativeSwapChainMethod = SwapChain.class.getDeclaredMethod("getNativeObject");
      swapChainInit = SwapChain.class.getDeclaredConstructor(long.class, Object.class);
      getNativeEngineMethod = Engine.class.getDeclaredMethod("getNativeObject");
      engineInit = Engine.class.getDeclaredConstructor(long.class);
      getNativeSwapChainMethod.setAccessible(true);
      swapChainInit.setAccessible(true);
      getNativeEngineMethod.setAccessible(true);
      engineInit.setAccessible(true);
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't get native getters", e);
    }
  }

  public HeadlessEngineWrapper() throws ReflectiveOperationException {
    super(engineInit.newInstance(nCreateSwiftShaderEngine()));
  }

  @Override
  public void destroy() {
    try {
      Long nativeEngineHandle = (Long) getNativeEngineMethod.invoke(engine);
      nDestroySwiftShaderEngine(Preconditions.checkNotNull(nativeEngineHandle));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SwapChain createSwapChain(@NonNull Object surface) {
    try {
      Long nativeEngineHandle = (Long) getNativeEngineMethod.invoke(engine);
      @SuppressWarnings("nullness:assignment.type.incompatible") // b/140537868
      @NonNull
      Object fakeSurface = null;
      return swapChainInit.newInstance(
          nCreateSwiftShaderSwapChain(Preconditions.checkNotNull(nativeEngineHandle), 0),
          fakeSurface);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SwapChain createSwapChain(@NonNull Object surface, long flags) {
    try {
      Long nativeEngineHandle = (Long) getNativeEngineMethod.invoke(engine);
      @SuppressWarnings("nullness:assignment.type.incompatible") // b/140537868
      @NonNull
      Object fakeSurface = null;
      return swapChainInit.newInstance(
          nCreateSwiftShaderSwapChain(Preconditions.checkNotNull(nativeEngineHandle), flags),
          fakeSurface);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SwapChain createSwapChainFromNativeSurface(@NonNull NativeSurface surface, long flags) {
    try {
      Long nativeEngineHandle = (Long) getNativeEngineMethod.invoke(engine);
      @SuppressWarnings("nullness:assignment.type.incompatible") // b/140537868
      @NonNull
      Object fakeSurface = null;
      return swapChainInit.newInstance(
          nCreateSwiftShaderSwapChain(Preconditions.checkNotNull(nativeEngineHandle), flags),
          fakeSurface);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void destroySwapChain(@NonNull SwapChain swapChain) {
    try {
      Long nativeEngineHandle = (Long) getNativeEngineMethod.invoke(engine);
      Long swapChainHandle = (Long) getNativeSwapChainMethod.invoke(swapChain);
      nDestroySwiftShaderSwapChain(
          Preconditions.checkNotNull(nativeEngineHandle),
          Preconditions.checkNotNull(swapChainHandle));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  // LINT.IfChange(api)
  private static native long nCreateSwiftShaderEngine();

  private static native void nDestroySwiftShaderEngine(long nativeEngine);

  private static native long nCreateSwiftShaderSwapChain(long nativeEngine, long flags);

  private static native void nDestroySwiftShaderSwapChain(long nativeEngine, long nativeSwapChain);
  // LINT.ThenChange(
  //
  // //depot/google3/third_party/arcore/ar/sceneform/viewer/swiftshader/platform_swiftshader_jni.cc:api
  // )
}
