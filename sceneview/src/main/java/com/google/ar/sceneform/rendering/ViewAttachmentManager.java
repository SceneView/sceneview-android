package com.google.ar.sceneform.rendering;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Manages a {@link FrameLayout} that is attached directly to a {@link WindowManager} that other
 * views can be added and removed from.
 *
 * <p>To render a {@link View}, the {@link View} must be attached to a {@link WindowManager} so that
 * it can be properly drawn. This class encapsulates a {@link FrameLayout} that is attached to a
 * {@link WindowManager} that other views can be added to as children. This allows us to safely and
 * correctly draw the {@link View} associated with {@link ViewRenderable}'s while keeping them
 * isolated from the rest of the activities View hierarchy.
 *
 * <p>Additionally, this manages the lifecycle of the window to help ensure that the window is
 * added/removed from the WindowManager at the appropriate times.
 *
 * @hide
 */
// TODO: Create Unit Tests for this class.
public class ViewAttachmentManager {
  // View that owns the ViewAttachmentManager.
  // Used to post callbacks onto the UI thread.
  private final View ownerView;

  private final WindowManager windowManager;
  private final WindowManager.LayoutParams windowLayoutParams;

  private final FrameLayout frameLayout;
  private final ViewGroup.LayoutParams viewLayoutParams;

  private static final String VIEW_RENDERABLE_WINDOW = "ViewRenderableWindow";

  /**
   * Construct a ViewAttachmentManager.
   *
   * @param ownerView used by the ViewAttachmentManager to post callbacks on the UI thread
   */
  public ViewAttachmentManager(Context context, View ownerView) {
    this.ownerView = ownerView;

    windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    windowLayoutParams = createWindowLayoutParams();

    frameLayout = new FrameLayout(context);
    viewLayoutParams = createViewLayoutParams();
  }

  FrameLayout getFrameLayout() {
    return frameLayout;
  }

  public void onResume() {
    // A ownerView can only be added to the WindowManager after the activity has finished resuming.
    // Therefore, we must use post to ensure that the window is only added after resume is finished.
    ownerView.post(
        () -> {
          if (frameLayout.getParent() == null && ownerView.isAttachedToWindow()) {
            windowManager.addView(frameLayout, windowLayoutParams);
          }
        });
  }

  public void onPause() {
    // The ownerView must be removed from the WindowManager before the activity is destroyed, or the
    // window will be leaked. Therefore we add/remove the ownerView in resume/pause.
    if (frameLayout.getParent() != null) {
      windowManager.removeView(frameLayout);
    }
  }

  /**
   * Add a ownerView as a child of the {@link FrameLayout} that is attached to the {@link
   * WindowManager}.
   *
   * <p>Used by {@link RenderViewToExternalTexture} to ensure that the ownerView is drawn with all
   * appropriate lifecycle events being called correctly.
   */
  public void addView(View view) {
    if (view.getParent() == frameLayout) {
      return;
    }

    frameLayout.addView(view, viewLayoutParams);
  }

  /**
   * Remove a ownerView from the {@link FrameLayout} that is attached to the {@link WindowManager}.
   *
   * <p>Used by {@link RenderViewToExternalTexture} to remove ownerView's that no longer need to be
   * drawn.
   */
  public void removeView(View view) {
    if (view.getParent() != frameLayout) {
      return;
    }

    frameLayout.removeView(view);
  }

  private static WindowManager.LayoutParams createWindowLayoutParams() {
    WindowManager.LayoutParams params =
        new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT);
    params.setTitle(VIEW_RENDERABLE_WINDOW);

    return params;
  }

  private static ViewGroup.LayoutParams createViewLayoutParams() {
    ViewGroup.LayoutParams params =
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    return params;
  }
}
