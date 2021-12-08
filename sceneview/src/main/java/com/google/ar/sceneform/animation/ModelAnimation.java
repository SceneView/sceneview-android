package com.google.ar.sceneform.animation;

import android.text.TextUtils;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.util.Property;

import com.google.android.filament.gltfio.Animator;

import java.util.concurrent.TimeUnit;

/**
 * An ModelAnimation is a reusable set of keyframe tracks which represent an animation.
 * <p>
 * This class provides support for animating time positions on a targeted
 * {@link AnimatableModel}
 * <p>
 * <h2>Here are some use cases for animations :</h2>
 * <ul>
 * <li>
 *     On a very basic 3D model like a single infinite rotating sphere, you should not have to
 * use this class but probably instead just call
 * {@link AnimatableModel#animate()}
 * </li>
 * <li>
 * For a synchronised animation set like animating a cube and a sphere position and rotation at same
 * time or sequentially, please consider using an {@link android.animation.AnimatorSet} playing a
 * {@link ModelAnimator#ofAnimation(AnimatableModel, String...)}
 * or {@link ModelAnimator#ofPropertyValuesHolder(AnimatableModel, android.animation.PropertyValuesHolder...)}
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
 * In this case you should manage one {@link android.animation.ObjectAnimator} coming from
 * {@link ModelAnimator#ofAnimation(AnimatableModel, ModelAnimation...)} per action.
 * And an {@link android.animation.AnimatorSet} to play them sequentially or together.
 * </li>
 * </ul>
 */
public class ModelAnimation {

    private AnimatableModel model;
    private int index;
    private String name;
    private float duration;
    private int frameRate;

    /**
     * Time position is applied inside a global {@link android.view.Choreographer} frame callback
     * to ensure that the transformations are applied in a hierarchical order.
     */
    private float timePosition = 0;
    private boolean isDirty = false;

    /**
     * ModelAnimation constructed from an {@link Animator}
     *
     * @param name      This name should corresponds to the one defined and exported in the
     *                  {@link AnimatableModel}.
     *                  <br>Typically the action name defined in the 3D creation software.
     * @param index     Zero-based index of the target <code>animation</code> as defined in the
     *                  original {@link AnimatableModel}
     * @param duration  This original {@link AnimatableModel} duration
     * @param frameRate The frames per second defined in the original animation asset
     */
    public ModelAnimation(AnimatableModel model, String name, int index, float duration
            , int frameRate) {
        this.model = model;
        this.index = index;
        this.name = name;
        if (TextUtils.isEmpty(this.name)) {
            this.name = String.valueOf(index);
        }
        this.frameRate = frameRate;
        this.duration = duration;
    }

    /**
     * Returns The Zero-based index of the target <code>animation</code> as defined in the original
     * {@link AnimatableModel}
     */
    public int geIndex() {
        return index;
    }

    /**
     * Get the name of the <code>animation</code>
     * <p>
     * This name corresponds to the one defined and exported in the {@link AnimatableModel}.
     * <br>Typically the Action names defined in the 3D creation software.
     * </p>
     *
     * @return Weak reference to the string name of the <code>animation</code>, or
     * the <code>String.valueOf(animation.getIndex())</code>> if none was specified.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the duration of this animation in seconds.
     */
    public float getDuration() {
        return duration;
    }

    /**
     * Returns the duration of this animation in milliseconds.
     */
    public long getDurationMillis() {
        return secondsToMillis(getDuration());
    }

    /**
     * Get the frames per second originally defined in the
     * {@link android.graphics.drawable.Animatable}.
     *
     * @return The number of frames refresh during one second
     */
    public int getFrameRate() {
        return frameRate;
    }

    /**
     * Returns the total number of frames of this animation.
     */
    public int getFrameCount() {
        return timeToFrame(getDuration(), getFrameRate());
    }

    /**
     * Get the current time position in seconds at the current animation position.
     *
     * @return timePosition Elapsed time of interest in seconds. Between 0 and
     * {@link #getDuration()}
     * @see #getDuration()
     */
    public float getTimePosition() {
        return timePosition;
    }

    /**
     * Sets the current position of (seeks) the animation to the specified time position in seconds.
     * <p>
     * This method will apply rotation, translation, and scale to the {@link AnimatableModel} that
     * have been targeted.
     * </p>
     *
     * @param timePosition Elapsed time of interest in seconds. Between 0 and
     *                     {@link #getDuration()}
     * @see #getDuration()
     */
    public void setTimePosition(float timePosition) {
        this.timePosition = timePosition;
        setDirty(true);
    }

