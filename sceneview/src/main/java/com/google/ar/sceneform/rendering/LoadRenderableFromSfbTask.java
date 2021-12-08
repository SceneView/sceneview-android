package com.google.ar.sceneform.rendering;

import android.net.Uri;
import androidx.annotation.Nullable;
import android.util.Log;
import com.google.android.filament.IndexBuffer;
import com.google.android.filament.TextureSampler;
import com.google.android.filament.VertexBuffer;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.SceneformBundle.VersionException;
import com.google.ar.sceneform.utilities.Preconditions;
import com.google.ar.sceneform.utilities.SceneformBufferUtils;
import com.google.ar.schemas.lull.ModelDef;
import com.google.ar.schemas.lull.ModelIndexRange;
import com.google.ar.schemas.lull.ModelInstanceDef;

import com.google.ar.schemas.lull.Vec3;
import com.google.ar.schemas.lull.VertexAttribute;
import com.google.ar.schemas.lull.VertexAttributeType;
import com.google.ar.schemas.lull.VertexAttributeUsage;
import com.google.ar.schemas.sceneform.BoolInit;
import com.google.ar.schemas.sceneform.BoolVec2Init;
import com.google.ar.schemas.sceneform.BoolVec3Init;
import com.google.ar.schemas.sceneform.BoolVec4Init;
import com.google.ar.schemas.sceneform.CompiledMaterialDef;
import com.google.ar.schemas.sceneform.IntInit;
import com.google.ar.schemas.sceneform.IntVec2Init;
import com.google.ar.schemas.sceneform.IntVec3Init;
import com.google.ar.schemas.sceneform.IntVec4Init;
import com.google.ar.schemas.sceneform.ParameterDef;
import com.google.ar.schemas.sceneform.ParameterInitDef;
import com.google.ar.schemas.sceneform.ParameterInitDefType;
import com.google.ar.schemas.sceneform.SamplerDef;
import com.google.ar.schemas.sceneform.SamplerInit;
import com.google.ar.schemas.sceneform.ScalarInit;
import com.google.ar.schemas.sceneform.SceneformBundleDef;
import com.google.ar.schemas.sceneform.TransformDef;
import com.google.ar.schemas.sceneform.Vec2Init;
import com.google.ar.schemas.sceneform.Vec3Init;
import com.google.ar.schemas.sceneform.Vec4Init;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Task for initializing a Renderable with data from an SFB. */
@SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"}) // CompletableFuture
class LoadRenderableFromSfbTask<T extends Renderable> {
  private static class ModelTexture {
    String name;
    @Nullable Texture data;

    ModelTexture(String name) {
      this.name = name;
      this.data = null;
    }
  }

  private static final String TAG = LoadRenderableFromSfbTask.class.getSimpleName();
  private final T renderable;
  private final RenderableInternalData renderableData;
  @Nullable private final Uri renderableUri;

  private ModelDef modelDef;
  private ModelInstanceDef modelInstanceDef;
  private TransformDef transformDef;

  private int meshCount;
  private int textureCount;

  private int vertexCount;
  private int vertexStride;

  private int indexCount;
  private IndexBuffer.Builder.IndexType indexType;
  private ByteBuffer vertexBufferData;
  private ByteBuffer indexBufferData;

  private final ArrayList<ModelTexture> textures = new ArrayList<>();
  private final ArrayList<Material> compiledMaterials = new ArrayList<>();
  private final ArrayList<Integer> compiledMaterialIndex = new ArrayList<>();
  private final ArrayList<MaterialParameters> materialParameters = new ArrayList<>();
  private final ArrayList<String> materialNames = new ArrayList<>();

  private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
  private static final int BYTES_PER_SHORT = 2;
  private static final int BYTES_PER_INT = 4;

  LoadRenderableFromSfbTask(T renderable, @Nullable Uri renderableUri) {
    this.renderable = renderable;
    IRenderableInternalData data = renderable.getRenderableData();
    if (data instanceof RenderableInternalData) {
      this.renderableData = (RenderableInternalData) data;
    } else {
      throw new IllegalStateException("Expected task type " + TAG);
    }
    this.renderableUri = renderableUri;
  }

