package io.github.sceneview.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.animation.VectorConverters.Float3VectorConverter
import io.github.sceneview.animation.VectorConverters.QuaternionVectorConverter
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale

object Transition {

    /**
     * Creates an animation of Float type that runs infinitely as a part of the given
     * [InfiniteTransition].
     *
     * Once the animation is created, it will run from [initialValue] to [targetValue] and repeat.
     * Depending on the [RepeatMode] of the provided [animationSpec], the animation could either
     * restart after each iteration (i.e. [RepeatMode.Restart]), or reverse after each iteration
     * (i.e. [RepeatMode.Reverse]).
     *
     * If [initialValue] or [targetValue] is changed at any point during the animation, the
     * animation will be restarted with the new [initialValue] and [targetValue]. __Note__: this
     * means continuity will *not* be preserved.
     *
     * A [label] for differentiating this animation from others in android studio.
     *
     * @see [InfiniteTransition.animateValue]
     * @see [androidx.compose.animation.animateColor]
     */
    @Composable
    fun InfiniteTransition.animateFloat3(
        initialValue: Float3,
        targetValue: Float3,
        animationSpec: InfiniteRepeatableSpec<Float3>,
        label: String = "Float3Animation"
    ): State<Float3> =
        animateValue(initialValue, targetValue, Float3VectorConverter, animationSpec, label)

    /**
     * Creates a [Float3] animation as a part of the given [Transition]. This means the states of
     * this animation will be managed by the [Transition].
     *
     * [targetValueByState] is used as a mapping from a target state to the target value of this
     * animation. [Transition] will be using this mapping to determine what value to target this
     * animation towards. __Note__ that [targetValueByState] is a composable function. This means
     * the mapping function could access states, CompositionLocals, themes, etc. If the targetValue
     * changes outside of a [Transition] run (i.e. when the [Transition] already reached its
     * targetState), the [Transition] will start running again to ensure this animation reaches its
     * new target smoothly.
     *
     * An optional [transitionSpec] can be provided to specify (potentially different) animation for
     * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
     * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
     * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all
     * transition destinations.
     *
     * [label] is used to differentiate from other animations in the same transition in Android
     * Studio.
     *
     * @return A [State] object, the value of which is updated by animation
     */
    @Composable
    inline fun <S> Transition<S>.animateFloat3(
        noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Float3> = { spring() },
        label: String = "Float3Animation",
        targetValueByState: @Composable (state: S) -> Float3
    ): State<Float3> =
        animateValue(Float3VectorConverter, transitionSpec, label, targetValueByState)

    /**
     * Creates an animation of Float type that runs infinitely as a part of the given
     * [InfiniteTransition].
     *
     * Once the animation is created, it will run from [initialValue] to [targetValue] and repeat.
     * Depending on the [RepeatMode] of the provided [animationSpec], the animation could either
     * restart after each iteration (i.e. [RepeatMode.Restart]), or reverse after each iteration
     * (i.e. [RepeatMode.Reverse]).
     *
     * If [initialValue] or [targetValue] is changed at any point during the animation, the
     * animation will be restarted with the new [initialValue] and [targetValue]. __Note__: this
     * means continuity will *not* be preserved.
     *
     * A [label] for differentiating this animation from others in android studio.
     *
     * @see [InfiniteTransition.animateValue]
     * @see [androidx.compose.animation.animateColor]
     */
    @Composable
    fun InfiniteTransition.animateQuaternion(
        initialValue: Quaternion,
        targetValue: Quaternion,
        animationSpec: InfiniteRepeatableSpec<Quaternion>,
        label: String = "Float3Animation"
    ): State<Quaternion> =
        animateValue(initialValue, targetValue, QuaternionVectorConverter, animationSpec, label)

    /**
     * Creates a [Quaternion] animation as a part of the given [Transition]. This means the states of
     * this animation will be managed by the [Transition].
     *
     * [targetValueByState] is used as a mapping from a target state to the target value of this
     * animation. [Transition] will be using this mapping to determine what value to target this
     * animation towards. __Note__ that [targetValueByState] is a composable function. This means
     * the mapping function could access states, CompositionLocals, themes, etc. If the targetValue
     * changes outside of a [Transition] run (i.e. when the [Transition] already reached its
     * targetState), the [Transition] will start running again to ensure this animation reaches its
     * new target smoothly.
     *
     * An optional [transitionSpec] can be provided to specify (potentially different) animation for
     * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
     * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
     * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all
     * transition destinations.
     *
     * [label] is used to differentiate from other animations in the same transition in Android
     * Studio.
     *
     * @return A [State] object, the value of which is updated by animation
     */
    @Composable
    inline fun <S> Transition<S>.animateQuaternion(
        noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Quaternion> =
            { spring() },
        label: String = "QuaternionAnimation",
        targetValueByState: @Composable (state: S) -> Quaternion
    ): State<Quaternion> =
        animateValue(QuaternionVectorConverter, transitionSpec, label, targetValueByState)

    /**
     * @see animateFloat3
     */
    @Composable
    fun InfiniteTransition.animatePosition(
        initialValue: Position,
        targetValue: Position,
        animationSpec: InfiniteRepeatableSpec<Position>,
        label: String = "PositionAnimation"
    ): State<Position> = animateFloat3(initialValue, targetValue, animationSpec, label)

    /**
     * @see animateFloat3
     */
    @Composable
    inline fun <S> Transition<S>.animatePosition(
        noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Position> = { spring() },
        label: String = "PositionAnimation",
        targetValueByState: @Composable (state: S) -> Position
    ): State<Position> =
        animateValue(Float3VectorConverter, transitionSpec, label, targetValueByState)

    /**
     * @see animateFloat3
     */
    @Composable
    fun InfiniteTransition.animateRotation(
        initialValue: Rotation,
        targetValue: Rotation,
        animationSpec: InfiniteRepeatableSpec<Rotation>,
        label: String = "RotationAnimation"
    ): State<Rotation> = animateFloat3(initialValue, targetValue, animationSpec, label)

    /**
     * @see animateFloat3
     */
    @Composable
    inline fun <S> Transition<S>.animateRotation(
        noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Rotation> = { spring() },
        label: String = "RotationAnimation",
        targetValueByState: @Composable (state: S) -> Rotation
    ): State<Rotation> =
        animateValue(Float3VectorConverter, transitionSpec, label, targetValueByState)

    /**
     * @see animateFloat3
     */
    @Composable
    fun InfiniteTransition.animateScale(
        initialValue: Scale,
        targetValue: Scale,
        animationSpec: InfiniteRepeatableSpec<Scale>,
        label: String = "ScaleAnimation"
    ): State<Scale> = animateFloat3(initialValue, targetValue, animationSpec, label)

    /**
     * @see animateFloat3
     */
    @Composable
    inline fun <S> Transition<S>.animateScale(
        noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Scale> = { spring() },
        label: String = "ScaleAnimation",
        targetValueByState: @Composable (state: S) -> Scale
    ): State<Scale> =
        animateValue(Float3VectorConverter, transitionSpec, label, targetValueByState)
}