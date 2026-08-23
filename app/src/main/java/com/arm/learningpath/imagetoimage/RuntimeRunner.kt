package com.arm.learningpath.imagetoimage

import android.net.Uri
import java.io.File

interface RuntimeRunner : AutoCloseable {
    fun load(modelDir: File, config: ModelConfig): Long
    fun runImage(imageUri: Uri, boxPrompt: FloatArray): RunResult
    override fun close()
}