  /**
   * @param inputStreamCreator supplies {@link Renderable} in serialized format
   * @return {@link CompletableFuture} for a new {@link Renderable}
   */
  public CompletableFuture<T> downloadAndProcessRenderable(
      Callable<InputStream> inputStreamCreator) {

    CompletableFuture<T> result =
        CompletableFuture.supplyAsync(
                // Download byte buffer via thread pool
                () -> {
                  ByteBuffer assetData =
                      SceneformBufferUtils.inputStreamToByteBuffer(inputStreamCreator);

                  // Parse byte buffer via thread pool
                  SceneformBundleDef sfb = byteBufferToSfb(assetData);
                  setCollisionShape(sfb);
                  // Create sub-assets including material parameters, textures and geometry
                  loadModel(sfb);
                  return sfb;
                },
                ThreadPools.getThreadPoolExecutor())
            .thenComposeAsync(
                sfb -> {
                  loadAnimations(sfb);

                  // Load textures and wait for them to finish.
                  return loadTexturesAsync(sfb);
                },
                ThreadPools.getMainExecutor())
            .thenApplyAsync(
                sfb -> {
                  // Fill in the material parameters. could be done on another thread, but kept here
                  // to reduce switching.
                  buildMaterialParameters(sfb);
                  return setupFilament(sfb);
                },
                ThreadPools.getMainExecutor());

    result.exceptionally(
        // Log Exception if there was one.
        throwable -> {
          throw new CompletionException(throwable);
        });

    return result;
  }

  
  private void loadAnimations(SceneformBundleDef sfb) {return ;}

























  private SceneformBundleDef byteBufferToSfb(ByteBuffer assetData) {
    try {
      SceneformBundleDef sfb;
      sfb = SceneformBundle.tryLoadSceneformBundle(assetData);
      if (sfb != null) {
        return sfb;
      }
    } catch (VersionException e) {
      throw new CompletionException(e);
    }
    throw new AssertionError("No RCB file at uri: " + renderableUri);
  }

  private SceneformBundleDef setCollisionShape(SceneformBundleDef sfb) {
    try {
      renderable.collisionShape = SceneformBundle.readCollisionGeometry(sfb);
      return sfb;
    } catch (IOException e) {
      throw new CompletionException("Unable to get collision geometry from sfb", e);
    }
  }

  private SceneformBundleDef loadModel(SceneformBundleDef sfb) {
    // Prepare the flatbuffer data
    transformDef = sfb.transform();
    modelDef = sfb.model();
    Preconditions.checkNotNull(modelDef, "Model error: ModelDef is invalid.");

    modelInstanceDef = modelDef.lods(0);
    Preconditions.checkNotNull(modelInstanceDef, "Lull Model error: ModelInstanceDef is invalid.");

    // The data buffers for Geometry have to stick around anyway, so go ahead and load them
    // now. The Filament buffers will be created in createAssetFromBuffer()
    buildGeometry();
    return sfb;
  }

  private T setupFilament(SceneformBundleDef sfb) {
    Preconditions.checkNotNull(sfb);
    setupFilamentGeometryBuffers();
    setupFilamentMaterials(sfb);
    setupRenderableData();
    renderable.getId().update();
    return renderable;
  }

  private void setupFilamentGeometryBuffers() {
    IEngine engine = EngineInstance.getEngine();

    IndexBuffer indexBuffer =
        new IndexBuffer.Builder()
            .indexCount(indexCount)
            .bufferType(indexType)
            .build(engine.getFilamentEngine());
    indexBuffer.setBuffer(engine.getFilamentEngine(), indexBufferData);
    renderableData.setIndexBuffer(indexBuffer);

    VertexBuffer.Builder vertexBufferBuilder =
        new VertexBuffer.Builder().vertexCount(vertexCount).bufferCount(1);

    int vertexAttributeCount = modelInstanceDef.vertexAttributesLength();
    int byteOffset = 0;
    for (int i = 0; i < vertexAttributeCount; i++) {
      VertexAttribute attribute = modelInstanceDef.vertexAttributes(i);
      VertexBuffer.VertexAttribute filamentAttribute =
          getFilamentVertexAttribute(attribute.usage());
      if (filamentAttribute != null) {
        vertexBufferBuilder.attribute(
            filamentAttribute,
            0,
            getFilamentAttributeType(attribute.type()),
            byteOffset,
            vertexStride);
        if (isAttributeNormalized(attribute.usage())) {
          vertexBufferBuilder.normalized(filamentAttribute);
        }
      }

      byteOffset += getVertexAttributeTypeSizeInBytes(attribute.type());
    }

    VertexBuffer vertexBuffer = vertexBufferBuilder.build(engine.getFilamentEngine());
    vertexBuffer.setBufferAt(engine.getFilamentEngine(), 0, vertexBufferData);
    renderableData.setVertexBuffer(vertexBuffer);

    setupAnimation();
  }

  
  private void setupAnimation() {return ;}









