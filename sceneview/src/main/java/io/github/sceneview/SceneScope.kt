package io.github.sceneview

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import io.github.sceneview.geometries.Cube
import io.github.sceneview.geometries.Cylinder
import io.github.sceneview.geometries.Plane
import io.github.sceneview.geometries.Sphere
import io.github.sceneview.geometries.UvScale
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CameraNode as CameraNodeImpl
import io.github.sceneview.node.CubeNode as CubeNodeImpl
import io.github.sceneview.node.CylinderNode as CylinderNodeImpl
import io.github.sceneview.node.ImageNode as ImageNodeImpl
import io.github.sceneview.node.LightNode as LightNodeImpl
import io.github.sceneview.node.MeshNode as MeshNodeImpl
import io.github.sceneview.node.ModelNode as ModelNodeImpl
import io.github.sceneview.node.Node as NodeImpl
import io.github.sceneview.node.PlaneNode as PlaneNodeImpl
import io.github.sceneview.node.SphereNode as SphereNodeImpl
import io.github.sceneview.node.ViewNode2

/**
 * DSL marker annotation that prevents implicit access to outer [SceneScope] from inside a
 * [NodeScope], enforcing correct nesting.
 */
@DslMarker
annotation class SceneDsl

/**
 * The composable DSL scope for building 3D scenes inside [Scene].
 *
 * `SceneScope` is the receiver of `Scene { }` content blocks. Every node type — models, lights,
 * geometry, images, Compose UI planes, custom meshes — is a `@Composable` function in this scope.
 * Nodes enter the Filament scene on first composition and are automatically destroyed when they
 * leave, with no manual lifecycle management.
 *
 * Build scenes the same way you build Compose UI: nest composables, react to state, use
 * `remember` and `LaunchedEffect`. The 3D scene graph mirrors the Compose tree.
 *
 * ```kotlin
 * Scene(modifier = Modifier.fillMaxSize()) {
 *     // Async model — null while loading, node appears on recomposition when ready
 *     rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
 *         ModelNode(modelInstance = instance, scaleToUnits = 0.5f)
 *     }
 *     // Nested nodes build a scene graph hierarchy
 *     Node(position = Position(y = 1.0f)) {
 *         CubeNode(size = Size(0.1f))
 *         SphereNode(radius = 0.05f)
 *     }
 *     LightNode(type = LightManager.Type.DIRECTIONAL)
 * }
 * ```
 *
 * @param engine            The Filament [Engine] shared with the parent [Scene].
 * @param modelLoader       [ModelLoader] for loading glTF/GLB models.
 * @param materialLoader    [MaterialLoader] for creating material instances.
 * @param environmentLoader [EnvironmentLoader] for loading HDR/KTX environments.
 * @param _nodes            Internal SnapshotStateList backing the scene's root node list.
 */
