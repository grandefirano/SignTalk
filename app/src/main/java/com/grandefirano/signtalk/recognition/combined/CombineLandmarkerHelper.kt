package com.google.mediapipe.examples.facelandmarker.combined

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.examples.facelandmarker.face.FaceLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.hand.HandLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.pose.PoseLandmarkerHelper
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode

class CombineLandmarkerHelper(
    val faceLandmarkerHelper: FaceLandmarkerHelper,
    val handLandmarkerHelper: HandLandmarkerHelper,
    val poseLandmarkerHelper: PoseLandmarkerHelper,
    var runningMode: RunningMode = RunningMode.LIVE_STREAM
) {

    companion object {
        const val DELEGATE_CPU = 0
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    init {
        faceLandmarkerHelper.setupFaceLandmarker(runningMode)
        handLandmarkerHelper.setupHandLandmarker(runningMode)
        poseLandmarkerHelper.setupPoseLandmarker()
    }

    fun clearLandmarker() {
        faceLandmarkerHelper.clearFaceLandmarker()
        handLandmarkerHelper.clearHandLandmarker()
        poseLandmarkerHelper.clearPoseLandmarker()
    }

    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {

        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream" +
                        " while not using RunningMode.LIVE_STREAM"
            )
        }
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
        //TODO: maybe detect instead of detectAsync but on coroutines back thread
        faceLandmarkerHelper.detectAsync(mpImage, frameTime)
        handLandmarkerHelper.detectAsync(mpImage, frameTime)
        poseLandmarkerHelper.detectAsync(mpImage, frameTime)
    }

    fun isFaceClose(): Boolean {
        return faceLandmarkerHelper.isClose()
    }

    fun setupFaceLandmarker() {
        faceLandmarkerHelper.setupFaceLandmarker(runningMode)
    }

    fun isHandClose(): Boolean {
        return handLandmarkerHelper.isClose()
    }

    fun setupHandLandmarker() {
        handLandmarkerHelper.setupHandLandmarker(runningMode)
    }

    fun isPoseClose(): Boolean {
        return poseLandmarkerHelper.isClose()
    }

    fun setupPoseLandmarker() {
        poseLandmarkerHelper.setupPoseLandmarker()
    }

}