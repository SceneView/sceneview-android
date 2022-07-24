package com.google.ar.sceneform.rendering;

/**
 * Represents shared data used by {@link ViewRenderable}s for rendering. The data will be released
 * when all {@link ViewRenderable}s using this data are finalized.
 */

public class ViewRenderableInternalData {
  private final RenderViewToExternalTexture renderView;

  public ViewRenderableInternalData(RenderViewToExternalTexture renderView) {
    this.renderView = renderView;
  }

  public RenderViewToExternalTexture getRenderView() {
    return renderView;
  }
}
