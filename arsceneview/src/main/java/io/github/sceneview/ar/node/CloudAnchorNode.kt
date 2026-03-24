package io.github.sceneview.ar.node

import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.core.HostCloudAnchorFuture
import com.google.ar.core.Session
import com.google.ar.core.TrackingState

/**
 * An [AnchorNode] that supports ARCore Cloud Anchors for persistent, cross-device AR experiences.
 *
 * Cloud Anchors allow an anchor's pose to be hosted to the Google Cloud ARCore API and later
 * resolved on a different device or session using the returned cloud anchor ID.
 *
 * ### Hosting
 * After creating a `CloudAnchorNode` with a local [Anchor], call [host] to upload it. On success,
 * [onHosted] is invoked with the cloud anchor ID that can be shared with other devices.
 *
 * ### Resolving
 * Use [CloudAnchorNode.resolve] (companion) to resolve a previously hosted cloud anchor by ID.
 *
 * Requires `Config.CloudAnchorMode.ENABLED` in the ARCore session configuration.
 *
 * @param engine                 The Filament [Engine].
 * @param anchor                 The local [Anchor] to associate with a cloud anchor.
 * @param cloudAnchorId          The cloud anchor ID if already resolved; `null` when hosting.
 * @param onTrackingStateChanged Callback invoked when tracking state changes.
 * @param onUpdated              Callback invoked each frame while the anchor is updated.
 * @param onHosted               Callback invoked when cloud hosting completes (success or fail).
 */
open class CloudAnchorNode constructor(
    engine: Engine,
    anchor: Anchor,
    cloudAnchorId: String? = null,
    onTrackingStateChanged: ((TrackingState) -> Unit)? = null,
    onUpdated: ((Anchor?) -> Unit)? = null,
    var onHosted: ((cloudAnchorId: String?, state: CloudAnchorState) -> Unit)? = null
) : AnchorNode(
    engine = engine,
    anchor = anchor,
    onTrackingStateChanged = onTrackingStateChanged,
    onUpdated = onUpdated
) {
    var cloudAnchorId: String? = cloudAnchorId
        private set

    /**
     * The current cloud anchor state of the [anchor].
     */
    var hostState = CloudAnchorState.NONE
        private set

    var hostTask: HostCloudAnchorFuture? = null

    /**
     * Hosts a Cloud Anchor based on the [Anchor]
     *
     * @param ttlDays The lifetime of the anchor in days.
     * @param onCompleted Called when the task completes successfully or with an error.
     *
     * @see [Session.hostCloudAnchorWithTtl] for more details.
     */
    fun host(
        session: Session,
        ttlDays: Int = 1,
        onCompleted: ((cloudAnchorId: String?, state: CloudAnchorState) -> Unit)? = null
    ) {
        cancelHost()
        hostTask = session.hostCloudAnchorAsync(anchor, ttlDays) { cloudAnchorId, state ->
            onHosted(cloudAnchorId, state)
            onCompleted?.invoke(cloudAnchorId, state)
            hostTask = null
        }
    }

    open fun onHosted(cloudAnchorId: String?, state: CloudAnchorState) {
        this.hostState = state
        this.cloudAnchorId = cloudAnchorId
        onHosted?.invoke(cloudAnchorId, state)
    }

    fun cancelHost() {
        hostTask?.cancel()
        hostState = CloudAnchorState.NONE
        hostTask = null
    }

    override fun destroy() {
        cancelHost()

        super.destroy()
    }

    companion object {
        /**
         * Resolves a Cloud Anchor
         *
         * The [anchor] is replaced with a new anchor returned by [Session.resolveCloudAnchor].
         *
         * @param cloudAnchorId The Cloud Anchor ID of the Cloud Anchor.
         * @param onCompleted Called when the task completes successfully or with an error.
         *
         * @see [Session.resolveCloudAnchorAsync] for more details.
         */
        fun resolve(
            engine: Engine,
            session: Session,
            cloudAnchorId: String,
            onCompleted: (state: CloudAnchorState, node: CloudAnchorNode?) -> Unit
        ) = session.resolveCloudAnchorAsync(cloudAnchorId) { anchor, state ->
            onCompleted(state, anchor?.takeIf { !state.isError }?.let {
                CloudAnchorNode(engine, it, cloudAnchorId)
            })
        }
    }
}