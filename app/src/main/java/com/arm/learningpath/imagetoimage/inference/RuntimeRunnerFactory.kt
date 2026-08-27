package com.arm.learningpath.imagetoimage.inference

import android.content.Context
import com.arm.learningpath.imagetoimage.catalog.ModelConfig
import com.arm.learningpath.imagetoimage.inference.models.mobilesam.MobileSamExecuTorchAdapter
import com.arm.learningpath.imagetoimage.inference.segmentation.LiteRtImageToImageAdapter
import com.arm.learningpath.imagetoimage.inference.segmentation.OnnxImageToImageAdapter

object RuntimeRunnerFactory {
    fun create(context: Context, config: ModelConfig): RuntimeRunner {
        if (config.adapterId == "mobile-sam-executorch") {
            return MobileSamExecuTorchAdapter()
        }

        return when (config.runtime) {
            "litert", "tflite" -> LiteRtImageToImageAdapter()
            "onnx", "onnxruntime" -> OnnxImageToImageAdapter()
            else -> error("No adapter is registered for '${config.id}'. Add a model-specific adapter before adding this model to the catalog.")
        }
    }
}
