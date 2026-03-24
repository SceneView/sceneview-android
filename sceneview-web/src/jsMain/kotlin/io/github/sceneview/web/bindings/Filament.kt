@file:JsModule("filament")
@file:JsNonModule

package io.github.sceneview.web.bindings

import org.w3c.dom.HTMLCanvasElement

// Filament.js external declarations — Kotlin/JS bindings for the WASM renderer
// These map 1:1 to the filament npm package TypeScript definitions.

external fun init(assets: Array<String>, onReady: () -> Unit)

external class Engine {
    companion object {
        fun create(canvas: HTMLCanvasElement, contextOptions: dynamic = definedExternally): Engine
        fun destroy(engine: Engine)
    }

    fun createRenderer(): Renderer
    fun createScene(): Scene
    fun createView(): View
    fun createCamera(entity: Int): Camera
    fun createSwapChain(): SwapChain
    fun createAssetLoader(): AssetLoader
    fun createIblFromKtx1(buffer: dynamic): IndirectLight
    fun createSkyFromKtx1(buffer: dynamic): Skybox
    fun destroyRenderer(renderer: Renderer)
    fun destroyScene(scene: Scene)
    fun destroyView(view: View)
    fun destroySwapChain(swapChain: SwapChain)

    val transformManager: TransformManager
    val renderableManager: RenderableManager
    val lightManager: LightManager
}

external class Renderer {
    fun render(swapChain: SwapChain, view: View)
    fun beginFrame(swapChain: SwapChain, timestamp: Double = definedExternally): Boolean
    fun endFrame()
    fun renderView(view: View)
    fun setClearOptions(options: dynamic)
}

external class Scene {
    fun addEntity(entity: Int)
    fun addEntities(entities: dynamic)
    fun remove(entity: Int)
    fun removeEntities(entities: dynamic)
    fun setIndirectLight(light: IndirectLight?)
    fun setSkybox(skybox: Skybox?)
    fun getLightCount(): Int
    fun getRenderableCount(): Int
}

external class View {
    fun setCamera(camera: Camera)
    fun setScene(scene: Scene)
    fun setViewport(viewport: dynamic)
    fun setBloomOptions(options: dynamic)
    fun setAmbientOcclusionOptions(options: dynamic)
    fun setTemporalAntiAliasingOptions(options: dynamic)
    fun setColorGrading(grading: dynamic)
    fun setRenderTarget(target: dynamic)
}

external class Camera {
    fun setProjection(
        projection: Int,
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        near: Double,
        far: Double
    )

    fun setProjectionFov(
        fovInDegrees: Double,
        aspect: Double,
        near: Double,
        far: Double,
        fov: Int = definedExternally
    )

    fun lookAt(
        eyeX: Double, eyeY: Double, eyeZ: Double,
        centerX: Double, centerY: Double, centerZ: Double,
        upX: Double, upY: Double, upZ: Double
    )

    fun setExposure(aperture: Double, shutterSpeed: Double, sensitivity: Double)
    fun setModelMatrix(matrix: dynamic)
    fun getProjectionMatrix(): dynamic
    fun getViewMatrix(): dynamic
}

external class SwapChain

external class IndirectLight
external class Skybox

external class TransformManager {
    fun getInstance(entity: Int): Int
    fun setTransform(instance: Int, matrix: dynamic)
    fun getTransform(instance: Int): dynamic
}

external class RenderableManager {
    fun getInstance(entity: Int): Int
    fun setMaterialInstanceAt(instance: Int, primitiveIndex: Int, materialInstance: dynamic)
}

external class LightManager {
    fun getInstance(entity: Int): Int
    fun setIntensity(instance: Int, intensity: Double)
    fun setColor(instance: Int, r: Float, g: Float, b: Float)
    fun setDirection(instance: Int, x: Float, y: Float, z: Float)
    fun setPosition(instance: Int, x: Float, y: Float, z: Float)
}

external class AssetLoader {
    fun createAsset(buffer: dynamic): FilamentAsset?
    fun createInstancedAsset(buffer: dynamic, instances: dynamic): FilamentAsset?
}

external class FilamentAsset {
    fun getEntities(): dynamic
    fun getRoot(): Int
    fun getBoundingBox(): dynamic
    fun releaseSourceData()
    val animator: Animator?
}

external class Animator {
    fun applyAnimation(animationIndex: Int, time: Double = definedExternally)
    fun updateBoneMatrices()
    fun getAnimationCount(): Int
    fun getAnimationDuration(animationIndex: Int): Double
    fun getAnimationName(animationIndex: Int): String
}

// Entity utilities
external object EntityManager {
    fun get(): EntityManagerInstance
}

external class EntityManagerInstance {
    fun create(): Int
}
