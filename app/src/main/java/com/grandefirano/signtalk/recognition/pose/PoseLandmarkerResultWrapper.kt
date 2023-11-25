package com.google.mediapipe.examples.facelandmarker.pose

import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

data class PoseLandmarkerResultWrapper(
    val poseLandmarkerResult: PoseLandmarkerResult?,
    val frameNumber: Int,
    val finishTimeMilis: Long
)

fun PoseLandmarkerResultWrapper.withNewLandmark(
    poseLandmarkerResult: PoseLandmarkerResult,
    finishTimeMilis: Long
): PoseLandmarkerResultWrapper {
    val nextNumber = this.frameNumber.plus(1)
    return this.copy(
        poseLandmarkerResult = poseLandmarkerResult,
        frameNumber = nextNumber,
        finishTimeMilis = finishTimeMilis
    )
}

