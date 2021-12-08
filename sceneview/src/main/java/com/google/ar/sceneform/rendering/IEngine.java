package com.google.ar.sceneform.rendering;

import com.google.android.filament.Camera;
import com.google.android.filament.Engine;
import com.google.android.filament.Entity;
import com.google.android.filament.Fence;
import com.google.android.filament.IndexBuffer;
import com.google.android.filament.IndirectLight;
import com.google.android.filament.LightManager;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.NativeSurface;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.Scene;
import com.google.android.filament.Skybox;
import com.google.android.filament.Stream;
import com.google.android.filament.SwapChain;
import com.google.android.filament.TransformManager;
import com.google.android.filament.VertexBuffer;
import com.google.android.filament.View;

/** Engine interface to support multiple flavors of the getFilamentEngine filament engine. */
public interface IEngine {

  Engine getFilamentEngine();

  boolean isValid();

  void destroy();

  // SwapChain

  /** Valid surface types: - Android: Surface - Other: none */
  SwapChain createSwapChain(Object surface);

  /**
   * Valid surface types: - Android: Surface - Other: none
   *
   * <p>Flags: see CONFIG flags in SwapChain.
   *
   * @see SwapChain#CONFIG_DEFAULT
   * @see SwapChain#CONFIG_TRANSPARENT
   * @see SwapChain#CONFIG_READABLE
   */
  SwapChain createSwapChain(Object surface, long flags);

  SwapChain createSwapChainFromNativeSurface(NativeSurface surface, long flags);

  void destroySwapChain(SwapChain swapChain);

  // View

  View createView();

  void destroyView(View view);

  // Renderer

  com.google.android.filament.Renderer createRenderer();

  void destroyRenderer(com.google.android.filament.Renderer renderer);

  // Camera

  Camera createCamera();

  Camera createCamera(@Entity int entity);

  // Scene

  Scene createScene();

  void destroyScene(Scene scene);

  // Stream

  void destroyStream(Stream stream);

  // Fence

  Fence createFence();

  void destroyFence(Fence fence);

  // others...

  void destroyIndexBuffer(IndexBuffer indexBuffer);

  void destroyVertexBuffer(VertexBuffer vertexBuffer);

  void destroyIndirectLight(IndirectLight ibl);

  void destroyMaterial(com.google.android.filament.Material material);

  void destroyMaterialInstance(MaterialInstance materialInstance);

  void destroySkybox(Skybox skybox);

  void destroyTexture(com.google.android.filament.Texture texture);

  void destroyEntity(@Entity int entity);

  // Managers

  TransformManager getTransformManager();

  LightManager getLightManager();

  RenderableManager getRenderableManager();

  void flushAndWait();
}
