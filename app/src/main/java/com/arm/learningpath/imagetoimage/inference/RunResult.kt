package com.arm.learningpath.imagetoimage.inference

import android.graphics.Bitmap

data class RunResult(
    val summary: String,
    val loadTimeMs: Long,
    val runTimeMs: Long,
    val resultBitmap: Bitmap? = null,
)