@SceneDsl
open class SceneScope @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) constructor(
    val engine: Engine,
    val modelLoader: ModelLoader,
    val materialLoader: MaterialLoader,
    val environmentLoader: EnvironmentLoader,
    internal val _nodes: SnapshotStateList<NodeImpl>
) {

    // ── Attachment helpers ────────────────────────────────────────────────────────────────────────

    /**
     * Attach [node] to this scope's container. Overridden in [NodeScope] to attach to a parent.
     */
    internal open fun attach(node: NodeImpl) {
        _nodes.add(node)
    }

    /**
     * Detach [node] from this scope's container. Overridden in [NodeScope] to remove from parent.
     */
    internal open fun detach(node: NodeImpl) {
        _nodes.remove(node)
    }

    // ── Base Node ─────────────────────────────────────────────────────────────────────────────────

    /**
     * A base scene-graph node with no renderable geometry.
     *
     * Useful as a pivot/anchor point for grouping other nodes or for attaching a camera.
     *
     * @param position  Local position relative to the scene root (or parent node in [NodeScope]).
     * @param rotation  Local rotation in Euler angles (degrees).
     * @param scale     Local scale.
     * @param isVisible Whether the node (and all its children) should be rendered.
     * @param isEditable Whether the node can be interactively moved/rotated/scaled.
     * @param apply     Additional imperative configuration applied once on creation.
     * @param content   Optional child nodes declared in a [NodeScope].
     */
    @Composable
    fun Node(
        position: Position = Position(x = 0f),
        rotation: Rotation = Rotation(x = 0f),
        scale: Scale = Scale(x = 1f),
        isVisible: Boolean = true,
        isEditable: Boolean = false,
        apply: NodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine) { NodeImpl(engine = engine).apply(apply) }
        SideEffect {
            node.position = position
            node.rotation = rotation
            node.scale = scale
            node.isVisible = isVisible
            node.isEditable = isEditable
        }
        NodeLifecycle(node, content)
    }

    // ── ModelNode ─────────────────────────────────────────────────────────────────────────────────

    /**
     * A node that renders a glTF/GLB 3D model.
     *
     * Typically used in combination with [rememberModelInstance] to load a model asynchronously:
     * ```kotlin
     * Scene {
     *     rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
     *         ModelNode(modelInstance = instance, scaleToUnits = 0.5f)
     *     }
     * }
     * ```
     *
     * @param modelInstance The loaded model instance to render.
     * @param autoAnimate   Whether to automatically play all animations in the model.
     * @param scaleToUnits  If set, uniformly scales the model to fit within a cube of this size (in meters).
     * @param centerOrigin  Origin alignment relative to the model's bounding box.
     *                      - `null` keeps the model's original center
     *                      - `Position(0,0,0)` centers horizontally and vertically
     *                      - `Position(0,-1,0)` = centered horizontally, bottom-aligned
     * @param position      Local position.
     * @param rotation      Local rotation in Euler angles (degrees).
     * @param scale         Local scale.
     * @param isVisible     Whether to render the node.
     * @param isEditable    Whether the node can be interactively transformed.
     * @param apply         Additional imperative configuration applied once on creation.
     * @param content       Optional child nodes declared in a [NodeScope].
     */
    @Composable
    fun ModelNode(
        modelInstance: ModelInstance,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        position: Position = Position(x = 0f),
        rotation: Rotation = Rotation(x = 0f),
        scale: Scale = Scale(x = 1f),
        isVisible: Boolean = true,
        isEditable: Boolean = false,
        apply: ModelNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine, modelInstance) {
            ModelNodeImpl(
                modelInstance = modelInstance,
                autoAnimate = autoAnimate,
                scaleToUnits = scaleToUnits,
                centerOrigin = centerOrigin
            ).apply {
                this.position = position
                this.rotation = rotation
                // Don't reset scale here — scaleToUnitCube() already set it in the constructor.
                // If scaleToUnits is null, scale stays at its default (Scale(1f)); apply() can
                // override either way.
                if (scaleToUnits == null) this.scale = scale
                this.isVisible = isVisible
                this.isEditable = isEditable
                apply()
            }
        }
        SideEffect {
            node.position = position
            node.rotation = rotation
            // Don't clobber scaleToUnits-computed scale on every recomposition.
            if (scaleToUnits == null) node.scale = scale
            node.isVisible = isVisible
            node.isEditable = isEditable
        }
        NodeLifecycle(node, content)
    }

    // ── LightNode ─────────────────────────────────────────────────────────────────────────────────

    /**
     * A light source node (directional, point, spot, or sun).
     *
     * At least one light must be added to the scene to see anything unless using unlit materials.
     *
     * @param type       The [LightManager.Type] of light (DIRECTIONAL, POINT, SPOT, SUN, etc.).
     * @param apply      Builder configuration for [LightManager.Builder] (color, intensity,
     *                   direction, falloff, etc.).
     * @param nodeApply  Additional imperative configuration on the [LightNodeImpl] after creation.
     * @param content    Optional child nodes declared in a [NodeScope].
     */
    @Composable
    fun LightNode(
        type: LightManager.Type,
        apply: LightManager.Builder.() -> Unit = {},
        nodeApply: LightNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine, type) {
            LightNodeImpl(engine = engine, type = type, apply = apply).apply(nodeApply)
        }
        NodeLifecycle(node, content)
    }

    // ── CameraNode ────────────────────────────────────────────────────────────────────────────────

    /**
     * A virtual camera node that can be used as a secondary viewpoint.
     *
     * **Note:** This does NOT automatically become the scene's active rendering camera.
     * The main rendering camera is configured via the `cameraNode` parameter of [Scene].
     * Use this composable to add cameras as named scene nodes (e.g. imported from a glTF model).
     *
     * @param apply   Configuration applied to the [CameraNodeImpl] on creation.
     * @param content Optional child nodes declared in a [NodeScope].
     */
    @Composable
    fun CameraNode(
        apply: CameraNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine) {
            // Use the secondary constructor so that engine.createCamera(entity) is called,
            // properly registering the entity as a Filament camera component.
            CameraNodeImpl(engine = engine).apply(apply)
        }
        NodeLifecycle(node, content)
    }

    // ── CubeNode ──────────────────────────────────────────────────────────────────────────────────

    /**
     * A box/cube geometry node.
     *
     * @param size             Full size (width × height × depth) of the cube in meters.
     * @param center           Center of the cube in local space.
     * @param materialInstance The material instance to apply to all faces.
     * @param apply            Additional configuration on the [CubeNodeImpl].
     * @param content          Optional child nodes.
     */
    @Composable
    fun CubeNode(
        size: Size = Cube.DEFAULT_SIZE,
        center: Position = Cube.DEFAULT_CENTER,
        materialInstance: MaterialInstance? = null,
        position: Position = Position(x = 0f),
        rotation: Rotation = Rotation(x = 0f),
        apply: CubeNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine) {
            CubeNodeImpl(
                engine = engine,
                size = size,
                center = center,
                materialInstance = materialInstance
            ).apply(apply)
        }
        SideEffect {
            node.updateGeometry(center = center, size = size)
            node.position = position
            node.rotation = rotation
        }
        NodeLifecycle(node, content)
    }

    // ── SphereNode ────────────────────────────────────────────────────────────────────────────────

    /**
     * A sphere geometry node.
     *
     * @param radius           Radius of the sphere in meters.
     * @param center           Center of the sphere in local space.
     * @param stacks           Number of horizontal subdivisions.
     * @param slices           Number of vertical subdivisions.
     * @param materialInstance The material instance to apply.
     * @param apply            Additional configuration on the [SphereNodeImpl].
     * @param content          Optional child nodes.
     */
    @Composable
    fun SphereNode(
        radius: Float = Sphere.DEFAULT_RADIUS,
        center: Position = Sphere.DEFAULT_CENTER,
        stacks: Int = Sphere.DEFAULT_STACKS,
        slices: Int = Sphere.DEFAULT_SLICES,
        materialInstance: MaterialInstance? = null,
        apply: SphereNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine) {
            SphereNodeImpl(
                engine = engine,
                radius = radius,
                center = center,
                stacks = stacks,
                slices = slices,
                materialInstance = materialInstance
            ).apply(apply)
        }
        SideEffect {
            node.updateGeometry(radius = radius, center = center, stacks = stacks, slices = slices)
        }
        NodeLifecycle(node, content)
    }

    // ── CylinderNode ──────────────────────────────────────────────────────────────────────────────

    /**
     * A cylinder geometry node.
     *
     * @param radius           Radius of the cylinder in meters.
     * @param height           Height of the cylinder in meters.
     * @param center           Center of the cylinder in local space.
     * @param sideCount        Number of sides (polygon resolution of the cylinder).
     * @param materialInstance The material instance to apply.
     * @param apply            Additional configuration on the [CylinderNodeImpl].
     * @param content          Optional child nodes.
     */
    @Composable
    fun CylinderNode(
        radius: Float = Cylinder.DEFAULT_RADIUS,
        height: Float = Cylinder.DEFAULT_HEIGHT,
        center: Position = Cylinder.DEFAULT_CENTER,
        sideCount: Int = Cylinder.DEFAULT_SIDE_COUNT,
        materialInstance: MaterialInstance? = null,
        position: Position = Position(x = 0f),
        rotation: Rotation = Rotation(x = 0f),
        apply: CylinderNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine) {
            CylinderNodeImpl(
                engine = engine,
                radius = radius,
                height = height,
                center = center,
                sideCount = sideCount,
                materialInstance = materialInstance
            ).apply(apply)
        }
        SideEffect {
            node.updateGeometry(
                radius = radius,
                height = height,
                center = center,
                sideCount = sideCount
            )
            node.position = position
            node.rotation = rotation
        }
        NodeLifecycle(node, content)
    }

    // ── PlaneNode ─────────────────────────────────────────────────────────────────────────────────

    /**
     * A flat quad/plane geometry node.
     *
     * @param size             Width × height of the plane in meters.
     * @param center           Center of the plane in local space.
     * @param normal           Facing direction of the plane.
     * @param uvScale          UV texture coordinate scale.
     * @param materialInstance The material instance to apply.
     * @param apply            Additional configuration on the [PlaneNodeImpl].
     * @param content          Optional child nodes.
     */
    @Composable
    fun PlaneNode(
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        materialInstance: MaterialInstance? = null,
        position: Position = Position(x = 0f),
        rotation: Rotation = Rotation(x = 0f),
        apply: PlaneNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine) {
            PlaneNodeImpl(
                engine = engine,
                size = size,
                center = center,
                normal = normal,
                uvScale = uvScale,
                materialInstance = materialInstance
            ).apply(apply)
        }
        SideEffect {
            node.updateGeometry(size = size, center = center, normal = normal)
            node.position = position
            node.rotation = rotation
        }
        NodeLifecycle(node, content)
    }

    // ── ImageNode ─────────────────────────────────────────────────────────────────────────────────

    /**
     * A flat plane node that renders a [Bitmap] image.
     *
     * The plane size is automatically derived from the image's aspect ratio unless [size] is set.
     *
     * @param bitmap           The image to display.
     * @param size             Override size. `null` derives size from the bitmap's aspect ratio.
     * @param center           Center of the image plane in local space.
     * @param normal           Facing direction of the image plane.
     * @param apply            Additional configuration on the [ImageNodeImpl].
     * @param content          Optional child nodes.
     */
    @Composable
    fun ImageNode(
        bitmap: Bitmap,
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        apply: ImageNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine, materialLoader, bitmap) {
            ImageNodeImpl(
                materialLoader = materialLoader,
                bitmap = bitmap,
                size = size,
                center = center,
                normal = normal
            ).apply(apply)
        }
        SideEffect {
            node.bitmap = bitmap
        }
        NodeLifecycle(node, content)
    }

    /**
     * A flat plane node that renders an image loaded from [imageFileLocation] in the asset folder.
     *
     * @param imageFileLocation Path to the image file relative to the `assets` folder.
     * @param size              Override size. `null` derives size from the image's aspect ratio.
     * @param center            Center of the image plane in local space.
     * @param normal            Facing direction of the image plane.
     * @param apply             Additional configuration on the [ImageNodeImpl].
     * @param content           Optional child nodes.
     */
    @Composable
    fun ImageNode(
        imageFileLocation: String,
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        apply: ImageNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine, materialLoader, imageFileLocation) {
            ImageNodeImpl(
                materialLoader = materialLoader,
                imageFileLocation = imageFileLocation,
                size = size,
                center = center,
                normal = normal
            ).apply(apply)
        }
        NodeLifecycle(node, content)
    }

    /**
     * A flat plane node that renders an image from a drawable resource.
     *
     * @param imageResId Drawable resource ID.
     * @param size       Override size. `null` derives size from the image's aspect ratio.
     * @param center     Center of the image plane in local space.
     * @param normal     Facing direction of the image plane.
     * @param apply      Additional configuration on the [ImageNodeImpl].
     * @param content    Optional child nodes.
     */
    @Composable
    fun ImageNode(
        @DrawableRes imageResId: Int,
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        apply: ImageNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine, materialLoader, imageResId) {
            ImageNodeImpl(
                materialLoader = materialLoader,
                imageResId = imageResId,
                size = size,
                center = center,
                normal = normal
            ).apply(apply)
        }
        NodeLifecycle(node, content)
    }

    // ── ViewNode ──────────────────────────────────────────────────────────────────────────────────

    /**
     * A node that renders Jetpack Compose UI content onto a flat plane in 3D space.
     *
     * The Compose [viewContent] is rendered to an OpenGL texture and displayed on a plane node,
     * enabling rich UI overlays in the 3D scene (labels, info cards, HUDs, etc.).
     *
     * **Requires a [ViewNode2.WindowManager]** — obtain one with [rememberViewNodeManager]:
     * ```kotlin
     * val windowManager = rememberViewNodeManager()
     * Scene {
     *     ViewNode(windowManager = windowManager) {
     *         Text("Hello from 3D!")
     *     }
     * }
     * ```
     *
     * @param windowManager         The [ViewNode2.WindowManager] to attach the view to.
     * @param unlit                 If `true`, ignores scene lighting (always fully bright).
     * @param invertFrontFaceWinding Inverts face winding — useful for front-facing AR cameras.
     * @param apply                 Additional configuration on the [ViewNode2] instance.
     * @param content               Optional 3D child nodes in a [NodeScope].
     * @param viewContent           The Compose UI to render inside the 3D node.
     */
    @Composable
    fun ViewNode(
        windowManager: ViewNode2.WindowManager,
        unlit: Boolean = false,
        invertFrontFaceWinding: Boolean = false,
        apply: ViewNode2.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null,
        viewContent: @Composable () -> Unit
    ) {
        val node = remember(engine, windowManager) {
            ViewNode2(
                engine = engine,
                windowManager = windowManager,
                materialLoader = materialLoader,
                unlit = unlit,
                invertFrontFaceWinding = invertFrontFaceWinding,
                content = viewContent
            ).apply(apply)
        }
        NodeLifecycle(node, content)
    }

    // ── MeshNode ──────────────────────────────────────────────────────────────────────────────────

    /**
     * A node with custom mesh geometry defined by a [VertexBuffer] and [IndexBuffer].
     *
     * @param primitiveType    How vertices are interpreted (TRIANGLES, LINES, POINTS, etc.).
     * @param vertexBuffer     The GPU vertex buffer.
     * @param indexBuffer      The GPU index buffer.
     * @param materialInstance Optional material to apply to the mesh.
     * @param apply            Additional configuration on the [MeshNodeImpl].
     * @param content          Optional child nodes.
     */
    @Composable
    fun MeshNode(
        primitiveType: RenderableManager.PrimitiveType,
        vertexBuffer: VertexBuffer,
        indexBuffer: IndexBuffer,
        materialInstance: MaterialInstance? = null,
        apply: MeshNodeImpl.() -> Unit = {},
        content: (@Composable NodeScope.() -> Unit)? = null
    ) {
        val node = remember(engine, vertexBuffer, indexBuffer) {
            MeshNodeImpl(
                engine = engine,
                primitiveType = primitiveType,
                vertexBuffer = vertexBuffer,
                indexBuffer = indexBuffer,
                materialInstance = materialInstance
            ).apply(apply)
        }
        NodeLifecycle(node, content)
    }

    // ── Internal lifecycle helper ─────────────────────────────────────────────────────────────────

    /**
     * Internal helper shared by all node composables.
     *
     * Attaches [node] to this scope's container on entry and detaches/destroys it on exit.
     * Also runs any nested [content] inside a [NodeScope] receiver.
     */
    @Composable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    fun NodeLifecycle(
        node: NodeImpl,
        content: (@Composable NodeScope.() -> Unit)?
    ) {
        DisposableEffect(node) {
            attach(node)
            onDispose {
                detach(node)
                node.destroy()
            }
        }
        if (content != null) {
            NodeScope(parentNode = node, scope = this).content()
        }
    }
}

