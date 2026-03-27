package io.github.sceneview.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.text.TextUtils
import android.util.Property
import android.view.animation.LinearInterpolator
import java.lang.ref.WeakReference

/**
 * This class provides support for animating an [AnimatableModel]
 * <h2>Usage</h2>
 *
 * <p>
 * By default the [ModelAnimator] can play the full [ModelAnimation] starting from 0 to
 * the animation duration with:
 * </p>
 * <pre>
 * [ModelAnimator.ofAnimation]
 * </pre>
 * <p>
 * If you want to specify a start and end, you should use:
 * </p>
 * <pre>
 * [ModelAnimator.ofAnimationTime]
 * </pre>
 * <pre>
 * [ModelAnimator.ofAnimationFrame]
 * </pre>
 * <pre>
 * [ModelAnimator.ofAnimationFraction]
 * </pre>
 *
 * <h2>Use cases</h2>
 *
 * <h3>Simple usage</h3>
 * <p>
 * On a very basic 3D model like a single infinite rotating sphere, you should not have to
 * use [ModelAnimator] but probably instead just call:
 * </p>
 * <pre>
 * [AnimatableModel.animate]
 * </pre>
 *
 * <h3>Single Model with Single Animation</h3>
 * <p>
 * If you want to animate a single model to a specific timeline position, use:
 * </p>
 * <pre>
 * [ModelAnimator.ofAnimationTime]
 * </pre>
 * <pre>
 * [ModelAnimator.ofAnimationFrame]
 * </pre>
 * <pre>
 * [ModelAnimator.ofAnimationFraction]
 * </pre>
 * <ul>
 * <li>
 * A single time, frame, fraction value will go from the actual position to the desired one
 * </li>
 * <li>
 * Two values means form value1 to value2
 * </li>
 * <li>
 * More than two values means form value1 to value2 then to value3
 * </ul>
 * <i>Example:</i>
 * <pre>
 * ModelAnimator.ofAnimationFraction(model, "VerticalTranslation", 0f, 0.8f, 0f).start();
 * </pre>
 *
 * <h3>Single Model with Multiple Animations</h3>
 * <p>
 * If the model is a character, for example, there may be one ModelAnimation for a walkcycle, a
 * second for a jump, a third for sidestepping and so on.
 * </p>
 * <pre>
 * [ModelAnimator.ofAnimation]
 * </pre>
 * <pre>
 * [ModelAnimator.ofMultipleAnimations]
 * </pre>
 * <i>Example:</i>
 * <p>
 * Here you can see that no call to `animator.cancel()` is required because the
 * `animator.setAutoCancel(boolean)` is set to true by default.
 * </p>
 * <pre>
 * ObjectAnimator walkAnimator = ModelAnimator.ofAnimation(model, "walk");
 * walkButton.setOnClickListener(v -> walkAnimator.start());
 *
 * ObjectAnimator runAnimator = ModelAnimator.ofAnimation(model, "run");
 * runButton.setOnClickListener(v -> runAnimator.start());
 * </pre>
 * <i>or sequentially:</i>
 * <pre>
 * AnimatorSet animatorSet = new AnimatorSet();
 * animatorSet.playSequentially(ModelAnimator.ofMultipleAnimations(model, "walk", "run"));
 * animatorSet.start();
 * </pre>
 * <h3>Multiple Models with Multiple Animations</h3>
 * <p>
 * For a synchronised animation set like animating a complete scene with multiple models
 * time or sequentially, please consider using an [android.animation.AnimatorSet] with one
 * [ModelAnimator] parametrized per step :
 * </p>
 * <i>Example:</i>
 * <pre>
 * AnimatorSet completeFly = new AnimatorSet();
 *
 * ObjectAnimator liftOff = ModelAnimator.ofAnimationFraction(airPlaneModel, "FlyAltitude",0, 40);
 * liftOff.setInterpolator(new AccelerateInterpolator());
 *
 * AnimatorSet flying = new AnimatorSet();
 * ObjectAnimator flyAround = ModelAnimator.ofAnimation(airPlaneModel, "FlyAround");
 * flyAround.setRepeatCount(ValueAnimator.INFINITE);
 * flyAround.setDuration(10000);
 * ObjectAnimator airportBusHome = ModelAnimator.ofAnimationFraction(busModel, "Move", 0);
 * flying.playTogether(flyAround, airportBusHome);
 *
 * ObjectAnimator land = ModelAnimator.ofAnimationFraction(airPlaneModel, "FlyAltitude", 0);
 * land.setInterpolator(new DecelerateInterpolator());
 *
 * completeFly.playSequentially(liftOff, flying, land);
 * </pre>
 *
 * <h3>Morphing animation</h3>
 * <p>
 * Assuming a character object has a skeleton, one keyframe track could store the data for the
 * position changes of the lower arm bone over time, a different track the data for the rotation
 * changes of the same bone, a third the track position, rotation or scaling of another bone, and so
 * on. It should be clear, that an ModelAnimation can act on lots of such tracks.
 * </p>
 * <p>
 * Assuming the model has morph targets (for example one morph target showing a friendly face
 * and another showing an angry face), each track holds the information as to how the influence of a
 * certain morph target changes during the performance of the clip.
 * </p>
 * <p>
 * In a glTF context, this [android.animation.Animator] updates matrices according to glTF
 * `animation` and `skin` definitions.
 * </p>
 *
 * <h3>[ModelAnimator] can be used for two things</h3>
 * <ul>
 * <li>
 * Updating matrices in `TransformManager` components according to the model
 * `animation` definitions.
 * </li>
 * <li>
 *     Updating bone matrices in `RenderableManager` components according to the model
 *     `skin` definitions.
 * </li>
 * </ul>
 * <p>
 * Every PropertyValuesHolder that applies a modification on the time position of the animation
 * must use the ModelAnimation.TIME_POSITION instead of its own Property in order to possibly cancel
 * any ObjectAnimator operating time modifications on the same ModelAnimation.
 * </p>
 * <p>
 * More information about Animator:
 * <a href="https://developer.android.com/guide/topics/graphics/prop-animation">
 * https://developer.android.com/guide/topics/graphics/prop-animation
 * </a>
 * </p>
 */
