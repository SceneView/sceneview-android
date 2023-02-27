package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;

import com.google.android.filament.MaterialInstance;
import com.google.android.filament.TransformManager;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.common.TransformProvider;
import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.RenderableDefinition.Submesh;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.github.sceneview.Filament;
import io.github.sceneview.SceneView;

/**
 * Renders a single ARCore Plane.
 */
public class PlaneVisualizer implements TransformProvider {
    private static final String TAG = PlaneVisualizer.class.getSimpleName();

    private final SceneView sceneView;
    private final Plane plane;

    private final Matrix planeMatrix = new Matrix();

    private boolean isPlaneAddedToScene = false;
    private boolean isEnabled = false;
    private boolean isShadowReceiver = false;
    private boolean isVisible = false;

    @Nullable
    private ModelRenderable planeRenderable = null;
    @Nullable
    private RenderableInstance planeRenderableInstance;

    private final ArrayList<Vertex> vertices = new ArrayList<>();
    private final ArrayList<Integer> triangleIndices = new ArrayList<>();
    private final RenderableDefinition renderableDefinition;
    @Nullable
    private Submesh planeSubmesh;
    @Nullable
    private Submesh shadowSubmesh;

    private static final int VERTS_PER_BOUNDARY_VERT = 2;

    // Feather distance 0.2 meters.
    private static final float FEATHER_LENGTH = 0.2f;

    // Feather scale over the distance between plane center and vertices.
    private static final float FEATHER_SCALE = 0.2f;

