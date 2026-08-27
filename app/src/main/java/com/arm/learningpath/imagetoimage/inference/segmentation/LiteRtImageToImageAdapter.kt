package com.arm.learningpath.imagetoimage.inference.segmentation

import com.arm.learningpath.imagetoimage.catalog.ModelConfig
import com.arm.learningpath.imagetoimage.image.DecodedImage
import com.arm.learningpath.imagetoimage.inference.RunResult
import com.arm.learningpath.imagetoimage.inference.RuntimeRunner
import java.io.File

class LiteRtImageToImageAdapter : RuntimeRunner {
    override fun load(modelDir: File, config: ModelConfig): Long {
        error("Complete LiteRtImageToImageAdapter before loading '${config.id}'.")
    }

    override fun runImage(image: DecodedImage, boxPrompt: FloatArray): RunResult {
        error("Complete LiteRtImageToImageAdapter before running inference.")
    }

    override fun close() {
    }
}
