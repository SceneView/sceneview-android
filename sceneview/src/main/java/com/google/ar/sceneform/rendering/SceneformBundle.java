package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.collision.CollisionShape;
import com.google.ar.sceneform.collision.Sphere;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.schemas.sceneform.CollisionShapeType;
import com.google.ar.schemas.sceneform.SceneformBundleDef;
import com.google.ar.schemas.sceneform.SuggestedCollisionShapeDef;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Helper functions for loading and processing rendercore bundles.
 *
 * @hide
 */
public final class SceneformBundle {
  private static final String TAG = SceneformBundle.class.getSimpleName();
  // TODO: This 'version range' is too narrow
  public static final float RCB_MAJOR_VERSION = 0.54f;
  public static final int RCB_MINOR_VERSION = 2;
  private static final char[] RCB_SIGNATURE = {'R', 'B', 'U', 'N'};
  // Per flatbuffer documentation, a buffer signature is written to
  // bytes 4 - 7 inclusively.
  private static final int SIGNATURE_OFFSET = 4;

  static class VersionException extends Exception {
    public VersionException(String message) {
      super(message);
    }
  }

  @Nullable
  public static SceneformBundleDef tryLoadSceneformBundle(ByteBuffer buffer)
      throws VersionException {

    // Test the file signature to see if this is a real Rendercore Bundle.
    if (isSceneformBundle(buffer)) {
      buffer.rewind();
      SceneformBundleDef bundle = SceneformBundleDef.getRootAsSceneformBundleDef(buffer);
      float majorVersion = bundle.version().majorVersion();
      int minorVersion = bundle.version().minorVersion();
      if (RCB_MAJOR_VERSION < bundle.version().majorVersion()) {
        throw new VersionException(
            "Sceneform bundle (.sfb) version not supported, max version supported is "
                + RCB_MAJOR_VERSION
                + ".X. Version requested for loading is "
                + majorVersion
                + "."
                + minorVersion);
      }
      return bundle;
    }

    return null;
  }

  public static CollisionShape readCollisionGeometry(SceneformBundleDef rcb) throws IOException {
    SuggestedCollisionShapeDef shape = rcb.suggestedCollisionShape();
    int type = shape.type();
    switch (type) {
      case CollisionShapeType.Box:
        Box box = new Box();
        box.setCenter(new Vector3(shape.center().x(), shape.center().y(), shape.center().z()));
        box.setSize(new Vector3(shape.size().x(), shape.size().y(), shape.size().z()));
        return box;
      case CollisionShapeType.Sphere:
        Sphere sphere = new Sphere();
        sphere.setCenter(new Vector3(shape.center().x(), shape.center().y(), shape.center().z()));
        sphere.setRadius(shape.size().x());
        return sphere;
      default:
        throw new IOException("Invalid collisionCollisionGeometry type.");
    }
  }

  public static boolean isSceneformBundle(ByteBuffer buffer) {
    for (int i = 0; i < RCB_SIGNATURE.length; ++i) {
      if (buffer.get(SIGNATURE_OFFSET + i) != RCB_SIGNATURE[i]) {
        return false;
      }
    }
    return true;
  }
}
