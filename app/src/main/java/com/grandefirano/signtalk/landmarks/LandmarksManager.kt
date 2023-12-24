package com.grandefirano.signtalk.landmarks

import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.grandefirano.signtalk.camera.detectLiveStream
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksRepository
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksResult
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksRepository
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksResult
import com.grandefirano.signtalk.prediction.CombinedLandmarks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LandmarksManager @Inject constructor(
    private val poseLandmarksRepository: PoseLandmarksRepository,
    private val handLandmarksRepository: HandLandmarksRepository,
) {

    companion object {
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    val poseLandmarks: StateFlow<PoseLandmarksResult> =
        poseLandmarksRepository.poseLandmarks
    val handLandmarks: StateFlow<HandLandmarksResult> =
        handLandmarksRepository.handLandmarks

    private var counterXX = 0
    private var counterXX2222 = 0
    private var counterXXSaved = 0L


    init {
        handLandmarksRepository.setupHandLandmarker()
        poseLandmarksRepository.setupPoseLandmarker()
    }

    fun setupLandmarkerIfClosed() {
        if (handLandmarksRepository.isClose()) {
            handLandmarksRepository.setupHandLandmarker()
        }
        if (poseLandmarksRepository.isClose()) {
            poseLandmarksRepository.setupPoseLandmarker()
        }
    }

    fun cleanLandmarker() {
        handLandmarksRepository.clearHandLandmarker()
        poseLandmarksRepository.clearPoseLandmarker()
    }

    fun detectCombined(imageProxy: ImageProxy) {
        detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = true
        ) { mpImage, frameTime ->
            //TODO: maybe detect instead of detectAsync but on coroutines back thread
            handLandmarksRepository.detectAsync(mpImage, frameTime)
            poseLandmarksRepository.detectAsync(mpImage, frameTime)
        }
    }
}