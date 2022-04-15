package com.google.ar.sceneform;

import android.view.MotionEvent;

import androidx.annotation.VisibleForTesting;

import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.MathHelper;
import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.CameraProvider;
import com.google.ar.sceneform.utilities.Preconditions;

import dev.romainguy.kotlin.math.Float3;
import dev.romainguy.kotlin.math.Quaternion;
import io.github.sceneview.SceneView;
import io.github.sceneview.node.Node;
import io.github.sceneview.node.NodeParent;

/**
 * Represents a virtual camera, which determines the perspective through which the scene is viewed.
 *
 * <p>If the camera is part of an ArSceneView, then the camera automatically tracks the
 * camera pose from ARCore. Additionally, the following methods will throw {@link
 * UnsupportedOperationException} when called:
 *
 * <ul>
 *   <li>{@link #setParent(NodeParent)} - Camera's parent cannot be changed, it is always the scene.
 *   <li>{@link #setPosition(Float3)} - Camera's position cannot be changed, it is controlled
 *       by the ARCore camera pose.
 *   <li>{@link #setRotation(Float3)} - Camera's rotation cannot be changed, it is
 *       controlled by the ARCore camera pose.
 *   <li>{@link #setPosition(Float3)} - Camera's position cannot be changed, it is controlled
 *       by the ARCore camera pose.
 *   <li>{@link #setRotation(Float3)} - Camera's rotation cannot be changed, it is
 *       controlled by the ARCore camera pose.
 * </ul>
 * <p>
 * All other functionality in Node is supported. You can access the position and rotation of the
 * camera, assign a collision shape to the camera, or add children to the camera. Disabling the
 * camera turns off rendering.
 */
public class Camera extends Node implements CameraProvider {
    protected final Matrix viewMatrix = new Matrix();
    protected final Matrix projectionMatrix = new Matrix();

    private static final Float3 DEFAULT_POSITION = new Float3(0.0f, 0.0f, 0.0f);
    private static final Quaternion DEFAULT_QUATERNION = new Quaternion();

    public static final float DEFAULT_NEAR_PLANE = 0.01f;
    public static final float DEFAULT_FAR_PLANE = 30.0f;
    public static final int FALLBACK_VIEW_WIDTH = 1920;
    public static final int FALLBACK_VIEW_HEIGHT = 1080;

    // Default vertical field of view for non-ar camera.
    private static final float DEFAULT_VERTICAL_FOV_DEGREES = 90.0f;

    protected float nearPlane = DEFAULT_NEAR_PLANE;
    protected float farPlane = DEFAULT_FAR_PLANE;

    private float verticalFov = DEFAULT_VERTICAL_FOV_DEGREES;

    // isArCamera will be true if the Camera is part of an ArSceneView, false otherwise.
    private boolean isFixed;
    protected boolean areMatricesInitialized;

    public Camera(SceneView scene) {
        this(scene, true);
    }

