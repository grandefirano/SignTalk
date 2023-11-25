package com.google.mediapipe.examples.facelandmarker.hand

import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

data class HandLandmarkerResultWrapper(
    val handLandmarkerResult: HandLandmarkerResult?,
    val frameNumber: Int,
    val finishTimeMilis: Long
)

fun HandLandmarkerResultWrapper.withNewLandmark(
    handLandmarkerResult: HandLandmarkerResult,
    finishTimeMilis: Long
): HandLandmarkerResultWrapper {
    val nextNumber = this.frameNumber.plus(1)
    return this.copy(
        handLandmarkerResult = handLandmarkerResult,
        frameNumber = nextNumber,
        finishTimeMilis = finishTimeMilis
    )
}

