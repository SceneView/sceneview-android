package io.github.sceneview.demo.demos.internal

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * The final model of a scan ([RerunMesh]) as one glTF 2.0 binary: a single `room` mesh, indexed
 * triangles with `POSITION`, `NORMAL` and `COLOR_0` (normalised `UNSIGNED_BYTE` RGBA, linear, as
 * glTF requires) and one matte, lit, single-sided material that multiplies the vertex colours.
 * Metres, Y up, the session's world space: what Blender, three.js and SceneView open as they are.
 */
object RerunMeshGlb {
    const val MIME_TYPE = "model/gltf-binary"

    /**
     * The `.glb` bytes of [mesh], written straight into the one array returned: a full room's
     * model is tens of MB, and a phone holds the mesh, this and the renderer's copy at once.
     */
    fun write(mesh: RerunMesh, generator: String = "SceneView Rerun"): ByteArray {
        val n = mesh.vertexCount
        val positionBytes = n * 12
        val normalBytes = n * 12
        val colorBytes = n * 4
        val indexBytes = mesh.indices.size * 4
        val binBytes = positionBytes + normalBytes + colorBytes + indexBytes
        val views = listOf(
            view(0, positionBytes, ARRAY_BUFFER, stride = 12),
            view(positionBytes, normalBytes, ARRAY_BUFFER, stride = 12),
            view(positionBytes + normalBytes, colorBytes, ARRAY_BUFFER, stride = 4),
            view(positionBytes + normalBytes + colorBytes, indexBytes, ELEMENT_ARRAY_BUFFER, stride = null),
        )
        val accessors = accessors(mesh)
        val json = jsonOf(
            "asset" to jsonOf("version" to "2.0", "generator" to generator),
            "scene" to 0,
            "scenes" to listOf(jsonOf("nodes" to listOf(0))),
            "nodes" to listOf(jsonOf("name" to "room", "mesh" to 0)),
            "meshes" to listOf(
                jsonOf(
                    "name" to "room",
                    "primitives" to listOf(
                        jsonOf(
                            "attributes" to jsonOf("POSITION" to 0, "NORMAL" to 1, "COLOR_0" to 2),
                            "indices" to 3,
                            "material" to 0,
                            "mode" to TRIANGLES,
                        ),
                    ),
                ),
            ),
            "materials" to listOf(
                jsonOf(
                    "name" to "room",
                    "pbrMetallicRoughness" to jsonOf(
                        "baseColorFactor" to listOf(1.0, 1.0, 1.0, 1.0),
                        "metallicFactor" to 0.0,
                        "roughnessFactor" to ROUGHNESS,
                    ),
                ),
            ),
            "buffers" to listOf(jsonOf("byteLength" to binBytes)),
            "bufferViews" to views,
            "accessors" to accessors,
        )
        return glb(RerunJson.write(json).toByteArray(Charsets.UTF_8), binBytes) { putBinary(mesh) }
    }

    private fun accessors(mesh: RerunMesh): List<JsonMap> {
        val n = mesh.vertexCount
        val b = mesh.bounds()
        return listOf(
            jsonOf(
                "bufferView" to 0, "componentType" to FLOAT, "count" to n, "type" to "VEC3",
                "min" to listOf(b[0], b[1], b[2]).map(Float::toDouble),
                "max" to listOf(b[3], b[4], b[5]).map(Float::toDouble),
            ),
            jsonOf("bufferView" to 1, "componentType" to FLOAT, "count" to n, "type" to "VEC3"),
            jsonOf(
                "bufferView" to 2, "componentType" to UNSIGNED_BYTE, "normalized" to true,
                "count" to n, "type" to "VEC4",
            ),
            jsonOf(
                "bufferView" to 3, "componentType" to UNSIGNED_INT,
                "count" to mesh.indices.size, "type" to "SCALAR",
            ),
        )
    }

    private fun ByteBuffer.putBinary(mesh: RerunMesh) {
        mesh.positions.forEach { putFloat(it) }
        mesh.normals.forEach { putFloat(it) }
        for (c in mesh.colors) {
            put(LINEAR[(c shr 16) and 0xFF])
            put(LINEAR[(c shr 8) and 0xFF])
            put(LINEAR[c and 0xFF])
            put(0xFF.toByte())
        }
        mesh.indices.forEach { putInt(it) }
    }

    private fun view(offset: Int, length: Int, target: Int, stride: Int?): JsonMap =
        jsonOf("buffer" to 0, "byteOffset" to offset, "byteLength" to length, "target" to target).also {
            if (stride != null) it["byteStride"] = stride
        }

    /** The container around [json] and a binary chunk of [binBytes] that [writeBin] puts in place. */
    private fun glb(json: ByteArray, binBytes: Int, writeBin: ByteBuffer.() -> Unit): ByteArray {
        val jsonPadded = pad(json.size)
        val binPadded = pad(binBytes)
        val total = HEADER + CHUNK_HEADER + jsonPadded + CHUNK_HEADER + binPadded
        val out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(GLB_MAGIC).putInt(2).putInt(total)
        out.putInt(jsonPadded).putInt(CHUNK_JSON).put(json)
        repeat(jsonPadded - json.size) { out.put(' '.code.toByte()) }
        out.putInt(binPadded).putInt(CHUNK_BIN)
        val start = out.position()
        out.writeBin()
        check(out.position() - start == binBytes) { "binary chunk is ${out.position() - start}, not $binBytes" }
        repeat(binPadded - binBytes) { out.put(0) }
        return out.array()
    }

    private fun pad(size: Int) = (size + 3) and 3.inv()

    /** sRGB byte → linear byte, glTF's colour space for `COLOR_0`. */
    private val LINEAR = ByteArray(256) { i ->
        val c = i / 255.0
        val linear = if (c <= SRGB_KNEE) {
            c / SRGB_LINEAR_SLOPE
        } else {
            ((c + SRGB_OFFSET) / (1 + SRGB_OFFSET)).pow(SRGB_GAMMA)
        }
        (linear * 255 + 0.5).toInt().coerceIn(0, 255).toByte()
    }

    private const val ROUGHNESS = 0.9
    private const val SRGB_KNEE = 0.04045
    private const val SRGB_LINEAR_SLOPE = 12.92
    private const val SRGB_OFFSET = 0.055
    private const val SRGB_GAMMA = 2.4

    private const val HEADER = 12
    private const val CHUNK_HEADER = 8
    private const val GLB_MAGIC = 0x46546C67
    private const val CHUNK_JSON = 0x4E4F534A
    private const val CHUNK_BIN = 0x004E4942
    private const val ARRAY_BUFFER = 34962
    private const val ELEMENT_ARRAY_BUFFER = 34963
    private const val FLOAT = 5126
    private const val UNSIGNED_BYTE = 5121
    private const val UNSIGNED_INT = 5125
    private const val TRIANGLES = 4
}
