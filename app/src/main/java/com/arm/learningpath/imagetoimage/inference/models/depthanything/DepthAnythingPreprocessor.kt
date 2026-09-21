package com.arm.learningpath.imagetoimage.inference.models.depthanything

import com.arm.learningpath.imagetoimage.image.DecodedImage
import org.pytorch.executorch.Tensor
import kotlin.math.floor

object DepthAnythingPreprocessor {
    private val mean = doubleArrayOf(0.485, 0.456, 0.406)
    private val standardDeviation = doubleArrayOf(0.229, 0.224, 0.225)

    fun prepare(
        image: DecodedImage,
        targetWidth: Int,
        targetHeight: Int,
    ): DepthAnythingInput {
        require(targetWidth > 0 && targetHeight > 0) {
            "Depth Anything input dimensions must be positive."
        }

        val sourceWidth = image.bitmap.width
        val sourceHeight = image.bitmap.height
        val pixels = IntArray(sourceWidth * sourceHeight)
        image.bitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight)
        val tensorData = resizeAndNormalize(
            pixels,
            sourceWidth,
            sourceHeight,
            targetWidth,
            targetHeight,
        )

        return DepthAnythingInput(
            imageTensor = Tensor.fromBlob(
                tensorData,
                longArrayOf(1, 3, targetHeight.toLong(), targetWidth.toLong()),
            ),
            dimensions = DepthAnythingImageDimensions(
                sourceWidth = image.sourceWidth,
                sourceHeight = image.sourceHeight,
                decodedWidth = image.bitmap.width,
                decodedHeight = image.bitmap.height,
            ),
        )
    }

    internal fun resizeAndNormalize(
        source: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): FloatArray {
        require(sourceWidth > 0 && sourceHeight > 0) { "Source dimensions must be positive." }
        require(targetWidth > 0 && targetHeight > 0) { "Target dimensions must be positive." }
        require(source.size == sourceWidth * sourceHeight) {
            "Source pixel count does not match its dimensions."
        }

        val planeSize = targetWidth * targetHeight
        val result = FloatArray(3 * planeSize)
        for (targetY in 0 until targetHeight) {
            val sourceY = (targetY + 0.5) * sourceHeight / targetHeight - 0.5
            for (targetX in 0 until targetWidth) {
                val sourceX = (targetX + 0.5) * sourceWidth / targetWidth - 0.5
                val rgb = bicubicRgb(source, sourceWidth, sourceHeight, sourceX, sourceY)
                val index = targetY * targetWidth + targetX
                for (channel in 0..2) {
                    result[channel * planeSize + index] =
                        ((rgb[channel] - mean[channel]) / standardDeviation[channel]).toFloat()
                }
            }
        }
        return result
    }

    private fun bicubicRgb(
        source: IntArray,
        width: Int,
        height: Int,
        x: Double,
        y: Double,
    ): DoubleArray {
        val baseX = floor(x).toInt()
        val baseY = floor(y).toInt()
        val totals = DoubleArray(3)
        var totalWeight = 0.0

        for (offsetY in -1..2) {
            val sampleY = (baseY + offsetY).coerceIn(0, height - 1)
            val weightY = cubicWeight(y - (baseY + offsetY))
            for (offsetX in -1..2) {
                val sampleX = (baseX + offsetX).coerceIn(0, width - 1)
                val weight = weightY * cubicWeight(x - (baseX + offsetX))
                val color = source[sampleY * width + sampleX]
                totals[0] += (((color shr 16) and 0xff) / 255.0) * weight
                totals[1] += (((color shr 8) and 0xff) / 255.0) * weight
                totals[2] += ((color and 0xff) / 255.0) * weight
                totalWeight += weight
            }
        }

        require(totalWeight != 0.0) { "Bicubic resize produced an invalid weight." }
        return DoubleArray(3) { channel -> totals[channel] / totalWeight }
    }

    private fun cubicWeight(distance: Double): Double {
        val value = kotlin.math.abs(distance)
        val coefficient = -0.75
        return when {
            value <= 1.0 ->
                (coefficient + 2.0) * value * value * value -
                    (coefficient + 3.0) * value * value + 1.0
            value < 2.0 ->
                coefficient * value * value * value -
                    5.0 * coefficient * value * value +
                    8.0 * coefficient * value - 4.0 * coefficient
            else -> 0.0
        }
    }
}
