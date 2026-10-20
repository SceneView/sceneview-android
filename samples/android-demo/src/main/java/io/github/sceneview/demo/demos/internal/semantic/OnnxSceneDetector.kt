package io.github.sceneview.demo.demos.internal.semantic

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.nio.FloatBuffer

/**
 * A YOLOX detector ([YoloxCodec]) run by ONNX Runtime: `onnxruntime-android` in the app, the
 * desktop `onnxruntime` in the JVM tests — one API, so this class is both. [model] is the
 * `.onnx` file's bytes; the input's size is read from it (416 for Tiny and Nano).
 */
class OnnxSceneDetector(
    model: ByteArray,
    override val name: String = "yolox",
    threads: Int = DEFAULT_THREADS,
    private val minScore: Float = YoloxCodec.MIN_SCORE,
    private val keep: (String) -> Boolean = { SceneLabel.ofDetector(it) != null },
) : SceneDetector {
    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = OrtSession.SessionOptions().use { options ->
        options.setIntraOpNumThreads(threads)
        environment.createSession(model, options)
    }
    private val inputName = session.inputNames.first()

    /** The model's square input side, in pixels. */
    val size: Int = ((session.inputInfo.getValue(inputName).info as TensorInfo).shape.last()).toInt()
        .takeIf { it > 0 } ?: DEFAULT_SIZE

    private val input = FloatArray(3 * size * size)

    @Synchronized
    override fun detect(image: SceneImage): List<Detection2D> {
        val ratio = YoloxCodec.input(image, size, input)
        val shape = longArrayOf(1, 3, size.toLong(), size.toLong())
        return OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                val output = result.get(0) as OnnxTensor
                val dims = (output.info as TensorInfo).shape
                val buffer = output.floatBuffer
                val values = FloatArray(buffer.remaining()).also { buffer.get(it) }
                val columns = dims.last().toInt()
                YoloxCodec.decode(
                    values, values.size / columns, columns, size, ratio, image.width, image.height,
                    minScore = minScore, keep = keep,
                )
            }
        }
    }

    override fun close() = session.close()

    companion object {
        const val DEFAULT_THREADS = 2
        const val DEFAULT_SIZE = 416
    }
}
