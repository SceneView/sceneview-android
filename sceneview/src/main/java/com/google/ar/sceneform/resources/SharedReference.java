package com.google.ar.sceneform.resources;

/**
 * Used for managing memory of shared object using reference counting.
 *
 * @hide
 */
public abstract class SharedReference {
  private int referenceCount = 0;

  public void retain() {
    referenceCount++;
  }

  public void release() {
    referenceCount--;
    dispose();
  }

  protected abstract void onDispose();

  private void dispose() {
    if (referenceCount > 0) {
      return;
    }

    onDispose();
  }
}
