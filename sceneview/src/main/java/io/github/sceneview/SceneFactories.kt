package io.github.sceneview

import android.content.Context
import android.opengl.EGLContext
import com.google.android.filament.ColorGrading
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Skybox
import com.google.android.filament.ToneMapper
import com.google.android.filament.View
import com.google.android.filament.View.AntiAliasing
import com.google.android.filament.View.QualityLevel
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Utils
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.environment.Environment
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.managers.color
import io.github.sceneview.math.Position
import io.github.sceneview.math.colorOf
import io.github.sceneview.math.toColor
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ViewNode2
import io.github.sceneview.utils.OpenGL
import io.github.sceneview.utils.readBuffer

// Initialize Filament once (triggered when this file's class is first loaded)
private val filamentInit: Unit = run {
    Gltfio.init()
    Filament.init()
    Utils.init()
}

const val DEFAULT_MAIN_LIGHT_COLOR_TEMPERATURE = 6_500.0f
const val DEFAULT_MAIN_LIGHT_COLOR_INTENSITY = 100_000.0f
val DEFAULT_MAIN_LIGHT_COLOR = Colors.cct(DEFAULT_MAIN_LIGHT_COLOR_TEMPERATURE).toColor()
val DEFAULT_MAIN_LIGHT_INTENSITY = DEFAULT_MAIN_LIGHT_COLOR_INTENSITY
val DEFAULT_OBJECT_POSITION = Position(0.0f, 0.0f, -4.0f)

fun createEglContext(): EGLContext {
    filamentInit  // ensure init
    return OpenGL.createEglContext()
}

fun createEngine(eglContext: EGLContext): Engine = Engine.create(eglContext)

fun createScene(engine: Engine): com.google.android.filament.Scene = engine.createScene()

fun createView(engine: Engine): View = engine.createView().apply {
    renderQuality = renderQuality.apply {
        hdrColorBuffer = QualityLevel.MEDIUM
    }
    dynamicResolutionOptions = dynamicResolutionOptions.apply {
        enabled = false
        homogeneousScaling = true
        quality = QualityLevel.MEDIUM
    }
    multiSampleAntiAliasingOptions = multiSampleAntiAliasingOptions.apply {
        enabled = false
    }
    antiAliasing = AntiAliasing.FXAA
    ambientOcclusionOptions = ambientOcclusionOptions.apply {
        enabled = false
    }
    colorGrading = ColorGrading.Builder()
        .toneMapper(ToneMapper.Filmic())
        .build(engine)
    setShadowingEnabled(false)
}

fun createRenderer(engine: Engine): Renderer = engine.createRenderer()

fun createCameraNode(engine: Engine): CameraNode = DefaultCameraNode(engine)

fun createMainLightNode(engine: Engine): LightNode = DefaultLightNode(engine)

fun createDefaultCameraManipulator(
    orbitHomePosition: Position? = null,
    targetPosition: Position? = null
) = CameraGestureDetector.DefaultCameraManipulator(
    orbitHomePosition = orbitHomePosition,
    targetPosition = targetPosition
)

fun createViewNodeManager(context: Context) = ViewNode2.WindowManager(context)

fun createEnvironment(
    environmentLoader: EnvironmentLoader,
    isOpaque: Boolean = true
) = createEnvironment(
    engine = environmentLoader.engine,
    isOpaque = isOpaque,
    indirectLight = KTX1Loader.createIndirectLight(
        environmentLoader.engine,
        environmentLoader.context.assets.readBuffer("environments/neutral/neutral_ibl.ktx"),
    )
)

fun createEnvironment(
    engine: Engine,
    isOpaque: Boolean = true,
    indirectLight: IndirectLight? = null,
    skybox: Skybox? = Skybox.Builder()
        .color(colorOf(rgb = 0.0f, a = if (isOpaque) 1.0f else 0.0f).toFloatArray())
        .build(engine),
    sphericalHarmonics: List<Float>? = null
) = Environment(indirectLight, skybox, sphericalHarmonics)

fun createCollisionSystem(view: View) = CollisionSystem(view)

class DefaultCameraNode(engine: Engine) : CameraNode(engine) {
    init {
        transform = io.github.sceneview.math.Transform(position = Position(0.0f, 0.0f, 1.0f))
        setExposure(16.0f, 1.0f / 125.0f, 100.0f)
    }
}

class DefaultLightNode(engine: Engine) : LightNode(
    engine = engine,
    type = LightManager.Type.DIRECTIONAL,
    apply = {
        color(DEFAULT_MAIN_LIGHT_COLOR)
        intensity(DEFAULT_MAIN_LIGHT_COLOR_INTENSITY)
        direction(0.0f, -1.0f, 0.0f)
        castShadows(true)
    }
)