    public void setEnabled(boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;
            updatePlane();
        }
    }

    public void setShadowReceiver(boolean shadowReceiver) {
        if (isShadowReceiver != shadowReceiver) {
            isShadowReceiver = shadowReceiver;
            updatePlane();
        }
    }

    public void setVisible(boolean visible) {
        if (isVisible != visible) {
            isVisible = visible;
            updatePlane();
        }
    }

    public PlaneVisualizer(SceneView sceneView, Plane plane) {
        this.sceneView = sceneView;
        this.plane = plane;

        renderableDefinition = RenderableDefinition
                .builder()
                .setVertices(vertices)
                .build();
    }

    public void setShadowMaterial(MaterialInstance materialInstance) {
        if (shadowSubmesh == null) {
            shadowSubmesh = Submesh.builder()
                    .setTriangleIndices(triangleIndices)
                    .setMaterial(materialInstance)
                    .build();
        } else {
            shadowSubmesh.setMaterial(materialInstance);
        }

        if (planeRenderable != null) {
            updateRenderable();
        }
    }

    public void setPlaneMaterial(MaterialInstance materialInstance) {
        if (planeSubmesh == null) {
            planeSubmesh = Submesh.builder()
                    .setTriangleIndices(triangleIndices)
                    .setMaterial(materialInstance)
                    .build();
        } else {
            planeSubmesh.setMaterial(materialInstance);
        }

        if (planeRenderable != null) {
            updateRenderable();
        }
    }

    @Override
    public Matrix getTransformationMatrix() {
        return planeMatrix;
    }

    public void updatePlane() {
        if (!isEnabled || (!isVisible && !isShadowReceiver)) {
            removePlaneFromScene();
            return;
        }

        if (plane.getTrackingState() != TrackingState.TRACKING) {
            removePlaneFromScene();
            return;
        }

        // Set the transformation matrix to the pose of the plane.
        plane.getCenterPose().toMatrix(planeMatrix.data, 0);

        // Calculate the mesh for the plane.
        boolean success = updateRenderableDefinitionForPlane();
        if (!success) {
            removePlaneFromScene();
            return;
        }

        updateRenderable();
        addPlaneToScene();
    }

    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    void updateRenderable() {
        List<Submesh> submeshes = renderableDefinition.getSubmeshes();
        submeshes.clear();

        // the order of the meshes is important here, because we set the blendOrder based on
        // the index below.
        if (isVisible && (planeSubmesh != null)) {
            submeshes.add(planeSubmesh);
        }

        if (isShadowReceiver && (shadowSubmesh != null)) {
            submeshes.add(shadowSubmesh);
        }

        if (submeshes.isEmpty()) {
            removePlaneFromScene();
            return;
        }

        if (planeRenderable == null) {
            try {
                planeRenderable = ModelRenderable.builder()
                        .setSource(renderableDefinition)
                        .build()
                        .get();
                planeRenderable.setShadowCaster(false);
                // Creating a Renderable is immediate when using RenderableDefinition.
            } catch (InterruptedException | ExecutionException ex) {
                throw new AssertionError("Unable to create plane renderable.");
            }
            planeRenderableInstance = planeRenderable.createInstance(this);
        } else {
            planeRenderable.updateFromDefinition(renderableDefinition);
        }

        // The plane must always be drawn before the shadow, we use the blendOrder to enforce that.
        // this works because both sub-meshes will be sorted at the same distance from the camera
        // since they're part of the same renderable. The blendOrder, determines the sorting in
        // that situation.
        if (planeRenderableInstance != null && submeshes.size() > 1) {
            planeRenderableInstance.setBlendOrderAt(0, 0); // plane
            planeRenderableInstance.setBlendOrderAt(1, 1); // shadow
        }
        planeRenderableInstance.prepareForDraw(sceneView);

        TransformManager transformManager = Filament.getTransformManager();
        int instance = transformManager.getInstance(planeRenderableInstance.entity);
        transformManager.setTransform(instance, planeRenderableInstance.getWorldModelMatrix().data);
    }

    public void destroy() {
        removePlaneFromScene();

        if (planeRenderableInstance != null) {
            planeRenderableInstance.destroy();
        }
        planeRenderable = null;
    }

    private void addPlaneToScene() {
        if (isPlaneAddedToScene || planeRenderableInstance == null) {
            return;
        }
        sceneView.addEntity(planeRenderableInstance.getRenderedEntity());

        isPlaneAddedToScene = true;
    }

    private void removePlaneFromScene() {
        if (!isPlaneAddedToScene || planeRenderableInstance == null) {
            return;
        }

        sceneView.removeEntity(planeRenderableInstance.getRenderedEntity());

        isPlaneAddedToScene = false;
    }

    private boolean updateRenderableDefinitionForPlane() {
        FloatBuffer boundary = plane.getPolygon();

        if (boundary == null) {
            return false;
        }

        boundary.rewind();
        int boundaryVertices = boundary.limit() / 2;

        if (boundaryVertices == 0) {
            return false;
        }

        int numVertices = boundaryVertices * VERTS_PER_BOUNDARY_VERT;
        vertices.clear();
        vertices.ensureCapacity(numVertices);

        int numIndices = (boundaryVertices * 6) + ((boundaryVertices - 2) * 3);
        triangleIndices.clear();
        triangleIndices.ensureCapacity(numIndices);

        Vector3 normal = Vector3.up();

        // Copy the perimeter vertices into the vertex buffer and add in the y-coordinate.
        while (boundary.hasRemaining()) {
            float x = boundary.get();
            float z = boundary.get();
            vertices.add(Vertex.builder().setPosition(new Vector3(x, 0.0f, z)).setNormal(normal).build());
        }

        // Generate the interior vertices.
        boundary.rewind();
        while (boundary.hasRemaining()) {
            float x = boundary.get();
            float z = boundary.get();

            float magnitude = (float) Math.hypot(x, z);
            float scale = 1.0f - FEATHER_SCALE;
            if (magnitude != 0.0f) {
                scale = 1.0f - Math.min(FEATHER_LENGTH / magnitude, FEATHER_SCALE);
            }

            vertices.add(
                    Vertex.builder()
                            .setPosition(new Vector3(x * scale, 1.0f, z * scale))
                            .setNormal(normal)
                            .build());
        }

        int firstOuterVertex = 0;
        int firstInnerVertex = (short) boundaryVertices;

        // Generate triangle (4, 5, 6) and (4, 6, 7).
        for (int i = 0; i < boundaryVertices - 2; ++i) {
            triangleIndices.add(firstInnerVertex);
            triangleIndices.add(firstInnerVertex + i + 1);
            triangleIndices.add(firstInnerVertex + i + 2);
        }

        // Generate triangle (0, 1, 4), (4, 1, 5), (5, 1, 2), (5, 2, 6), (6, 2, 3), (6, 3, 7)
        // (7, 3, 0), (7, 0, 4)
        for (int i = 0; i < boundaryVertices; ++i) {
            int outerVertex1 = firstOuterVertex + i;
            int outerVertex2 = firstOuterVertex + ((i + 1) % boundaryVertices);
            int innerVertex1 = firstInnerVertex + i;
            int innerVertex2 = firstInnerVertex + ((i + 1) % boundaryVertices);

            triangleIndices.add(outerVertex1);
            triangleIndices.add(outerVertex2);
            triangleIndices.add(innerVertex1);

            triangleIndices.add(innerVertex1);
            triangleIndices.add(outerVertex2);
            triangleIndices.add(innerVertex2);
        }

        return true;
    }
}