  private void setupFilamentMaterials(SceneformBundleDef sfb) {
    int compiledMaterialLength = sfb.compiledMaterialsLength();

    for (int i = 0; i < compiledMaterialLength; ++i) {
      CompiledMaterialDef compiledMaterial = sfb.compiledMaterials(i);

      // If the same material buffer exists in multiple places this will ensure we
      // only load it into graphics memory once.
      int materialId = compiledMaterial.compiledMaterialAsByteBuffer().hashCode();

      // use the registry to get the material or create it if needed
      ByteBuffer copy;
      try {
        copy = SceneformBufferUtils.copyByteBuffer(compiledMaterial.compiledMaterialAsByteBuffer());
      } catch (IOException e) {
        throw new CompletionException("Failed to create material", e);
      }

      CompletableFuture<Material> materialFuture =
          Material.builder().setSource(copy).setRegistryId(materialId).build();

      @SuppressWarnings("nullness")
      Material material = materialFuture.getNow(null);

      // Material should always be loaded immediately because the source is a raw byte buffer.
      if (material == null) {
        throw new AssertionError("Material wasn't loaded.");
      }

      compiledMaterials.add(material);
    }
  }

  private void setupRenderableData() {
    // Get the bounds.
    final Vec3 modelMinAabb = modelDef.boundingBox().min();
    final Vector3 minAabb = new Vector3(modelMinAabb.x(), modelMinAabb.y(), modelMinAabb.z());
    final Vec3 modelMaxAabb = modelDef.boundingBox().max();
    final Vector3 maxAabb = new Vector3(modelMaxAabb.x(), modelMaxAabb.y(), modelMaxAabb.z());
    Vector3 extentsAabb = Vector3.subtract(maxAabb, minAabb).scaled(0.5f);
    Vector3 centerAabb = Vector3.add(minAabb, extentsAabb);
    renderableData.setExtentsAabb(extentsAabb);
    renderableData.setCenterAabb(centerAabb);
    // Finding a scale of 0 indicates a default-initialized (i.e. invalid) structure.
    if (transformDef != null && transformDef.scale() != 0.0f) {
      Vec3 modelOffset = transformDef.offset();
      Vector3 offset = new Vector3(modelOffset.x(), modelOffset.y(), modelOffset.z());
      renderableData.setTransformScale(transformDef.scale());
      renderableData.setTransformOffset(offset);
    }

    ArrayList<Material> materialBindings = renderable.getMaterialBindings();
    ArrayList<String> renderableMaterialNames = renderable.getMaterialNames();
    materialBindings.clear();
    renderableMaterialNames.clear();
    for (int m = 0; m < meshCount; ++m) {
      final ModelIndexRange range = modelInstanceDef.ranges(m);
      final int start = (int) range.start();
      final int end = (int) range.end();

      int materialIndex = compiledMaterialIndex.get(m);
      Material material = compiledMaterials.get(materialIndex).makeCopy();
      MaterialParameters params = materialParameters.get(m);
      material.copyMaterialParameters(params);

      RenderableInternalData.MeshData meshData = new RenderableInternalData.MeshData();
      materialBindings.add(material);
      renderableMaterialNames.add(materialNames.get(m));
      meshData.indexStart = start;
      meshData.indexEnd = end;
      renderableData.getMeshes().add(meshData);
    }
  }

