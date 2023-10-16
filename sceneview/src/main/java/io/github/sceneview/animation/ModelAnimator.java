package io.github.sceneview.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.text.TextUtils;
import android.util.Property;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class provides support for animating an {@link AnimatableModel}
 * <h2>Usage</h2>
 *
 * <p>
 * By default the {@link ModelAnimator} can play the full {@link ModelAnimation} starting from 0 to
 * the animation duration with:
 * </p>
 * <pre>
 * {@link ModelAnimator#ofAnimation(AnimatableModel, String...)}
 * </pre>
 * <p>
 * If you want to specify a start and end, you should use:
 * </p>
 * <pre>
 * {@link ModelAnimator#ofAnimationTime(AnimatableModel, String, float...)}
 * </pre>
 * <pre>
 * {@link ModelAnimator#ofAnimationFrame(AnimatableModel, String, int...)}
 * </pre>
 * <pre>
 * {@link ModelAnimator#ofAnimationFraction(AnimatableModel, String, float...)}
 * </pre>
 *
 * <h2>Use cases</h2>
 *
 * <h3>Simple usage</h3>
 * <p>
 * On a very basic 3D model like a single infinite rotating sphere, you should not have to
 * use {@link ModelAnimator} but probably instead just call:
 * </p>
 * <pre>
 * {@link AnimatableModel#animate(boolean)}
 * </pre>
 *
 * <h3>Single Model with Single Animation</h3>
 * <p>
 * If you want to animate a single model to a specific timeline position, use:
 * </p>
 * <pre>
 * {@link ModelAnimator#ofAnimationTime(AnimatableModel, String, float...)}
 * </pre>
 * <pre>
 * {@link ModelAnimator#ofAnimationFrame(AnimatableModel, String, int...)}
 * </pre>
 * <pre>
 * {@link ModelAnimator#ofAnimationFraction(AnimatableModel, String, float...)}
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
 * {@link ModelAnimator#ofAnimation(AnimatableModel, String...)}
 * </pre>
 * <pre>
 * {@link ModelAnimator#ofMultipleAnimations(AnimatableModel, String...)}
 * </pre>
 * <i>Example:</i>
 * <p>
 * Here you can see that no call to <code>animator.cancel()</code> is required because the
 * <code>animator.setAutoCancel(boolean)</code> is set to true by default.
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
 * time or sequentially, please consider using an {@link android.animation.AnimatorSet} with one
 * {@link ModelAnimator} parametrized per step :
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
 * In a glTF context, this {@link Animator} updates matrices according to glTF
 * <code>animation</code> and <code>skin</code> definitions.
 * </p>
 *
 * <h3>{@link ModelAnimator} can be used for two things</h3>
 * <ul>
 * <li>
 * Updating matrices in <code>TransformManager</code> components according to the model
 * <code>animation</code> definitions.
 * </li>
 * <li>
 *     Updating bone matrices in <code>RenderableManager</code> components according to the model
 *     <code>skin</code> definitions.
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
public class ModelAnimator {

    /**
     * Constructs and returns an {@link ObjectAnimator} for all {@link ModelAnimation}
     * inside an {@link AnimatableModel}.
     * <b>The setAutoCancel(true) won't work</b>
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model The targeted model to animate
     * @return The constructed ObjectAnimator
     * @see #ofAnimation(AnimatableModel, ModelAnimation...)
     */
    public static ObjectAnimator ofAllAnimations(AnimatableModel model) {
        ModelAnimation[] animations = new ModelAnimation[model.getAnimationCount()];
        for (int i = 0; i < animations.length; i++) {
            animations[i] = model.getAnimation(i);
        }
        return ofAnimation(model, animations);
    }

    /**
     * Constructs and returns list of {@link ObjectAnimator} given names inside an
     * {@link AnimatableModel}.
     * Can be used directly with {@link android.animation.AnimatorSet#playTogether(Collection)}
     * {@link android.animation.AnimatorSet#playSequentially(List)}
     *
     * @param model The targeted model to animate
     * @return The constructed ObjectAnimator
     * @see #ofAnimation(AnimatableModel, ModelAnimation...)
     */
    public static List<ObjectAnimator> ofMultipleAnimations(AnimatableModel model
            , String... animationNames) {
        List<ObjectAnimator> objectAnimators = new ArrayList<>();
        for (int i = 0; i < animationNames.length; i++) {
            objectAnimators.add(ofAnimation(model, animationNames[i]));
        }
        return objectAnimators;
    }

    /**
     * Constructs and returns an {@link ObjectAnimator} for targeted {@link ModelAnimation} with
     * a given name inside an {@link AnimatableModel}.
     * <b>The setAutoCancel(true) won't work for new call with different animations.</b>
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model          The targeted model to animate
     * @param animationNames The string names of the animations.
     *                       <br>This name should correspond to the one defined and exported in
     *                       the model.
     *                       <br>Typically the action name defined in the 3D creation software.
     *                       {@link ModelAnimation#getName()}
     * @return The constructed ObjectAnimator
     * @see #ofAnimation(AnimatableModel, ModelAnimation...)
     */
    public static ObjectAnimator ofAnimation(AnimatableModel model, String... animationNames) {
        ModelAnimation[] animations = new ModelAnimation[animationNames.length];
        for (int i = 0; i < animationNames.length; i++) {
            animations[i] = getAnimationByName(model, animationNames[i]);
        }
        return ofAnimation(model, animations);
    }

    /**
     * Constructs and returns an {@link ObjectAnimator} for targeted {@link ModelAnimation} with
     * a given index inside an {@link AnimatableModel}.
     * <b>The setAutoCancel(true) won't work for new call with different animations.</b>
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model            The targeted animatable to animate
     * @param animationIndexes Zero-based indexes for the animations of interest.
     * @return The constructed ObjectAnimator
     * @see #ofAnimation(AnimatableModel, ModelAnimation...)
     */
    public static ObjectAnimator ofAnimation(AnimatableModel model, int... animationIndexes) {
        ModelAnimation[] animations = new ModelAnimation[animationIndexes.length];
        for (int i = 0; i < animationIndexes.length; i++) {
            animations[i] = model.getAnimation(animationIndexes[i]);
        }
        return ofAnimation(model, animations);
    }

    /**
     * Constructs and returns an {@link ObjectAnimator} for a targeted {@link ModelAnimation} inside
     * an {@link AnimatableModel}.
     * <b>The setAutoCancel(true) won't work for new call with different animations.</b>
     * This method applies by default this to the returned ObjectAnimator :
     * <ul>
     * <li>The duration value to the max {@link ModelAnimation#getDuration()} in order to
     * match the original animation speed.</li>
     * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
     * interpolation.</li>
     * </ul>
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model      The targeted animatable to animate
     * @param animations The animations of interest
     * @return The constructed ObjectAnimator
     */
    public static ObjectAnimator ofAnimation(AnimatableModel model, ModelAnimation... animations) {
        android.animation.PropertyValuesHolder[] propertyValuesHolders = new android.animation.PropertyValuesHolder[animations.length];
        long duration = 0;
        for (int i = 0; i < animations.length; i++) {
            duration = Math.max(duration, animations[i].getDurationMillis());
            propertyValuesHolders[i] = PropertyValuesHolder.ofAnimationTime(animations[i], 0, animations[i].getDuration());
        }
        ObjectAnimator objectAnimator = ofPropertyValuesHolder(model, propertyValuesHolders);
        objectAnimator.setDuration(duration);
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                for (ModelAnimation animation : animations) {
                    animation.setTimePosition(0);
                }
            }
        });
        objectAnimator.setAutoCancel(true);
        return objectAnimator;
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * time values.
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model         The targeted model to animate
     * @param animationName The string name of the animation.
     *                      <br>This name should correspond to the one defined and exported in
     *                      the model.
     *                      <br>Typically the action name defined in the 3D creation software.
     *                      {@link ModelAnimation#getName()}
     * @param times         The elapsed times (between 0 and {@link ModelAnimation#getDuration()}
     *                      that the {@link ModelAnimation} will animate between.
     * @return The constructed ObjectAnimator
     * @see #ofAnimationTime(AnimatableModel, ModelAnimation, float...)
     * @see ModelAnimation#getName()
     */
    public static ObjectAnimator ofAnimationTime(AnimatableModel model
            , String animationName, float... times) {
        return ofAnimationTime(model, getAnimationByName(model, animationName)
                , times);
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * time values.
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model          The targeted model to animate
     * @param animationIndex Zero-based index for the animation of interest.
     * @param times          The elapsed times (between 0 and {@link ModelAnimation#getDuration()}
     *                       that the {@link ModelAnimation} will animate between.
     * @return The constructed ObjectAnimator
     * @see #ofAnimationTime(AnimatableModel, ModelAnimation, float...)
     */
    public static ObjectAnimator ofAnimationTime(AnimatableModel model, int animationIndex
            , float... times) {
        return ofAnimationTime(model, model.getAnimation(animationIndex)
                , times);
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * time values.
     * <p>
     * Time values can help you targeting a specific position on an animation coming from
     * a 3D creation software with a default times based timeline.
     * It's the 3D designer responsibility to tell you what specific timeline position
     * corresponds to a specific model appearance.
     * </p>
     * <ul>
     * <li>A single value implies that that value is the one being animated to starting from the
     * actual value on the provided {@link ModelAnimation}.</li>
     * <li>Two values imply a starting and ending values.</li>
     * <li>More than two values imply a starting value, values to animate through along the way,
     * and an ending value (these values will be distributed evenly across the duration of the
     * animation).</li>
     * </ul>
     * <p>
     * The properties (time, frame,... position) are applied to the {@link AnimatableModel}
     * <br>This method applies by default this to the returned ObjectAnimator :
     * </p>
     * <ul>
     * <li>The duration value to the {@link ModelAnimation#getDuration()} in order to
     * match the original animation speed.</li>
     * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
     * interpolation.</li>
     * </ul>
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model     The targeted model to animate
     * @param animation The animation of interest
     * @param times     The elapsed times (between 0 and {@link ModelAnimation#getDuration()}
     *                  that the {@link ModelAnimation} will animate between.
     * @return The constructed ObjectAnimator
     */
    public static ObjectAnimator ofAnimationTime(AnimatableModel model
            , ModelAnimation animation, float... times) {
        return ofPropertyValuesHolder(model, animation
                , PropertyValuesHolder.ofAnimationTime(animation, times));
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * frame values.
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model         The targeted model to animate
     * @param animationName The string name of the animation.
     *                      <br>This name should correspond to the one defined and exported in
     *                      the model.
     *                      <br>Typically the action name defined in the 3D creation software.
     *                      {@link ModelAnimation#getName()}
     * @param frames        The frame numbers (between 0 and {@link ModelAnimation#getFrameCount()} that
     *                      the {@link ModelAnimation} will animate between.
     * @return The constructed ObjectAnimator
     * @see #ofAnimationFrame(AnimatableModel, ModelAnimation, int...)
     * @see ModelAnimation#getName()
     */
    public static ObjectAnimator ofAnimationFrame(AnimatableModel model, String animationName, int... frames) {
        return ofAnimationFrame(model, getAnimationByName(model, animationName), frames);
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * frame values.
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model          The targeted model to animate
     * @param animationIndex Zero-based index for the animation of interest.
     * @param frames         The frame numbers (between 0 and {@link ModelAnimation#getFrameCount()} that
     *                       the {@link ModelAnimation} will animate between.
     * @return The constructed ObjectAnimator
     * @see #ofAnimationFrame(AnimatableModel, ModelAnimation, int...)
     */
    public static ObjectAnimator ofAnimationFrame(AnimatableModel model, int animationIndex, int... frames) {
        return ofAnimationFrame(model, model.getAnimation(animationIndex), frames);
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * frame values.
     * <p>
     * Frame number can help you targeting a specific position on an animation coming from
     * a 3D creation software with a frame numbers based timeline.
     * It's the 3D designer responsibility to tell you what specific timeline position
     * corresponds to a specific model appearance.
     * <br>The corresponding time of a frame number is calculated using
     * {@link ModelAnimation#getFrameRate()}.
     * </p>
     * <ul>
     * <li>A single value implies that that value is the one being animated to starting from the
     * actual value on the provided {@link ModelAnimation}.</li>
     * <li>Two values imply a starting and ending values.</li>
     * <li>More than two values imply a starting value, values to animate through along the way,
     * and an ending value (these values will be distributed evenly across the duration of the
     * animation).</li>
     * </ul>
     * <p>
     * The properties (time, frame,... position) are applied to the {@link AnimatableModel}
     * <br>This method applies by default this to the returned ObjectAnimator :
     * </p>
     * <ul>
     * <li>The duration value to the {@link ModelAnimation#getDuration()} in order to
     * match the original animation speed.</li>
     * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
     * interpolation.</li>
     * </ul>
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model     The targeted model to animate
     * @param animation The animation of interest
     * @param frames    The frame numbers (between 0 and {@link ModelAnimation#getFrameCount()} that
     *                  the {@link ModelAnimation} will animate between.
     * @return The constructed ObjectAnimator
     * @see #ofAnimationTime(AnimatableModel, ModelAnimation, float...)
     */
    public static ObjectAnimator ofAnimationFrame(AnimatableModel model
            , ModelAnimation animation, int... frames) {
        return ofPropertyValuesHolder(model, animation
                , PropertyValuesHolder.ofAnimationFrame(animation, frames));
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * fraction values.
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model         The targeted model to animate
     * @param animationName The string name of the animation.
     *                      <br>This name should correspond to the one defined and exported in
     *                      the model.
     *                      <br>Typically the action name defined in the 3D creation software.
     *                      {@link ModelAnimation#getName()}
     * @param fractions     The fractions (percentage) (between 0 and 1)
     * @return The constructed ObjectAnimator
     * @see #ofAnimationFraction(AnimatableModel, ModelAnimation, float...)
     * @see ModelAnimation#getName()
     */
    public static ObjectAnimator ofAnimationFraction(AnimatableModel model, String animationName, float... fractions) {
        return ofAnimationFraction(model, getAnimationByName(model, animationName), fractions);
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * fraction values.
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model          The targeted model to animate
     * @param animationIndex Zero-based index for the animation of interest.
     * @param fractions      The fractions (percentage) (between 0 and 1)
     * @return The constructed ObjectAnimator
     * @see #ofAnimationFraction(AnimatableModel, ModelAnimation, float...)
     */
    public static ObjectAnimator ofAnimationFraction(AnimatableModel model, int animationIndex, float... fractions) {
        return ofAnimationFraction(model, model.getAnimation(animationIndex), fractions);
    }

    /**
     * Constructs and returns an ObjectAnimator clipping a {@link ModelAnimation} to a given set of
     * fraction values.
     * <ul>
     * <li>A single value implies that that value is the one being animated to starting from the
     * actual value on the provided {@link ModelAnimation}.</li>
     * <li>Two values imply a starting and ending values.</li>
     * <li>More than two values imply a starting value, values to animate through along the way,
     * and an ending value (these values will be distributed evenly across the duration of the
     * animation).</li>
     * </ul>
     * <p>
     * The properties (time, frame,... position) are applied to the {@link AnimatableModel}
     * This method applies by default this to the returned ObjectAnimator :
     * </p>
     * <ul>
     * <li>The duration value to the {@link ModelAnimation#getDuration()} in order to
     * match the original animation speed.</li>
     * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
     * interpolation.</li>
     * </ul>
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model     The targeted model to animate
     * @param animation The animation of interest
     * @param fractions The fractions (percentage) (between 0 and 1)
     * @return The constructed ObjectAnimator
     * @see #ofAnimationTime(AnimatableModel, ModelAnimation, float...)
     */
    public static ObjectAnimator ofAnimationFraction(AnimatableModel model
            , ModelAnimation animation, float... fractions) {
        return ofPropertyValuesHolder(model, animation
                , PropertyValuesHolder.ofAnimationFraction(animation, fractions));
    }

    private static ObjectAnimator ofPropertyValuesHolder(AnimatableModel model, ModelAnimation animation, android.animation.PropertyValuesHolder value) {
        ObjectAnimator objectAnimator = ofPropertyValuesHolder(model, value);
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                animation.setTimePosition(0);
            }
        });
        objectAnimator.setDuration(animation.getDurationMillis());
        objectAnimator.setAutoCancel(true);
        return objectAnimator;
    }

    /**
     * Constructs and returns an ObjectAnimator a {@link ModelAnimation} applying
     * PropertyValuesHolders.
     * <ul>
     * <li>A single value implies that that value is the one being animated to starting from the
     * actual value on the provided {@link ModelAnimation}.</li>
     * <li>Two values imply a starting and ending values.</li>
     * <li>More than two values imply a starting value, values to animate through along the way,
     * and an ending value (these values will be distributed evenly across the duration of the
     * animation).</li>
     * </ul>
     * <p>
     * The properties (time, frame,... position) are applied to the {@link AnimatableModel}
     * <br>This method applies by default this to the returned ObjectAnimator :
     * </p>
     * <ul>
     * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
     * interpolation.</li>
     * </ul>
     * <p>Don't forget to call {@link ObjectAnimator#start()}</p>
     *
     * @param model  The targeted model to animate
     * @param values A set of PropertyValuesHolder objects whose values will be animated between over time.
     * @return The constructed ObjectAnimator
     */
    public static ObjectAnimator ofPropertyValuesHolder(AnimatableModel model, android.animation.PropertyValuesHolder... values) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(model, values);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
        return objectAnimator;
    }

    /**
     * Get the associated <code>Animation</code> by name or null if none exist with the given name.
     * <p>
     * This name should correspond to the one defined and exported in the model.
     * <br>Typically the action name defined in the 3D creation software.
     * </p>
     *
     * @param name Weak reference to the string name of the animation or the
     *             <code>String.valueOf(animation.getIndex())</code>> if none was specified.
     */
    private static ModelAnimation getAnimationByName(AnimatableModel model, String name) {
        for (int i = 0; i < model.getAnimationCount(); i++) {
            ModelAnimation animation = model.getAnimation(i);
            if (TextUtils.equals(animation.getName(), name)) {
                return model.getAnimation(i);
            }
        }
        return null;
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
     * <p><b>and</b> insure a less consuming {@link android.view.Choreographer.FrameCallback} than
     * using {@link android.animation.AnimatorSet#playTogether(Animator...)}</p>
     * <p><b>but</b> If you want to use the
     * {@link ObjectAnimator#setAutoCancel(boolean)} functionality, you have to
     * take care of this :</p>
     *
     * <pre>
     * ObjectAnimator.hasSameTargetAndProperties(Animator anim) {
     *      PropertyValuesHolder[] theirValues = ((ObjectAnimator) anim).getValues();
     *      if (((ObjectAnimator) anim).getTarget() == getTarget() &&
     *              mValues.length == theirValues.length) {
     *          for (int i = 0; i < mValues.length; ++i) {
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

    public static class PropertyValuesHolder {

        /**
         * Constructs and returns a PropertyValuesHolder for a targeted {@link ModelAnimation}.
         * This method applies by default this to the returned ObjectAnimator :
         * <ul>
         * <li>The duration value to the {@link ModelAnimation#getDuration()} in order to
         * match the original animation speed.</li>
         * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
         * interpolation.</li>
         * </ul>
         *
         * @param animation The animation of interest
         * @return The constructed PropertyValuesHolder object.
         */
        public static android.animation.PropertyValuesHolder ofAnimation(ModelAnimation animation) {
            return ofAnimationTime(animation, 0, animation.getDuration());
        }

        /**
         * Constructs and returns a PropertyValuesHolder for a targeted animation set of time
         * values.
         *
         * @param animationName The string name of the animation.
         *                      <br>This name should correspond to the one defined and exported in
         *                      the model.
         *                      <br>Typically the action name defined in the 3D creation software.
         *                      {@link ModelAnimation#getName()}
         * @param times         The elapsed times (between 0 and {@link ModelAnimation#getDuration()}
         *                      that the {@link ModelAnimation} will animate between.
         * @return The constructed PropertyValuesHolder object.
         * @see #ofAnimationTime(ModelAnimation, float...)
         */
        public static android.animation.PropertyValuesHolder ofAnimationTime(String animationName, float... times) {
            return android.animation.PropertyValuesHolder.ofFloat(new AnimationProperty<>(animationName, ModelAnimation.TIME_POSITION), times);
        }

        /**
         * Constructs and returns a PropertyValuesHolder for a targeted {@link ModelAnimation} with
         * a given set of time values.
         * <ul>
         * <li>A single value implies that that value is the one being animated to starting from the
         * actual value on the provided {@link ModelAnimation}.</li>
         * <li>Two values imply a starting and ending values.</li>
         * <li>More than two values imply a starting value, values to animate through along the way,
         * and an ending value (these values will be distributed evenly across the duration of the
         * animation).</li>
         * </ul>
         * <p>
         * The properties (time, frame,... position) are applied to the {@link AnimatableModel}
         * <br>This method applies by default this to the returned ObjectAnimator :
         * </p>
         * <ul>
         * <li>The duration value to the {@link ModelAnimation#getDuration()} in order to
         * match the original animation speed.</li>
         * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
         * interpolation.</li>
         * </ul>
         *
         * @param animation The animation of interest
         * @param times     The elapsed times (between 0 and {@link ModelAnimation#getDuration()}
         *                  that the {@link ModelAnimation} will animate between.
         * @return The constructed PropertyValuesHolder object.
         */
        public static android.animation.PropertyValuesHolder ofAnimationTime(ModelAnimation animation, float... times) {
            return android.animation.PropertyValuesHolder.ofFloat(new AnimationProperty<>(animation, ModelAnimation.TIME_POSITION), times);
        }

        /**
         * Constructs and returns a PropertyValuesHolder for a targeted {@link ModelAnimation} with
         * a given set of fame values.
         *
         * @param animationName The string name of the animation.
         *                      <br>This name should correspond to the one defined and exported in
         *                      the model.
         *                      <br>Typically the action name defined in the 3D creation software.
         *                      {@link ModelAnimation#getName()}
         * @param frames        The frame numbers (between 0 and
         *                      {@link ModelAnimation#getFrameCount()} that
         *                      the {@link ModelAnimation} will animate between.
         * @return The constructed PropertyValuesHolder object.
         * @see #ofAnimationFrame(String, int...)
         */
        public static android.animation.PropertyValuesHolder ofAnimationFrame(String animationName, int... frames) {
            return android.animation.PropertyValuesHolder.ofInt(new AnimationProperty<>(animationName, ModelAnimation.FRAME_POSITION), frames);
        }

        /**
         * Constructs and returns a PropertyValuesHolder for a targeted {@link ModelAnimation} with
         * a given set of frame values.
         * <ul>
         * <li>A single value implies that that value is the one being animated to starting from the
         * actual value on the provided {@link ModelAnimation}.</li>
         * <li>Two values imply a starting and ending values.</li>
         * <li>More than two values imply a starting value, values to animate through along the way,
         * and an ending value (these values will be distributed evenly across the duration of the
         * animation).</li>
         * </ul>
         * <p>
         * The properties (time, frame,... position) are applied to the {@link AnimatableModel}
         * <br>This method applies by default this to the returned ObjectAnimator :
         * </p>
         * <ul>
         * <li>The duration value to the {@link ModelAnimation#getDuration()} in order to
         * match the original animation speed.</li>
         * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
         * interpolation.</li>
         * </ul>
         *
         * @param animation The animation of interest
         * @param frames    The frame numbers (between 0 and {@link ModelAnimation#getFrameCount()} that
         *                  the {@link ModelAnimation} will animate between.
         * @return The constructed PropertyValuesHolder object.
         */
        public static android.animation.PropertyValuesHolder ofAnimationFrame(ModelAnimation animation, int... frames) {
            return android.animation.PropertyValuesHolder.ofInt(new AnimationProperty<>(animation, ModelAnimation.FRAME_POSITION), frames);
        }


        /**
         * Constructs and returns a PropertyValuesHolder for a targeted {@link ModelAnimation} with
         * a given set of fraction values.
         *
         * @param animationName The string name of the animation.
         *                      <br>This name should correspond to the one defined and exported in
         *                      the model.
         *                      <br>Typically the action name defined in the 3D creation software.
         *                      {@link ModelAnimation#getName()}
         * @param fractions     The fractions (percentage) (between 0 and 1)
         * @return The constructed PropertyValuesHolder object.
         * @see #ofAnimationFraction(ModelAnimation, float...)
         */
        public static android.animation.PropertyValuesHolder ofAnimationFraction(String animationName, float... fractions) {
            return android.animation.PropertyValuesHolder.ofFloat(new AnimationProperty<>(animationName, ModelAnimation.FRACTION_POSITION), fractions);
        }

        /**
         * Constructs and returns a PropertyValuesHolder for a targeted {@link ModelAnimation} with
         * a given set of fraction values.
         * <ul>
         * <li>A single value implies that that value is the one being animated to starting from the
         * actual value on the provided {@link ModelAnimation}.</li>
         * <li>Two values imply a starting and ending values.</li>
         * <li>More than two values imply a starting value, values to animate through along the way,
         * and an ending value (these values will be distributed evenly across the duration of the
         * animation).</li>
         * </ul>
         * <p>
         * The properties (time, frame,... position) are applied to the {@link AnimatableModel}
         * <br>This method applies by default this to the returned ObjectAnimator :
         * </p>
         * <ul>
         * <li>The duration value to the {@link ModelAnimation#getDuration()} in order to
         * match the original animation speed.</li>
         * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
         * interpolation.</li>
         * </ul>
         *
         * @param animation The animation of interest
         * @param fractions The fractions (percentage) (between 0 and 1)
         * @return The constructed PropertyValuesHolder object.
         */
        public static android.animation.PropertyValuesHolder ofAnimationFraction(ModelAnimation animation, float... fractions) {
            return android.animation.PropertyValuesHolder.ofFloat(new AnimationProperty<>(animation, ModelAnimation.FRACTION_POSITION), fractions);
        }

        /**
         * Internal class to manage a sub Renderable Animation property
         */
        private static class AnimationProperty<T> extends Property<AnimatableModel, T> {

            WeakReference<ModelAnimation> animation;
            String animationName = null;
            Property<ModelAnimation, T> property;

            public AnimationProperty(ModelAnimation animation, Property<ModelAnimation, T> property) {
                super(property.getType(), "animation[" + animation.getName() + "]." + property.getName());
                this.property = property;
                this.animation = new WeakReference<>(animation);
            }

            public AnimationProperty(String animationName, Property<ModelAnimation, T> property) {
                super(property.getType(), "animation[" + animationName + "]." + property.getName());
                this.property = property;
                this.animationName = animationName;
            }

            @Override
            public void set(AnimatableModel object, T value) {
                property.set(getAnimation(object), value);
            }

            @Override
            public T get(AnimatableModel object) {
                return property.get(getAnimation(object));
            }

            private ModelAnimation getAnimation(AnimatableModel model) {
                if (animation == null && animation.get() == null) {
                    animation = new WeakReference<>(getAnimationByName(model, animationName));
                }
                return animation.get();
            }
        }
    }
}
