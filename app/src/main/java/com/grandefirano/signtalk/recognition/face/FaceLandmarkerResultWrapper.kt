package com.google.mediapipe.examples.facelandmarker.face

import com.google.mediapipe.examples.facelandmarker.fragment.CameraFragment
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

data class FaceLandmarkerResultWrapper(
    val faceLandmarkerResult: FaceLandmarkerResult?,
    val frameNumber: Int,
    val finishTimeMilis: Long
)

fun FaceLandmarkerResultWrapper.withNewLandmark(
    faceLandmarkerResult: FaceLandmarkerResult,
    finishTimeMilis: Long
): FaceLandmarkerResultWrapper {
    val nextNumber = this.frameNumber.plus(1)
    return this.copy(
        faceLandmarkerResult = faceLandmarkerResult,
        frameNumber = nextNumber,
        finishTimeMilis = finishTimeMilis
    )
}

