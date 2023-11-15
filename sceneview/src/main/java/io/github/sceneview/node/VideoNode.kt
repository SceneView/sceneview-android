package io.github.sceneview.node

// TODO: To finish
//open class VideoNode private constructor(
//    engine: Engine,
//    val materialLoader: MaterialLoader,
//    entity: Entity = EntityManager.get().create(),
//    parent: Node? = null,
//    mediaPlayer: MediaPlayer,
//    val texture: Texture,
//    /**
//     * `null` to adjust size on the normalized image size
//     */
//    val size: Size? = null,
//    center: Position = Plane.DEFAULT_CENTER,
//    normal: Direction = Plane.DEFAULT_NORMAL,
//    builder: RenderableManager.Builder.() -> Unit = {}
//) : PlaneNode(
//    engine, entity, parent,
//    size ?: normalize(Size(mediaPlayer.videoWidth.toFloat(), mediaPlayer.videoHeight.toFloat())),
//    center, normal,
//    materialLoader.createVideoInstance(texture),
//    builder
//) {
//    var mediaPlayer = mediaPlayer
//        set(value) {
//            field = value
//            texture.setExternalStream()setBitmap(engine, value)
//            if (size == null) {
//                updateGeometry(
//                    size = normalize(Size(value.videoWidth.toFloat(), value.videoHeight.toFloat()))
//                )
//            }
//        }
//
//    init {
//        if (size == null) {
//            mediaPlayer.doOnVideoSized { player, width, height ->
//                if (player == this.player) {
//                    updateGeometry(size = normalize(Size(width.toFloat(), height.toFloat())))
//                }
//            }
//        }
//    }
//}
//
//open class VideoNode(
//    videoMaterial: VideoMaterial,
//    val player: MediaPlayer,
//    size: Size = Plane.DEFAULT_SIZE,
//    center: Position = Plane.DEFAULT_CENTER,
//    normal: Direction = Plane.DEFAULT_NORMAL,
//    scaleToVideoRatio: Boolean = true,
//    /**
//     * Keep the video aspect ratio.
//     * - `true` to fit within max width or height
//     * - `false` the video will be stretched to the node scale
//     */
//    keepAspectRatio: Boolean = true,
//    /**
//     * The parent node.
//     *
//     * If set to null, this node will not be attached.
//     *
//     * The local position, rotation, and scale of this node will remain the same.
//     * Therefore, the world position, rotation, and scale of this node may be different after the
//     * parent changes.
//     */
//    parent: Node? = null,
//    renderableApply: RenderableManager.Builder.() -> Unit = {}
//) : PlaneNode(
//    engine = videoMaterial.engine,
//    size = size,
//    center = center,
//    normal = normal,
//    materialInstance = videoMaterial.instance,
//    parent = parent,
//    renderableApply = renderableApply
//) {
//
//
//    constructor(
//        materialLoader: MaterialLoader,
//        player: MediaPlayer,
//        chromaKeyColor: Int? = null,
//        scaleToVideoRatio: Boolean = true,
//        /**
//         * Keep the video aspect ratio.
//         * - `true` to fit within max width or height
//         * - `false` the video will be stretched to the node scale
//         */
//        keepAspectRatio: Boolean = true,
//        size: Size = Plane.DEFAULT_SIZE,
//        center: Position = Plane.DEFAULT_CENTER,
//        normal: Direction = Plane.DEFAULT_NORMAL,
//        /**
//         * The parent node.
//         *
//         * If set to null, this node will not be attached.
//         *
//         * The local position, rotation, and scale of this node will remain the same.
//         * Therefore, the world position, rotation, and scale of this node may be different after the
//         * parent changes.
//         */
//        parent: Node? = null,
//        renderableApply: RenderableManager.Builder.() -> Unit = {}
//    ) : this(
//        videoMaterial = materialLoader.createVideoInstance(player, chromaKeyColor),
//        player = player,
//        scaleToVideoRatio = scaleToVideoRatio,
//        keepAspectRatio = keepAspectRatio,
//        size = size,
//        center = center,
//        normal = normal,
//        parent = parent,
//        renderableApply = renderableApply
//    )
//
//    override fun destroy() {
//        super.destroy()
//
//        player.stop()
//    }
//}
//
//
//fun MediaPlayer.doOnVideoSized(block: (player: MediaPlayer, videoWidth: Int, videoHeight: Int) -> Unit) {
//    if (videoWidth > 0 && videoHeight > 0) {
//        block(this, videoWidth, videoHeight)
//    } else {
//        setOnVideoSizeChangedListener { _, width: Int, height: Int ->
//            if (width > 0 && height > 0) {
//                block(this, width, height)
//            }
//        }
//    }
//}