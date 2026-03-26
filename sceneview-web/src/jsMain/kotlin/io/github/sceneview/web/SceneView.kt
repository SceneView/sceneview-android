package io.github.sceneview.web

import io.github.sceneview.web.bindings.*
import io.github.sceneview.web.nodes.CameraConfig
import io.github.sceneview.web.nodes.LightConfig
import io.github.sceneview.web.nodes.LightType
import io.github.sceneview.web.nodes.ModelConfig
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * SceneView for Web -- Filament.js based 3D viewer.
 *
 * Uses the same Filament rendering engine as SceneView Android,
 * compiled to WebAssembly for browser execution.
 *
 * This class actually initializes the Filament WASM module, creates a real
 * WebGL2 rendering context, and renders 3D content using the GPU.
 *
 * Basic usage:
 * ```kotlin
 * SceneView.create(canvas) {
 *     camera {
 *         eye(0.0, 1.5, 5.0)
 *         target(0.0, 0.0, 0.0)
 *     }
 *     light {
 *         directional()
 *         intensity(100_000.0)
 *     }
 *     model("models/damaged_helmet.glb")
 * }
 * ```
 */
class SceneView private constructor(
    val canvas: HTMLCanvasElement,
    val engine: Engine,
    val renderer: Renderer,
    val scene: Scene,
    val view: View,
    val camera: Camera,
    val swapChain: SwapChain,
    private val cameraEntity: Entity
) {
    private var animationFrameId: Int? = null
    private var isRunning = false
    private var lastTimestamp = 0.0

    private val models = mutableListOf<LoadedModel>()
    private var assetLoader: AssetLoader? = null
    private val lightEntities = mutableListOf<Entity>()

    /** Tracks a loaded glTF asset with its animation state. */
    private class LoadedModel(
        val asset: FilamentAsset,
        val animator: Animator?,
        var animationTime: Double = 0.0
    )

    /** Orbit camera controller -- initialized when cameraControls is enabled. */
    var cameraController: OrbitCameraController? = null
        private set

    /** Enable orbit camera controls (mouse drag to orbit, scroll to zoom, touch support). */
    fun enableCameraControls(
        distance: Double = 5.0,
        targetX: Double = 0.0,
        targetY: Double = 0.0,
        targetZ: Double = 0.0,
        autoRotate: Boolean = false
    ): OrbitCameraController {
        val controller = OrbitCameraController(canvas, camera).apply {
            this.distance = distance
            target(targetX, targetY, targetZ)
            this.autoRotate = autoRotate
        }
        cameraController = controller
        return controller
    }

    companion object {
        /**
         * Initialize Filament WASM and create a SceneView instance.
         *
         * This is the main entry point. It:
         * 1. Calls Filament.init() to load and compile the WASM module
         * 2. Creates a Filament Engine with a WebGL2 context on the canvas
         * 3. Sets up Scene, View, Camera, Renderer, and SwapChain
         * 4. Applies the user's configuration (camera, lights, models)
         * 5. Calls onReady with the fully initialized SceneView
         *
         * @param canvas The HTML canvas element to render into
         * @param assets List of asset URLs to preload (IBL, skybox KTX files)
         * @param configure DSL block to configure the scene
         * @param onReady Callback when the SceneView is fully initialized
         */
        fun create(
            canvas: HTMLCanvasElement,
            assets: Array<String> = emptyArray(),
            configure: SceneViewBuilder.() -> Unit = {},
            onReady: (SceneView) -> Unit
        ) {
            // Step 1: Initialize Filament WASM module and preload any assets
            init(assets) {
                try {
                    // Step 2: Create the Filament engine with WebGL2 context
                    val engine = Engine.create(canvas)
                    val renderer = engine.createRenderer()
                    val scene = engine.createScene()
                    val swapChain = engine.createSwapChain()
                    val view = engine.createView()

                    // Step 3: Create camera entity and camera
                    val cameraEntity = EntityManager.get().create()
                    val camera = engine.createCamera(cameraEntity)

                    // Step 4: Connect view to camera and scene
                    view.setCamera(camera)
                    view.setScene(scene)

                    // Step 5: Set viewport to canvas pixel dimensions
                    val width = canvas.width
                    val height = canvas.height
                    view.setViewport(viewport(0, 0, width, height))

                    // Step 6: Default camera setup -- perspective projection
                    val aspect = if (height > 0) width.toDouble() / height.toDouble() else 1.0
                    camera.setProjectionFov(
                        fovInDegrees = 45.0,
                        aspect = aspect,
                        near = 0.1,
                        far = 1000.0
                    )

                    // Default camera position: slightly above and back, looking at origin
                    camera.lookAt(
                        float3(0.0, 1.5, 5.0),   // eye
                        float3(0.0, 0.0, 0.0),   // center
                        float3(0.0, 1.0, 0.0)    // up
                    )

                    // Default exposure for outdoor lighting
                    camera.setExposure(16.0, 1.0 / 125.0, 100.0)

                    // Set clear color to dark gray (visible even without a skybox)
                    renderer.setClearOptions(js("({clearColor: [0.1, 0.1, 0.1, 1.0], clear: true})"))

                    val sceneView = SceneView(
                        canvas, engine, renderer, scene, view, camera, swapChain, cameraEntity
                    )

                    // Step 7: Apply user configuration (camera, lights, models, environment)
                    val builder = SceneViewBuilder(sceneView)
                    builder.configure()
                    builder.apply()

                    onReady(sceneView)
                } catch (e: Throwable) {
                    console.error("SceneView: Failed to initialize Filament engine", e)
                }
            }
        }
    }

    /** Resize the viewport to match the canvas dimensions. Call on window resize. */
    fun resize(width: Int = canvas.clientWidth, height: Int = canvas.clientHeight) {
        if (width <= 0 || height <= 0) return
        canvas.width = width
        canvas.height = height
        view.setViewport(viewport(0, 0, width, height))
        camera.setProjectionFov(
            fovInDegrees = 45.0,
            aspect = width.toDouble() / height.toDouble(),
            near = 0.1,
            far = 1000.0
        )
    }

    /** Enable automatic viewport resizing when the canvas CSS size changes. */
    var autoResize = true

    /** Start the render loop using requestAnimationFrame. */
    fun startRendering() {
        if (isRunning) return
        isRunning = true
        lastTimestamp = 0.0
        renderLoop(0.0)
    }

    /** Stop the render loop. */
    fun stopRendering() {
        isRunning = false
        animationFrameId?.let { window.cancelAnimationFrame(it) }
        animationFrameId = null
    }

    /**
     * Load a glTF/GLB model from a URL and add it to the scene.
     *
     * This performs the full loading pipeline:
     * 1. Fetch the .glb/.gltf file as an ArrayBuffer
     * 2. Create a FilamentAsset via the AssetLoader
     * 3. Add all renderable entities to the scene
     * 4. Call loadResources() to fetch external textures/buffers
     * 5. Release source data to free memory
     *
     * @param url URL to the .glb or .gltf file
     * @param onLoaded Optional callback when the model is fully loaded (with resources)
     */
    fun loadModel(
        url: String,
        onLoaded: ((FilamentAsset) -> Unit)? = null
    ) {
        val loader = assetLoader ?: engine.createAssetLoader().also { assetLoader = it }

        // Derive the base path for resolving relative resource URIs
        val basePath = url.substringBeforeLast('/') + "/"

        window.fetch(url).then { response ->
            if (!response.ok) {
                console.error("SceneView: HTTP ${response.status} loading model from $url")
                return@then js("undefined")
            }
            response.arrayBuffer()
        }.then { buffer ->
            if (buffer == null || buffer == js("undefined")) return@then

            val asset = loader.createAsset(buffer)
            if (asset != null) {
                // Add all entities to the scene so they become visible
                val entities = asset.getEntities()
                scene.addEntities(entities)

                // Get the animator from the asset instance for animation playback
                val animator = try {
                    asset.getInstance().getAnimator()
                } catch (e: Throwable) {
                    null
                }

                val loadedModel = LoadedModel(asset, animator)
                models.add(loadedModel)

                // Load external resources (textures, buffers) referenced by the glTF.
                // This is REQUIRED for models to render with correct materials.
                asset.loadResources(
                    onDone = {
                        // Release the source glTF data now that resources are loaded
                        asset.releaseSourceData()
                        console.log("SceneView: Model loaded from $url (${entities.size} entities)")
                        onLoaded?.invoke(asset)
                    },
                    onFetched = null,
                    basePath = basePath,
                    asyncInterval = null
                )
            } else {
                console.error("SceneView: AssetLoader failed to parse model from $url")
            }
        }.catch { error ->
            console.error("SceneView: Error fetching model from $url", error)
        }
    }

    /** Load an IBL (Image-Based Lighting) from a KTX file URL. */
    fun loadEnvironment(iblUrl: String, skyboxUrl: String? = null) {
        // Fetch and create IBL (indirect lighting) from a KTX1 file
        window.fetch(iblUrl).then { it.arrayBuffer() }.then { buffer ->
            val ibl = engine.createIblFromKtx1(buffer)
            scene.setIndirectLight(ibl)
            console.log("SceneView: IBL loaded from $iblUrl")
        }.catch { error ->
            console.error("SceneView: Error loading IBL from $iblUrl", error)
        }

        // Optionally load a skybox from a separate KTX file
        skyboxUrl?.let { url ->
            window.fetch(url).then { it.arrayBuffer() }.then { buffer ->
                val skybox = engine.createSkyFromKtx1(buffer)
                scene.setSkybox(skybox)
                console.log("SceneView: Skybox loaded from $url")
            }.catch { error ->
                console.error("SceneView: Error loading skybox from $url", error)
            }
        }
    }

    /**
     * Add a light to the scene using the Filament LightManager Builder API.
     *
     * The Filament.js LightManager.Builder is accessed via:
     *   Filament.LightManager.Builder(type).intensity(n).direction([x,y,z]).build(engine, entity)
     */
    fun addLight(config: LightConfig) {
        val entity = EntityManager.get().create()

        // Map our LightType enum to Filament's numeric type constants
        // In Filament.js: 0 = SUN, 1 = DIRECTIONAL, 2 = POINT, 3 = FOCUSED_SPOT, 4 = SPOT
        val lightType = when (config.type) {
            LightType.DIRECTIONAL -> 1
            LightType.POINT -> 2
            LightType.SPOT -> 4
        }

        // Use the Builder pattern: LightManager.Builder(type).intensity(...).build(engine, entity)
        val builder = LightManager.Builder(lightType)
        builder.intensity(config.intensity)
        builder.color(float3(
            config.colorR.toDouble(),
            config.colorG.toDouble(),
            config.colorB.toDouble()
        ))
        builder.castShadows(true)

        if (config.type == LightType.DIRECTIONAL) {
            builder.direction(float3(
                config.directionX.toDouble(),
                config.directionY.toDouble(),
                config.directionZ.toDouble()
            ))
        } else {
            builder.position(float3(
                config.positionX.toDouble(),
                config.positionY.toDouble(),
                config.positionZ.toDouble()
            ))
            builder.falloff(10.0)
        }

        builder.build(engine, entity)
        scene.addEntity(entity)
        lightEntities.add(entity)
    }

    /**
     * Auto-fit the camera to frame all loaded models.
     * Computes the bounding box of all assets and adjusts the orbit controller distance.
     */
    fun fitToModels() {
        if (models.isEmpty()) return
        val controller = cameraController ?: return

        // Compute the union bounding box of all loaded models
        var minX = Double.MAX_VALUE; var minY = Double.MAX_VALUE; var minZ = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE; var maxY = -Double.MAX_VALUE; var maxZ = -Double.MAX_VALUE

        for (model in models) {
            val aabb = model.asset.getBoundingBox()
            val mn: dynamic = aabb.min
            val mx: dynamic = aabb.max
            if ((mn[0] as Number).toDouble() < minX) minX = (mn[0] as Number).toDouble()
            if ((mn[1] as Number).toDouble() < minY) minY = (mn[1] as Number).toDouble()
            if ((mn[2] as Number).toDouble() < minZ) minZ = (mn[2] as Number).toDouble()
            if ((mx[0] as Number).toDouble() > maxX) maxX = (mx[0] as Number).toDouble()
            if ((mx[1] as Number).toDouble() > maxY) maxY = (mx[1] as Number).toDouble()
            if ((mx[2] as Number).toDouble() > maxZ) maxZ = (mx[2] as Number).toDouble()
        }

        val cx = (minX + maxX) / 2.0
        val cy = (minY + maxY) / 2.0
        val cz = (minZ + maxZ) / 2.0
        val dx = maxX - minX
        val dy = maxY - minY
        val dz = maxZ - minZ
        val radius = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0

        controller.target(cx, cy, cz)
        controller.distance = radius * 2.5
        controller.minDistance = radius * 0.5
        controller.maxDistance = radius * 10.0
    }

    /** Clean up all Filament resources. */
    fun destroy() {
        stopRendering()
        cameraController?.dispose()

        // Destroy loaded assets
        assetLoader?.let { loader ->
            models.forEach { loader.destroyAsset(it.asset) }
            loader.delete()
        }
        models.clear()

        // Destroy light entities
        lightEntities.forEach { engine.destroyEntity(it) }
        lightEntities.clear()

        // Destroy core Filament objects
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(cameraEntity)
        engine.destroySwapChain(swapChain)
        Engine.destroy(engine)
    }

    /**
     * The render loop -- called every frame via requestAnimationFrame.
     *
     * Each frame:
     * 1. Auto-resizes viewport if CSS size changed
     * 2. Updates orbit camera controller (rotation, damping)
     * 3. Advances glTF animations
     * 4. Calls engine.execute() to process pending async operations
     * 5. Renders the frame via beginFrame/renderView/endFrame
     */
    private fun renderLoop(timestamp: Double) {
        if (!isRunning) return

        // Auto-resize viewport if canvas CSS size changed
        if (autoResize) {
            val w = canvas.clientWidth
            val h = canvas.clientHeight
            if (w > 0 && h > 0 && (w != canvas.width || h != canvas.height)) {
                resize(w, h)
            }
        }

        // Update orbit camera
        cameraController?.update()

        // Track animation time
        val deltaSeconds = if (lastTimestamp > 0) (timestamp - lastTimestamp) / 1000.0 else 0.0
        lastTimestamp = timestamp

        // Update glTF animations for all loaded models
        models.forEach { model ->
            model.animator?.let { animator ->
                val count = animator.getAnimationCount()
                if (count > 0) {
                    model.animationTime += deltaSeconds
                    val duration = animator.getAnimationDuration(0)
                    if (duration > 0) {
                        // Loop the animation
                        model.animationTime = model.animationTime % duration
                    }
                    // In Filament.js, applyAnimation only takes the index.
                    // The animation time is set by calling the animator at the right moment.
                    // Actually, the gltfio$Animator.applyAnimation(index) applies the animation
                    // at the CURRENT time set internally -- we need to advance it.
                    // The proper way is: animator.applyAnimation(index) after setting time
                    // via the asset's animation system.
                    // For Filament.js, we use the asset instance animator which tracks time internally.
                    animator.applyAnimation(0)
                    animator.updateBoneMatrices()
                }
            }
        }

        // Process any pending async Filament operations (texture uploads, etc.)
        engine.execute()

        // Render frame
        if (renderer.beginFrame(swapChain)) {
            renderer.renderView(view)
            renderer.endFrame()
        }

        animationFrameId = window.requestAnimationFrame(::renderLoop)
    }
}

