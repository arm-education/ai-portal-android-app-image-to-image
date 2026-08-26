package com.arm.learningpath.imagetoimage.inference.models.mobilesam

import com.arm.learningpath.imagetoimage.catalog.ModelConfig
import com.arm.learningpath.imagetoimage.image.DecodedImage
import com.arm.learningpath.imagetoimage.inference.RunResult
import com.arm.learningpath.imagetoimage.inference.RuntimeRunner
import org.pytorch.executorch.EValue
import org.pytorch.executorch.MethodMetadata
import org.pytorch.executorch.Module
import java.io.File

class MobileSamExecuTorchAdapter : RuntimeRunner {
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

    override fun runImage(image: DecodedImage, boxPrompt: FloatArray): RunResult {
        val selected = config ?: error("Call load() before runImage().")
        val activeModule = module ?: error("Call load() before runImage().")
        val started = System.nanoTime()
        val inputSize = selected.inputImageSize.takeIf { it > 0 } ?: 1024
        val input = MobileSamPreprocessor.prepare(image, inputSize, boxPrompt)

        val outputs = activeModule.execute(
            "forward",
            EValue.from(input.imageTensor),
            EValue.from(input.pointCoords),
        )
        val output = MobileSamPostprocessor.decode(outputs, input)
        input.recyclePreparedBitmap()
        return RunResult(output.summary, loadTimeMs, elapsedMs(started), output.resultBitmap)
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

}
