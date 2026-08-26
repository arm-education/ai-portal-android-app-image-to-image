package com.arm.learningpath.imagetoimage.inference.models.mobilesam

import android.graphics.Bitmap

data class MobileSamOutput(
    val summary: String,
    val resultBitmap: Bitmap,
)