    /**
     * Get the current frame number at the current animation position.
     *
     * @return Frame number on the timeline. Between 0 and {@link #getFrameCount()
     * @see #getTimePosition()
     * @see #getFrameCount()
     */
    public int getFramePosition() {
        return getFrameAtTime(getTimePosition());
    }

    /**
     * Sets the current position of (seeks) the animation to the specified frame number according to
     * the {@link #getFrameRate()}.
     *
     * @param frameNumber Frame number in the timeline. Between 0 and {@link #getFrameCount()}
     * @see #setTimePosition(float)
     * @see #getFrameCount()
     */
    public void setFramePosition(int frameNumber) {
        setTimePosition(getTimeAtFrame(frameNumber));
    }

    /**
     * Get the fractional value at the current animation position.
     *
     * @return The fractional (percent) position. Between 0 and 1
     * @see #getTimePosition()
     */
    public float getFractionPosition() {
        return getFractionAtTime(getTimePosition());
    }

    /**
     * Sets the current position of (seeks) the animation to the specified fraction
     * position.
     *
     * @param fractionPosition The fractional (percent) position. Between 0 and 1.
     * @see #setTimePosition(float)
     */
    public void setFractionPosition(float fractionPosition) {
        setTimePosition(getTimeAtFraction(fractionPosition));
    }

