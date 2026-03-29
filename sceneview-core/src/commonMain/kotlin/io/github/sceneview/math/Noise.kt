package io.github.sceneview.math

import kotlin.math.floor

/**
 * Procedural noise functions for terrain, textures, and procedural geometry.
 *
 * Implements classic Perlin noise (gradient noise) and value noise,
 * plus fractal Brownian motion (fBm) layering for natural-looking results.
 */
object Noise {

    // Permutation table (Ken Perlin's original)
    private val perm = intArrayOf(
        151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225,
        140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148,
        247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
        57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175,
        74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122,
        60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54,
        65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169,
        200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64,
        52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212,
        207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213,
        119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
        129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104,
        218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
        81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157,
        184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93,
        222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
    )

    // Doubled permutation table to avoid wrapping
    private val p = IntArray(512) { perm[it and 255] }

    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)

    private fun grad(hash: Int, x: Float, y: Float, z: Float): Float {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return (if (h and 1 == 0) u else -u) + (if (h and 2 == 0) v else -v)
    }

    private fun grad2d(hash: Int, x: Float, y: Float): Float {
        val h = hash and 3
        return when (h) {
            0 -> x + y
            1 -> -x + y
            2 -> x - y
            else -> -x - y
        }
    }

    /**
     * 2D Perlin noise. Returns a value in approximately [-1, 1].
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @return Noise value in approximately [-1, 1].
     */
    fun perlin2D(x: Float, y: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val xf = x - floor(x)
        val yf = y - floor(y)
        val u = fade(xf)
        val v = fade(yf)

        val aa = p[p[xi] + yi]
        val ab = p[p[xi] + yi + 1]
        val ba = p[p[xi + 1] + yi]
        val bb = p[p[xi + 1] + yi + 1]

        val x1 = lerp(grad2d(aa, xf, yf), grad2d(ba, xf - 1f, yf), u)
        val x2 = lerp(grad2d(ab, xf, yf - 1f), grad2d(bb, xf - 1f, yf - 1f), u)
        return lerp(x1, x2, v)
    }

    /**
     * 3D Perlin noise. Returns a value in approximately [-1, 1].
     *
     * Based on Ken Perlin's improved noise (2002).
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     * @return Noise value in approximately [-1, 1].
     */
    fun perlin3D(x: Float, y: Float, z: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val zi = floor(z).toInt() and 255
        val xf = x - floor(x)
        val yf = y - floor(y)
        val zf = z - floor(z)
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        val aaa = p[p[p[xi] + yi] + zi]
        val aba = p[p[p[xi] + yi + 1] + zi]
        val aab = p[p[p[xi] + yi] + zi + 1]
        val abb = p[p[p[xi] + yi + 1] + zi + 1]
        val baa = p[p[p[xi + 1] + yi] + zi]
        val bba = p[p[p[xi + 1] + yi + 1] + zi]
        val bab = p[p[p[xi + 1] + yi] + zi + 1]
        val bbb = p[p[p[xi + 1] + yi + 1] + zi + 1]

        val x1 = lerp(grad(aaa, xf, yf, zf), grad(baa, xf - 1f, yf, zf), u)
        val x2 = lerp(grad(aba, xf, yf - 1f, zf), grad(bba, xf - 1f, yf - 1f, zf), u)
        val y1 = lerp(x1, x2, v)

        val x3 = lerp(grad(aab, xf, yf, zf - 1f), grad(bab, xf - 1f, yf, zf - 1f), u)
        val x4 = lerp(grad(abb, xf, yf - 1f, zf - 1f), grad(bbb, xf - 1f, yf - 1f, zf - 1f), u)
        val y2 = lerp(x3, x4, v)

        return lerp(y1, y2, w)
    }

    /**
     * Fractal Brownian Motion (fBm) using 2D Perlin noise.
     *
     * Layers multiple octaves of noise at increasing frequency and decreasing amplitude
     * to produce natural-looking, multi-scale patterns. Useful for terrain heightmaps,
     * cloud textures, and procedural materials.
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param octaves Number of noise layers. More octaves = more detail. Default 6.
     * @param lacunarity Frequency multiplier per octave. Default 2.0.
     * @param persistence Amplitude multiplier per octave. Default 0.5.
     * @return Noise value (range depends on octaves, typically in [-1, 1]).
     */
    fun fbm2D(
        x: Float, y: Float,
        octaves: Int = 6,
        lacunarity: Float = 2f,
        persistence: Float = 0.5f
    ): Float {
        var value = 0f
        var amplitude = 1f
        var frequency = 1f
        var maxAmplitude = 0f

        for (i in 0 until octaves) {
            value += perlin2D(x * frequency, y * frequency) * amplitude
            maxAmplitude += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }

        return value / maxAmplitude
    }

    /**
     * Fractal Brownian Motion using 3D Perlin noise.
     *
     * @see fbm2D for parameter descriptions.
     */
    fun fbm3D(
        x: Float, y: Float, z: Float,
        octaves: Int = 6,
        lacunarity: Float = 2f,
        persistence: Float = 0.5f
    ): Float {
        var value = 0f
        var amplitude = 1f
        var frequency = 1f
        var maxAmplitude = 0f

        for (i in 0 until octaves) {
            value += perlin3D(x * frequency, y * frequency, z * frequency) * amplitude
            maxAmplitude += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }

        return value / maxAmplitude
    }

    // Private lerp to avoid import ambiguity
    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)
}
