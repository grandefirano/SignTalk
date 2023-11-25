package com.grandefirano.signtalk.landmarks

import androidx.camera.core.ImageProxy
import com.grandefirano.signtalk.landmarks.face.FaceLandmarkerHelper
import com.grandefirano.signtalk.landmarks.hand.HandLandmarkerHelper
import com.grandefirano.signtalk.landmarks.face.FaceLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.hand.HandLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarkerHelper
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarkerResultWrapper
import com.grandefirano.signtalk.camera.detectLiveStream
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class LandmarksManager @Inject constructor(
    private val faceLandmarkerHelper: FaceLandmarkerHelper,
    private val poseLandmarkerHelper: PoseLandmarkerHelper,
    private val handLandmarkerHelper: HandLandmarkerHelper,
) {

    companion object {
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    val faceLandmarks: StateFlow<FaceLandmarkerResultWrapper> =
        faceLandmarkerHelper.faceLandmarks
    val poseLandmarks: StateFlow<PoseLandmarkerResultWrapper> =
        poseLandmarkerHelper.poseLandmarks
    val handLandmarks: StateFlow<HandLandmarkerResultWrapper> =
        handLandmarkerHelper.handLandmarks

    init {
        faceLandmarkerHelper.setupFaceLandmarker()
        handLandmarkerHelper.setupHandLandmarker()
        poseLandmarkerHelper.setupPoseLandmarker()
    }

    fun setupLandmarkerIfClosed() {
        if (faceLandmarkerHelper.isClose()) {
            faceLandmarkerHelper.setupFaceLandmarker()
        }
        if (handLandmarkerHelper.isClose()) {
            handLandmarkerHelper.setupHandLandmarker()
        }
        if (poseLandmarkerHelper.isClose()) {
            poseLandmarkerHelper.setupPoseLandmarker()
        }
    }

    fun cleanLandmarker() {
        faceLandmarkerHelper.clearFaceLandmarker()
        handLandmarkerHelper.clearHandLandmarker()
        poseLandmarkerHelper.clearPoseLandmarker()
    }

    fun detectCombined(imageProxy: ImageProxy) {
        detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = true
        ) { mpImage, frameTime ->
            //TODO: maybe detect instead of detectAsync but on coroutines back thread
            faceLandmarkerHelper.detectAsync(mpImage, frameTime)
            handLandmarkerHelper.detectAsync(mpImage, frameTime)
            poseLandmarkerHelper.detectAsync(mpImage, frameTime)
        }
    }
}