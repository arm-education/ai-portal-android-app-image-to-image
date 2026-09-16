package com.arm.learningpath.imagetoimage.inference.models.depthanything

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthAnythingPostprocessorTest {
    @Test
    fun mapsMinimumToBlackAndMaximumToWhite() {
        val result = DepthAnythingPostprocessor.normalizeToGrayscale(
            floatArrayOf(2.0f, 3.0f, 4.0f),
            width = 3,
            height = 1,
        )

        assertArrayEquals(byteArrayOf(0, 128.toByte(), 255.toByte()), result.values)
        assertEquals(2.0f, result.minimum)
        assertEquals(4.0f, result.maximum)
        assertFalse(result.isConstant)
    }

    @Test
    fun constantMapProducesBlackPixelsWithoutDivisionByZero() {
        val result = DepthAnythingPostprocessor.normalizeToGrayscale(
            floatArrayOf(5.0f, 5.0f),
            width = 2,
            height = 1,
        )

        assertArrayEquals(byteArrayOf(0, 0), result.values)
        assertTrue(result.isConstant)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteDisparity() {
        DepthAnythingPostprocessor.normalizeToGrayscale(
            floatArrayOf(0.0f, Float.NaN),
            width = 2,
            height = 1,
        )
    }
}
