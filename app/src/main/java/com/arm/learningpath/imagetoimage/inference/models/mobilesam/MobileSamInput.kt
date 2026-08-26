package com.arm.learningpath.imagetoimage.inference.models.mobilesam

import android.graphics.Bitmap
import org.pytorch.executorch.Tensor

data class MobileSamInput(
    val preparedBitmap: Bitmap,
    val sourceBitmap: Bitmap,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val boxPrompt: FloatArray,
    val imageTensor: Tensor,
    val pointCoords: Tensor,
) {
    fun recyclePreparedBitmap() {
        if (preparedBitmap !== sourceBitmap) {
            preparedBitmap.recycle()
        }
    }
}
