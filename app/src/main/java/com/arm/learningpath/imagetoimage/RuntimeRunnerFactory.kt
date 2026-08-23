package com.arm.learningpath.imagetoimage

import android.content.Context

object RuntimeRunnerFactory {
    fun create(context: Context, config: ModelConfig): RuntimeRunner {
        if (config.adapterId == "mobile-sam-executorch") {
            return ExecuTorchSegmentationAdapter(context)
        }

        return when (config.runtime) {
            "litert", "tflite" -> LiteRtImageToImageAdapter()
            "onnx", "onnxruntime" -> OnnxImageToImageAdapter()
            else -> error("No adapter is registered for '${config.id}'. Add a model-specific adapter before adding this model to the catalog.")
        }
    }
}