  private void buildGeometry() {
    ByteBuffer vertexData = modelInstanceDef.vertexDataAsByteBuffer();

    Preconditions.checkNotNull(
        vertexData, "Model Instance geometry data is invalid (vertexData is null).");

    int vertexDataCount = modelInstanceDef.vertexDataLength();
    meshCount = modelInstanceDef.rangesLength();

    int bytesPerVertex = LullModel.getByteCountPerVertex(modelInstanceDef);
    vertexCount = vertexDataCount / bytesPerVertex;

    // TODO: Fix crash in filament when using flatbuffer buffers directly.
    if (modelInstanceDef.indices32Length() > 0) {
      // 32 bit indices
      indexCount = modelInstanceDef.indices32Length();
      indexType = IndexBuffer.Builder.IndexType.UINT;
      indexBufferData = ByteBuffer.allocateDirect(indexCount * BYTES_PER_INT);
      indexBufferData.put(modelInstanceDef.indices32AsByteBuffer());
    } else if (modelInstanceDef.indices16Length() > 0) {
      // 16 bit indices
      indexCount = modelInstanceDef.indices16Length();
      indexType = IndexBuffer.Builder.IndexType.USHORT;
      indexBufferData = ByteBuffer.allocateDirect(indexCount * BYTES_PER_SHORT);
      indexBufferData.put(modelInstanceDef.indices16AsByteBuffer());
    } else {
      throw new AssertionError(
          "Model Instance geometry data is invalid (model has no index data).");
    }
    indexBufferData.flip();

    vertexBufferData = ByteBuffer.allocateDirect(vertexData.remaining());
    Preconditions.checkNotNull(vertexBufferData, "Failed to allocate geometry for FilamentModel.");

    vertexBufferData.put(vertexData);
    vertexBufferData.flip();

    // Calculate vertex stride
    vertexStride = 0;
    int vertexAttributeCount = modelInstanceDef.vertexAttributesLength();
    for (int i = 0; i < vertexAttributeCount; i++) {
      VertexAttribute attribute = modelInstanceDef.vertexAttributes(i);
      vertexStride += getVertexAttributeTypeSizeInBytes(attribute.type());

      // TODO: check all attributes available.
    }
  }

  // TODO: Return a future for all texture loads, use theComposeAsync to
  // combine it in downloadAndProcessRenderable
  private CompletableFuture<SceneformBundleDef> loadTexturesAsync(SceneformBundleDef sfb) {
    textureCount = sfb.samplersLength();

    CompletableFuture<?>[] textureFutures = new CompletableFuture<?>[textureCount];

    for (int t = 0; t < textureCount; ++t) {
      final SamplerDef samplerDef = sfb.samplers(t);
      ModelTexture texture = new ModelTexture(samplerDef.name());
      textures.add(texture);
      CompletableFuture<Texture> textureFuture = null;

      int rawUsage = samplerDef.params().usageType();
      Texture.Usage[] usageValues = Texture.Usage.values();
      if (rawUsage >= usageValues.length) {
        throw new AssertionError("Invalid Texture Usage: " + rawUsage);
      }
      Texture.Usage usage = usageValues[rawUsage];

      if (samplerDef.dataLength() != 0) {
        // loading texture from RCB
        ByteBuffer data = samplerDef.dataAsByteBuffer();
        // BUG(b/74619992): An extra copy to input stream is made here to avoid a JNI crash
        ByteArrayInputStream wrappedInputStream =
            new ByteArrayInputStream(data.array(), data.arrayOffset(), data.capacity());
        // position the stream to the image buffer
        boolean premultiplyAlpha = (usage == Texture.Usage.COLOR_MAP);
        wrappedInputStream.skip(data.position());
        // TODO: The registryId should be populated with a sha1sum

        textureFuture =
            Texture.builder()
                .setUsage(usage)
                .setSampler(samplerDefToSampler(samplerDef))
                .setPremultiplied(premultiplyAlpha)
                .setSource(
                    () -> {
                      Preconditions.checkNotNull(wrappedInputStream);
                      return wrappedInputStream;
                    })
                .build();
      } else {
        throw new IllegalStateException("Unable to load texture, no sampler definition.");
      }

      textureFutures[t] =
          textureFuture
              .thenAccept(textureData -> texture.data = textureData)
              .exceptionally(
                  throwable -> {
                    throw new CompletionException("Texture Load Error", throwable);
                  });
    }

    CompletableFuture<Void> allTexturesFuture = CompletableFuture.allOf(textureFutures);

    return allTexturesFuture.thenApply((unused) -> sfb);
  }

