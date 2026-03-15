package io.github.sceneview.animation

import android.text.TextUtils
import android.util.FloatProperty
import android.util.IntProperty
import android.util.Property
import com.google.android.filament.gltfio.Animator
import java.util.concurrent.TimeUnit

/**
 * An ModelAnimation is a reusable set of keyframe tracks which represent an animation.
 * <p>
 * This class provides support for animating time positions on a targeted
 * [AnimatableModel]
 * <p>
 * <h2>Here are some use cases for animations :</h2>
 * <ul>
 * <li>
 *     On a very basic 3D model like a single infinite rotating sphere, you should not have to
 * use this class but probably instead just call
 * [AnimatableModel.animate]
 * </li>
 * <li>
 * For a synchronised animation set like animating a cube and a sphere position and rotation at same
 * time or sequentially, please consider using an [android.animation.AnimatorSet] playing a
 * [ModelAnimator.ofAnimation] or [ModelAnimator.ofPropertyValuesHolder]
 * </li>
 * <li>
 * If the mesh is a character, for example, there may be one ModelAnimation for a walkcycle, a
 * second for a jump, a third for sidestepping and so on.
 * <br>Assuming a character object has a skeleton, one keyframe track could store the data for the
 * position changes of the lower arm bone over time, a different track the data for the rotation
 * changes of the same bone, a third the track position, rotation or scaling of another bone, and so
 * on. It should be clear, that an ModelAnimation can act on lots of such tracks.
 * <br>Assuming the model has morph targets (for example one morph target showing a friendly face
 * and another showing an angry face), each track holds the information as to how the influence of a
 * certain morph target changes during the performance of the clip.
 * In this case you should manage one [android.animation.ObjectAnimator] coming from
 * [ModelAnimator.ofAnimation] per action.
 * And an [android.animation.AnimatorSet] to play them sequentially or together.
 * </li>
 * </ul>
 */
