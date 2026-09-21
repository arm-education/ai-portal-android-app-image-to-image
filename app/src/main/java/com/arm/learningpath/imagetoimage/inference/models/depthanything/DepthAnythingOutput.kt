package com.arm.learningpath.imagetoimage.inference.models.depthanything

import android.graphics.Bitmap

data class DepthAnythingOutput(
    val summary: String,
    val resultBitmap: Bitmap,
)
