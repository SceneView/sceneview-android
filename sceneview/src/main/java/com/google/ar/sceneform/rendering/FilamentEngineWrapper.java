package com.google.ar.sceneform.rendering;

import com.google.android.filament.Camera;
import com.google.android.filament.Engine;
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

/** Wraps calls to Filament engine. */
public class FilamentEngineWrapper implements IEngine {

  final Engine engine;

  public FilamentEngineWrapper(Engine engine) {
    this.engine = engine;
  }

  @Override
  public Engine getFilamentEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return engine.isValid();
  }

  @Override
  public void destroy() {
    engine.destroy();
  }

  @Override
  public SwapChain createSwapChain(Object surface) {
    return engine.createSwapChain(surface);
  }

  @Override
  public SwapChain createSwapChain(Object surface, long flags) {
    return engine.createSwapChain(surface, flags);
  }

  @Override
  public SwapChain createSwapChainFromNativeSurface(NativeSurface surface, long flags) {
    return engine.createSwapChainFromNativeSurface(surface, flags);
  }

  @Override
  public void destroySwapChain(SwapChain swapChain) {
    engine.destroySwapChain(swapChain);
  }

  @Override
  public View createView() {
    return engine.createView();
  }

  @Override
  public void destroyView(View view) {
    engine.destroyView(view);
  }

  @Override
  public com.google.android.filament.Renderer createRenderer() {
    return engine.createRenderer();
  }

  @Override
  public void destroyRenderer(com.google.android.filament.Renderer renderer) {
    engine.destroyRenderer(renderer);
  }

  @Override
  public Camera createCamera() {
    return engine.createCamera(engine.getEntityManager().create());
  }

  @Override
  public Camera createCamera(int entity) {
    return engine.createCamera(entity);
  }

  @Override
  public Scene createScene() {
    return engine.createScene();
  }

  @Override
  public void destroyScene(Scene scene) {
    engine.destroyScene(scene);
  }

  @Override
  public void destroyStream(Stream stream) {
    engine.destroyStream(stream);
  }

  @Override
  public Fence createFence() {
    return engine.createFence();
  }

  @Override
  public void destroyFence(Fence fence) {
    engine.destroyFence(fence);
  }

  @Override
  public void destroyIndexBuffer(IndexBuffer indexBuffer) {
    engine.destroyIndexBuffer(indexBuffer);
  }

  @Override
  public void destroyVertexBuffer(VertexBuffer vertexBuffer) {
    engine.destroyVertexBuffer(vertexBuffer);
  }

  @Override
  public void destroyIndirectLight(IndirectLight ibl) {
    engine.destroyIndirectLight(ibl);
  }

  @Override
  public void destroyMaterial(com.google.android.filament.Material material) {
    engine.destroyMaterial(material);
  }

  @Override
  public void destroyMaterialInstance(MaterialInstance materialInstance) {
    engine.destroyMaterialInstance(materialInstance);
  }

  @Override
  public void destroySkybox(Skybox skybox) {
    engine.destroySkybox(skybox);
  }

  @Override
  public void destroyTexture(com.google.android.filament.Texture texture) {
    engine.destroyTexture(texture);
  }

  @Override
  public void destroyEntity(int entity) {
    engine.destroyEntity(entity);
  }

  @Override
  public TransformManager getTransformManager() {
    return engine.getTransformManager();
  }

  @Override
  public LightManager getLightManager() {
    return engine.getLightManager();
  }

  @Override
  public RenderableManager getRenderableManager() {
    return engine.getRenderableManager();
  }

  @Override
  public void flushAndWait() {
    engine.flushAndWait();
  }
}
