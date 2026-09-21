package com.arm.learningpath.imagetoimage.inference.models.depthanything

import com.arm.learningpath.imagetoimage.catalog.ModelConfig
import com.arm.learningpath.imagetoimage.image.DecodedImage
import com.arm.learningpath.imagetoimage.inference.RunResult
import com.arm.learningpath.imagetoimage.inference.RuntimeRunner
import org.pytorch.executorch.EValue
import org.pytorch.executorch.MethodMetadata
import org.pytorch.executorch.Module
import java.io.File

class DepthAnythingExecuTorchAdapter : RuntimeRunner {
    private var module: Module? = null
    private var config: ModelConfig? = null
    private var loadTimeMs: Long = 0

    override fun load(modelDir: File, config: ModelConfig): Long {
        val started = System.nanoTime()
        validateModelConfig(config)
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

    override fun runImage(image: DecodedImage, boxPrompt: FloatArray): RunResult {
        val selected = config ?: error("Call load() before runImage().")
        val activeModule = module ?: error("Call load() before runImage().")
        val inputWidth = requireNotNull(selected.inputImageWidth)
        val inputHeight = requireNotNull(selected.inputImageHeight)
        val outputWidth = requireNotNull(selected.outputImageWidth)
        val outputHeight = requireNotNull(selected.outputImageHeight)
        val started = System.nanoTime()
        val input = DepthAnythingPreprocessor.prepare(image, inputWidth, inputHeight)
        val outputs = activeModule.execute("forward", EValue.from(input.imageTensor))
        val output = DepthAnythingPostprocessor.decode(
            outputs,
            input,
            outputWidth,
            outputHeight,
        )
        return RunResult(output.summary, loadTimeMs, elapsedMs(started), output.resultBitmap)
    }

    override fun close() {
        module?.close()
        module = null
    }

    private fun validateModelConfig(config: ModelConfig) {
        require(config.isDepthEstimation) {
            "Depth Anything requires the depth-estimation workload."
        }
        require(config.runtime == "executorch") {
            "Depth Anything requires the ExecuTorch runtime."
        }
        require(config.inputImageWidth == 686 && config.inputImageHeight == 518) {
            "Depth Anything requires input shape [1, 3, 518, 686]."
        }
        require(config.outputImageWidth == 686 && config.outputImageHeight == 518) {
            "Depth Anything requires output shape [1, 518, 686]."
        }
        require(config.colorFormat == "RGB") { "Depth Anything requires RGB input." }
        require(config.normalization == "imagenet_mean_std") {
            "Depth Anything requires ImageNet normalization."
        }
        require(config.promptType == "none" && config.defaultBoxPrompt == null) {
            "Depth Anything does not accept a prompt."
        }
        require(config.inputTensorShapes == listOf("[1,3,518,686]")) {
            "Depth Anything catalog input shape does not match the adapter."
        }
        require(config.outputTensorShapes == listOf("[1,518,686]")) {
            "Depth Anything catalog output shape does not match the adapter."
        }
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

    private fun validateModelFiles(modelDir: File, config: ModelConfig) {
        require(config.modelFile.isNotBlank()) {
            "Model file is required for '${config.id}'."
        }
        requireExistingFile(modelDir, config.modelFile, "ExecuTorch model file")
        config.externalDataFiles.forEach {
            requireExistingFile(modelDir, it, "External data file")
        }
    }

    private fun requireExistingFile(modelDir: File, relativePath: String, label: String) {
        require(relativePath.isNotBlank()) { "$label path is blank." }
        require(!File(relativePath).isAbsolute) {
            "$label must be relative to the app-local model directory: $relativePath"
        }
        val canonicalModelDir = modelDir.canonicalFile
        val file = File(modelDir, relativePath).canonicalFile
        require(
            file.path == canonicalModelDir.path ||
                file.path.startsWith(canonicalModelDir.path + File.separator),
        ) {
            "$label must stay below ${canonicalModelDir.absolutePath}: $relativePath"
        }
        require(file.isFile) { "$label is missing: ${file.absolutePath}" }
    }

    private fun elapsedMs(started: Long): Long {
        return (System.nanoTime() - started) / 1_000_000
    }
}
