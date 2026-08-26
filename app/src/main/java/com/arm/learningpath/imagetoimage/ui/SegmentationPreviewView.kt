package com.arm.learningpath.imagetoimage.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class SegmentationPreviewView(context: Context) : View(context) {
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 140, 145)
        style = Paint.Style.STROKE
        strokeWidth = dp(2).toFloat()
        pathEffect = DashPathEffect(floatArrayOf(dp(8).toFloat(), dp(6).toFloat()), 0f)
    }
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(85, 106, 114)
        textAlign = Paint.Align.CENTER
        textSize = dp(15).toFloat()
    }

    private var bitmap: Bitmap? = null
    private var maskOverlay: Bitmap? = null
    private var promptBox: FloatArray = floatArrayOf(102.4f, 102.4f, 921.6f, 921.6f)
    private var promptInputSize: Int = 1024

    fun setBitmap(image: Bitmap) {
        bitmap = image
        maskOverlay = null
        invalidate()
    }

    fun setResultBitmap(result: Bitmap) {
        maskOverlay = result
        invalidate()
    }

    fun setPromptBox(box: FloatArray, inputSize: Int) {
        if (box.size == 4 && inputSize > 0) {
            promptBox = box.copyOf()
            promptInputSize = inputSize
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val activeBitmap = bitmap
        if (activeBitmap == null) {
            canvas.drawText("No image selected", width / 2.0f, height / 2.0f, placeholderPaint)
            return
        }

        val destination = imageDestination(activeBitmap)
        canvas.drawBitmap(activeBitmap, null, destination, imagePaint)
        maskOverlay?.let { canvas.drawBitmap(it, null, destination, imagePaint) }
        if (maskOverlay == null) {
            drawPromptBox(canvas, destination)
        }
    }

    private fun imageDestination(activeBitmap: Bitmap): RectF {
        val viewRatio = width / height.toFloat()
        val imageRatio = activeBitmap.width / activeBitmap.height.toFloat()
        return if (imageRatio > viewRatio) {
            val displayHeight = width / imageRatio
            val top = (height - displayHeight) / 2f
            RectF(0f, top, width.toFloat(), top + displayHeight)
        } else {
            val displayWidth = height * imageRatio
            val left = (width - displayWidth) / 2f
            RectF(left, 0f, left + displayWidth, height.toFloat())
        }
    }

    private fun drawPromptBox(canvas: Canvas, imageRect: RectF) {
        val scale = promptInputSize.toFloat()
        val left = imageRect.left + imageRect.width() * (promptBox[0] / scale)
        val top = imageRect.top + imageRect.height() * (promptBox[1] / scale)
        val right = imageRect.left + imageRect.width() * (promptBox[2] / scale)
        val bottom = imageRect.top + imageRect.height() * (promptBox[3] / scale)
        val rect = RectF(left, top, right, bottom)
        canvas.drawRect(rect, promptPaint)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