    /**
     * Internal usage for applying changes according to rendering update hierarchy.
     * <br>Time position must be applied inside a global {@link android.view.Choreographer} frame
     * callback to ensure that the transformations are applied in a hierarchical order.
     *
     * @return true if changes has been made
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Set the state of this object properties to changed.
     * And tell the {@link AnimatableModel} to take care of it.
     */
    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
        if (isDirty) {
            model.onModelAnimationChanged(this);
        }
    }

    /**
     * Get the elapsed time in seconds of a frame position
     *
     * @param frame Frame number on the timeline
     * @return Elapsed time of interest in seconds
     */
    public float getTimeAtFrame(int frame) {
        return frameToTime(frame, getFrameRate());
    }

    /**
     * Get the frame position at the elapsed time in seconds.
     *
     * @param time Elapsed time of interest in seconds
     * @return The frame number at the specified time
     */
    public int getFrameAtTime(float time) {
        return timeToFrame(time, getFrameRate());
    }

    /**
     * Get the elapsed time in seconds of a fraction position
     *
     * @param fraction The fractional (from 0 to 1) value of interest
     * @return Elapsed time at the specified fraction
     */
    public float getTimeAtFraction(float fraction) {
        return fractionToTime(fraction, getDuration());
    }

    /**
     * Get the fraction position at the elapsed time in seconds.
     *
     * @param time Elapsed time of interest in seconds.
     * @return The fractional (from 0 to 1) value at the specified time
     */
    public float getFractionAtTime(float time) {
        return timeToFraction(time, getDuration());
    }

    /**
     * Get the elapsed time in seconds of a frame position
     *
     * @param frame     Frame number on the timeline
     * @param frameRate The frames per second of the animation
     * @return Elapsed time of interest in seconds
     */
    public static float frameToTime(int frame, int frameRate) {
        return (float) frame / (float) frameRate;
    }

    /**
     * Get the frame position at the elapsed time in seconds.
     *
     * @param time      Elapsed time of interest in seconds.
     * @param frameRate The frames per second of the animation
     * @return The frame number at the specified time
     */
    public static int timeToFrame(float time, int frameRate) {
        return (int) (time * frameRate);
    }

    /**
     * Get the elapsed time in seconds of a fraction position
     *
     * @param fraction The fractional (from 0 to 1) value of interest
     * @param duration Duration in seconds
     * @return Elapsed time at the specified fraction
     */
    public static float fractionToTime(float fraction, float duration) {
        return fraction * duration;
    }

    /**
     * Get the fraction position at the elapsed time in seconds.
     *
     * @param time     Elapsed time of interest in seconds.
     * @param duration Duration in seconds
     * @return The fractional (from 0 to 1) value at the specified time
     */
    public static float timeToFraction(float time, float duration) {
        return time / duration;
    }

    /**
     * Convert time in seconds to time in millis
     *
     * @param time Elapsed time of interest in seconds.
     * @return Elapsed time of interest in milliseconds
     */
    public static long secondsToMillis(float time) {
        return (long) (time * (float) TimeUnit.SECONDS.toMillis(1));
    }

    /**
     * A Property wrapper around the <code>timePosition</code> functionality handled by the
     * {@link ModelAnimation#setTimePosition(float)} and {@link ModelAnimation#getTimePosition()}
     * methods.
     */
    public static final FloatProperty<ModelAnimation> TIME_POSITION = new FloatProperty<ModelAnimation>("timePosition") {
        @Override
        public void setValue(ModelAnimation object, float value) {
            object.setTimePosition(value);
        }

        @Override
        public Float get(ModelAnimation object) {
            return object.getTimePosition();
        }
    };

    /**
     * A Property wrapper around the <code>framePosition</code> functionality handled by the
     * {@link ModelAnimation#setFramePosition(int)} and {@link ModelAnimation#getFramePosition()}
     * methods
     */
    public static final Property<ModelAnimation, Integer> FRAME_POSITION = new IntProperty<ModelAnimation>("framePosition") {
        @Override
        public void setValue(ModelAnimation object, int value) {
            object.setFramePosition(value);
        }

        @Override
        public Integer get(ModelAnimation object) {
            return object.getFramePosition();
        }
    };

    /**
     * A Property wrapper around the <code>fractionPosition</code> functionality handled by the
     * {@link ModelAnimation#setFractionPosition(float)} and {@link ModelAnimation#getFractionPosition()}
     * methods
     */
    public static final Property<ModelAnimation, Float> FRACTION_POSITION = new FloatProperty<ModelAnimation>("fractionPosition") {
        @Override
        public void setValue(ModelAnimation object, float value) {
            object.setFractionPosition(value);
        }

        @Override
        public Float get(ModelAnimation object) {
            return object.getFractionPosition();
        }
    };

    /**
     * This class holds information about a property and the values that that property
     * should take during an animation.
     * PropertyValuesHolder objects can be used to create animations with ObjectAnimator or
     * that operate on several different properties in parallel.
     * <p>
     * Using this {@link PropertyValuesHolder} provide an handled {@link ModelAnimator} canceling
     * since we target a same object and those PropertyValuesHolder have the same property name
     */
    public static class PropertyValuesHolder {

        /**
         * Constructs and returns a PropertyValuesHolder with a given set of time values.
         *
         * @param times The times that the {@link ModelAnimation} will animate between.
         *              A time value must be between 0 and {@link ModelAnimation#getDuration()}
         * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
         */
        public static android.animation.PropertyValuesHolder ofTime(float... times) {
            return android.animation.PropertyValuesHolder.ofFloat(ModelAnimation.TIME_POSITION, times);
        }

        /**
         * Constructs and returns a PropertyValuesHolder with a given set of frame values.
         *
         * <b><u>Warning</u></b>
         * Every PropertyValuesHolder that applies a modification on the time position of the
         * animation should use the ModelAnimation.TIME_POSITION instead of its own Property in order
         * to possibly cancel any ObjectAnimator operating time modifications on the same
         * ModelAnimation.
         * {@link android.animation.ObjectAnimator#setAutoCancel(boolean)} will have no effect
         * for different property names
         * <p>
         * That's why we avoid using an ModelAnimation.FRAME_POSITION or ModelAnimation.FRACTION_POSITION Property
         *
         * @param frames The frames that the {@link ModelAnimation} will animate between.
         * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
         */
        public static android.animation.PropertyValuesHolder ofFrame(int... frames) {
            return android.animation.PropertyValuesHolder.ofInt(ModelAnimation.FRAME_POSITION, frames);
        }

        /**
         * Constructs and returns a PropertyValuesHolder with a given set of fraction values.
         *
         * <b><u>Warning</u></b>
         * Every PropertyValuesHolder that applies a modification on the time position of the
         * animation should use the ModelAnimation.TIME_POSITION instead of its own Property in order
         * to possibly cancel any ObjectAnimator operating time modifications on the same
         * ModelAnimation.
         * {@link android.animation.ObjectAnimator#setAutoCancel(boolean)} will have no effect
         * for different property names
         * <p>
         * That's why we avoid using an ModelAnimation.FRAME_POSITION or ModelAnimation.FRACTION_POSITION Property
         *
         * @param fractions The fractions that the {@link ModelAnimation} will animate between.
         * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
         */
        public static android.animation.PropertyValuesHolder ofFraction(float... fractions) {
            return android.animation.PropertyValuesHolder.ofFloat(ModelAnimation.FRACTION_POSITION, fractions);
        }
    }
}
