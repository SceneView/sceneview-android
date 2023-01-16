package com.google.ar.sceneform.utilities;

/**
 * Used to identify when the state of an object has changed by incrementing an integer id. Other
 * classes can determine when this object has changed by polling to see if the id has changed.
 *
 * <p>This is useful as an alternative to an event listener subscription model when there is no safe
 * point in the lifecycle of an object to unsubscribe from the event listeners. Unlike event
 * listeners, this cannot cause memory leaks.
 *
 * @hide
 */
public class ChangeId {
  public static final int EMPTY_ID = 0;

  private int id = EMPTY_ID;

  public int get() {
    return id;
  }

  public boolean isEmpty() {
    return id == EMPTY_ID;
  }

  public boolean checkChanged(int id) {
    return this.id != id && !isEmpty();
  }

  public void update() {
    id++;

    // Skip EMPTY_ID if the id has cycled all the way around.
    if (id == EMPTY_ID) {
      id++;
    }
  }
}
