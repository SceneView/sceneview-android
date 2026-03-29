package io.github.sceneview

/**
 * Marks declarations that are **experimental** in the SceneView API.
 *
 * Experimental APIs may change or be removed in future releases without prior notice.
 * Using them requires an explicit opt-in:
 *
 * ```kotlin
 * @OptIn(ExperimentalSceneViewApi::class)
 * fun myFeature() { ... }
 * ```
 */
@RequiresOptIn(
    message = "This SceneView API is experimental. It may change or be removed in future releases.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
annotation class ExperimentalSceneViewApi