  private static Texture.Sampler samplerDefToSampler(SamplerDef samplerDef) {
    Texture.Sampler.WrapMode wrapModeR =
        filamentWrapModeToWrapMode(TextureSampler.WrapMode.values()[samplerDef.params().wrapR()]);
    Texture.Sampler.WrapMode wrapModeS =
        filamentWrapModeToWrapMode(TextureSampler.WrapMode.values()[samplerDef.params().wrapS()]);
    Texture.Sampler.WrapMode wrapModeT =
        filamentWrapModeToWrapMode(TextureSampler.WrapMode.values()[samplerDef.params().wrapT()]);

    return Texture.Sampler.builder()
        .setMinFilter(samplerDefToMinFilter(samplerDef))
        .setMagFilter(samplerDefToMagFilter(samplerDef))
        .setWrapModeR(wrapModeR)
        .setWrapModeS(wrapModeS)
        .setWrapModeT(wrapModeT)
        .build();
  }

  private SceneformBundleDef buildMaterialParameters(SceneformBundleDef sfb) {
    int materialsCount = sfb.materialsLength();
    if (materialsCount == 0) {
      Log.i(TAG, "Building materials but the sceneform bundle has no materials");
      return sfb;
    }

    for (int m = 0; m < meshCount; ++m) {

      // material to submesh mapping is generally 1:1
      int materialIndex = m;
      // if submesh count exceeds the material count
      // use that last material
      if (materialsCount <= m) {
        materialIndex = materialsCount - 1;
      }

      com.google.ar.schemas.sceneform.MaterialDef materialDef = sfb.materials(materialIndex);

      if (materialDef == null) {
        Log.e(TAG, "Material " + m + " is null.");
        continue;
      }

      // map the parameters to the compiled material
      compiledMaterialIndex.add(materialDef.compiledIndex());

      // flatbuffers supports in-place methods for getting values,
      // creating cache to hold those values before copying to parameter
      ParameterDef parameterCache = new ParameterDef();
      ParameterInitDef parameterInitCache = new ParameterInitDef();
      ScalarInit scalarCache = new ScalarInit();
      Vec2Init vec2Cache = new Vec2Init();
      Vec3Init vec3Cache = new Vec3Init();
      Vec4Init vec4Cache = new Vec4Init();
      BoolInit boolCache = new BoolInit();
      BoolVec2Init bool2Cache = new BoolVec2Init();
      BoolVec3Init bool3Cache = new BoolVec3Init();
      BoolVec4Init bool4Cache = new BoolVec4Init();
      IntInit intCache = new IntInit();
      IntVec2Init int2Cache = new IntVec2Init();
      IntVec3Init int3Cache = new IntVec3Init();
      IntVec4Init int4Cache = new IntVec4Init();
      SamplerInit samplerCache = new SamplerInit();

      MaterialParameters materialParameters = new MaterialParameters();

      int paramCount = materialDef.parametersLength();
      for (int i = 0; i < paramCount; ++i) {
        materialDef.parameters(parameterCache, i);
        parameterCache.initialValue(parameterInitCache);

        String id = parameterCache.id();
        byte parameterType = parameterInitCache.initType();
        switch (parameterType) {
          case ParameterInitDefType.NullInit:
            // Nothing to do
            break;
          case ParameterInitDefType.ScalarInit:
            parameterInitCache.init(scalarCache);
            materialParameters.setFloat(id, scalarCache.value());
            break;
          case ParameterInitDefType.Vec2Init:
            parameterInitCache.init(vec2Cache);
            materialParameters.setFloat2(id, vec2Cache.x(), vec2Cache.y());
            break;
          case ParameterInitDefType.Vec3Init:
            parameterInitCache.init(vec3Cache);
            materialParameters.setFloat3(id, vec3Cache.x(), vec3Cache.y(), vec3Cache.z());
            break;
          case ParameterInitDefType.Vec4Init:
            parameterInitCache.init(vec4Cache);
            materialParameters.setFloat4(
                id, vec4Cache.x(), vec4Cache.y(), vec4Cache.z(), vec4Cache.w());
            break;
          case ParameterInitDefType.BoolInit:
            parameterInitCache.init(boolCache);
            materialParameters.setBoolean(id, boolCache.value());
            break;
          case ParameterInitDefType.BoolVec2Init:
            parameterInitCache.init(bool2Cache);
            materialParameters.setBoolean2(id, bool2Cache.x(), bool2Cache.y());
            break;
          case ParameterInitDefType.BoolVec3Init:
            parameterInitCache.init(bool3Cache);
            materialParameters.setBoolean3(id, bool3Cache.x(), bool3Cache.y(), bool3Cache.z());
            break;
          case ParameterInitDefType.BoolVec4Init:
            parameterInitCache.init(bool4Cache);
            materialParameters.setBoolean4(
                id, bool4Cache.x(), bool4Cache.y(), bool4Cache.z(), bool4Cache.w());
            break;
          case ParameterInitDefType.IntInit:
            parameterInitCache.init(intCache);
            materialParameters.setInt(id, intCache.value());
            break;
          case ParameterInitDefType.IntVec2Init:
            parameterInitCache.init(int2Cache);
            materialParameters.setInt2(id, int2Cache.x(), int2Cache.y());
            break;
          case ParameterInitDefType.IntVec3Init:
            parameterInitCache.init(int3Cache);
            materialParameters.setInt3(id, int3Cache.x(), int3Cache.y(), int3Cache.z());
            break;
          case ParameterInitDefType.IntVec4Init:
            parameterInitCache.init(int4Cache);
            materialParameters.setInt4(
                id, int4Cache.x(), int4Cache.y(), int4Cache.z(), int4Cache.w());
            break;
          case ParameterInitDefType.SamplerInit:
            parameterInitCache.init(samplerCache);
            String path = samplerCache.path();
            Texture texture = getTextureByName(path);
            if (texture != null) {
              materialParameters.setTexture(id, texture);
            }
            break;
          case ParameterInitDefType.ExternalSamplerInit:
            // No-op; handled externally from this loader.
            break;
          default:
            Log.e(TAG, "Unknown parameter type: " + id);
        }
      }

      this.materialParameters.add(materialParameters);
      String materialName = materialDef.name();
      this.materialNames.add(materialName != null ? materialName : "");
    }
    return sfb;
  }

