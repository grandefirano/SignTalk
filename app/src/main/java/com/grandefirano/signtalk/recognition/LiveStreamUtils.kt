package com.grandefirano.signtalk.recognition

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
    //TODO: can combine frames base on the frame time
    val frameTime = SystemClock.uptimeMillis()
    // Copy out RGB bits from the frame to a bitmap buffer
    val bitmapBuffer =
        Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
    imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
    imageProxy.close()
    val matrix = Matrix().apply {
        // Rotate the frame received from the camera to be in the same direction as it'll be shown
        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

        // flip image if user use front camera
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
    // Convert the input Bitmap object to an MPImage object to run inference
    val mpImage = BitmapImageBuilder(rotatedBitmap).build()
    callback(mpImage, frameTime)
}