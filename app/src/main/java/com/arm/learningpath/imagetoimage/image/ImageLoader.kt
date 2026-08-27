package com.arm.learningpath.imagetoimage.image

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import kotlin.math.ceil

object ImageLoader {
    fun decode(context: Context, uri: Uri, targetSize: Int): DecodedImage {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        var sourceWidth = 0
        var sourceHeight = 0
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            sourceWidth = info.size.width
            sourceHeight = info.size.height
            val largestDimension = maxOf(info.size.width, info.size.height)
            val requestedSize = targetSize.takeIf { it > 0 } ?: largestDimension
            val sampleSize = ceil(largestDimension / requestedSize.toDouble()).toInt().coerceAtLeast(1)

            decoder.setTargetSampleSize(sampleSize)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return DecodedImage(bitmap, sourceWidth, sourceHeight)
    }
}
