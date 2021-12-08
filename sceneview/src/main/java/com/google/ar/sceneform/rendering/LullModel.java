package com.google.ar.sceneform.rendering;

import android.util.Log;
import com.google.android.filament.TextureSampler.MagFilter;
import com.google.android.filament.TextureSampler.MinFilter;
import com.google.android.filament.TextureSampler.WrapMode;
import com.google.ar.schemas.lull.ModelInstanceDef;
import com.google.ar.schemas.lull.TextureFiltering;
import com.google.ar.schemas.lull.VertexAttribute;
import com.google.ar.schemas.lull.VertexAttributeType;
import java.nio.ByteBuffer;

/**
 * Helper functions for loading and processing lull models.
 *
 * @hide
 */
public class LullModel {
  private static final String TAG = LullModel.class.getName();

  // map from lull wrap mode to filament wrap mode
  public static final WrapMode[] fromLullWrapMode =
      new WrapMode[] {
        WrapMode.CLAMP_TO_EDGE, // Maps from lull clamp-to-border (index 0)
        WrapMode.CLAMP_TO_EDGE, // Maps from lull clamp-to-edge (index 0)
        WrapMode.MIRRORED_REPEAT, // Maps from lull mirrored-repeat (index 2)
        WrapMode.CLAMP_TO_EDGE, // Maps from lull mirrored-clamp-to-edge (index 0)
        WrapMode.REPEAT, // Maps from lull repeat (index 4)
      };

  public static boolean isLullModel(ByteBuffer buffer) {
    // LullModel header = 0x12, 0x00, 0x00, 0x00
    final int lullModelHeaderLen = 4;
    return buffer.limit() > lullModelHeaderLen
        && buffer.get(0) < 32
        && buffer.get(1) == 0x00
        && buffer.get(2) == 0x00;
  }

  public static int getByteCountPerVertex(ModelInstanceDef modelInstanceDef) {
    int vertexAttributeCount = modelInstanceDef.vertexAttributesLength();
    int bytesPerVertex = 0;
    for (int i = 0; i < vertexAttributeCount; i++) {
      VertexAttribute attribute = modelInstanceDef.vertexAttributes(i);
      switch (attribute.type()) {
        case VertexAttributeType.Vec3f:
          bytesPerVertex += 12;
          break;
        case VertexAttributeType.Vec4f:
          bytesPerVertex += 16;
          break;
        case VertexAttributeType.Vec2f:
        case VertexAttributeType.Vec4us:
          bytesPerVertex += 8;
          break;
        case VertexAttributeType.Scalar1f:
        case VertexAttributeType.Vec2us:
        case VertexAttributeType.Vec4ub:
          bytesPerVertex += 4;
          break;
        case VertexAttributeType.Empty:
        default:
          break;
      }
    }
    return bytesPerVertex;
  }

  public static MinFilter fromLullToMinFilter(com.google.ar.schemas.lull.TextureDef textureDef) {
    switch (textureDef.minFilter()) {
      case TextureFiltering.Nearest:
        return MinFilter.NEAREST;
      case TextureFiltering.Linear:
        return MinFilter.LINEAR;
      case TextureFiltering.NearestMipmapNearest:
        return MinFilter.NEAREST_MIPMAP_NEAREST;
      case TextureFiltering.LinearMipmapNearest:
        return MinFilter.LINEAR_MIPMAP_NEAREST;
      case TextureFiltering.NearestMipmapLinear:
        return MinFilter.NEAREST_MIPMAP_LINEAR;
      case TextureFiltering.LinearMipmapLinear:
        return MinFilter.LINEAR_MIPMAP_LINEAR;
      default:
        {
          Log.e(TAG, textureDef.name() + ": Sampler has unknown min filter");
        }
    }

    return MinFilter.NEAREST;
  }

  public static MagFilter fromLullToMagFilter(com.google.ar.schemas.lull.TextureDef textureDef) {
    switch (textureDef.magFilter()) {
      case TextureFiltering.Nearest:
        return MagFilter.NEAREST;
      case TextureFiltering.Linear:
        return MagFilter.LINEAR;
      default:
        {
          Log.e(TAG, textureDef.name() + ": Sampler has unknown mag filter");
        }
    }

    return MagFilter.NEAREST;
  }
}