class ModelAnimation(
    private val model: AnimatableModel,
    private var name: String,
    private val index: Int,
    private val duration: Float,
    private val frameRate: Int
) {

    /**
     * Time position is applied inside a global [android.view.Choreographer] frame callback
     * to ensure that the transformations are applied in a hierarchical order.
     */
    internal var timePosition: Float = 0f
    internal var isDirty: Boolean = false

    init {
        if (TextUtils.isEmpty(this.name)) {
            this.name = index.toString()
        }
    }

    /**
     * Returns The Zero-based index of the target `animation` as defined in the original
     * [AnimatableModel]
     */
    fun geIndex(): Int {
        return index
    }

    /**
     * Get the name of the `animation`
     * <p>
     * This name corresponds to the one defined and exported in the [AnimatableModel].
     * <br>Typically the Action names defined in the 3D creation software.
     * </p>
     *
     * @return Weak reference to the string name of the `animation`, or
     * the `String.valueOf(animation.getIndex())`> if none was specified.
     */
    fun getName(): String {
        return name
    }

    /**
     * Returns the duration of this animation in seconds.
     */
    fun getDuration(): Float {
        return duration
    }

    /**
     * Returns the duration of this animation in milliseconds.
     */
    fun getDurationMillis(): Long {
        return secondsToMillis(getDuration())
    }

    /**
     * Get the frames per second originally defined in the
     * [android.graphics.drawable.Animatable].
     *
     * @return The number of frames refresh during one second
     */
    fun getFrameRate(): Int {
        return frameRate
    }

    /**
     * Returns the total number of frames of this animation.
     */
    fun getFrameCount(): Int {
        return timeToFrame(getDuration(), getFrameRate())
    }

    /**
     * Get the current time position in seconds at the current animation position.
     *
     * @return timePosition Elapsed time of interest in seconds. Between 0 and
     * [getDuration]
     * @see getDuration
     */
    fun getTimePosition(): Float {
        return timePosition
    }

    /**
     * Sets the current position of (seeks) the animation to the specified time position in seconds.
     * <p>
     * This method will apply rotation, translation, and scale to the [AnimatableModel] that
     * have been targeted.
     * </p>
     *
     * @param timePosition Elapsed time of interest in seconds. Between 0 and
     *                     [getDuration]
     * @see getDuration
     */
    fun setTimePosition(timePosition: Float) {
        this.timePosition = timePosition
        setDirty(true)
    }

    /**
     * Get the current frame number at the current animation position.
     *
     * @return Frame number on the timeline. Between 0 and [getFrameCount]
     * @see getTimePosition
     * @see getFrameCount
     */
    fun getFramePosition(): Int {
        return getFrameAtTime(getTimePosition())
    }

    /**
     * Sets the current position of (seeks) the animation to the specified frame number according to
     * the [getFrameRate].
     *
     * @param frameNumber Frame number in the timeline. Between 0 and [getFrameCount]
     * @see setTimePosition
     * @see getFrameCount
     */
    fun setFramePosition(frameNumber: Int) {
        setTimePosition(getTimeAtFrame(frameNumber))
    }

    /**
     * Get the fractional value at the current animation position.
     *
     * @return The fractional (percent) position. Between 0 and 1
     * @see getTimePosition
     */
    fun getFractionPosition(): Float {
        return getFractionAtTime(getTimePosition())
    }

    /**
     * Sets the current position of (seeks) the animation to the specified fraction
     * position.
     *
     * @param fractionPosition The fractional (percent) position. Between 0 and 1.
     * @see setTimePosition
     */
    fun setFractionPosition(fractionPosition: Float) {
        setTimePosition(getTimeAtFraction(fractionPosition))
    }

    /**
     * Internal usage for applying changes according to rendering update hierarchy.
     * <br>Time position must be applied inside a global [android.view.Choreographer] frame
     * callback to ensure that the transformations are applied in a hierarchical order.
     *
     * @return true if changes has been made
     */
    fun isDirty(): Boolean {
        return isDirty
    }

    /**
     * Set the state of this object properties to changed.
     * And tell the [AnimatableModel] to take care of it.
     */
    fun setDirty(isDirty: Boolean) {
        this.isDirty = isDirty
        if (isDirty) {
            model.onModelAnimationChanged(this)
        }
    }

    /**
     * Get the elapsed time in seconds of a frame position
     *
     * @param frame Frame number on the timeline
     * @return Elapsed time of interest in seconds
     */
    fun getTimeAtFrame(frame: Int): Float {
        return frameToTime(frame, getFrameRate())
    }

    /**
     * Get the frame position at the elapsed time in seconds.
     *
     * @param time Elapsed time of interest in seconds
     * @return The frame number at the specified time
     */
    fun getFrameAtTime(time: Float): Int {
        return timeToFrame(time, getFrameRate())
    }

    /**
     * Get the elapsed time in seconds of a fraction position
     *
     * @param fraction The fractional (from 0 to 1) value of interest
     * @return Elapsed time at the specified fraction
     */
    fun getTimeAtFraction(fraction: Float): Float {
        return fractionToTime(fraction, getDuration())
    }

    /**
     * Get the fraction position at the elapsed time in seconds.
     *
     * @param time Elapsed time of interest in seconds.
     * @return The fractional (from 0 to 1) value at the specified time
     */
    fun getFractionAtTime(time: Float): Float {
        return timeToFraction(time, getDuration())
    }

    companion object {

        /**
         * Get the elapsed time in seconds of a frame position
         *
         * @param frame     Frame number on the timeline
         * @param frameRate The frames per second of the animation
         * @return Elapsed time of interest in seconds
         */
        @JvmStatic
        fun frameToTime(frame: Int, frameRate: Int): Float {
            return frame.toFloat() / frameRate.toFloat()
        }

        /**
         * Get the frame position at the elapsed time in seconds.
         *
         * @param time      Elapsed time of interest in seconds.
         * @param frameRate The frames per second of the animation
         * @return The frame number at the specified time
         */
        @JvmStatic
        fun timeToFrame(time: Float, frameRate: Int): Int {
            return (time * frameRate).toInt()
        }

        /**
         * Get the elapsed time in seconds of a fraction position
         *
         * @param fraction The fractional (from 0 to 1) value of interest
         * @param duration Duration in seconds
         * @return Elapsed time at the specified fraction
         */
        @JvmStatic
        fun fractionToTime(fraction: Float, duration: Float): Float {
            return fraction * duration
        }

        /**
         * Get the fraction position at the elapsed time in seconds.
         *
         * @param time     Elapsed time of interest in seconds.
         * @param duration Duration in seconds
         * @return The fractional (from 0 to 1) value at the specified time
         */
        @JvmStatic
        fun timeToFraction(time: Float, duration: Float): Float {
            return time / duration
        }

        /**
         * Convert time in seconds to time in millis
         *
         * @param time Elapsed time of interest in seconds.
         * @return Elapsed time of interest in milliseconds
         */
        @JvmStatic
        fun secondsToMillis(time: Float): Long {
            return (time * TimeUnit.SECONDS.toMillis(1).toFloat()).toLong()
        }

        /**
         * A Property wrapper around the `timePosition` functionality handled by the
         * [ModelAnimation.setTimePosition] and [ModelAnimation.getTimePosition]
         * methods.
         */
        @JvmField
        val TIME_POSITION: FloatProperty<ModelAnimation> = object : FloatProperty<ModelAnimation>("timePosition") {
            override fun setValue(obj: ModelAnimation, value: Float) {
                obj.setTimePosition(value)
            }

            override fun get(obj: ModelAnimation): Float {
                return obj.getTimePosition()
            }
        }

        /**
         * A Property wrapper around the `framePosition` functionality handled by the
         * [ModelAnimation.setFramePosition] and [ModelAnimation.getFramePosition]
         * methods
         */
        @JvmField
        val FRAME_POSITION: Property<ModelAnimation, Int> = object : IntProperty<ModelAnimation>("framePosition") {
            override fun setValue(obj: ModelAnimation, value: Int) {
                obj.setFramePosition(value)
            }

            override fun get(obj: ModelAnimation): Int {
                return obj.getFramePosition()
            }
        }

        /**
         * A Property wrapper around the `fractionPosition` functionality handled by the
         * [ModelAnimation.setFractionPosition] and [ModelAnimation.getFractionPosition]
         * methods
         */
        @JvmField
        val FRACTION_POSITION: Property<ModelAnimation, Float> = object : FloatProperty<ModelAnimation>("fractionPosition") {
            override fun setValue(obj: ModelAnimation, value: Float) {
                obj.setFractionPosition(value)
            }

            override fun get(obj: ModelAnimation): Float {
                return obj.getFractionPosition()
            }
        }
    }

    /**
     * This class holds information about a property and the values that that property
     * should take during an animation.
     * PropertyValuesHolder objects can be used to create animations with ObjectAnimator or
     * that operate on several different properties in parallel.
     * <p>
     * Using this [PropertyValuesHolder] provide an handled [ModelAnimator] canceling
     * since we target a same object and those PropertyValuesHolder have the same property name
     */
    class PropertyValuesHolder {

        companion object {

            /**
             * Constructs and returns a PropertyValuesHolder with a given set of time values.
             *
             * @param times The times that the [ModelAnimation] will animate between.
             *              A time value must be between 0 and [ModelAnimation.getDuration]
             * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
             */
            @JvmStatic
            fun ofTime(vararg times: Float): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofFloat(TIME_POSITION, *times)
            }

            /**
             * Constructs and returns a PropertyValuesHolder with a given set of frame values.
             *
             * <b><u>Warning</u></b>
             * Every PropertyValuesHolder that applies a modification on the time position of the
             * animation should use the ModelAnimation.TIME_POSITION instead of its own Property in order
             * to possibly cancel any ObjectAnimator operating time modifications on the same
             * ModelAnimation.
             * [android.animation.ObjectAnimator.setAutoCancel] will have no effect
             * for different property names
             * <p>
             * That's why we avoid using an ModelAnimation.FRAME_POSITION or ModelAnimation.FRACTION_POSITION Property
             *
             * @param frames The frames that the [ModelAnimation] will animate between.
             * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
             */
            @JvmStatic
            fun ofFrame(vararg frames: Int): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofInt(FRAME_POSITION, *frames)
            }

            /**
             * Constructs and returns a PropertyValuesHolder with a given set of fraction values.
             *
             * <b><u>Warning</u></b>
             * Every PropertyValuesHolder that applies a modification on the time position of the
             * animation should use the ModelAnimation.TIME_POSITION instead of its own Property in order
             * to possibly cancel any ObjectAnimator operating time modifications on the same
             * ModelAnimation.
             * [android.animation.ObjectAnimator.setAutoCancel] will have no effect
             * for different property names
             * <p>
             * That's why we avoid using an ModelAnimation.FRAME_POSITION or ModelAnimation.FRACTION_POSITION Property
             *
             * @param fractions The fractions that the [ModelAnimation] will animate between.
             * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
             */
            @JvmStatic
            fun ofFraction(vararg fractions: Float): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofFloat(FRACTION_POSITION, *fractions)
            }
        }
    }
}
