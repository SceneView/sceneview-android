package com.google.ar.sceneform.animation;


import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.text.TextUtils;
import android.view.animation.LinearInterpolator;

import com.google.ar.sceneform.utilities.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * An AnimatableModel is an object whose properties can be animated by an {@link ModelAnimation}.
 * The object animation transformation can be done at the desired frame callback place.
 * <br<This means that it's up to the AnimatableModel to apply transformations as soon as the
 * {@link ModelAnimation} updates the value (like in a standard
 * {@link ObjectAnimator} context) or to apply them inside a global/common
 * frame callback.
 * <p>
 * An AnimatableModel can, for example, update the data of each animated property managed by an
 * {@link com.google.android.filament.gltfio.Animator}.
 */
public interface AnimatableModel {

    /**
     * Get the associated {@link ModelAnimation} at the given index or throw
     * an {@link IndexOutOfBoundsException}.
     *
     * @param animationIndex Zero-based index for the animation of interest.
     */
    ModelAnimation getAnimation(int animationIndex);

    /**
     * Returns the number of {@link ModelAnimation} definitions in the model.
     */
    int getAnimationCount();

    /**
     * Called form the {@link ModelAnimation} when it dirty state changed.
     */
    default void onModelAnimationChanged(ModelAnimation animation) {
        if(applyAnimationChange(animation)) {
            animation.setDirty(false);
        }
    }

    /**
     * Occurs when a {@link ModelAnimation} has received any property changed.
     * <br>Depending on the returned value, the {@link ModelAnimation} will set his isDirty to false
     * or not.
     * <br>You can choose between applying changes on the {@link ObjectAnimator}
     * {@link android.view.Choreographer.FrameCallback} or use your own
     * {@link android.view.Choreographer} to handle an update/render update hierarchy.
     * <br>Time position should be applied inside a global {@link android.view.Choreographer} frame
     * callback to ensure that the transformations are applied in a hierarchical order.
     *
     * @return true is the changes have been applied/handled
     */
    boolean applyAnimationChange(ModelAnimation animation);

    /**
     * Get the associated {@link ModelAnimation} by name or null if none exist with the given name.
     */
    default ModelAnimation getAnimation(String name) {
        int index = getAnimationIndex(name);
        return index != -1 ? getAnimation(index) : null;
    }

    /**
     * Get the associated {@link ModelAnimation} by name or throw an Exception if none exist with
     * the given name.
     */
    default ModelAnimation getAnimationOrThrow(String name) {
        return Preconditions.checkNotNull(getAnimation(name), "No animation found with the given name");
    }