// ── NodeScope ─────────────────────────────────────────────────────────────────────────────────────

/**
 * Composable DSL scope for declaring child nodes under a specific parent node.
 *
 * `NodeScope` is the receiver of the optional `content` trailing lambda accepted by every node
 * composable in [SceneScope]. Composables declared inside are attached as children of
 * [parentNode] rather than the scene root, mirroring how nested `Column`/`Box` composables
 * work in standard Compose UI.
 *
 * ```kotlin
 * Scene {
 *     Node(position = Position(y = 0.5f)) {  // <- this block is a NodeScope
 *         ModelNode(modelInstance = helmet)   // child of the Node above
 *         CubeNode(size = Size(0.05f))        // sibling, also a child of Node
 *     }
 * }
 * ```
 *
 * @param parentNode The node that newly declared composables are attached to as children.
 * @param scope      The parent [SceneScope] providing shared resources (engine, loaders, etc.).
 */
@SceneDsl
class NodeScope internal constructor(
    val parentNode: NodeImpl,
    scope: SceneScope
) : SceneScope(
    engine = scope.engine,
    modelLoader = scope.modelLoader,
    materialLoader = scope.materialLoader,
    environmentLoader = scope.environmentLoader,
    _nodes = scope._nodes
) {
    /**
     * Attaches [node] as a child of [parentNode].
     */
    override fun attach(node: NodeImpl) {
        parentNode.addChildNode(node)
    }

    /**
     * Removes [node] from [parentNode]'s children.
     */
    override fun detach(node: NodeImpl) {
        parentNode.removeChildNode(node)
    }
}
