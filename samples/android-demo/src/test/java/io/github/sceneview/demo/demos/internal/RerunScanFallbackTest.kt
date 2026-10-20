package io.github.sceneview.demo.demos.internal

import io.github.sceneview.demo.demos.RerunModelSource
import io.github.sceneview.demo.demos.buildScanModel
import io.github.sceneview.demo.demos.internal.model.Box
import io.github.sceneview.demo.demos.internal.model.CleanScenes
import io.github.sceneview.demo.demos.internal.model.ModelPhoto
import io.github.sceneview.demo.demos.internal.model.ModelPhotos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * A model the result screen falls back to — the points when meshing fails, the raw surface when
 * the finish fails — is shown but never kept as the scan's model: the next open tries again,
 * instead of reading the fallback back for good. Robolectric for the fallbacks' `Log` lines.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RerunScanFallbackTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the points shown when meshing fails are not kept as the scan's model`() {
        val model = assertNotNull(buildScanModel(RerunModelSource.Synthetic(meshFails = true)) {})
        assertEquals(RerunScanModel.Kind.Points, model.kind)

        val store = RerunSessionStore(folder.newFolder("sessions"))
        val saved = store.save(bundled(), "Kitchen", RerunSessionSource.Recorded, 1_000_000L)
        store.saveModel(saved.id, model)
        assertNull("a fallback read back for good", store.model(saved.id))

        val withModel = store.save(bundled(), "Hall", RerunSessionSource.Recorded, 2_000_000L, model = model)
        assertNull("a fallback read back for good", store.model(withModel.id))
    }

    @Test
    fun `the raw surface shown when the finish fails is not kept as the scan's model`() {
        val box = Box(0f, 0f, 0f, 1.2f, 0.9f, 1.0f)
        val tsdf = CleanScenes.fuse(Box(-0.2f, -0.2f, -0.2f, 1.4f, 1.1f, 1.2f), NOISE_M, NOISE_M) { x, y, z ->
            box.sdf(x, y, z)
        }
        // A key photo that no longer decodes: the finish fails, and the raw surface is shown.
        val unreadable = ModelPhoto(RerunSyntheticRoom.lookPose(Vec3(0.6f, 0.45f, 2.6f), 0f, 0f)) {
            error("unreadable photo")
        }
        val source = RerunModelSource.Live(tsdf, cloud = null, photos = ModelPhotos(LENS, listOf(unreadable)))
        val model = assertNotNull(buildScanModel(source) {})
        assertEquals(RerunScanModel.Kind.Mesh, model.kind)

        val store = RerunSessionStore(folder.newFolder("sessions"))
        val saved = store.save(bundled(), "Kitchen", RerunSessionSource.Recorded, 1_000_000L)
        store.saveModel(saved.id, model)
        assertNull("a raw surface read back as the finished model", store.model(saved.id))
    }

    private fun <T> assertNotNull(value: T?): T {
        org.junit.Assert.assertNotNull(value)
        return value!!
    }

    private fun bundled(): RerunCapturePack {
        val dir = listOf(
            File("src/main/assets/${RerunReplayAssets.DIR}"),
            File("samples/android-demo/src/main/assets/${RerunReplayAssets.DIR}"),
        ).first { it.isDirectory }
        return RerunCapturePack(
            manifest = File(dir, "showcase-manifest.json").readBytes(),
            log = File(dir, "showcase-session.jsonl").readBytes(),
            media = File(dir, "showcase-media.bin").readBytes(),
        )
    }

    private companion object {
        const val NOISE_M = 0.002f
        val LENS = ReplayLens(0.35f, 0.6f)
    }
}
