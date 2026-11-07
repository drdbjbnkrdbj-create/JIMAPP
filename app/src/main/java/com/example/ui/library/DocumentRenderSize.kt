package com.example.ui.library

import kotlin.math.sqrt

internal object DocumentRenderSize {
    const val MAX_DIMENSION = 2048
    const val MAX_PIXELS = 2_000_000L

    fun imageSampleSize(width: Int, height: Int): Int {
        require(width > 0 && height > 0)
        var sampleSize = 1
        while (
            width / sampleSize > MAX_DIMENSION ||
            height / sampleSize > MAX_DIMENSION ||
            (width / sampleSize).toLong() * (height / sampleSize) > MAX_PIXELS
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun pageSize(width: Int, height: Int): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val dimensionScale = MAX_DIMENSION.toFloat() / maxOf(width, height)
        val pixelScale = sqrt(MAX_PIXELS.toDouble() / (width.toDouble() * height)).toFloat()
        val scale = minOf(1f, dimensionScale, pixelScale)
        return (width * scale).toInt().coerceAtLeast(1) to
            (height * scale).toInt().coerceAtLeast(1)
    }
}
