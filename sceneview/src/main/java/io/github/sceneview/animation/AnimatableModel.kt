package io.github.sceneview.animation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.text.TextUtils
import android.view.animation.LinearInterpolator
import io.github.sceneview.collision.Preconditions

/**
 * An AnimatableModel is an object whose properties can be animated by an [ModelAnimation].
 * The object animation transformation can be done at the desired frame callback place.
 * <br>This means that it's up to the AnimatableModel to apply transformations as soon as the
 * [ModelAnimation] updates the value (like in a standard
 * [ObjectAnimator] context) or to apply them inside a global/common
 * frame callback.
 * <p>
 * An AnimatableModel can, for example, update the data of each animated property managed by an
 * [com.google.android.filament.gltfio.Animator].
 */
interface AnimatableModel {

    /**
     * Get the associated [ModelAnimation] at the given index or throw
     * an [IndexOutOfBoundsException].
     *
     * @param animationIndex Zero-based index for the animation of interest.
     */
    fun getAnimation(animationIndex: Int): ModelAnimation

    /**
     * Returns the number of [ModelAnimation] definitions in the model.
     */
    fun getAnimationCount(): Int

    /**
     * Called from the [ModelAnimation] when it dirty state changed.
     */
    fun onModelAnimationChanged(animation: ModelAnimation) {
        if (applyAnimationChange(animation)) {
            animation.setDirty(false)
        }
    }

    /**
     * Occurs when a [ModelAnimation] has received any property changed.
     * <br>Depending on the returned value, the [ModelAnimation] will set his isDirty to false
     * or not.
     * <br>You can choose between applying changes on the [ObjectAnimator]
     * [android.view.Choreographer.FrameCallback] or use your own
     * [android.view.Choreographer] to handle an update/render update hierarchy.
     * <br>Time position should be applied inside a global [android.view.Choreographer] frame
     * callback to ensure that the transformations are applied in a hierarchical order.
     *
     * @return true is the changes have been applied/handled
     */
    fun applyAnimationChange(animation: ModelAnimation): Boolean

    /**
     * Get the associated [ModelAnimation] by name or null if none exist with the given name.
     */
    fun getAnimation(name: String): ModelAnimation? {
        val index = getAnimationIndex(name)
        return if (index != -1) getAnimation(index) else null
    }

    /**
     * Get the associated [ModelAnimation] by name or throw an Exception if none exist with
     * the given name.
     */
    fun getAnimationOrThrow(name: String): ModelAnimation {
        return Preconditions.checkNotNull(getAnimation(name), "No animation found with the given name")
    }

    /**
     * Get the Zero-based index for the animation name of interest or -1 if not found.
     */
    fun getAnimationIndex(name: String): Int {
        for (i in 0 until getAnimationCount()) {
            if (TextUtils.equals(getAnimation(i).getName(), name)) {
                return i
            }
        }
        return -1
    }

    /**
     * Get the name of the [ModelAnimation] at the Zero-based index
     * <p>
     * This name corresponds to the one defined and exported in the renderable asset.
     * Typically the Action names defined in the 3D creation software.
     * </p>
     *
     * @return The string name of the [ModelAnimation],
     * `String.valueOf(animation.getIndex())`> if none was specified.
     */
    fun getAnimationName(animationIndex: Int): String {
        return getAnimation(animationIndex).getName()
    }

    /**
     * Get the names of the [ModelAnimation]
     * <p>
     * This names correspond to the ones defined and exported in the renderable asset.
     * Typically the Action names defined in the 3D creation software.
     * </p>
     *
     * @return The string name of the [ModelAnimation],
     * `String.valueOf(animation.getIndex())`> if none was specified.
     */
    fun getAnimationNames(): List<String> {
        val names = mutableListOf<String>()
        for (i in 0 until getAnimationCount()) {
            names.add(getAnimation(i).getName())
        }
        return names
    }

