package com.arm.learningpath.imagetoimage.ui

import com.arm.learningpath.imagetoimage.catalog.ModelConfig

object ModelUiPolicy {
    fun showsBoxPrompt(config: ModelConfig): Boolean = config.promptType == "box-corners"

    fun runActionLabel(config: ModelConfig): String {
        return if (config.isDepthEstimation) "Run depth estimation" else "Run segmentation"
    }

    fun resultDescription(config: ModelConfig): String {
        return if (config.isDepthEstimation) {
            "Depth map shown in grayscale: white is nearer"
        } else {
            "Segmentation mask shown in cyan"
        }
    }
}
