package com.arm.learningpath.imagetoimage.inference.models.depthanything

import android.graphics.Bitmap
import android.graphics.Color
import org.pytorch.executorch.DType
import org.pytorch.executorch.EValue
import java.util.Locale
import kotlin.math.roundToInt

object DepthAnythingPostprocessor {
    fun decode(
        outputs: Array<EValue>,
        input: DepthAnythingInput,
        outputWidth: Int,
        outputHeight: Int,
    ): DepthAnythingOutput {
        require(outputs.size == 1 && outputs[0].isTensor) {
            "Depth Anything forward must return one relative-disparity tensor."
        }
        val outputTensor = outputs[0].toTensor()
        require(outputTensor.dtype() == DType.FLOAT) { "Depth Anything output must be float32." }
        val expectedShape = longArrayOf(1, outputHeight.toLong(), outputWidth.toLong())
        require(outputTensor.shape().contentEquals(expectedShape)) {
            "Expected depth output shape ${expectedShape.contentToString()}, " +
                "received ${outputTensor.shape().contentToString()}."
        }
        return decodeDisparity(
            outputTensor.getDataAsFloatArray(),
            outputWidth,
            outputHeight,
            input.sourceWidth,
            input.sourceHeight,
        )
    }

    private fun decodeDisparity(
        disparity: FloatArray,
        outputWidth: Int,
        outputHeight: Int,
        sourceWidth: Int,
        sourceHeight: Int,
    ): DepthAnythingOutput {
        val normalized = normalizeToGrayscale(disparity, outputWidth, outputHeight)
        val pixels = IntArray(normalized.values.size) { index ->
            val value = normalized.values[index].toInt() and 0xff
            Color.rgb(value, value, value)
        }
        val modelBitmap = Bitmap.createBitmap(
            pixels,
            outputWidth,
            outputHeight,
            Bitmap.Config.ARGB_8888,
        )
        val resultBitmap = if (sourceWidth == outputWidth && sourceHeight == outputHeight) {
            modelBitmap
        } else {
            Bitmap.createScaledBitmap(modelBitmap, sourceWidth, sourceHeight, true).also {
                modelBitmap.recycle()
            }
        }
        val summary = buildString {
            appendLine("Depth Anything V2 inference complete.")
            appendLine("Original image: $sourceWidth x $sourceHeight")
            appendLine("Model input and output: $outputWidth x $outputHeight")
            appendLine(
                String.format(
                    Locale.US,
                    "Relative disparity range: %.6f to %.6f",
                    normalized.minimum,
                    normalized.maximum,
                ),
            )
            if (normalized.isConstant) {
                appendLine("The disparity map is constant, so the rendered map is black.")
            }
            append("White pixels represent nearer regions; black pixels represent farther regions.")
        }
        return DepthAnythingOutput(summary, resultBitmap)
    }

    internal fun normalizeToGrayscale(
        disparity: FloatArray,
        width: Int,
        height: Int,
    ): NormalizedDisparity {
        require(width > 0 && height > 0) { "Depth output dimensions must be positive." }
        require(disparity.size == width * height) {
            "Expected ${width * height} disparity values, received ${disparity.size}."
        }
        require(disparity.all { it.isFinite() }) {
            "Depth output contains a non-finite disparity value."
        }

        val minimum = disparity.minOrNull() ?: error("Depth output is empty.")
        val maximum = disparity.maxOrNull() ?: error("Depth output is empty.")
        val range = maximum - minimum
        if (range == 0.0f) {
            return NormalizedDisparity(ByteArray(disparity.size), minimum, maximum, true)
        }
        val values = ByteArray(disparity.size) { index ->
            (((disparity[index] - minimum) / range) * 255.0f)
                .coerceIn(0.0f, 255.0f)
                .roundToInt()
                .toByte()
        }
        return NormalizedDisparity(values, minimum, maximum, false)
    }

    internal data class NormalizedDisparity(
        val values: ByteArray,
        val minimum: Float,
        val maximum: Float,
        val isConstant: Boolean,
    )
}