    /**
     * Return true if [getAnimationCount] > 0
     */
    fun hasAnimations(): Boolean {
        return getAnimationCount() > 0
    }

    /**
     * Sets the current position of (seeks) the animation to the specified time position in seconds.
     * <p>
     * This method will apply rotation, translation, and scale to the Renderable that have been
     * targeted. Uses `TransformManager`
     * </p>
     *
     * @param timePosition Elapsed time of interest in seconds.
     *                     Between 0 and the max value of [ModelAnimation.getDuration].
     * @see ModelAnimation.getDuration
     */
    fun setAnimationsTimePosition(timePosition: Float) {
        for (i in 0 until getAnimationCount()) {
            getAnimation(i).setTimePosition(timePosition)
        }
    }

    /**
     * Sets the current position of (seeks) all the animations to the specified frame number according
     * to the [ModelAnimation.getFrameRate]
     * <p>
     * This method will apply rotation, translation, and scale to the Renderable that have been
     * targeted. Uses `TransformManager`
     *
     * @param framePosition Frame number on the timeline.
     *                      Between 0 and [ModelAnimation.getFrameCount].
     * @see ModelAnimation.getFrameCount
     */
    fun setAnimationsFramePosition(framePosition: Int) {
        for (i in 0 until getAnimationCount()) {
            getAnimation(i).setFramePosition(framePosition)
        }
    }

    /**
     * Constructs and returns an [ObjectAnimator] for all [ModelAnimation]
     * of this object.
     * <h3>Don't forget to call [ObjectAnimator.start]</h3>
     *
     * @param repeat repeat/loop the animation
     * @return The constructed ObjectAnimator
     * @see ModelAnimator.ofAnimationTime
     */
    fun animate(repeat: Boolean): ObjectAnimator {
        val animator = ModelAnimator.ofAllAnimations(this)
        if (repeat) {
            animator.repeatCount = ValueAnimator.INFINITE
        }
        return animator
    }

    /**
     * Constructs and returns an [ObjectAnimator] for targeted [ModelAnimation] with a
     * given name of this object.
     * <br><b>Don't forget to call [ObjectAnimator.start]</b>
     *
     * @param animationNames The string names of the animations.
     *                       <br>This name should correspond to the one defined and exported in
     *                       the model.
     *                       <br>Typically the action name defined in the 3D creation software.
     *                       [ModelAnimation.getName]
     * @return The constructed ObjectAnimator
     * @see ModelAnimator.ofAnimationTime
     */
    fun animate(vararg animationNames: String): ObjectAnimator {
        return ModelAnimator.ofAnimation(this, *animationNames)
    }

    /**
     * Constructs and returns an [ObjectAnimator] for targeted [ModelAnimation] with a
     * a given index of this object.
     * <br><b>Don't forget to call [ObjectAnimator.start]</b>
     *
     * @param animationIndexes Zero-based indexes for the animations of interest.
     * @return The constructed ObjectAnimator
     * @see ModelAnimator.ofAnimationTime
     */
    fun animate(vararg animationIndexes: Int): ObjectAnimator {
        return ModelAnimator.ofAnimation(this, *animationIndexes)
    }

    /**
     * Constructs and returns an [ObjectAnimator] for a targeted [ModelAnimation] of
     * this object.
     * <b>The setAutoCancel(true) won't work for new call with different animations.</b>
     * <br>This method applies by default this to the returned ObjectAnimator :
     * <ul>
     * <li>The duration value to the max [ModelAnimation.getDuration] in order to
     * match the original animation speed.</li>
     * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
     * interpolation.</li>
     * </ul>
     * <br><b>Don't forget to call [ObjectAnimator.start]</b>
     *
     * @param animations The animations of interest
     * @return The constructed ObjectAnimator
     * @see ModelAnimator.ofAnimationTime
     */
    fun animate(vararg animations: ModelAnimation): ObjectAnimator {
        return ModelAnimator.ofAnimation(this, *animations)
    }
}
