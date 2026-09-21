package com.arm.learningpath.imagetoimage.inference.models.depthanything

import org.pytorch.executorch.Tensor

data class DepthAnythingInput(
    val imageTensor: Tensor,
    val dimensions: DepthAnythingImageDimensions,
)

data class DepthAnythingImageDimensions(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val decodedWidth: Int,
    val decodedHeight: Int,
) {
    init {
        require(sourceWidth > 0 && sourceHeight > 0) { "Source image dimensions must be positive." }
        require(decodedWidth > 0 && decodedHeight > 0) { "Decoded image dimensions must be positive." }
    }

    val renderWidth: Int
        get() = decodedWidth

    val renderHeight: Int
        get() = decodedHeight
}
