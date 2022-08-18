/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.ux;

import android.content.Context;
import android.net.Uri;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import com.google.android.filament.TransformManager;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.AugmentedFace.RegionType;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableDefinition;
import com.google.ar.sceneform.rendering.RenderableDefinition.Submesh;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.rendering.Vertex;
import com.google.ar.sceneform.rendering.Vertex.UvCoordinate;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import io.github.sceneview.Filament;
import io.github.sceneview.SceneView;
import io.github.sceneview.ar.arcore.PoseKt;
import io.github.sceneview.ar.node.ArNode;
import io.github.sceneview.ar.node.PlacementMode;
import io.github.sceneview.node.ModelNode;
import io.github.sceneview.utils.FrameTime;

/**
 * Node used to render visual effects on a face with ARCore's {@link AugmentedFace} feature.
 *
 * <p>The visual effects can consist of up to two components: The face mesh and the face regions
 * renderable.
 *
 * <p>The face mesh is used for visualizing 2d images mapped to the surface of someone's face. The
 * face regions renderable is used for visualizing 3d objects (e.g. fox nose and ears) that will
 * move with the regions of the face that are tracked by ARCore.
 *
 * <p>When creating a face regions Renderable, the regions are mapped to bones in the model. Use the
 * provided sample .fbx file to export a face regions renderable in the correct format.
 *
 * <p>This node is positioned to match the pose from {@link AugmentedFace#getCenterPose}.
 *
 * <p>The visual effects will be disabled when the AugmentedFace isn't tracking or the AugmentedFace
 * is set to null.
 */

