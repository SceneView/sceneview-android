package com.google.ar.sceneform.rendering;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.google.android.filament.Engine;
import io.github.sceneview.collision.Preconditions;

import java.util.ArrayList;

/**
 * Used to render an android view to a native open GL texture that can then be rendered by open GL.
 *
 * <p>To correctly draw a hardware accelerated animated view to a surface texture, the view MUST be
 * attached to a window and drawn to a real DisplayListCanvas, which is a hidden class. To achieve
 * this, the following is done:
 *
 * <ul>
 *   <li>Attach RenderViewToSurfaceTexture to the WindowManager.
 *   <li>Override dispatchDraw.
 *   <li>Call super.dispatchDraw with the real DisplayListCanvas
 *   <li>Draw the clear color the DisplayListCanvas so that it isn't visible on screen.
 *   <li>Draw the view to the SurfaceTexture every frame. This must be done every frame, because the
 *       view will not be marked as dirty when child views are animating when hardware accelerated.
 * </ul>
 *
 * @hide
 */

public class RenderViewToExternalTexture extends FrameLayout {
  /** Interface definition for a callback to be invoked when the size of the view changes. */
  public interface OnViewSizeChangedListener {
    void onViewSizeChanged(int width, int height);
  }

  private final View view;
  private final ExternalTexture externalTexture;
  private final Picture picture = new Picture();
  private boolean hasDrawnToSurfaceTexture = false;

  @Nullable private ViewAttachmentManager viewAttachmentManager;
  private final ArrayList<OnViewSizeChangedListener> onViewSizeChangedListeners = new ArrayList<>();

  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  RenderViewToExternalTexture(Engine engine, Context context, View view) {
    super(context);
    Preconditions.checkNotNull(view, "Parameter \"view\" was null.");

    externalTexture = new ExternalTexture(engine);

    this.view = view;
    addView(view);
  }

  /**
   * Register a callback to be invoked when the size of the view changes.
   *
   * @param onViewSizeChangedListener the listener to attach
   */
  void addOnViewSizeChangedListener(OnViewSizeChangedListener onViewSizeChangedListener) {
    if (!onViewSizeChangedListeners.contains(onViewSizeChangedListener)) {
      onViewSizeChangedListeners.add(onViewSizeChangedListener);
    }
  }

  /**
   * Remove a callback to be invoked when the size of the view changes.
   *
   * @param onViewSizeChangedListener the listener to remove
   */
  void removeOnViewSizeChangedListener(OnViewSizeChangedListener onViewSizeChangedListener) {
    onViewSizeChangedListeners.remove(onViewSizeChangedListener);
  }

  ExternalTexture getExternalTexture() {
    return externalTexture;
  }

  public boolean isViewTextureReady() {
    return externalTexture.getFilamentStream().getTimestamp() > 0;
  }

  boolean hasDrawnToSurfaceTexture() {
    return hasDrawnToSurfaceTexture;
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    externalTexture.getSurfaceTexture().setDefaultBufferSize(view.getWidth(), view.getHeight());
  }

  @Override
  public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    for (OnViewSizeChangedListener onViewSizeChangedListener : onViewSizeChangedListeners) {
      onViewSizeChangedListener.onViewSizeChanged(width, height);
    }
  }

  @Override
  public void dispatchDraw(Canvas canvas) {
    // Sanity that the surface is valid.
    Surface targetSurface = externalTexture.getSurface();
    if (!targetSurface.isValid()) {
      return;
    }

    if (view.isDirty()) {
        Canvas pictureCanvas = picture.beginRecording(view.getWidth(), view.getHeight());
        pictureCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        super.dispatchDraw(pictureCanvas);
        picture.endRecording();

        Canvas surfaceCanvas = targetSurface.lockCanvas(null);
        picture.draw(surfaceCanvas);
        targetSurface.unlockCanvasAndPost(surfaceCanvas);

        hasDrawnToSurfaceTexture = true;
    }

    invalidate();
  }

  public void attachView(ViewAttachmentManager viewAttachmentManager) {
    if (this.viewAttachmentManager != null) {
      if (this.viewAttachmentManager != viewAttachmentManager) {
        throw new IllegalStateException(
            "Cannot use the same ViewRenderable with multiple SceneViews.");
      }

      return;
    }

    this.viewAttachmentManager = viewAttachmentManager;
    viewAttachmentManager.addView(this);
  }

  void detachView() {
    if (viewAttachmentManager != null) {
      viewAttachmentManager.removeView(this);
      viewAttachmentManager = null;
    }
  }

  void destroy() {
    detachView();

    externalTexture.destroy();
  }
}
