package io.github.sceneview.ar.performance

/**
 * ### Provides access to the performance configuration
 */
object Performance {

    /**
     * ### The current performance configuration
     *
     * The performance configuration can be changed at any time. However, the parameters won't be
     * updated for objects that have been already created. The default configuration is [Unlimited].
     */
    var configuration: Configuration = Unlimited()

    /**
     * ### The performance configuration
     *
     * Groups all parameters related to performance and allows changing them globally in a single
     * line of code. Classes get the parameters from the current performance [configuration].
     */
    interface Configuration {

        /**
         * ### The anchor pose update interval in [io.github.sceneview.ar.node.ArNode]
         *
         * @see io.github.sceneview.ar.node.ArNode.anchorPoseUpdateInterval
         */
        val anchorPoseUpdateInterval: Double

        /**
         * ### The maximum number of hit tests per second in [io.github.sceneview.ar.node.ArModelNode]
         *
         * @see io.github.sceneview.ar.node.ArModelNode.maxHitTestPerSecond
         */
        val maxHitTestsPerSecond: Int
    }

    /**
     * ### The unlimited performance configuration
     *
     * The goal of this configuration is to provide the best AR experience that is similar to the
     * official ARCore samples.
     */
    open class Unlimited : Configuration {

        override val anchorPoseUpdateInterval = 0.0

        override val maxHitTestsPerSecond = Int.MAX_VALUE
    }

    /**
     * ### The optimized performance configuration
     *
     * The goal of this configuration is to improve power consumption and performance.
     */
    open class Optimized : Configuration {

        override val anchorPoseUpdateInterval = 0.1

        override val maxHitTestsPerSecond = 10
    }
}