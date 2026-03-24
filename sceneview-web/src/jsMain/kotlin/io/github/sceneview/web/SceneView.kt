package io.github.sceneview.web

import io.github.sceneview.web.bindings.*
import io.github.sceneview.web.nodes.CameraConfig
import io.github.sceneview.web.nodes.LightConfig
import io.github.sceneview.web.nodes.LightType
import io.github.sceneview.web.nodes.ModelConfig
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * SceneView for Web — Filament.js based 3D viewer.
 *
 * Uses the same Filament rendering engine as SceneView Android,
 * compiled to WebAssembly for browser execution.
 *
 * Basic usage:
 * ```kotlin
 * val sceneView = SceneView.create(canvas) {
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
    val swapChain: SwapChain
) {
    private var animationFrameId: Int? = null
    private var isRunning = false

    private val models = mutableListOf<FilamentAsset>()
    private var assetLoader: AssetLoader? = null

    /** Orbit camera controller — initialized when cameraControls is enabled. */
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
            init(assets) {
                val engine = Engine.create(canvas)
                val renderer = engine.createRenderer()
                val scene = engine.createScene()
                val view = engine.createView()
                val swapChain = engine.createSwapChain()
                val cameraEntity = EntityManager.get().create()
                val camera = engine.createCamera(cameraEntity)

                view.setCamera(camera)
                view.setScene(scene)

                // Set viewport to canvas size
                val width = canvas.width
                val height = canvas.height
                val viewport = js("[]")
                viewport.push(0, 0, width, height)
                view.setViewport(viewport)

                // Default camera setup
                camera.setProjectionFov(
                    fovInDegrees = 45.0,
                    aspect = width.toDouble() / height.toDouble(),
                    near = 0.1,
                    far = 1000.0
                )
                camera.lookAt(
                    0.0, 1.5, 5.0,  // eye
                    0.0, 0.0, 0.0,  // center
                    0.0, 1.0, 0.0   // up
                )

                // Default exposure
                camera.setExposure(16.0, 1.0 / 125.0, 100.0)

                val sceneView = SceneView(canvas, engine, renderer, scene, view, camera, swapChain)

                // Apply user configuration
                val builder = SceneViewBuilder(sceneView)
                builder.configure()
                builder.apply()

                onReady(sceneView)
            }
        }
    }

    /** Start the render loop. */
    fun startRendering() {
        if (isRunning) return
        isRunning = true
        renderLoop(0.0)
    }

    /** Stop the render loop. */
    fun stopRendering() {
        isRunning = false
        animationFrameId?.let { window.cancelAnimationFrame(it) }
        animationFrameId = null
    }

    /** Load a glTF/GLB model from a URL. */
    fun loadModel(
        url: String,
        onLoaded: ((FilamentAsset) -> Unit)? = null
    ) {
        val loader = assetLoader ?: engine.createAssetLoader().also { assetLoader = it }

        window.fetch(url).then { response ->
            response.arrayBuffer()
        }.then { buffer ->
            val asset = loader.createAsset(buffer)
            if (asset != null) {
                scene.addEntities(asset.getEntities())
                asset.releaseSourceData()
                models.add(asset)
                onLoaded?.invoke(asset)
            } else {
                console.error("SceneView: Failed to load model from $url")
            }
        }.catch { error ->
            console.error("SceneView: Error loading model from $url", error)
        }
    }

    /** Load an IBL (Image-Based Lighting) from a KTX file URL. */
    fun loadEnvironment(iblUrl: String, skyboxUrl: String? = null) {
        window.fetch(iblUrl).then { it.arrayBuffer() }.then { buffer ->
            val ibl = engine.createIblFromKtx1(buffer)
            scene.setIndirectLight(ibl)
        }
        skyboxUrl?.let { url ->
            window.fetch(url).then { it.arrayBuffer() }.then { buffer ->
                val skybox = engine.createSkyFromKtx1(buffer)
                scene.setSkybox(skybox)
            }
        }
    }

    /**
     * Add a light to the scene.
     *
     * Uses Filament's LightManager to create a light entity and configure it.
     */
    fun addLight(config: LightConfig) {
        val entity = EntityManager.get().create()
        val lm = engine.lightManager

        // Filament light type mapping — use js() to access the LightManager.Type enum
        val lightType = when (config.type) {
            LightType.DIRECTIONAL -> 0 // LightManager.Type.DIRECTIONAL
            LightType.POINT -> 1       // LightManager.Type.POINT
            LightType.SPOT -> 3        // LightManager.Type.SPOT
        }

        // Build light via the LightManager — using dynamic JS interop
        // since Filament.js uses a builder pattern not directly expressible in Kotlin externals
        js("Filament.LightManager.Builder(lightType).intensity(config.intensity).build(engine, entity)")

        val instance = lm.getInstance(entity)
        lm.setColor(instance, config.colorR, config.colorG, config.colorB)

        if (config.type == LightType.DIRECTIONAL) {
            lm.setDirection(instance, config.directionX, config.directionY, config.directionZ)
        } else {
            lm.setPosition(instance, config.positionX, config.positionY, config.positionZ)
        }

        scene.addEntity(entity)
    }

    /** Clean up all resources. */
    fun destroy() {
        stopRendering()
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroySwapChain(swapChain)
        Engine.destroy(engine)
    }

    private fun renderLoop(timestamp: Double) {
        if (!isRunning) return

        // Update orbit camera
        cameraController?.update()

        // Update animations
        models.forEach { asset ->
            asset.animator?.let { animator ->
                if (animator.getAnimationCount() > 0) {
                    animator.applyAnimation(0)
                    animator.updateBoneMatrices()
                }
            }
        }

        // Render frame
        if (renderer.beginFrame(swapChain, timestamp)) {
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
