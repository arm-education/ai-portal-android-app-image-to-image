package com.arm.learningpath.imagetoimage.inference.models.depthanything

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DepthAnythingPreprocessorTest {
    @Test
    fun normalizesRgbChannelsIntoNchwPlanes() {
        val red = 0xffff0000.toInt()
        val green = 0xff00ff00.toInt()

        val result = DepthAnythingPreprocessor.resizeAndNormalize(
            intArrayOf(red, green),
            sourceWidth = 2,
            sourceHeight = 1,
            targetWidth = 2,
            targetHeight = 1,
        )

        assertEquals(6, result.size)
        assertArrayEquals(
            floatArrayOf(
                (1.0f - 0.485f) / 0.229f,
                (0.0f - 0.485f) / 0.229f,
                (0.0f - 0.456f) / 0.224f,
                (1.0f - 0.456f) / 0.224f,
                (0.0f - 0.406f) / 0.225f,
                (0.0f - 0.406f) / 0.225f,
            ),
            result,
            0.0001f,
        )
    }

    @Test
    fun directResizeProducesRequestedRectangularTensorSize() {
        val result = DepthAnythingPreprocessor.resizeAndNormalize(
            intArrayOf(0xff7f7f7f.toInt()),
            sourceWidth = 1,
            sourceHeight = 1,
            targetWidth = 4,
            targetHeight = 3,
        )

        assertEquals(3 * 4 * 3, result.size)
    }

    @Test
    fun resultRenderingUsesTheBoundedDecodedImageDimensions() {
        val dimensions = DepthAnythingImageDimensions(
            sourceWidth = 12_000,
            sourceHeight = 9_000,
            decodedWidth = 667,
            decodedHeight = 500,
        )

        assertEquals(667, dimensions.renderWidth)
        assertEquals(500, dimensions.renderHeight)
    }
}
