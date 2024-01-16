package com.grandefirano.signtalk.landmarks

import androidx.camera.core.ImageProxy
import com.grandefirano.signtalk.camera.detectLiveStream
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksManager
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksResult
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksManager
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksResult
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LandmarksManager @Inject constructor(
    private val poseLandmarksManager: PoseLandmarksManager,
    private val handLandmarksManager: HandLandmarksManager,
) {

    companion object {
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    val poseLandmarks: StateFlow<PoseLandmarksResult> =
        poseLandmarksManager.poseLandmarks
    val handLandmarks: StateFlow<HandLandmarksResult> =
        handLandmarksManager.handLandmarks

    init {
        handLandmarksManager.setupHandLandmarker()
        poseLandmarksManager.setupPoseLandmarker()
    }

    fun setupLandmarkerIfClosed() {
        if (handLandmarksManager.isClose()) {
            handLandmarksManager.setupHandLandmarker()
        }
        if (poseLandmarksManager.isClose()) {
            poseLandmarksManager.setupPoseLandmarker()
        }
    }

    fun cleanLandmarker() {
        handLandmarksManager.clearHandLandmarker()
        poseLandmarksManager.clearPoseLandmarker()
    }

    fun detectCombinedLandmarks(imageProxy: ImageProxy) {
        detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = true
        ) { mpImage, frameTime ->
            handLandmarksManager.detectAsync(mpImage, frameTime)
            poseLandmarksManager.detectAsync(mpImage, frameTime)
        }
    }
}