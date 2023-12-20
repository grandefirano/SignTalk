package com.grandefirano.signtalk.landmarks

import androidx.camera.core.ImageProxy
import com.grandefirano.signtalk.camera.detectLiveStream
import com.grandefirano.signtalk.landmarks.hand.HandLandmarkerHelper
import com.grandefirano.signtalk.landmarks.hand.HandLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarkerHelper
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarkerResultWrapper
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LandmarksManager @Inject constructor(
    private val poseLandmarkerHelper: PoseLandmarkerHelper,
    private val handLandmarkerHelper: HandLandmarkerHelper,
) {

    companion object {
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    val poseLandmarks: StateFlow<PoseLandmarkerResultWrapper> =
        poseLandmarkerHelper.poseLandmarks
    val handLandmarks: StateFlow<HandLandmarkerResultWrapper> =
        handLandmarkerHelper.handLandmarks

    init {
        handLandmarkerHelper.setupHandLandmarker()
        poseLandmarkerHelper.setupPoseLandmarker()
    }

    fun setupLandmarkerIfClosed() {
        if (handLandmarkerHelper.isClose()) {
            handLandmarkerHelper.setupHandLandmarker()
        }
        if (poseLandmarkerHelper.isClose()) {
            poseLandmarkerHelper.setupPoseLandmarker()
        }
    }

    fun cleanLandmarker() {
        handLandmarkerHelper.clearHandLandmarker()
        poseLandmarkerHelper.clearPoseLandmarker()
    }

    fun detectCombined(imageProxy: ImageProxy) {
        detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = true
        ) { mpImage, frameTime ->
            //TODO: maybe detect instead of detectAsync but on coroutines back thread
            handLandmarkerHelper.detectAsync(mpImage, frameTime)
            poseLandmarkerHelper.detectAsync(mpImage, frameTime)
        }
    }
}