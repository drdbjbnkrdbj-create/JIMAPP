package com.example

import com.example.ui.library.DocumentRenderSize
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentRenderSizeTest {
    @Test
    fun pdfPageDimensionsStayWithinReaderBitmapLimits() {
        val (width, height) = DocumentRenderSize.pageSize(12_000, 18_000)

        assertTrue(width <= DocumentRenderSize.MAX_DIMENSION)
        assertTrue(height <= DocumentRenderSize.MAX_DIMENSION)
        assertTrue(width.toLong() * height <= DocumentRenderSize.MAX_PIXELS)
    }

    @Test
    fun imageSampleSizeBoundsDecodedDimensionsAndArea() {
        val sampleSize = DocumentRenderSize.imageSampleSize(12_000, 8_000)
        val width = 12_000 / sampleSize
        val height = 8_000 / sampleSize

        assertTrue(width <= DocumentRenderSize.MAX_DIMENSION)
        assertTrue(height <= DocumentRenderSize.MAX_DIMENSION)
        assertTrue(width.toLong() * height <= DocumentRenderSize.MAX_PIXELS)
    }
}
