package io.github.sceneview.ar.arcore

import android.content.Context
import android.view.Display
import android.view.WindowManager
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.*
import io.github.sceneview.ar.ArSceneLifecycle
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.defaultApproximateDistance
import io.github.sceneview.utils.FrameTime

class ArSession(
    val lifecycle: ArSceneLifecycle,
    features: Set<Feature> = setOf()
) : Session(lifecycle.context, features), ArSceneLifecycleObserver {

    val display: Display by lazy {
        (lifecycle.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).getDefaultDisplay()
    }

    // We use device display sizes by default cause the onSurfaceChanged may be called after the
    // onResume and ARCore doesn't appreciate that we do things on session before calling
    // setDisplayGeometry()
    // = flickering screen if the phone is locked when starting the app
    var displayRotation = display.rotation
    var displayWidth = display.width
    var displayHeight = display.height

    var isResumed = false

    // TODO: See if it really has a performance impact
//    private var _config: Config? = null

    var hasAugmentedImageDatabase = false

    /**
     * ### The distance at which to create an InstantPlacementPoint in meters
     *
     * This is only used while the tracking method for the returned point is
     * InstantPlacementPoint.TrackingMethod#SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
     */
    val approximateDistance = defaultApproximateDistance

    /**
     * ### The most recent ARCore Frame if it is available
     *
     * The frame is updated at the beginning of each drawing frame.
     * Callers of this method should not retain a reference to the return value, since it will be
     * invalid to use the ARCore frame starting with the next frame.
     */
    var currentFrame: ArFrame? = null
        private set

    var allTrackables: List<Trackable> = listOf()

    init {
        lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        resume()
    }

    override fun resume() {
        isResumed = true
        super.resume()

        // Don't remove this code-block. It is important to correctly set the DisplayGeometry for
        // the ArCore-Session if for example the permission Dialog is shown on the screen.
        // If we remove this part, the camera is flickering if returned from the permission Dialog.
        setDisplayGeometry(displayRotation, displayWidth, displayHeight)
        lifecycle.dispatchEvent<ArSceneLifecycleObserver> { onArSessionResumed(this@ArSession) }
    }

    override fun onPause(owner: LifecycleOwner) {
        pause()
    }

    override fun pause() {
        isResumed = false
        super.pause()
    }

    /**
     * ### Explicitly close the ARCore session to release native resources.
     *
     * Review the API reference for important considerations before calling close() in apps with
     * more complicated lifecycle requirements: [Session.close]
     */
    override fun onDestroy(owner: LifecycleOwner) {
        close()
        super.onDestroy(owner)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        setDisplayGeometry(display.rotation, width, height)
    }

    override fun onArFrame(arFrame: ArFrame) {
        allTrackables = getAllTrackables(Trackable::class.java).toList()
    }

    fun update(frameTime: FrameTime): ArFrame? {
        // Check if no frame or same timestamp, no drawing.
        return super.update()?.let { frame ->
            ArFrame(this, frameTime, frame).also {
                currentFrame = it
            }
        }
    }

    override fun setDisplayGeometry(rotation: Int, widthPx: Int, heightPx: Int) {
        displayRotation = rotation
        displayWidth = widthPx
        displayHeight = heightPx
        if (isResumed) {
            super.setDisplayGeometry(displayRotation, widthPx, heightPx)
        }
    }

    /**
     * ### Define the session config used by ARCore
     *
     * Prefer calling this method before the global (Activity or Fragment) onResume() cause the session
     * base configuration in made there.
     * Any later calls (after onSessionResumed()) to this function are not completely sure be taken in
     * account by ARCore (even if most of them will work)
     *
     * Please check that all your Session Config parameters are taken in account by ARCore at
     * runtime.
     *
     * @param config the apply block for the new config
     */
    fun configure(config: (Config) -> Unit) {
        super.configure(this.config.apply {
            config(this)

            if (depthMode != Config.DepthMode.DISABLED && !isDepthModeSupported(depthMode)) {
                depthMode = Config.DepthMode.DISABLED
            }

            // Light estimation is not usable with front camera
            if (cameraConfig.facingDirection == CameraConfig.FacingDirection.FRONT
                && lightEstimationMode != Config.LightEstimationMode.DISABLED
            ) {
                lightEstimationMode = Config.LightEstimationMode.DISABLED
            }
            hasAugmentedImageDatabase = (augmentedImageDatabase?.numImages ?: 0) > 0
        })
        lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
            onArSessionConfigChanged(this@ArSession, this@ArSession.config)
        }
    }

    var focusMode: Config.FocusMode
        get() = config.focusMode
        set(value) {
            if (focusMode != value) {
                configure {
                    it.focusMode = value
                }
            }
        }

    var planeFindingEnabled: Boolean
        get() = config.planeFindingEnabled
        set(value) {
            if (planeFindingEnabled != value) {
                configure {
                    it.planeFindingEnabled = value
                }
            }
        }

    var planeFindingMode: Config.PlaneFindingMode
        get() = config.planeFindingMode
        set(value) {
            if (planeFindingMode != value) {
                configure {
                    it.planeFindingMode = value
                }
            }
        }

    val depthEnabled get() = depthMode != Config.DepthMode.DISABLED

    var depthMode: Config.DepthMode
        get() = config.depthMode
        set(value) {
            if (depthMode != value) {
                configure {
                    it.depthMode = value
                }
            }
        }

    var instantPlacementEnabled: Boolean
        get() = config.instantPlacementEnabled
        set(value) {
            if (instantPlacementEnabled != value) {
                configure {
                    it.instantPlacementEnabled = value
                }
            }
        }

    var cloudAnchorEnabled: Boolean
        get() = config.cloudAnchorEnabled
        set(value) {
            if (cloudAnchorEnabled != value) {
                configure {
                    it.cloudAnchorEnabled = value
                }
            }
        }

    var geospatialEnabled: Boolean
        get() = config.geospatialEnabled
        set(value) {
            if (geospatialEnabled != value) {
                configure {
                    it.geospatialEnabled = value
                }
            }
        }

    /**
     * ### The behavior of the lighting estimation subsystem
     *
     * These modes consist of separate APIs that allow for granular and realistic lighting
     * estimation for directional lighting, shadows, specular highlights, and reflections.
     */
    var lightEstimationMode: Config.LightEstimationMode
        get() = config.lightEstimationMode
        set(value) {
            if (lightEstimationMode != value) {
                configure {
                    it.lightEstimationMode = value
                }
            }
        }

    /**
     * ### Retrieve the session tracked Planes
     */
    val allPlanes: List<Plane> get() = allTrackables.mapNotNull { it as? Plane }

    /**
     * ### Retrieve if the session has already tracked a Plane
     *
     * @return true if the session has tracked at least one Plane
     */
    val hasTrackedPlane: Boolean
        get() = allPlanes.filter {
            it.trackingState in listOf(TrackingState.TRACKING, TrackingState.PAUSED)
        }.isNotEmpty()

    /**
     * ### Retrieve if the session frame is currently tracking a Plane
     *
     * @return true if the session frame is fully tracking at least one Plane
     */
    val isTrackingPlane: Boolean get() = currentFrame?.isTrackingPlane == true

    /**
     * ### Retrieve the session tracked Augmented Images
     */
    val allAugmentedImages: List<AugmentedImage> get() = allTrackables.mapNotNull { it as? AugmentedImage }

    /**
     * ### Retrieve if the session frame is currently tracking an Augmented Image
     *
     * @return true if the session frame is fully tracking at least one Augmented Image
     */
    val isTrackingAugmentedImage: Boolean get() = currentFrame?.isTrackingAugmentedImage == true

    /**
     * ### Retrieve if the session has already tracked an Augmented Image
     *
     * @return true if the session has tracked at least one Augmented Image
     */
    val hasTrackedAugmentedImage: Boolean
        get() = allAugmentedImages.filter {
            it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING &&
                    it.trackingState in listOf(TrackingState.TRACKING, TrackingState.PAUSED)
        }.isNotEmpty()

    /**
     * ### Retrieve the session tracked Augmented Faces
     */
    val allAugmentedFaces: List<AugmentedFace> get() = allTrackables.mapNotNull { it as? AugmentedFace }

    /**
     * ### Retrieve if the session frame is currently tracking an Augmented Face
     *
     * @return true if the session frame is fully tracking at least one Augmented Face
     */
    val isTrackingAugmentedFace: Boolean get() = currentFrame?.isTrackingAugmentedFace == true

    /**
     * ### Retrieve if the session has already tracked an Augmented Face
     *
     * @return true if the session has tracked at least one Augmented Face
     */
    val hasTrackedAugmentedFace: Boolean
        get() = allAugmentedFaces.filter {
            it.trackingState in listOf(TrackingState.TRACKING, TrackingState.PAUSED)
        }.isNotEmpty()
}

var Config.planeFindingEnabled
    get() = planeFindingMode != Config.PlaneFindingMode.DISABLED
    set(value) {
        planeFindingMode = if (value) {
            Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        } else {
            Config.PlaneFindingMode.DISABLED
        }
    }

var Config.instantPlacementEnabled
    get() = instantPlacementMode != Config.InstantPlacementMode.DISABLED
    set(value) {
        instantPlacementMode = if (value) {
            Config.InstantPlacementMode.LOCAL_Y_UP
        } else {
            Config.InstantPlacementMode.DISABLED
        }
    }

var Config.cloudAnchorEnabled
    get() = cloudAnchorMode != Config.CloudAnchorMode.DISABLED
    set(value) {
        cloudAnchorMode = if (value) {
            Config.CloudAnchorMode.ENABLED
        } else {
            Config.CloudAnchorMode.DISABLED
        }
    }

var Config.geospatialEnabled
    get() = geospatialMode != Config.GeospatialMode.DISABLED
    set(value) {
        geospatialMode = if (value) {
            Config.GeospatialMode.ENABLED
        } else {
            Config.GeospatialMode.DISABLED
        }
    }