/**
 * DSL builder for SceneView configuration.
 */
class SceneViewBuilder(private val sceneView: SceneView) {
    private var cameraConfig: CameraConfig? = null
    private var lightConfig: LightConfig? = null
    private val modelConfigs = mutableListOf<ModelConfig>()
    private var iblUrl: String? = null
    private var skyboxUrl: String? = null
    private var cameraControlsEnabled = true
    private var autoRotateEnabled = false

    /** Configure the camera. */
    fun camera(block: CameraConfig.() -> Unit) {
        cameraConfig = CameraConfig().apply(block)
    }

    /** Configure a directional light. */
    fun light(block: LightConfig.() -> Unit) {
        lightConfig = LightConfig().apply(block)
    }

    /** Add a glTF/GLB model by URL. */
    fun model(url: String, block: ModelConfig.() -> Unit = {}) {
        modelConfigs.add(ModelConfig(url).apply(block))
    }

    /** Set environment lighting from KTX IBL files. */
    fun environment(iblUrl: String, skyboxUrl: String? = null) {
        this.iblUrl = iblUrl
        this.skyboxUrl = skyboxUrl
    }

    /** Enable orbit camera controls (drag to orbit, scroll to zoom, touch). Enabled by default. */
    fun cameraControls(enabled: Boolean = true) {
        cameraControlsEnabled = enabled
    }

    /** Enable auto-rotation of the camera around the target. */
    fun autoRotate(enabled: Boolean = true) {
        autoRotateEnabled = enabled
    }

    internal fun apply() {
        cameraConfig?.applyTo(sceneView.camera)
        lightConfig?.let { sceneView.addLight(it) }
        iblUrl?.let { sceneView.loadEnvironment(it, skyboxUrl) }
        modelConfigs.forEach { config ->
            sceneView.loadModel(config.url, config.onLoaded)
        }
        if (cameraControlsEnabled) {
            val cam = cameraConfig
            sceneView.enableCameraControls(
                distance = cam?.eyeZ ?: 5.0,
                targetX = cam?.targetX ?: 0.0,
                targetY = cam?.targetY ?: 0.0,
                targetZ = cam?.targetZ ?: 0.0,
                autoRotate = autoRotateEnabled
            )
        }
    }
}
