package com.grandefirano.signtalk.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage

fun detectLiveStream(
    imageProxy: ImageProxy,
    isFrontCamera: Boolean,
    callback: (MPImage, frameTime: Long) -> Unit
) {
    val frameTime = SystemClock.uptimeMillis()
    val bitmapBuffer =
        Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
    imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
    imageProxy.close()
    val matrix = Matrix().apply {
        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        if (isFrontCamera) {
            postScale(
                -1f,
                1f,
                imageProxy.width.toFloat(),
                imageProxy.height.toFloat()
            )
        }
    }
    val rotatedBitmap = Bitmap.createBitmap(
        bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
        matrix, true
    )
    val mpImage = BitmapImageBuilder(rotatedBitmap).build()
    callback(mpImage, frameTime)
}