  @Nullable
  private Texture getTextureByName(String name) {
    for (int t = 0; t < textureCount; ++t) {
      if (Objects.equals(name, textures.get(t).name)) {
        return textures.get(t).data;
      }
    }
    return null;
  }

  private static int getVertexAttributeTypeSizeInBytes(int attributeType) {
    int sizeInBytes = 0;
    switch (attributeType) {
      case VertexAttributeType.Empty:
        sizeInBytes = 0;
        break;
      case VertexAttributeType.Scalar1f:
        sizeInBytes = BYTES_PER_FLOAT;
        break;
      case VertexAttributeType.Vec2f:
        sizeInBytes = 2 * BYTES_PER_FLOAT;
        break;
      case VertexAttributeType.Vec3f:
        sizeInBytes = 3 * BYTES_PER_FLOAT;
        break;
      case VertexAttributeType.Vec4f:
        sizeInBytes = 4 * BYTES_PER_FLOAT;
        break;
      case VertexAttributeType.Vec2us:
        sizeInBytes = 2 * BYTES_PER_SHORT;
        break;
      case VertexAttributeType.Vec4us:
        sizeInBytes = 4 * BYTES_PER_SHORT;
        break;
      case VertexAttributeType.Vec4ub:
        sizeInBytes = 4;
        break;
      default:
        throw new AssertionError("Unsupported VertexAttributeType value: " + attributeType);
    }
    return sizeInBytes;
  }

  private boolean isAttributeNormalized(int attributeUsage) {
    return attributeUsage == VertexAttributeUsage.Color
        || attributeUsage == VertexAttributeUsage.BoneWeights;
  }

