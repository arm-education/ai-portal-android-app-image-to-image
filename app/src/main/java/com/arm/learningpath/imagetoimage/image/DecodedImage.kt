package com.arm.learningpath.imagetoimage.image

import android.graphics.Bitmap

data class DecodedImage(
    val bitmap: Bitmap,
    val sourceWidth: Int,
    val sourceHeight: Int,
)
