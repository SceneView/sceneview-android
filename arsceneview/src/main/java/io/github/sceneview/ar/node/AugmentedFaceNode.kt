package io.github.sceneview.ar.node

import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import com.google.android.filament.VertexBuffer.AttributeType
import com.google.android.filament.VertexBuffer.VertexAttribute.POSITION
import com.google.android.filament.VertexBuffer.VertexAttribute.TANGENTS
import com.google.android.filament.VertexBuffer.VertexAttribute.UV0
import com.google.ar.core.AugmentedFace
import com.google.ar.core.AugmentedFace.RegionType
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import io.github.sceneview.node.MeshNode

/**
 * AR Augmented Face positioned 3D model node
 *
 * Describes a face detected by ARCore and provides methods to access additional center and face
 * region poses as well as face mesh related data.
 *
 * Augmented Faces supports front-facing (selfie) camera only, and does not support attaching
 * anchors nor raycast hit testing. [Trackable.createAnchor] will result in an
 * `IllegalStateException`.
 *
 * To use Augmented Faces, enable the feature in the session. This can be done at session creation
 * time, or at any time during session runtime:
 *
 * ```
 * Session session = new Session(context, EnumSet.of(Session.Feature.FRONT_CAMERA));
 * Config config = ...
 * config.setAugmentedFaceMode(AugmentedFaceMode.MESH3D);
 * session.configure(config);
 * }
 * ```
 *
 * When Augmented Face mode is enabled, ARCore updates the list of detected faces for each frame.
 * Use [Session.getAllTrackables] and [Trackable.getTrackingState] to get a list of faces that have
 * valid meshes that can be rendered.
 *
 * ```
 * for (AugmentedFace face : session.getAllTrackables(AugmentedFace.class)) {
 *   if (face.getTrackingState() == TrackingState.TRACKING) {
 *     // Render face mesh ...
 *   }
 * }
 * }
 * ```
 *
 * Faces provide static mesh data that does not change during the session, as well as pose and mesh
 * data that is updated each frame:
 *
 * ```
 * // UVs and indices can be cached as they do not change during the session.
 * FloatBuffer uvs = face.getMeshTextureCoordinates();
 * ShortBuffer indices = face.getMeshTriangleIndices();
 *
 * // Center and region poses, mesh vertices, and normals are updated each frame.
 * Pose facePose = face.getCenterPose();
 * FloatBuffer faceVertices = face.getMeshVertices();
 * FloatBuffer faceNormals = face.getMeshNormals();
 * }
 * ```
 */
open class AugmentedFaceNode(
    engine: Engine,
    val augmentedFace: AugmentedFace,
    meshMaterialInstance: MaterialInstance? = null,
    builder: RenderableManager.Builder.() -> Unit = {},
    onTrackingStateChanged: ((TrackingState) -> Unit)? = null,
    onUpdated: ((AugmentedFace) -> Unit)? = null
) : TrackableNode<AugmentedFace>(
    engine = engine,
    onTrackingStateChanged = onTrackingStateChanged,
    onUpdated = onUpdated
) {

    /**
     * The center of the face, defined to have the origin located behind the nose and between the
     * two cheek bones.
     *
     * Z+ is forward out of the nose, Y+ is upwards, and X+ is towards the left.
     * The units are in meters. When the face trackable state is TRACKING, this pose is synced with
     * the latest frame. When face trackable state is PAUSED, an identity pose will be returned.
     *
     * Use [regionNodes] to retrieve poses of specific regions of the face.
     */
    val centerNode = PoseNode(engine).apply { parent = this@AugmentedFaceNode }

    val meshNode = MeshNode(
        engine = engine,
        primitiveType = PrimitiveType.TRIANGLES,
        vertexBuffer = VertexBuffer.Builder()
            // Position + Normals + UV Coordinates
            .bufferCount(3)
            // Position Attribute (x, y, z)
            .attribute(POSITION, 0, AttributeType.FLOAT3)
            // Tangents Attribute (Quaternion: x, y, z, w)
            .attribute(TANGENTS, 1, AttributeType.FLOAT4)
            .normalized(TANGENTS)
            // Uv Attribute (x, y)
            .attribute(UV0, 2, AttributeType.FLOAT2)
            .build(engine).apply {
                setBufferAt(engine, 2, augmentedFace.meshTextureCoordinates)
            },
        indexBuffer = IndexBuffer.Builder()
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .indexCount(augmentedFace.meshTriangleIndices.limit())
            .build(engine).apply {
                setBuffer(engine, augmentedFace.meshTriangleIndices)
            },
        materialInstance = meshMaterialInstance,
        builder = builder
    ).apply { parent = centerNode }

    /**
     * The region nodes at the tip of the nose, the detected face's left side of the forehead,
     * the detected face's right side of the forehead.
     *
     * Defines face regions to query the pose for. Left and right are defined relative to the person
     * that the mesh belongs to. To retrieve the center pose use [AugmentedFace.getCenterPose].
     */
    val regionNodes = RegionType.values().associateWith {
        PoseNode(engine).apply { parent = this@AugmentedFaceNode }
    }

    init {
        trackable = augmentedFace
    }

    override fun update(trackable: AugmentedFace?) {
        super.update(trackable)

        if (augmentedFace.trackingState == TrackingState.TRACKING) {
            centerNode.pose = augmentedFace.centerPose
            meshNode.vertexBuffer.setBufferAt(engine, 0, augmentedFace.meshVertices)
            meshNode.vertexBuffer.setBufferAt(engine, 1, augmentedFace.meshTextureCoordinates)
            regionNodes.forEach { (regionType, regionNode) ->
                regionNode.pose = augmentedFace.getRegionPose(regionType)
            }
        }
    }
}