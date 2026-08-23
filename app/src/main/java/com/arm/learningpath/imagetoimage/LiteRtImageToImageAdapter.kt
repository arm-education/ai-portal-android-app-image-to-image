package com.arm.learningpath.imagetoimage

import android.net.Uri
import java.io.File

class LiteRtImageToImageAdapter : RuntimeRunner {
    override fun load(modelDir: File, config: ModelConfig): Long {
        error("Complete LiteRtImageToImageAdapter before loading '${config.id}'.")
    }

    override fun runImage(imageUri: Uri, boxPrompt: FloatArray): RunResult {
        error("Complete LiteRtImageToImageAdapter before running inference.")
    }

    override fun close() {
    }
}
