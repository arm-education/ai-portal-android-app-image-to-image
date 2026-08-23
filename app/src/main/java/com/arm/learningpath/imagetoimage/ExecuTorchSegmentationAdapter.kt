package com.arm.learningpath.imagetoimage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import org.pytorch.executorch.DType
import org.pytorch.executorch.EValue
import org.pytorch.executorch.MethodMetadata
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.util.Locale

class ExecuTorchSegmentationAdapter(
    private val context: Context,
) : RuntimeRunner {
    private var module: Module? = null
    private var config: ModelConfig? = null
    private var loadTimeMs: Long = 0

    override fun load(modelDir: File, config: ModelConfig): Long {
        val started = System.nanoTime()
        validateModelFiles(modelDir, config)
        val modelFile = File(modelDir, config.modelFile)
        val loadedModule = Module.load(modelFile.absolutePath, Module.LOAD_MODE_MMAP)
        try {
            validateModule(loadedModule)
            module?.close()
            module = loadedModule
            this.config = config
            loadTimeMs = elapsedMs(started)
            return loadTimeMs
        } catch (error: Throwable) {
            loadedModule.close()
            throw error
        }
    }

    override fun runImage(imageUri: Uri, boxPrompt: FloatArray): RunResult {
        val selected = config ?: error("Call load() before runImage().")
        val activeModule = module ?: error("Call load() before runImage().")
        val started = System.nanoTime()
        val bitmap = loadBitmap(imageUri)
        val inputSize = selected.inputImageSize.takeIf { it > 0 } ?: 1024
        val preparedBitmap = prepareBitmap(bitmap, inputSize)
        val imageTensor = Tensor.fromBlob(
            bitmapToNchw(preparedBitmap, inputSize),
            longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()),
        )
        val prompt = normalizedBoxPrompt(boxPrompt, inputSize)
        val pointCoords = Tensor.fromBlob(prompt, longArrayOf(1, 1, 2, 2))

        val outputs = activeModule.execute(
            "forward",
            EValue.from(imageTensor),
            EValue.from(pointCoords),
        )
        val output = summarizeMobileSamOutput(outputs, bitmap.width, bitmap.height, prompt)
        val resultBitmap = createMaskOverlay(preparedBitmap, output.maskLogits, output.bestIndex)
        if (preparedBitmap !== bitmap) {
            preparedBitmap.recycle()
        }
        return RunResult(output.summary, loadTimeMs, elapsedMs(started), resultBitmap)
    }

    override fun close() {
        module?.close()
        module = null
    }

    private fun validateModule(module: Module) {
        val methods = module.getMethods().toSet()
        require(methods.contains("forward")) {
            "The model is missing the forward method."
        }
        module.loadMethod("forward")
        val metadata: MethodMetadata = module.getMethodMetadata("forward")
        require(metadata.getBackends().contains("XnnpackBackend")) {
            "The model forward method does not declare the XNNPACK backend."
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Could not open selected image: $uri")
        inputStream.use {
            return BitmapFactory.decodeStream(it)
                ?: error("Could not decode selected image: $uri")
        }
    }

    private fun prepareBitmap(source: Bitmap, imageSize: Int): Bitmap {
        return if (source.width == imageSize && source.height == imageSize) {
            source
        } else {
            Bitmap.createScaledBitmap(source, imageSize, imageSize, true)
        }
    }

    private fun bitmapToNchw(prepared: Bitmap, imageSize: Int): FloatArray {
        val pixels = IntArray(imageSize * imageSize)
        prepared.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
        val planeSize = imageSize * imageSize
        val tensorData = FloatArray(3 * planeSize)
        for (index in pixels.indices) {
            val color = pixels[index]
            tensorData[index] = Color.red(color) / 255.0f
            tensorData[planeSize + index] = Color.green(color) / 255.0f
            tensorData[2 * planeSize + index] = Color.blue(color) / 255.0f
        }
        return tensorData
    }

    private fun normalizedBoxPrompt(boxPrompt: FloatArray, imageSize: Int): FloatArray {
        if (boxPrompt.size == 4) {
            return boxPrompt
        }
        val margin = imageSize * 0.10f
        return floatArrayOf(margin, margin, imageSize - margin, imageSize - margin)
    }

    private fun summarizeMobileSamOutput(
        outputs: Array<EValue>,
        originalWidth: Int,
        originalHeight: Int,
        boxPrompt: FloatArray,
    ): MobileSamOutput {
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
        val coverage = positivePixels * 100.0f / maskSize

        val summary = buildString {
            appendLine("MobileSAM inference complete.")
            appendLine("Original image: ${originalWidth} x ${originalHeight}")
            appendLine("Box prompt: ${boxPrompt.joinToString(prefix = "[", postfix = "]")}")
            appendLine("Selected mask: ${bestIndex + 1} of 3")
            appendLine(String.format(Locale.US, "Predicted IoU: %.4f", ious[bestIndex]))
            appendLine(String.format(Locale.US, "Low-resolution mask coverage: %.2f%%", coverage))
            appendLine(String.format(Locale.US, "Mask logit range: %.4f to %.4f", minLogit, maxLogit))
            appendLine("Mask overlay shown in cyan.")
        }
        return MobileSamOutput(summary, masks, bestIndex)
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

    private fun validateModelFiles(modelDir: File, config: ModelConfig) {
        require(config.modelFile.isNotBlank()) {
            "Model file is required for '${config.id}'."
        }
        requireExistingFile(modelDir, config.modelFile, "ExecuTorch model file")
        config.externalDataFiles.forEach { requireExistingFile(modelDir, it, "External data file") }
    }

    private fun requireExistingFile(modelDir: File, relativePath: String, label: String) {
        require(relativePath.isNotBlank()) {
            "$label path is blank."
        }
        require(!File(relativePath).isAbsolute) {
            "$label must be relative to the app-local model directory: $relativePath"
        }
        val canonicalModelDir = modelDir.canonicalFile
        val file = File(modelDir, relativePath).canonicalFile
        require(file.path == canonicalModelDir.path || file.path.startsWith(canonicalModelDir.path + File.separator)) {
            "$label must stay below ${canonicalModelDir.absolutePath}: $relativePath"
        }
        require(file.isFile) {
            "$label is missing: ${file.absolutePath}"
        }
    }

    private fun elapsedMs(started: Long): Long {
        return (System.nanoTime() - started) / 1_000_000
    }

    private data class MobileSamOutput(
        val summary: String,
        val maskLogits: FloatArray,
        val bestIndex: Int,
    )
}
