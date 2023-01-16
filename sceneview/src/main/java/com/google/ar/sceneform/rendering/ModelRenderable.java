package com.google.ar.sceneform.rendering;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.ar.sceneform.resources.ResourceRegistry;
import com.google.ar.sceneform.utilities.AndroidPreconditions;


/**
 * Renders a 3D Model by attaching it to a {@link com.google.ar.sceneform.Node} with {@link
 * com.google.ar.sceneform.Node#setRenderable(Renderable)}.
 *
 * <pre>{@code
 * future = ModelRenderable.builder().setSource(context, R.raw.renderable).build();
 * renderable = future.thenAccept(...);
 * }</pre>
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class ModelRenderable extends Renderable {
  



  



  private ModelRenderable(Builder builder) {
    super(builder);
  }

  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  private ModelRenderable(ModelRenderable other) {
    super(other);

    copyAnimationFrom(other);
  }

  













  














  












  
  private void copyAnimationFrom(ModelRenderable other) {return ;}








  /**
   * Creates a new instance of this ModelRenderable.
   *
   * <p>The new renderable will have unique copy of all mutable state. All materials referenced by
   * the ModelRenderable will also be instanced. Immutable data will be shared between the
   * instances.
   */
  @Override
  public ModelRenderable makeCopy() {
    return new ModelRenderable(this);
  }

  /** Constructs a {@link ModelRenderable}. */
  public static Builder builder() {
    AndroidPreconditions.checkMinAndroidApiLevel();
    return new Builder();
  }

  





  














  














  







  













  




  




  /** Factory class for {@link ModelRenderable}. */
  public static final class Builder extends Renderable.Builder<ModelRenderable, Builder> {

    /** @hide */
    @Override
    protected ModelRenderable makeRenderable() {
      return new ModelRenderable(this);
    }

    /** @hide */
    @Override
    protected Class<ModelRenderable> getRenderableClass() {
      return ModelRenderable.class;
    }

    /** @hide */
    @Override
    protected ResourceRegistry<ModelRenderable> getRenderableRegistry() {
      return ResourceManager.getInstance().getModelRenderableRegistry();
    }

    /** @hide */
    @Override
    protected Builder getSelf() {
      return this;
    }
  }
}