    public Camera(SceneView scene, boolean isFixed) {
        super();
        this.isFixed = isFixed;
        setPosition(DEFAULT_POSITION);
        setQuaternion(DEFAULT_QUATERNION);

        Preconditions.checkNotNull(scene, "Parameter \"scene\" was null.");
        super.setParent(scene);

        if (isFixed) {
            scene.addOnLayoutChangeListener(
                    (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                            refreshProjectionMatrix());
        }
    }

    /**
     * @hide
     */
    public void setNearClipPlane(float nearPlane) {
        this.nearPlane = nearPlane;

        // If this is an ArCamera, the projection matrix gets re-created when updateTrackedPose is
        // called every frame. Otherwise, update it now.
        if (isFixed) {
            refreshProjectionMatrix();
        }
    }

    @Override
    public float getNearClipPlane() {
        return nearPlane;
    }

    /**
     * @hide
     */
    public void setFarClipPlane(float farPlane) {
        this.farPlane = farPlane;

        // If this is an ArCamera, the projection matrix gets re-created when updateTrackedPose is
        // called every frame. Otherwise, update it now.
        if (isFixed) {
            refreshProjectionMatrix();
        }
    }

    /**
     * Sets the vertical field of view for the non-ar camera in degrees. If this is an AR camera, then
     * the fov comes from ARCore and cannot be set so an exception is thrown. The default is 90
     * degrees.
     *
     * @throws UnsupportedOperationException if this is an AR camera
     */

    public void setVerticalFovDegrees(float verticalFov) {
        this.verticalFov = verticalFov;

        if (isFixed) {
            refreshProjectionMatrix();
        }
    }

    /**
     * Gets the vertical field of view for the camera.
     *
     * <p>If this is an AR camera, then it is calculated based on the camera information from ARCore
     * and can vary between device. It can't be calculated until the first frame after the ARCore
     * session is resumed, in which case an IllegalStateException is thrown.
     *
     * <p>Otherwise, this will return the value set by {@link #setVerticalFovDegrees(float)}, with a
     * default of 90 degrees.
     *
     * @throws IllegalStateException if called before the first frame after ARCore is resumed
     */

    public float getVerticalFovDegrees() {
        return verticalFov;
    }

    @Override
    public float getFarClipPlane() {
        return farPlane;
    }

    /**
     * @hide Used internally (b/113516741)
     */
    @Override
    public Matrix getViewMatrix() {
        Matrix.invert(getTransformationMatrix(), viewMatrix);
        return viewMatrix;
    }

    /**
     * @hide Used internally (b/113516741) and within rendering package
     */
    @Override
    public Matrix getProjectionMatrix() {
        return projectionMatrix;
    }

    public Ray motionEventToRay(MotionEvent motionEvent) {
        Preconditions.checkNotNull(motionEvent, "Parameter \"motionEvent\" was null.");
        int index = motionEvent.getActionIndex();
        return screenPointToRay(motionEvent.getX(index), motionEvent.getY(index));
    }

    /**
     * Calculates a ray in world space going from the near-plane of the camera and going through a
     * point in screen space. Screen space is in Android device screen coordinates: TopLeft = (0, 0)
     * BottomRight = (Screen Width, Screen Height) The device coordinate space is unaffected by the
     * orientation of the device.
     *
     * @param x X position in device screen coordinates.
     * @param y Y position in device screen coordinates.
     */
    public Ray screenPointToRay(float x, float y) {
        Vector3 startPoint = new Vector3();
        Vector3 endPoint = new Vector3();

        unproject(x, y, 0.0f, startPoint);
        unproject(x, y, 1.0f, endPoint);

        Vector3 direction = Vector3.subtract(endPoint, startPoint);

        return new Ray(startPoint, direction);
    }

    /**
     * Convert a point from world space into screen space.
     *
     * <p>The X value is negative when the point is left of the viewport, between 0 and the width of
     * the {@link SceneView} when the point is within the viewport, and greater than the width when
     * the point is to the right of the viewport.
     *
     * <p>The Y value is negative when the point is below the viewport, between 0 and the height of
     * the {@link SceneView} when the point is within the viewport, and greater than the height when
     * the point is above the viewport.
     *
     * <p>The Z value is always 0 since the return value is a 2D coordinate.
     *
     * @param point the point in world space to convert
     * @return a new vector that represents the point in screen-space.
     */
    public Vector3 worldToScreenPoint(Vector3 point) {
        Matrix m = new Matrix();
        Matrix.multiply(projectionMatrix, getViewMatrix(), m);

        int viewWidth = getViewWidth();
        int viewHeight = getViewHeight();
        float x = point.x;
        float y = point.y;
        float z = point.z;
        float w = 1.0f;

        // Multiply the world point.
        Vector3 screenPoint = new Vector3();
        screenPoint.x = x * m.data[0] + y * m.data[4] + z * m.data[8] + w * m.data[12];
        screenPoint.y = x * m.data[1] + y * m.data[5] + z * m.data[9] + w * m.data[13];
        w = x * m.data[3] + y * m.data[7] + z * m.data[11] + w * m.data[15];

        // To clipping space.
        screenPoint.x = ((screenPoint.x / w) + 1.0f) * 0.5f;
        screenPoint.y = ((screenPoint.y / w) + 1.0f) * 0.5f;

        // To screen space.
        screenPoint.x = screenPoint.x * viewWidth;
        screenPoint.y = screenPoint.y * viewHeight;

        // Invert Y because screen Y points down and Sceneform Y points up.
        screenPoint.y = viewHeight - screenPoint.y;

        return screenPoint;
    }

    @Override
    public void onTransformChanged() {
        super.onTransformChanged();
    }

    /**
     * @hide Used to explicitly set the projection matrix for testing.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public void setProjectionMatrix(Matrix matrix) {
        projectionMatrix.set(matrix.data);
    }

    private boolean unproject(float x, float y, float z, final Vector3 dest) {
        Preconditions.checkNotNull(dest, "Parameter \"dest\" was null.");

        Matrix m = new Matrix();
        Matrix.multiply(projectionMatrix, getViewMatrix(), m);
        Matrix.invert(m, m);

        int viewWidth = getViewWidth();
        int viewHeight = getViewHeight();

        // Invert Y because screen Y points down and Sceneform Y points up.
        y = viewHeight - y;

        // Normalize between -1 and 1.
        x = x / viewWidth * 2.0f - 1.0f;
        y = y / viewHeight * 2.0f - 1.0f;
        z = 2.0f * z - 1.0f;
        float w = 1.0f;

        dest.x = x * m.data[0] + y * m.data[4] + z * m.data[8] + w * m.data[12];
        dest.y = x * m.data[1] + y * m.data[5] + z * m.data[9] + w * m.data[13];
        dest.z = x * m.data[2] + y * m.data[6] + z * m.data[10] + w * m.data[14];
        w = x * m.data[3] + y * m.data[7] + z * m.data[11] + w * m.data[15];

        if (MathHelper.almostEqualRelativeAndAbs(w, 0.0f)) {
            dest.set(0, 0, 0);
            return false;
        }

        w = 1.0f / w;
        dest.set(dest.scaled(w));
        return true;
    }

    private int getViewWidth() {
        SceneView scene = getSceneView();
        if (scene == null) {
            return FALLBACK_VIEW_WIDTH;
        }

        return scene.getWidth();
    }

    private int getViewHeight() {
        SceneView scene = getSceneView();
        if (scene == null) {
            return FALLBACK_VIEW_HEIGHT;
        }

        return scene.getHeight();
    }

    protected void refreshProjectionMatrix() {
        int width = getViewWidth();
        int height = getViewHeight();

        if (width == 0 || height == 0) {
            return;
        }

        float aspect = (float) width / (float) height;
        setPerspective(verticalFov, aspect, nearPlane, farPlane);
    }

    /**
     * Set the camera perspective based on the field of view, aspect ratio, near and far planes.
     * verticalFovInDegrees must be greater than zero and less than 180 degrees. far - near must be
     * greater than zero. aspect must be greater than zero. near and far must be greater than zero.
     *
     * @param verticalFovInDegrees vertical field of view in degrees.
     * @param aspect               aspect ratio of the viewport, which is widthInPixels / heightInPixels.
     * @param near                 distance in world units from the camera to the near plane, default is 0.1f
     * @param far                  distance in world units from the camera to the far plane, default is 100.0f
     * @throws IllegalArgumentException if any of the following preconditions are not met:
     *                                  <ul>
     *                                    <li>0 < verticalFovInDegrees < 180
     *                                    <li>aspect > 0
     *                                    <li>near > 0
     *                                    <li>far > near
     *                                  </ul>
     */
    private void setPerspective(float verticalFovInDegrees, float aspect, float near, float far) {
        if (verticalFovInDegrees <= 0.0f || verticalFovInDegrees >= 180.0f) {
            throw new IllegalArgumentException(
                    "Parameter \"verticalFovInDegrees\" is out of the valid range of (0, 180) degrees.");
        }
        if (aspect <= 0.0f) {
            throw new IllegalArgumentException("Parameter \"aspect\" must be greater than zero.");
        }

        final double fovInRadians = Math.toRadians((double) verticalFovInDegrees);
        final float top = (float) Math.tan(fovInRadians * 0.5) * near;
        final float bottom = -top;
        final float right = top * aspect;
        final float left = -right;

        setPerspective(left, right, bottom, top, near, far);
    }

    /**
     * Set the camera perspective projection in terms of six clip planes. right - left must be greater
     * than zero. top - bottom must be greater than zero. far - near must be greater than zero. near
     * and far must be greater than zero.
     *
     * @param left   offset in world units from the camera to the left plane, at the near plane.
     * @param right  offset in world units from the camera to the right plane, at the near plane.
     * @param bottom offset in world units from the camera to the bottom plane, at the near plane.
     * @param top    offset in world units from the camera to the top plane, at the near plane.
     * @param near   distance in world units from the camera to the near plane, default is 0.1f
     * @param far    distance in world units from the camera to the far plane, default is 100.0f
     * @throws IllegalArgumentException if any of the following preconditions are not met:
     *                                  <ul>
     *                                    <li>left != right
     *                                    <li>bottom != top
     *                                    <li>near > 0
     *                                    <li>far > near
     *                                  </ul>
     */
    private void setPerspective(
            float left, float right, float bottom, float top, float near, float far) {
        float[] data = projectionMatrix.data;

        if (left == right || bottom == top || near <= 0.0f || far <= near) {
            throw new IllegalArgumentException(
                    "Invalid parameters to setPerspective, valid values: "
                            + " width != height, bottom != top, near > 0.0f, far > near");
        }

        final float reciprocalWidth = 1.0f / (right - left);
        final float reciprocalHeight = 1.0f / (top - bottom);
        final float reciprocalDepthRange = 1.0f / (far - near);

        // Right-handed, column major 4x4 matrix.
        data[0] = 2.0f * near * reciprocalWidth;
        data[1] = 0.0f;
        data[2] = 0.0f;
        data[3] = 0.0f;

        data[4] = 0.0f;
        data[5] = 2.0f * near * reciprocalHeight;
        data[6] = 0.0f;
        data[7] = 0.0f;

        data[8] = (right + left) * reciprocalWidth;
        data[9] = (top + bottom) * reciprocalHeight;
        data[10] = -(far + near) * reciprocalDepthRange;
        data[11] = -1.0f;

        data[12] = 0.0f;
        data[13] = 0.0f;
        data[14] = -2.0f * far * near * reciprocalDepthRange;
        data[15] = 0.0f;

        nearPlane = near;
        farPlane = far;
        areMatricesInitialized = true;
    }
}
