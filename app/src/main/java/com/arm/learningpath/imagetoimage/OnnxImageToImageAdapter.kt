package com.arm.learningpath.imagetoimage

import android.net.Uri
import java.io.File

class OnnxImageToImageAdapter : RuntimeRunner {
    override fun load(modelDir: File, config: ModelConfig): Long {
        error("Complete OnnxImageToImageAdapter before loading '${config.id}'.")
    }

    override fun runImage(imageUri: Uri, boxPrompt: FloatArray): RunResult {
        error("Complete OnnxImageToImageAdapter before running inference.")
    }

    override fun close() {
    }
}
