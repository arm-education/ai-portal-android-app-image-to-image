package com.arm.learningpath.imagetoimage.inference.models.mobilesam

import android.graphics.Bitmap
import android.graphics.Color
import org.pytorch.executorch.DType
import org.pytorch.executorch.EValue
import java.util.Locale

object MobileSamPostprocessor {
    fun decode(outputs: Array<EValue>, input: MobileSamInput): MobileSamOutput {
        require(outputs.size >= 2 && outputs[0].isTensor && outputs[1].isTensor) {
            "MobileSAM forward must return mask logits and IoU predictions."
        }

        val lowResMasks = outputs[0].toTensor()
        val iouPredictions = outputs[1].toTensor()
        require(lowResMasks.dtype() == DType.FLOAT) {
            "Mask output must be float32."
        }
        require(iouPredictions.dtype() == DType.FLOAT) {
            "IoU output must be float32."
        }
        require(lowResMasks.shape().contentEquals(longArrayOf(1, 3, 256, 256))) {
            "Expected mask shape [1, 3, 256, 256], received ${lowResMasks.shape().contentToString()}."
        }
        require(iouPredictions.shape().contentEquals(longArrayOf(1, 3))) {
            "Expected IoU shape [1, 3], received ${iouPredictions.shape().contentToString()}."
        }

        val masks = lowResMasks.getDataAsFloatArray()
        val ious = iouPredictions.getDataAsFloatArray()
        val bestIndex = ious.indices.maxBy { ious[it] }
        val maskStats = maskStats(masks, bestIndex)
        val summary = buildSummary(input, bestIndex, ious[bestIndex], maskStats)
        val resultBitmap = createMaskOverlay(input.preparedBitmap, masks, bestIndex)
        return MobileSamOutput(summary, resultBitmap)
    }

    private fun maskStats(masks: FloatArray, bestIndex: Int): MaskStats {
        val maskSize = 256 * 256
        val maskOffset = bestIndex * maskSize
        var positivePixels = 0
        var minLogit = Float.POSITIVE_INFINITY
        var maxLogit = Float.NEGATIVE_INFINITY
        for (index in 0 until maskSize) {
            val value = masks[maskOffset + index]
            if (value > 0.0f) {
                positivePixels += 1
            }
            minLogit = minOf(minLogit, value)
            maxLogit = maxOf(maxLogit, value)
        }
        return MaskStats(
            coverage = positivePixels * 100.0f / maskSize,
            minLogit = minLogit,
            maxLogit = maxLogit,
        )
    }

    private fun buildSummary(
        input: MobileSamInput,
        bestIndex: Int,
        predictedIou: Float,
        stats: MaskStats,
    ): String {
        return buildString {
            appendLine("MobileSAM inference complete.")
            appendLine("Original image: ${input.sourceWidth} x ${input.sourceHeight}")
            appendLine("Box prompt: ${input.boxPrompt.joinToString(prefix = "[", postfix = "]")}")
            appendLine("Selected mask: ${bestIndex + 1} of 3")
            appendLine(String.format(Locale.US, "Predicted IoU: %.4f", predictedIou))
            appendLine(String.format(Locale.US, "Low-resolution mask coverage: %.2f%%", stats.coverage))
            appendLine(String.format(Locale.US, "Mask logit range: %.4f to %.4f", stats.minLogit, stats.maxLogit))
            appendLine("Mask overlay shown in cyan.")
        }
    }

    private fun createMaskOverlay(source: Bitmap, masks: FloatArray, bestIndex: Int): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        val maskSize = 256
        val maskOffset = bestIndex * maskSize * maskSize
        for (y in 0 until height) {
            val maskY = (y * maskSize / height).coerceIn(0, maskSize - 1)
            for (x in 0 until width) {
                val maskX = (x * maskSize / width).coerceIn(0, maskSize - 1)
                val logit = masks[maskOffset + maskY * maskSize + maskX]
                if (logit > 0.0f) {
                    val pixelIndex = y * width + x
                    pixels[pixelIndex] = Color.argb(115, 0, 188, 200)
                }
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private data class MaskStats(
        val coverage: Float,
        val minLogit: Float,
        val maxLogit: Float,
    )
}
