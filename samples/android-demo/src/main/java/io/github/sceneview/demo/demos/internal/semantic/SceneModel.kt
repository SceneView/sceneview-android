package io.github.sceneview.demo.demos.internal.semantic

import io.github.sceneview.demo.hdpack.HdAsset
import io.github.sceneview.demo.hdpack.HdPackIntegrityException
import io.github.sceneview.demo.hdpack.HdPackManifest
import io.github.sceneview.demo.hdpack.HdPackStore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException

/**
 * The furniture detector: YOLOX-Tiny (Apache-2.0), 20 MB, too heavy to ride in every APK for a
 * feature only a scan's replay uses. It is published on the `assets-v1` release under its own
 * SHA-256, downloaded the first time a replay recognises a scan, and kept in app storage.
 */
object SceneModel {
    /** The model's name in logs and in the corpus test's outputs. */
    const val NAME = "yolox_tiny"

    const val SHA256 = "427cc366d34e27ff7a03e2899b5e3671425c262ea2291f88bb942bc1cc70b0f7"
    const val BYTES = 20_219_662L

    /** Where `tools/fetch-assets.sh` reads the repository's binary assets from, too. */
    const val BASE_URL = "https://github.com/sceneview/sceneview/releases/download/assets-v1"

    /** The release names each file after its hash: this URL can never serve another model. */
    const val URL = "$BASE_URL/$SHA256"

    /** The detector as a one-file download for [HdPackStore], with the credit `CREDITS.md` gives it. */
    val ASSET = HdAsset(
        id = "yolox-tiny",
        title = "YOLOX-Tiny (COCO object detector, ONNX)",
        file = SHA256,
        sha256 = SHA256,
        bytes = BYTES,
        license = "Apache-2.0",
        author = "Megvii Inc. (Megvii-BaseDetection/YOLOX)",
        source = "https://github.com/Megvii-BaseDetection/YOLOX/releases/tag/0.1.1rc0",
    )
}

/** Where the detector is, as the replay tells the user. */
sealed interface SceneModelState {
    /** On the device and checked: recognition runs. */
    data object Ready : SceneModelState

    /** On the wire, [fraction] of it on the device. */
    data class Downloading(val fraction: Float) : SceneModelState

    /** No network, or the server failed: what arrived is kept, and a later replay resumes it. */
    data object Offline : SceneModelState

    /**
     * The file failed its SHA-256 or size check and was deleted, or ONNX Runtime would not load
     * it. Recognition is off; the replay runs without it.
     */
    data object Unusable : SceneModelState
}

/**
 * The detector's file in [dir], a folder of its own: downloaded once, resumed after a cut, checked
 * against its SHA-256 and size before it is renamed into place, and reused from then on. Built on
 * [HdPackStore] — a pack of one file — so the transfer, the hashing and the rename run on
 * [Dispatchers.IO], never on the caller's thread. Building one looks at [dir]: do it off the main
 * thread too.
 *
 * [client] and [baseUrl] are for tests; [asset] too, with a payload a fake server can serve.
 */
class SceneModelStore(
    dir: File,
    client: OkHttpClient? = null,
    baseUrl: String = SceneModel.BASE_URL,
    private val asset: HdAsset = SceneModel.ASSET,
) {
    private val store = HdPackManifest(version = 1, assets = listOf(asset)).let { manifest ->
        if (client == null) {
            HdPackStore(manifest, dir, baseUrl = baseUrl)
        } else {
            HdPackStore(manifest, dir, client, baseUrl)
        }
    }

    /** The detector already on the device, `null` until it has been downloaded and checked. */
    fun cached(): File? = store.readyFile(asset.id)

    /**
     * The detector's file, downloaded first if it is not on the device yet; `null` when it could
     * not be ([SceneModelState.Offline] or [SceneModelState.Unusable]). [onState] hears every step
     * on the caller's dispatcher — the progress while it downloads, then how it ended.
     */
    suspend fun fetch(onState: (SceneModelState) -> Unit = {}): File? {
        cached()?.let {
            onState(SceneModelState.Ready)
            return it
        }
        val part = File(store.fileFor(asset).path + PART_SUFFIX)
        onState(SceneModelState.Downloading(fraction(withContext(Dispatchers.IO) { part.length() })))
        return try {
            coroutineScope {
                val progress = launch(start = CoroutineStart.UNDISPATCHED) {
                    store.transfer.collect { transfer ->
                        transfer?.fractionOf(asset.id)?.let { onState(SceneModelState.Downloading(it)) }
                    }
                }
                try {
                    store.downloadMissing(only = setOf(asset.id))
                } finally {
                    progress.cancel()
                }
            }
            cached().also { onState(if (it != null) SceneModelState.Ready else SceneModelState.Unusable) }
        } catch (corrupt: HdPackIntegrityException) {
            // A file that grew past its size stops mid-stream, before the store deletes its part
            // file: nothing that failed a check is kept, whichever check it failed.
            withContext(Dispatchers.IO) { part.delete() }
            onState(SceneModelState.Unusable)
            null
        } catch (offline: IOException) {
            onState(SceneModelState.Offline)
            null
        }
    }

    private fun fraction(onDisk: Long): Float =
        if (asset.bytes > 0L) (onDisk.toFloat() / asset.bytes).coerceIn(0f, 1f) else 0f

    private companion object {
        /** [HdPackStore]'s temp file: `<file>.part`, kept across a cut. */
        const val PART_SUFFIX = ".part"
    }
}