    /**
     * Get the Zero-based index for the animation name of interest or -1 if not found.
     */
    default int getAnimationIndex(String name) {
        for (int i = 0; i < getAnimationCount(); i++) {
            if (TextUtils.equals(getAnimation(i).getName(), name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the name of the {@link ModelAnimation} at the Zero-based index
     * <p>
     * This name corresponds to the one defined and exported in the renderable asset.
     * Typically the Action names defined in the 3D creation software.
     * </p>
     *
     * @return The string name of the {@link ModelAnimation},
     * <code>String.valueOf(animation.getIndex())</code>> if none was specified.
     */
    default String getAnimationName(int animationIndex) {
        return getAnimation(animationIndex).getName();
    }

    /**
     * Get the names of the {@link ModelAnimation}
     * <p>
     * This names correspond to the ones defined and exported in the renderable asset.
     * Typically the Action names defined in the 3D creation software.
     * </p>
     *
     * @return The string name of the {@link ModelAnimation},
     * <code>String.valueOf(animation.getIndex())</code>> if none was specified.
     */
    default List<String> getAnimationNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < getAnimationCount(); i++) {
            names.add(getAnimation(i).getName());
        }
        return names;
    }

    /**
     * Return true if {@link #getAnimationCount()} > 0
     */
    default boolean hasAnimations() {
        return getAnimationCount() > 0;
    }

    /**
     * Sets the current position of (seeks) the animation to the specified time position in seconds.
     * This time should be
     * <p>
     * This method will apply rotation, translation, and scale to the Renderable that have been
     * targeted. Uses <code>TransformManager</code>
     * </p>
     *
     * @param timePosition Elapsed time of interest in seconds.
     *                     Between 0 and the max value of {@link ModelAnimation#getDuration()}.
     * @see ModelAnimation#getDuration()
     */
    default void setAnimationsTimePosition(float timePosition) {
        for (int i = 0; i < getAnimationCount(); i++) {
            getAnimation(i).setTimePosition(timePosition);
        }
    }

    /**
     * Sets the current position of (seeks) all the animations to the specified frame number according
     * to the {@link ModelAnimation#getFrameRate()}
     * <p>
     * This method will apply rotation, translation, and scale to the Renderable that have been
     * targeted. Uses <code>TransformManager</code>
     *
     * @param framePosition Frame number on the timeline.
     *                    Between 0 and {@link ModelAnimation#getFrameCount()}.
     * @see ModelAnimation#getFrameCount()
     */
    default void setAnimationsFramePosition(int framePosition) {
        for (int i = 0; i < getAnimationCount(); i++) {
            getAnimation(i).setFramePosition(framePosition);
        }
    }

    /**
     * Constructs and returns an {@link ObjectAnimator} for all {@link ModelAnimation}
     * of this object.
     * <h3>Don't forget to call {@link ObjectAnimator#start()}</h3>
     *
     * @param repeat repeat/loop the animation
     * @return The constructed ObjectAnimator
     * @see ModelAnimator#ofAnimationTime(AnimatableModel, ModelAnimation, float...)
     */
    default ObjectAnimator animate(boolean repeat) {
        ObjectAnimator animator = ModelAnimator.ofAllAnimations(this);
        if(repeat) {
            animator.setRepeatCount(ValueAnimator.INFINITE);
        }
        return animator;
    }

    /**
     * Constructs and returns an {@link ObjectAnimator} for targeted {@link ModelAnimation} with a
     * given name of this object.
     * <br><b>Don't forget to call {@link ObjectAnimator#start()}</b>
     *
     * @param animationNames The string names of the animations.
     *                       <br>This name should correspond to the one defined and exported in
     *                       the model.
     *                       <br>Typically the action name defined in the 3D creation software.
     *                       {@link ModelAnimation#getName()}
     * @return The constructed ObjectAnimator
     * @see ModelAnimator#ofAnimationTime(AnimatableModel, ModelAnimation, float...)
     */
    default ObjectAnimator animate(String... animationNames) {
        return ModelAnimator.ofAnimation(this, animationNames);
    }

    /**
     * Constructs and returns an {@link ObjectAnimator} for targeted {@link ModelAnimation} with a
     * a given index of this object.
     * <br><b>Don't forget to call {@link ObjectAnimator#start()}</b>
     *
     * @param animationIndexes Zero-based indexes for the animations of interest.
     * @return The constructed ObjectAnimator
     * @see ModelAnimator#ofAnimationTime(AnimatableModel, ModelAnimation, float...)
     */
    default ObjectAnimator animate(int... animationIndexes) {
        return ModelAnimator.ofAnimation(this, animationIndexes);
    }

    /**
     * Constructs and returns an {@link ObjectAnimator} for a targeted {@link ModelAnimation} of
     * this object.
     * <b>The setAutoCancel(true) won't work for new call with different animations.</b>
     * <br>This method applies by default this to the returned ObjectAnimator :
     * <ul>
     * <li>The duration value to the max {@link ModelAnimation#getDuration()} in order to
     * match the original animation speed.</li>
     * <li>The interpolator to {@link LinearInterpolator} in order to match the natural animation
     * interpolation.</li>
     * </ul>
     * <br><b>Don't forget to call {@link ObjectAnimator#start()}</b>
     *
     * @param animations The animations of interest
     * @return The constructed ObjectAnimator
     * @see ModelAnimator#ofAnimationTime(AnimatableModel, ModelAnimation, float...)
     */
    default ObjectAnimator animate(ModelAnimation... animations) {
        return ModelAnimator.ofAnimation(this, animations);
    }
}