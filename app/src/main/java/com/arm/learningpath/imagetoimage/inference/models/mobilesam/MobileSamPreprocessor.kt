package com.arm.learningpath.imagetoimage.inference.models.mobilesam

import android.graphics.Bitmap
import android.graphics.Color
import com.arm.learningpath.imagetoimage.image.DecodedImage
import org.pytorch.executorch.Tensor

object MobileSamPreprocessor {
    fun prepare(image: DecodedImage, inputSize: Int, boxPrompt: FloatArray): MobileSamInput {
        val preparedBitmap = prepareBitmap(image.bitmap, inputSize)
        val imageTensor = Tensor.fromBlob(
            bitmapToNchw(preparedBitmap, inputSize),
            longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()),
        )
        val prompt = normalizedBoxPrompt(boxPrompt, inputSize)
        val pointCoords = Tensor.fromBlob(prompt, longArrayOf(1, 1, 2, 2))

        return MobileSamInput(
            preparedBitmap = preparedBitmap,
            sourceBitmap = image.bitmap,
            sourceWidth = image.sourceWidth,
            sourceHeight = image.sourceHeight,
            boxPrompt = prompt,
            imageTensor = imageTensor,
            pointCoords = pointCoords,
        )
    }

    private fun prepareBitmap(source: Bitmap, imageSize: Int): Bitmap {
        return if (source.width == imageSize && source.height == imageSize) {
            source
        } else {
            Bitmap.createScaledBitmap(source, imageSize, imageSize, true)
        }
    }

    private fun bitmapToNchw(prepared: Bitmap, imageSize: Int): FloatArray {
        val pixels = IntArray(imageSize * imageSize)
        prepared.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
        val planeSize = imageSize * imageSize
        val tensorData = FloatArray(3 * planeSize)
        for (index in pixels.indices) {
            val color = pixels[index]
            tensorData[index] = Color.red(color) / 255.0f
            tensorData[planeSize + index] = Color.green(color) / 255.0f
            tensorData[2 * planeSize + index] = Color.blue(color) / 255.0f
        }
        return tensorData
    }

    private fun normalizedBoxPrompt(boxPrompt: FloatArray, imageSize: Int): FloatArray {
        if (boxPrompt.size == 4) {
            return boxPrompt
        }
        val margin = imageSize * 0.10f
        return floatArrayOf(margin, margin, imageSize - margin, imageSize - margin)
    }
}
