package com.arm.learningpath.imagetoimage.inference

import com.arm.learningpath.imagetoimage.catalog.ModelConfig
import com.arm.learningpath.imagetoimage.image.DecodedImage
import java.io.File

interface RuntimeRunner : AutoCloseable {
    fun load(modelDir: File, config: ModelConfig): Long
    fun runImage(image: DecodedImage, boxPrompt: FloatArray): RunResult
    override fun close()
}
