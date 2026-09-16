package com.arm.learningpath.imagetoimage.inference.models.depthanything

import org.pytorch.executorch.Tensor

data class DepthAnythingInput(
    val imageTensor: Tensor,
    val sourceWidth: Int,
    val sourceHeight: Int,
)