class ModelAnimator {

    companion object {

        /**
         * Constructs and returns an [ObjectAnimator] for all [ModelAnimation]
         * inside an [AnimatableModel].
         * <b>The setAutoCancel(true) won't work</b>
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model The targeted model to animate
         * @return The constructed ObjectAnimator
         * @see ofAnimation
         */
        @JvmStatic
        fun ofAllAnimations(model: AnimatableModel): ObjectAnimator {
            val animations = Array(model.getAnimationCount()) { i -> model.getAnimation(i) }
            return ofAnimation(model, *animations)
        }

        /**
         * Constructs and returns list of [ObjectAnimator] given names inside an
         * [AnimatableModel].
         * Can be used directly with [android.animation.AnimatorSet.playTogether]
         * [android.animation.AnimatorSet.playSequentially]
         *
         * @param model The targeted model to animate
         * @return The constructed ObjectAnimator
         * @see ofAnimation
         */
        @JvmStatic
        fun ofMultipleAnimations(model: AnimatableModel, vararg animationNames: String): List<ObjectAnimator> {
            val objectAnimators = mutableListOf<ObjectAnimator>()
            for (i in animationNames.indices) {
                objectAnimators.add(ofAnimation(model, animationNames[i]))
            }
            return objectAnimators
        }

        /**
         * Constructs and returns an [ObjectAnimator] for targeted [ModelAnimation] with
         * a given name inside an [AnimatableModel].
         * <b>The setAutoCancel(true) won't work for new call with different animations.</b>
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model          The targeted model to animate
         * @param animationNames The string names of the animations.
         *                       <br>This name should correspond to the one defined and exported in
         *                       the model.
         *                       <br>Typically the action name defined in the 3D creation software.
         *                       [ModelAnimation.getName]
         * @return The constructed ObjectAnimator
         * @see ofAnimation
         */
        @JvmStatic
        fun ofAnimation(model: AnimatableModel, vararg animationNames: String): ObjectAnimator {
            val animations = Array(animationNames.size) { i -> getAnimationByName(model, animationNames[i]) }
            return ofAnimation(model, *animations)
        }

        /**
         * Constructs and returns an [ObjectAnimator] for targeted [ModelAnimation] with
         * a given index inside an [AnimatableModel].
         * <b>The setAutoCancel(true) won't work for new call with different animations.</b>
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model            The targeted animatable to animate
         * @param animationIndexes Zero-based indexes for the animations of interest.
         * @return The constructed ObjectAnimator
         * @see ofAnimation
         */
        @JvmStatic
        fun ofAnimation(model: AnimatableModel, vararg animationIndexes: Int): ObjectAnimator {
            val animations = Array(animationIndexes.size) { i -> model.getAnimation(animationIndexes[i]) }
            return ofAnimation(model, *animations)
        }

        /**
         * Constructs and returns an [ObjectAnimator] for a targeted [ModelAnimation] inside
         * an [AnimatableModel].
         * <b>The setAutoCancel(true) won't work for new call with different animations.</b>
         * This method applies by default this to the returned ObjectAnimator :
         * <ul>
         * <li>The duration value to the max [ModelAnimation.getDuration] in order to
         * match the original animation speed.</li>
         * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
         * interpolation.</li>
         * </ul>
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model      The targeted animatable to animate
         * @param animations The animations of interest
         * @return The constructed ObjectAnimator
         */
        @JvmStatic
        fun ofAnimation(model: AnimatableModel, vararg animations: ModelAnimation): ObjectAnimator {
            val propertyValuesHolders = arrayOfNulls<android.animation.PropertyValuesHolder>(animations.size)
            var duration = 0L
            for (i in animations.indices) {
                duration = maxOf(duration, animations[i].getDurationMillis())
                propertyValuesHolders[i] = PropertyValuesHolder.ofAnimationTime(animations[i], 0f, animations[i].getDuration())
            }
            val objectAnimator = ofPropertyValuesHolder(model, *propertyValuesHolders.requireNoNulls())
            objectAnimator.duration = duration
            objectAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animator: Animator) {
                    super.onAnimationCancel(animator)
                    for (animation in animations) {
                        animation.setTimePosition(0f)
                    }
                }
            })
            objectAnimator.setAutoCancel(true)
            return objectAnimator
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * time values.
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model         The targeted model to animate
         * @param animationName The string name of the animation.
         *                      <br>This name should correspond to the one defined and exported in
         *                      the model.
         *                      <br>Typically the action name defined in the 3D creation software.
         *                      [ModelAnimation.getName]
         * @param times         The elapsed times (between 0 and [ModelAnimation.getDuration]
         *                      that the [ModelAnimation] will animate between.
         * @return The constructed ObjectAnimator
         * @see ofAnimationTime
         * @see ModelAnimation.getName
         */
        @JvmStatic
        fun ofAnimationTime(model: AnimatableModel, animationName: String, vararg times: Float): ObjectAnimator {
            return ofAnimationTime(model, getAnimationByName(model, animationName), *times)
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * time values.
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model          The targeted model to animate
         * @param animationIndex Zero-based index for the animation of interest.
         * @param times          The elapsed times (between 0 and [ModelAnimation.getDuration]
         *                       that the [ModelAnimation] will animate between.
         * @return The constructed ObjectAnimator
         * @see ofAnimationTime
         */
        @JvmStatic
        fun ofAnimationTime(model: AnimatableModel, animationIndex: Int, vararg times: Float): ObjectAnimator {
            return ofAnimationTime(model, model.getAnimation(animationIndex), *times)
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * time values.
         * <p>
         * Time values can help you targeting a specific position on an animation coming from
         * a 3D creation software with a default times based timeline.
         * It's the 3D designer responsibility to tell you what specific timeline position
         * corresponds to a specific model appearance.
         * </p>
         * <ul>
         * <li>A single value implies that that value is the one being animated to starting from the
         * actual value on the provided [ModelAnimation].</li>
         * <li>Two values imply a starting and ending values.</li>
         * <li>More than two values imply a starting value, values to animate through along the way,
         * and an ending value (these values will be distributed evenly across the duration of the
         * animation).</li>
         * </ul>
         * <p>
         * The properties (time, frame,... position) are applied to the [AnimatableModel]
         * <br>This method applies by default this to the returned ObjectAnimator :
         * </p>
         * <ul>
         * <li>The duration value to the [ModelAnimation.getDuration] in order to
         * match the original animation speed.</li>
         * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
         * interpolation.</li>
         * </ul>
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model     The targeted model to animate
         * @param animation The animation of interest
         * @param times     The elapsed times (between 0 and [ModelAnimation.getDuration]
         *                  that the [ModelAnimation] will animate between.
         * @return The constructed ObjectAnimator
         */
        @JvmStatic
        fun ofAnimationTime(model: AnimatableModel, animation: ModelAnimation, vararg times: Float): ObjectAnimator {
            return ofPropertyValuesHolder(model, animation, PropertyValuesHolder.ofAnimationTime(animation, *times))
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * frame values.
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model         The targeted model to animate
         * @param animationName The string name of the animation.
         *                      <br>This name should correspond to the one defined and exported in
         *                      the model.
         *                      <br>Typically the action name defined in the 3D creation software.
         *                      [ModelAnimation.getName]
         * @param frames        The frame numbers (between 0 and [ModelAnimation.getFrameCount] that
         *                      the [ModelAnimation] will animate between.
         * @return The constructed ObjectAnimator
         * @see ofAnimationFrame
         * @see ModelAnimation.getName
         */
        @JvmStatic
        fun ofAnimationFrame(model: AnimatableModel, animationName: String, vararg frames: Int): ObjectAnimator {
            return ofAnimationFrame(model, getAnimationByName(model, animationName), *frames)
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * frame values.
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model          The targeted model to animate
         * @param animationIndex Zero-based index for the animation of interest.
         * @param frames         The frame numbers (between 0 and [ModelAnimation.getFrameCount] that
         *                       the [ModelAnimation] will animate between.
         * @return The constructed ObjectAnimator
         * @see ofAnimationFrame
         */
        @JvmStatic
        fun ofAnimationFrame(model: AnimatableModel, animationIndex: Int, vararg frames: Int): ObjectAnimator {
            return ofAnimationFrame(model, model.getAnimation(animationIndex), *frames)
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * frame values.
         * <p>
         * Frame number can help you targeting a specific position on an animation coming from
         * a 3D creation software with a frame numbers based timeline.
         * It's the 3D designer responsibility to tell you what specific timeline position
         * corresponds to a specific model appearance.
         * <br>The corresponding time of a frame number is calculated using
         * [ModelAnimation.getFrameRate].
         * </p>
         * <ul>
         * <li>A single value implies that that value is the one being animated to starting from the
         * actual value on the provided [ModelAnimation].</li>
         * <li>Two values imply a starting and ending values.</li>
         * <li>More than two values imply a starting value, values to animate through along the way,
         * and an ending value (these values will be distributed evenly across the duration of the
         * animation).</li>
         * </ul>
         * <p>
         * The properties (time, frame,... position) are applied to the [AnimatableModel]
         * <br>This method applies by default this to the returned ObjectAnimator :
         * </p>
         * <ul>
         * <li>The duration value to the [ModelAnimation.getDuration] in order to
         * match the original animation speed.</li>
         * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
         * interpolation.</li>
         * </ul>
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model     The targeted model to animate
         * @param animation The animation of interest
         * @param frames    The frame numbers (between 0 and [ModelAnimation.getFrameCount] that
         *                  the [ModelAnimation] will animate between.
         * @return The constructed ObjectAnimator
         * @see ofAnimationTime
         */
        @JvmStatic
        fun ofAnimationFrame(model: AnimatableModel, animation: ModelAnimation, vararg frames: Int): ObjectAnimator {
            return ofPropertyValuesHolder(model, animation, PropertyValuesHolder.ofAnimationFrame(animation, *frames))
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * fraction values.
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model         The targeted model to animate
         * @param animationName The string name of the animation.
         *                      <br>This name should correspond to the one defined and exported in
         *                      the model.
         *                      <br>Typically the action name defined in the 3D creation software.
         *                      [ModelAnimation.getName]
         * @param fractions     The fractions (percentage) (between 0 and 1)
         * @return The constructed ObjectAnimator
         * @see ofAnimationFraction
         * @see ModelAnimation.getName
         */
        @JvmStatic
        fun ofAnimationFraction(model: AnimatableModel, animationName: String, vararg fractions: Float): ObjectAnimator {
            return ofAnimationFraction(model, getAnimationByName(model, animationName), *fractions)
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * fraction values.
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model          The targeted model to animate
         * @param animationIndex Zero-based index for the animation of interest.
         * @param fractions      The fractions (percentage) (between 0 and 1)
         * @return The constructed ObjectAnimator
         * @see ofAnimationFraction
         */
        @JvmStatic
        fun ofAnimationFraction(model: AnimatableModel, animationIndex: Int, vararg fractions: Float): ObjectAnimator {
            return ofAnimationFraction(model, model.getAnimation(animationIndex), *fractions)
        }

        /**
         * Constructs and returns an ObjectAnimator clipping a [ModelAnimation] to a given set of
         * fraction values.
         * <ul>
         * <li>A single value implies that that value is the one being animated to starting from the
         * actual value on the provided [ModelAnimation].</li>
         * <li>Two values imply a starting and ending values.</li>
         * <li>More than two values imply a starting value, values to animate through along the way,
         * and an ending value (these values will be distributed evenly across the duration of the
         * animation).</li>
         * </ul>
         * <p>
         * The properties (time, frame,... position) are applied to the [AnimatableModel]
         * This method applies by default this to the returned ObjectAnimator :
         * </p>
         * <ul>
         * <li>The duration value to the [ModelAnimation.getDuration] in order to
         * match the original animation speed.</li>
         * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
         * interpolation.</li>
         * </ul>
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model     The targeted model to animate
         * @param animation The animation of interest
         * @param fractions The fractions (percentage) (between 0 and 1)
         * @return The constructed ObjectAnimator
         * @see ofAnimationTime
         */
        @JvmStatic
        fun ofAnimationFraction(model: AnimatableModel, animation: ModelAnimation, vararg fractions: Float): ObjectAnimator {
            return ofPropertyValuesHolder(model, animation, PropertyValuesHolder.ofAnimationFraction(animation, *fractions))
        }

        private fun ofPropertyValuesHolder(
            model: AnimatableModel,
            animation: ModelAnimation,
            value: android.animation.PropertyValuesHolder
        ): ObjectAnimator {
            val objectAnimator = ofPropertyValuesHolder(model, value)
            objectAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animator: Animator) {
                    super.onAnimationCancel(animator)
                    animation.setTimePosition(0f)
                }
            })
            objectAnimator.duration = animation.getDurationMillis()
            objectAnimator.setAutoCancel(true)
            return objectAnimator
        }

        /**
         * Constructs and returns an ObjectAnimator a [ModelAnimation] applying
         * PropertyValuesHolders.
         * <ul>
         * <li>A single value implies that that value is the one being animated to starting from the
         * actual value on the provided [ModelAnimation].</li>
         * <li>Two values imply a starting and ending values.</li>
         * <li>More than two values imply a starting value, values to animate through along the way,
         * and an ending value (these values will be distributed evenly across the duration of the
         * animation).</li>
         * </ul>
         * <p>
         * The properties (time, frame,... position) are applied to the [AnimatableModel]
         * <br>This method applies by default this to the returned ObjectAnimator :
         * </p>
         * <ul>
         * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
         * interpolation.</li>
         * </ul>
         * <p>Don't forget to call [ObjectAnimator.start]</p>
         *
         * @param model  The targeted model to animate
         * @param values A set of PropertyValuesHolder objects whose values will be animated between over time.
         * @return The constructed ObjectAnimator
         */
        @JvmStatic
        fun ofPropertyValuesHolder(model: AnimatableModel, vararg values: android.animation.PropertyValuesHolder): ObjectAnimator {
            val objectAnimator = ObjectAnimator.ofPropertyValuesHolder(model, *values)
            objectAnimator.interpolator = LinearInterpolator()
            objectAnimator.repeatCount = ValueAnimator.INFINITE
            return objectAnimator
        }

        /**
         * Get the associated `Animation` by name or null if none exist with the given name.
         * <p>
         * This name should correspond to the one defined and exported in the model.
         * <br>Typically the action name defined in the 3D creation software.
         * </p>
         *
         * @param name Weak reference to the string name of the animation or the
         *             `String.valueOf(animation.getIndex())`> if none was specified.
         */
        private fun getAnimationByName(model: AnimatableModel, name: String): ModelAnimation {
            for (i in 0 until model.getAnimationCount()) {
                val animation = model.getAnimation(i)
                if (TextUtils.equals(animation.getName(), name)) {
                    return model.getAnimation(i)
                }
            }
            throw IllegalArgumentException("Animation '$name' not found in model")
        }
    }

    /**
     * This class holds information about a property and the values that that property
     * should take during an animation.
     * <p>PropertyValuesHolder objects can be used to create animations with ObjectAnimator or
     * that operate on several different properties in parallel.</p>
     *
     * <h2>Warning:</h2>
     * <p>Using this PropertyValuesHolder is very useful for targeting multiple
     * time or frame properties of multiple animations inside a same ObjectAnimator model</p>
     * <p><b>and</b> insure a less consuming [android.view.Choreographer.FrameCallback] than
     * using [android.animation.AnimatorSet.playTogether]</p>
     * <p><b>but</b> If you want to use the
     * [ObjectAnimator.setAutoCancel] functionality, you have to
     * take care of this :</p>
     *
     * <pre>
     * ObjectAnimator.hasSameTargetAndProperties(Animator anim) {
     *      PropertyValuesHolder[] theirValues = ((ObjectAnimator) anim).getValues();
     *      if (((ObjectAnimator) anim).getTarget() == getTarget() &amp;&amp;
     *              mValues.length == theirValues.length) {
     *          for (int i = 0; i &lt; mValues.length; ++i) {
     *              PropertyValuesHolder pvhMine = mValues[i];
     *              PropertyValuesHolder pvhTheirs = theirValues[i];
     *              if (pvhMine.getPropertyName() == null ||
     *                      !pvhMine.getPropertyName().equals(pvhTheirs.getPropertyName())) {
     *                  return false;
     *              }
     *          }
     *          return true;
     *      }
     *  }
     * </pre>
     *
     * @see ObjectAnimator
     */
    class PropertyValuesHolder {

        companion object {

            /**
             * Constructs and returns a PropertyValuesHolder for a targeted [ModelAnimation].
             * This method applies by default this to the returned ObjectAnimator :
             * <ul>
             * <li>The duration value to the [ModelAnimation.getDuration] in order to
             * match the original animation speed.</li>
             * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
             * interpolation.</li>
             * </ul>
             *
             * @param animation The animation of interest
             * @return The constructed PropertyValuesHolder object.
             */
            @JvmStatic
            fun ofAnimation(animation: ModelAnimation): android.animation.PropertyValuesHolder {
                return ofAnimationTime(animation, 0f, animation.getDuration())
            }

            /**
             * Constructs and returns a PropertyValuesHolder for a targeted animation set of time
             * values.
             *
             * @param animationName The string name of the animation.
             *                      <br>This name should correspond to the one defined and exported in
             *                      the model.
             *                      <br>Typically the action name defined in the 3D creation software.
             *                      [ModelAnimation.getName]
             * @param times         The elapsed times (between 0 and [ModelAnimation.getDuration]
             *                      that the [ModelAnimation] will animate between.
             * @return The constructed PropertyValuesHolder object.
             * @see ofAnimationTime
             */
            @JvmStatic
            fun ofAnimationTime(animationName: String, vararg times: Float): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofFloat(AnimationProperty(animationName, ModelAnimation.TIME_POSITION), *times)
            }

            /**
             * Constructs and returns a PropertyValuesHolder for a targeted [ModelAnimation] with
             * a given set of time values.
             * <ul>
             * <li>A single value implies that that value is the one being animated to starting from the
             * actual value on the provided [ModelAnimation].</li>
             * <li>Two values imply a starting and ending values.</li>
             * <li>More than two values imply a starting value, values to animate through along the way,
             * and an ending value (these values will be distributed evenly across the duration of the
             * animation).</li>
             * </ul>
             * <p>
             * The properties (time, frame,... position) are applied to the [AnimatableModel]
             * <br>This method applies by default this to the returned ObjectAnimator :
             * </p>
             * <ul>
             * <li>The duration value to the [ModelAnimation.getDuration] in order to
             * match the original animation speed.</li>
             * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
             * interpolation.</li>
             * </ul>
             *
             * @param animation The animation of interest
             * @param times     The elapsed times (between 0 and [ModelAnimation.getDuration]
             *                  that the [ModelAnimation] will animate between.
             * @return The constructed PropertyValuesHolder object.
             */
            @JvmStatic
            fun ofAnimationTime(animation: ModelAnimation, vararg times: Float): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofFloat(AnimationProperty(animation, ModelAnimation.TIME_POSITION), *times)
            }

            /**
             * Constructs and returns a PropertyValuesHolder for a targeted [ModelAnimation] with
             * a given set of frame values.
             *
             * @param animationName The string name of the animation.
             *                      <br>This name should correspond to the one defined and exported in
             *                      the model.
             *                      <br>Typically the action name defined in the 3D creation software.
             *                      [ModelAnimation.getName]
             * @param frames        The frame numbers (between 0 and
             *                      [ModelAnimation.getFrameCount] that
             *                      the [ModelAnimation] will animate between.
             * @return The constructed PropertyValuesHolder object.
             * @see ofAnimationFrame
             */
            @JvmStatic
            fun ofAnimationFrame(animationName: String, vararg frames: Int): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofInt(AnimationProperty(animationName, ModelAnimation.FRAME_POSITION), *frames)
            }

            /**
             * Constructs and returns a PropertyValuesHolder for a targeted [ModelAnimation] with
             * a given set of frame values.
             * <ul>
             * <li>A single value implies that that value is the one being animated to starting from the
             * actual value on the provided [ModelAnimation].</li>
             * <li>Two values imply a starting and ending values.</li>
             * <li>More than two values imply a starting value, values to animate through along the way,
             * and an ending value (these values will be distributed evenly across the duration of the
             * animation).</li>
             * </ul>
             * <p>
             * The properties (time, frame,... position) are applied to the [AnimatableModel]
             * <br>This method applies by default this to the returned ObjectAnimator :
             * </p>
             * <ul>
             * <li>The duration value to the [ModelAnimation.getDuration] in order to
             * match the original animation speed.</li>
             * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
             * interpolation.</li>
             * </ul>
             *
             * @param animation The animation of interest
             * @param frames    The frame numbers (between 0 and [ModelAnimation.getFrameCount] that
             *                  the [ModelAnimation] will animate between.
             * @return The constructed PropertyValuesHolder object.
             */
            @JvmStatic
            fun ofAnimationFrame(animation: ModelAnimation, vararg frames: Int): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofInt(AnimationProperty(animation, ModelAnimation.FRAME_POSITION), *frames)
            }

            /**
             * Constructs and returns a PropertyValuesHolder for a targeted [ModelAnimation] with
             * a given set of fraction values.
             *
             * @param animationName The string name of the animation.
             *                      <br>This name should correspond to the one defined and exported in
             *                      the model.
             *                      <br>Typically the action name defined in the 3D creation software.
             *                      [ModelAnimation.getName]
             * @param fractions     The fractions (percentage) (between 0 and 1)
             * @return The constructed PropertyValuesHolder object.
             * @see ofAnimationFraction
             */
            @JvmStatic
            fun ofAnimationFraction(animationName: String, vararg fractions: Float): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofFloat(AnimationProperty(animationName, ModelAnimation.FRACTION_POSITION), *fractions)
            }

            /**
             * Constructs and returns a PropertyValuesHolder for a targeted [ModelAnimation] with
             * a given set of fraction values.
             * <ul>
             * <li>A single value implies that that value is the one being animated to starting from the
             * actual value on the provided [ModelAnimation].</li>
             * <li>Two values imply a starting and ending values.</li>
             * <li>More than two values imply a starting value, values to animate through along the way,
             * and an ending value (these values will be distributed evenly across the duration of the
             * animation).</li>
             * </ul>
             * <p>
             * The properties (time, frame,... position) are applied to the [AnimatableModel]
             * <br>This method applies by default this to the returned ObjectAnimator :
             * </p>
             * <ul>
             * <li>The duration value to the [ModelAnimation.getDuration] in order to
             * match the original animation speed.</li>
             * <li>The interpolator to [LinearInterpolator] in order to match the natural animation
             * interpolation.</li>
             * </ul>
             *
             * @param animation The animation of interest
             * @param fractions The fractions (percentage) (between 0 and 1)
             * @return The constructed PropertyValuesHolder object.
             */
            @JvmStatic
            fun ofAnimationFraction(animation: ModelAnimation, vararg fractions: Float): android.animation.PropertyValuesHolder {
                return android.animation.PropertyValuesHolder.ofFloat(AnimationProperty(animation, ModelAnimation.FRACTION_POSITION), *fractions)
            }
        }

        /**
         * Internal class to manage a sub Renderable Animation property
         */
        private class AnimationProperty<T> : Property<AnimatableModel, T> {

            var animation: WeakReference<ModelAnimation>? = null
            var animationName: String? = null
            val property: Property<ModelAnimation, T>

            constructor(animation: ModelAnimation, property: Property<ModelAnimation, T>) :
                    super(property.type, "animation[${animation.getName()}].${property.name}") {
                this.property = property
                this.animation = WeakReference(animation)
            }

            constructor(animationName: String, property: Property<ModelAnimation, T>) :
                    super(property.type, "animation[$animationName].${property.name}") {
                this.property = property
                this.animationName = animationName
            }

            override fun set(`object`: AnimatableModel, value: T) {
                property.set(getAnimation(`object`), value)
            }

            override fun get(`object`: AnimatableModel): T {
                return property.get(getAnimation(`object`))
            }

            private fun getAnimation(model: AnimatableModel): ModelAnimation {
                val anim = animation?.get()
                if (anim != null) return anim
                val resolved = getAnimationByName(model, animationName ?: throw IllegalArgumentException("Animation name is null"))
                animation = WeakReference(resolved)
                return resolved
            }
        }
    }
}
