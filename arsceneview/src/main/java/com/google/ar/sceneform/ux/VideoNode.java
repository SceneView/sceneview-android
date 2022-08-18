package com.google.ar.sceneform.ux;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.PlaneFactory;
import com.google.ar.sceneform.rendering.Renderable;

import io.github.sceneview.math.MathUtilsKt;
import io.github.sceneview.node.ModelNode;
import io.github.sceneview.utils.FrameTime;

/**
 * Node that can show a video by passing a {@link MediaPlayer} instance. Note that
 * VideoNode does not manage video playback by itself (e.g. starting the video).
 * <p>
 * Filtering out a specific color in the video is also supported by
 * defining a chroma key color.
 * </p>
 * <p>
 * Optionally an {@link ExternalTexture} can be passed if multiple VideoNode instances
 * need to render the exact same instance of a video. This will also improve performance
 * dramatically instead of rendering each instance separately.
 * </p>
 */
//public class VideoNode extends ModelNode {
//  private static final String MATERIAL_PARAMETER_VIDEO_TEXTURE = "videoTexture";
//  private static final String MATERIAL_PARAMETER_CHROMA_KEY_COLOR = "keyColor";
//
//  protected Lifecycle lifecycle;
//  private final MediaPlayer player;
//  private final ExternalTexture texture;
//  private final Color chromaKeyColor;
//  private final Listener listener;
//  private boolean rotateAlwaysToCamera = false;
//
//  /**
//   * Create a new VideoNode for showing a video from a MediaPlayer instance inside a node on an
//   * adjusted plane renderable
//   *
//   * @param context  Resources context
//   * @param player   The video media player to render on the plane node
//   * @param listener Loading listener
//   */
//  public VideoNode(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Listener listener) {
//    this(context, lifecycle, player, null, null, listener);
//  }
//
//  /**
//   * Create a new VideoNode for showing a video from a MediaPlayer instance inside a node on an
//   * adjusted plane renderable with video transparency color set to chromaKeyColor.
//   *
//   * @param context        Resources context
//   * @param player         The video media player to render on the plane node
//   * @param chromaKeyColor Chroma Key color to made the video transparent from
//   * @param listener       Loading listener
//   */
//  public VideoNode(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Color chromaKeyColor,
//                   @Nullable Listener listener) {
//    this(context, lifecycle, player, chromaKeyColor, null, listener);
//  }
//
//  /**
//   * Create a new VideoNode for showing a video from a MediaPlayer instance inside a node on your
//   * own material renderable and material with video transparency color set to chromaKeyColor
//   *
//   * @param context        Resources context
//   * @param player         The video media player to render on the plane node
//   * @param chromaKeyColor Chroma Key color to made the video transparent from
//   * @param texture        Custom ExternalTexture for using your own renderable and material.
//   *                       Null for default Plane shape renderable.
//   * @param listener       Loading listener
//   */
//  //TODO: Remove the lifecycle param when kotlined
//  public VideoNode(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Color chromaKeyColor,
//                   @Nullable ExternalTexture texture, @Nullable Listener listener) {
//    this.lifecycle = lifecycle;
//    this.player = player;
//    this.texture = texture != null ? texture : new ExternalTexture(lifecycle);
//    this.chromaKeyColor = chromaKeyColor;
//    this.listener = listener;
//    init(context);
//  }
//
//  private void init(Context context) {
//    player.setSurface(texture.getSurface());
//    final String materialLocation;
//    if (chromaKeyColor != null) {
//      materialLocation = "sceneview/materials/video_stream_chroma_key.filamat";
//    } else {
//      materialLocation = "sceneview/materials/video_stream_plain.filamat";
//    }
//    Material.builder()
//            .setSource(context, Uri.parse(materialLocation))
//            .build(lifecycle)
//            .thenAccept(material -> {
//              material.setExternalTexture(MATERIAL_PARAMETER_VIDEO_TEXTURE, texture);
//              if (chromaKeyColor != null) {
//                material.setFloat4(MATERIAL_PARAMETER_CHROMA_KEY_COLOR, chromaKeyColor);
//              }
//              final Renderable renderable = createModel(player, material);
//              setModel(renderable);
//              onCreated(this);
//            })
//            .exceptionally(throwable -> {
//              onError(throwable);
//              return null;
//            });
//  }
//
//  /**
//   * Create the renderable on which the video will be displayed.
//   * Override this function for using a custom Model.
//   * Default return is a centered plane
//   *
//   * @param player   the media player
//   * @param material the material to apply to your custom model
//   * @return the renderable with the applied material.
//   */
//  public Renderable createModel(MediaPlayer player, Material material) {
//    final int width = player.getVideoWidth();
//    final int height = player.getVideoHeight();
//    final float x;
//    final float y;
//    if (width >= height) {
//      x = 1.0f;
//      y = (float) height / (float) width;
//    } else {
//      x = (float) width / (float) height;
//      y = 1.0f;
//    }
//    return makePlane(x, y, material);
//  }
//
//  public Renderable makePlane(float width, float height, Material material) {
//    return PlaneFactory.makePlane(
//            new Vector3(width, height, 0.0f),
//            new Vector3(0.0f, height / 2.0f, 0.0f),
//            material.filamentMaterialInstance,
//            lifecycle
//    );
//  }
//
//  @Override
//  public void onFrame(@NonNull FrameTime frameTime) {
//    super.onFrame(frameTime);
//
//    if (rotateAlwaysToCamera && getSceneView() != null) {
//      Vector3 cameraPosition = MathUtilsKt.toVector3(getSceneView().getCameraNode().getWorldPosition());
//      Vector3 cardPosition = MathUtilsKt.toVector3(getWorldPosition());
//      Vector3 direction = Vector3.subtract(cameraPosition, cardPosition);
//      Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
//      setQuaternion(MathUtilsKt.toNewQuaternion(lookRotation));
//    }
//  }
//
//  public MediaPlayer getPlayer() {
//    return player;
//  }
//
//  public ExternalTexture getTexture() {
//    return texture;
//  }
//
//  public Color getChromaKeyColor() {
//    return chromaKeyColor;
//  }
//
//  public boolean isRotateAlwaysToCamera() {
//    return rotateAlwaysToCamera;
//  }
//
//  /**
//   * If this Flag is set to true, the VideoNode will always
//   * point to the CameraNode (You).
//   *
//   * @param rotateAlwaysToCamera boolean
//   */
//  public void setRotateAlwaysToCamera(boolean rotateAlwaysToCamera) {
//    this.rotateAlwaysToCamera = rotateAlwaysToCamera;
//  }
//
//  private void onCreated(VideoNode videoNode) {
//    if (listener != null) {
//      listener.onCreated(videoNode);
//    }
//  }
//
//  private void onError(Throwable throwable) {
//    if (listener != null) {
//      listener.onError(throwable);
//    }
//  }
//
//  /**
//   * Listener for VideoNode loading
//   */
//  public interface OnCreateListener {
//    /**
//     * Something wrong happened during the VideoNode instantiation
//     *
//     * @param throwable corresponding error
//     */
//    void onError(Throwable throwable);
//  }
//
//  /**
//   * Listener for VideoNode loading
//   */
//  public interface Listener {
//
//    /**
//     * Called when the renderable and material have been set on this node.
//     *
//     * @param videoNode the created instance of the model.
//     */
//    void onCreated(VideoNode videoNode);
//
//    /**
//     * Something wrong happened during the VideoNode instantiation
//     *
//     * @param throwable corresponding error
//     */
//    void onError(Throwable throwable);
//  }
//
//  public static class Horizontal extends VideoNode {
//
//    public Horizontal(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Listener listener) {
//      super(context, lifecycle, player, listener);
//    }
//
//    public Horizontal(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Color chromaKeyColor, @Nullable Listener listener) {
//      super(context, lifecycle, player, chromaKeyColor, listener);
//    }
//
//    public Horizontal(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Color chromaKeyColor, @Nullable ExternalTexture texture, @Nullable Listener listener) {
//      super(context, lifecycle, player, chromaKeyColor, texture, listener);
//    }
//
//    @Override
//    public Renderable makePlane(float width, float height, Material material) {
//      return PlaneFactory.makePlane(
//              new Vector3(width, 0.0f, height),
//              new Vector3(0.0f, 0.0f, 0.0f),
//              material.filamentMaterialInstance,
//              lifecycle
//      );
//    }
//  }
//
//  public static class Vertical extends VideoNode {
//
//    public Vertical(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Listener listener) {
//      super(context, lifecycle, player, listener);
//    }
//
//    public Vertical(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Color chromaKeyColor, @Nullable Listener listener) {
//      super(context, lifecycle, player, chromaKeyColor, listener);
//    }
//
//    public Vertical(Context context, Lifecycle lifecycle, MediaPlayer player, @Nullable Color chromaKeyColor, @Nullable ExternalTexture texture, @Nullable Listener listener) {
//      super(context, lifecycle, player, chromaKeyColor, texture, listener);
//    }
//
//    @Override
//    public Renderable makePlane(float width, float height, Material material) {
//      return PlaneFactory.makePlane(
//              new Vector3(width, height, 0.0f),
//              new Vector3(0.0f, height / 2.0f, 0.0f),
//              material.filamentMaterialInstance,
//              lifecycle
//      );
//    }
//  }
//}