public class AugmentedFaceNode extends ArNode {
//    private static final String TAG = AugmentedFaceNode.class.getSimpleName();
//
//    // The augmented face to render visual effects for.
//    @Nullable
//    private AugmentedFace augmentedFace;
//
//    private Lifecycle lifecycle;
//
//    // Fields for nodes.
//    private final ModelNode faceMeshNode;
//    private final ModelNode faceRegionNode;
//
//    // Fields for face mesh renderable.
//    private final ArrayList<Vertex> vertices = new ArrayList<>();
//    private final ArrayList<Integer> triangleIndices = new ArrayList<>();
//    private final ArrayList<Submesh> submeshes = new ArrayList<>();
//    private final RenderableDefinition faceMeshDefinition;
//
//    private final HashMap<RegionType, Integer> faceMeshSkeleton = new HashMap<>();
//
//    private final float[] inverseRootNodeMatrix = new float[16];
//    private final float[] regionPoseMatrix = new float[16];
//    private final float[] matrix = new float[16];
//
//    @Nullable
//    private ModelRenderable faceMeshRenderable;
//    @Nullable
//    private Material defaultFaceMeshMaterial;
//    @Nullable
//    private Material overrideFaceMeshMaterial;
//    @Nullable
//    private Material faceMeshOccluderMaterial;
//
//    @Nullable
//    private Texture faceMeshTexture;
//
//    private static final String FACE_MESH_TEXTURE_MATERIAL_PARAMETER = "texture";
//
//    // Used to help ensure that the face mesh texture is rendered below the face mesh regions.
//    // This helps prevent z-sorting issues with transparent materials.
//    private static final int FACE_MESH_RENDER_PRIORITY =
//            Math.max(Renderable.RENDER_PRIORITY_FIRST, Renderable.RENDER_PRIORITY_DEFAULT - 1);
//
//    /**
//     * Create an AugmentedFaceNode with no AugmentedFace.
//     */
//    @SuppressWarnings({"initialization"})
//    public AugmentedFaceNode(Lifecycle lifecycle) {
//        super();
//
//        this.lifecycle = lifecycle;
//
//        faceMeshNode = new ModelNode();
//        faceMeshNode.setParent(this);
//
//        faceMeshDefinition =
//                RenderableDefinition.builder().setVertices(vertices).setSubmeshes(submeshes).build(lifecycle);
//
//        faceRegionNode = new ModelNode();
//        faceRegionNode.setParent(this);
//    }
//
//    /**
//     * Create an AugmentedFaceNode with the given AugmentedFace.
//     */
//    @SuppressWarnings({"initialization"})
//    public AugmentedFaceNode(Lifecycle lifecycle, AugmentedFace augmentedFace) {
//        this(lifecycle);
//
//        this.augmentedFace = augmentedFace;
//    }
//
//    /**
//     * Returns the AugmentedFace that this Node is applying visual effects to.
//     */
//    @Nullable
//    public AugmentedFace getAugmentedFace() {
//        return augmentedFace;
//    }
//
//    /**
//     * Sets the AugmentedFace that this node is applying visual effects to.
//     */
//    public void setAugmentedFace(@Nullable AugmentedFace face) {
//        augmentedFace = face;
//    }
//
//    /**
//     * Returns the texture rendered on the face mesh. Defaults to null.
//     *
//     * <p>Note: This is only used if the face mesh material hasn't been overridden.
//     */
//    @Nullable
//    public Texture getFaceMeshTexture() {
//        return faceMeshTexture;
//    }
//
//    /**
//     * Sets the texture rendered on the face mesh.
//     *
//     * <p>Note: This is only used if the face mesh material hasn't been overridden.
//     */
//    public void setFaceMeshTexture(@Nullable Texture texture) {
//        faceMeshTexture = texture;
//        updateSubmeshes();
//    }
//
//    /**
//     * Returns the material currently overriding how the face mesh is rendered. Defaults to null.
//     */
//    @Nullable
//    public Material getFaceMeshMaterialOverride() {
//        return overrideFaceMeshMaterial;
//    }
//
//    /**
//     * Sets the material used to render the face mesh. The overriding material will not use {@link
//     * #getFaceMeshTexture()}. Set back to null to revert to the default material.
//     */
//    public void setFaceMeshMaterialOverride(@Nullable Material material) {
//        overrideFaceMeshMaterial = material;
//        updateSubmeshes();
//    }
//
//    public RenderableInstance setFaceRegionsRenderable(ModelRenderable renderable) {
//        faceRegionNode.setModel(renderable);
//        RenderableInstance renderableInstance = faceRegionNode.getModelInstance();
//        updateSubmeshes();
//        extractBonesFromRenderable();
//        return renderableInstance;
//    }
//
//    private void extractBonesFromRenderable() {
//        if (!faceMeshSkeleton.isEmpty()) {
//            faceMeshSkeleton.clear();
//        }
//
//        for (RegionType type : RegionType.values()) {
//            String boneName = boneNameForRegion(type);
//            int entity = faceRegionNode.getModelInstance().getFilamentAsset().getFirstEntityByName(boneName);
//            if (entity == 0) {
//                Log.w(TAG, "Face mesh model is missing bone " + boneName + ". Tracking might not be accurate");
//                continue;
//            }
//            faceMeshSkeleton.put(type, entity);
//        }
//    }
//
//    @Nullable
//    public ModelRenderable getFaceRegionsRenderable() {
//        Renderable renderable = faceRegionNode.getModel();
//        if (renderable != null && !(renderable instanceof ModelRenderable)) {
//            throw new IllegalStateException("Face Regions Renderable must be a ModelRenderable.");
//        }
//
//        return (ModelRenderable) renderable;
//    }
//
//    @Override
//    public void onAttachedToScene(@NonNull SceneView sceneView) {
//        super.onAttachedToScene(sceneView);
//
//        Context context = sceneView.getContext();
//
//        // Face mesh material
//        Material.builder()
//                .setSource(context, Uri.parse("sceneview/materials/face_mesh.filamat"))
//                .build(lifecycle)
//                .handle((material, throwable) -> {
//                    if (throwable != null) {
//                        Log.e(TAG, "Unable to load face mesh material.", throwable);
//                        return false;
//                    }
//                    defaultFaceMeshMaterial = material;
//                    updateSubmeshes();
//                    return true;
//                });
//
//        // Face mesh occluder material
//        Material.builder()
//                .setSource(context, Uri.parse("sceneview/materials/face_mesh_occluder.filamat"))
//                .build(lifecycle)
//                .handle((material, throwable) -> {
//                    if (throwable != null) {
//                        Log.e(TAG, "Unable to load face mesh occluder material.", throwable);
//                        return false;
//                    }
//
//                    faceMeshOccluderMaterial = material;
//                    updateSubmeshes();
//                    return true;
//                });
//    }
//
//    @Override
//    public void onFrame(FrameTime frameTime) {
//        super.onFrame(frameTime);
//
//        boolean isTracking = isTracking();
//
//        // Only render the visual effects when the augmented face is tracking.
//        faceMeshNode.setVisible(isTracking);
//        faceRegionNode.setVisible(isTracking);
//
//        if (isTracking) {
//            updateTransform();
//            updateRegionNodes();
//            updateFaceMesh();
//        }
//    }
//
//    private void updateTransform() {
//        // Update this node to be positioned at the center pose of the face.
//        Pose centerPose = checkNotNull(augmentedFace).getCenterPose();
//        setPosition(PoseKt.getPosition(centerPose));
//        setQuaternion(PoseKt.getQuaternion(centerPose));
//    }
//
//    private void updateRegionNodes() {
//        if (augmentedFace == null) {
//            return;
//        }
//
//        TransformManager tfm = Filament.getTransformManager();
//
//        Matrix.invertM(inverseRootNodeMatrix, 0, getTransformationMatrix().data, 0);
//
//        for (RegionType type : RegionType.values()) {
//            Pose pose = augmentedFace.getRegionPose(type);
//
//            // Log Pose
////            Log.d(TAG, type + " " + pose.toString());
//
//            pose.toMatrix(regionPoseMatrix, 0);
//
//            Matrix.multiplyMM(matrix, 0, inverseRootNodeMatrix, 0, regionPoseMatrix, 0);
//
//            if (faceMeshSkeleton.containsKey(type)) {
//                int instance = tfm.getInstance(faceMeshSkeleton.get(type));
//                tfm.setTransform(instance, matrix);
//                tfm.getWorldTransform(instance, matrix);
//            }
//            // Log Position
////            float[] position = new float[4];
////            Matrix.multiplyMV(position, 0, matrix, 0, new float[] { 0, 0, 0, 1 }, 0);
////            Log.d(TAG, type + " " + Arrays.toString(position));
//        }
//        if (faceRegionNode != null && faceRegionNode.getModelInstance() != null) {
//            faceRegionNode.getModelInstance().getFilamentAsset().getAnimator().updateBoneMatrices();
//        }
//    }
//
//    @Override
//    public boolean isTracking() {
//        return augmentedFace != null && augmentedFace.getTrackingState() == TrackingState.TRACKING;
//    }
//
//    @SuppressWarnings("AndroidJdkLibsChecker")
//    private void updateFaceMesh() {
//        // Wait until the material is loaded.
//        if (defaultFaceMeshMaterial == null || faceMeshOccluderMaterial == null) {
//            return;
//        }
//
//        updateFaceMeshVerticesAndTriangles();
//
//        if (faceMeshRenderable == null) {
//            try {
//                faceMeshRenderable =
//                        ModelRenderable.builder().setSource(checkNotNull(faceMeshDefinition)).build(lifecycle).get();
//                faceMeshRenderable.setRenderPriority(FACE_MESH_RENDER_PRIORITY);
//            } catch (InterruptedException | ExecutionException ex) {
//                Log.e(TAG, "Failed to build faceMeshRenderable from definition", ex);
//            }
//
//            checkNotNull(faceMeshRenderable).setShadowReceiver(false);
//            checkNotNull(faceMeshRenderable).setShadowCaster(false);
//
//            faceMeshNode.setModel(faceMeshRenderable);
//        } else {
//            // Face mesh renderable already exists, so update it to match the face mesh definition.
//            faceMeshRenderable.updateFromDefinition(checkNotNull(faceMeshDefinition));
//        }
//    }
//
//    private void updateFaceMeshVerticesAndTriangles() {
//        AugmentedFace augmentedFace = checkNotNull(this.augmentedFace);
//
//        FloatBuffer verticesBuffer = augmentedFace.getMeshVertices();
//        verticesBuffer.rewind();
//        // Vertices in x, y, z packing.
//        int numVertices = verticesBuffer.limit() / 3;
//
//        FloatBuffer textureCoordsBuffer = augmentedFace.getMeshTextureCoordinates();
//        textureCoordsBuffer.rewind();
//        // Texture coordinates in u, v packing.
//        int numTextureCoords = textureCoordsBuffer.limit() / 2;
//
//        FloatBuffer normalsBuffer = augmentedFace.getMeshNormals();
//        normalsBuffer.rewind();
//        // Normals in x, y, z packing.
//        int numNormals = normalsBuffer.limit() / 3;
//
//        if (numVertices != numTextureCoords || numVertices != numNormals) {
//            throw new IllegalStateException(
//                    "AugmentedFace must have the same number of vertices, normals, and texture coordinates.");
//        }
//
//        vertices.ensureCapacity(numVertices);
//
//        for (int i = 0; i < numVertices; i++) {
//            // position.
//            float vX = verticesBuffer.get();
//            float vY = verticesBuffer.get();
//            float vZ = verticesBuffer.get();
//
//            // Normal.
//            float nX = normalsBuffer.get();
//            float nY = normalsBuffer.get();
//            float nZ = normalsBuffer.get();
//
//            // Uv coordinate.
//            float u = textureCoordsBuffer.get();
//            float v = textureCoordsBuffer.get();
//
//            if (i < vertices.size()) {
//                // Re-use existing vertex.
//                Vertex vertex = vertices.get(i);
//
//                Vector3 vertexPos = checkNotNull(vertex.getPosition());
//                vertexPos.set(vX, vY, vZ);
//
//                Vector3 normal = checkNotNull(vertex.getNormal());
//                normal.set(nX, nY, nZ);
//
//                UvCoordinate uvCoord = checkNotNull(vertex.getUvCoordinate());
//                uvCoord.x = u;
//                uvCoord.y = v;
//            } else {
//                // Create new vertex.
//                Vertex vertex =
//                        Vertex.builder()
//                                .setPosition(new Vector3(vX, vY, vZ))
//                                .setNormal(new Vector3(nX, nY, nZ))
//                                .setUvCoordinate(new UvCoordinate(u, v))
//                                .build();
//
//                vertices.add(vertex);
//            }
//        }
//
//        // Remove any extra vertices. In practice, this shouldn't happen.
//        // The number of vertices remains the same each frame.
//        while (vertices.size() > numVertices) {
//            vertices.remove(vertices.size() - 1);
//        }
//
//        ShortBuffer indicesBuffer = augmentedFace.getMeshTriangleIndices();
//        indicesBuffer.rewind();
//
//        // Only do this if the size doesn't match.
//        // The triangle indices of the face mesh don't change from frame to frame.
//        if (triangleIndices.size() != indicesBuffer.limit()) {
//            triangleIndices.clear();
//            triangleIndices.ensureCapacity(indicesBuffer.limit());
//
//            while (indicesBuffer.hasRemaining()) {
//                triangleIndices.add((int) indicesBuffer.get());
//            }
//        }
//    }
//
//    private void updateSubmeshes() {
//        Material currentFaceMeshMaterial = getFaceMeshMaterial();
//        if (defaultFaceMeshMaterial == null || currentFaceMeshMaterial == null) {
//            return;
//        }
//
//        Material faceMeshMaterial = checkNotNull(currentFaceMeshMaterial);
//        Material faceMeshOccluderMaterial = checkNotNull(this.faceMeshOccluderMaterial);
//
//        submeshes.clear();
//
//        Submesh occluderSubmesh =
//                Submesh.builder()
//                        .setTriangleIndices(triangleIndices)
//                        .setMaterial(faceMeshOccluderMaterial.filamentMaterialInstance)
//                        .build();
//        submeshes.add(occluderSubmesh);
//
//        if (faceMeshTexture != null) {
//            if (faceMeshMaterial == defaultFaceMeshMaterial) {
//                faceMeshMaterial.setTexture(FACE_MESH_TEXTURE_MATERIAL_PARAMETER, faceMeshTexture);
//            }
//
//            Submesh faceTextureSubmesh =
//                    Submesh.builder()
//                            .setTriangleIndices(triangleIndices)
//                            .setMaterial(faceMeshMaterial.filamentMaterialInstance)
//                            .build();
//            submeshes.add(faceTextureSubmesh);
//        }
//    }
//
//    @Nullable
//    private Material getFaceMeshMaterial() {
//        if (overrideFaceMeshMaterial != null) {
//            return overrideFaceMeshMaterial;
//        }
//
//        return defaultFaceMeshMaterial;
//    }
//
//    private static String boneNameForRegion(RegionType regionType) {
//        return regionType.name();
//    }
//
//    private static <T> T checkNotNull(@Nullable T reference) {
//        if (reference == null) {
//            throw new NullPointerException();
//        }
//
//        return reference;
//    }
}