  @Nullable
  private static VertexBuffer.VertexAttribute getFilamentVertexAttribute(int attributeUsage) {
    VertexBuffer.VertexAttribute filamentAttribute;
    switch (attributeUsage) {
      case VertexAttributeUsage.Position:
        filamentAttribute = VertexBuffer.VertexAttribute.POSITION;
        break;
      case VertexAttributeUsage.Color:
        filamentAttribute = VertexBuffer.VertexAttribute.COLOR;
        break;
      case VertexAttributeUsage.TexCoord:
        filamentAttribute = VertexBuffer.VertexAttribute.UV0;
        break;
      case VertexAttributeUsage.Orientation:
        filamentAttribute = VertexBuffer.VertexAttribute.TANGENTS;
        break;
      case VertexAttributeUsage.BoneIndices:
        filamentAttribute = VertexBuffer.VertexAttribute.BONE_INDICES;
        break;
      case VertexAttributeUsage.BoneWeights:
        filamentAttribute = VertexBuffer.VertexAttribute.BONE_WEIGHTS;
        break;
      default:
        filamentAttribute = null;
        break;
    }
    return filamentAttribute;
  }

  private static VertexBuffer.AttributeType getFilamentAttributeType(int attributeType) {
    VertexBuffer.AttributeType filamentAttributeType;
    switch (attributeType) {
      case VertexAttributeType.Scalar1f:
        filamentAttributeType = VertexBuffer.AttributeType.FLOAT;
        break;
      case VertexAttributeType.Vec2f:
        filamentAttributeType = VertexBuffer.AttributeType.FLOAT2;
        break;
      case VertexAttributeType.Vec3f:
        filamentAttributeType = VertexBuffer.AttributeType.FLOAT3;
        break;
      case VertexAttributeType.Vec4f:
        filamentAttributeType = VertexBuffer.AttributeType.FLOAT4;
        break;
      case VertexAttributeType.Vec2us:
        filamentAttributeType = VertexBuffer.AttributeType.USHORT2;
        break;
      case VertexAttributeType.Vec4us:
        filamentAttributeType = VertexBuffer.AttributeType.USHORT4;
        break;
      case VertexAttributeType.Vec4ub:
        filamentAttributeType = VertexBuffer.AttributeType.UBYTE4;
        break;
      default:
        throw new AssertionError("Unsupported VertexAttributeType value: " + attributeType);
    }
    return filamentAttributeType;
  }

  private static Texture.Sampler.MagFilter samplerDefToMagFilter(SamplerDef samplerDef) {
    TextureSampler.MagFilter filamentMagFilter =
        TextureSampler.MagFilter.values()[samplerDef.params().magFilter()];

    switch (filamentMagFilter) {
      case NEAREST:
        return Texture.Sampler.MagFilter.NEAREST;
      case LINEAR:
        return Texture.Sampler.MagFilter.LINEAR;
    }
    throw new IllegalArgumentException("Invalid MagFilter");
  }

  private static Texture.Sampler.MinFilter samplerDefToMinFilter(SamplerDef samplerDef) {
    TextureSampler.MinFilter filamentMinFilter =
        TextureSampler.MinFilter.values()[samplerDef.params().minFilter()];

    switch (filamentMinFilter) {
      case NEAREST:
        return Texture.Sampler.MinFilter.NEAREST;
      case LINEAR:
        return Texture.Sampler.MinFilter.LINEAR;
      case NEAREST_MIPMAP_NEAREST:
        return Texture.Sampler.MinFilter.NEAREST_MIPMAP_NEAREST;
      case LINEAR_MIPMAP_NEAREST:
        return Texture.Sampler.MinFilter.LINEAR_MIPMAP_NEAREST;
      case NEAREST_MIPMAP_LINEAR:
        return Texture.Sampler.MinFilter.NEAREST_MIPMAP_LINEAR;
      case LINEAR_MIPMAP_LINEAR:
        return Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR;
    }
    throw new IllegalArgumentException("Invalid MinFilter");
  }

  private static Texture.Sampler.WrapMode filamentWrapModeToWrapMode(
      TextureSampler.WrapMode wrapMode) {
    switch (wrapMode) {
      case CLAMP_TO_EDGE:
        return Texture.Sampler.WrapMode.CLAMP_TO_EDGE;
      case REPEAT:
        return Texture.Sampler.WrapMode.REPEAT;
      case MIRRORED_REPEAT:
        return Texture.Sampler.WrapMode.MIRRORED_REPEAT;
    }
    throw new IllegalArgumentException("Invalid WrapMode");
  }
}
