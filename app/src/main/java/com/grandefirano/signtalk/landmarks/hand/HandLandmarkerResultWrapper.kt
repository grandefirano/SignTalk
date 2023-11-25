package com.grandefirano.signtalk.landmarks.hand

data class HandLandmarkerResultWrapper(
    val handResultBundle: HandLandmarkerHelper.HandResultBundle?,
    val frameNumber: Int,
)

fun initHandLandmarkerResultWrapper(): HandLandmarkerResultWrapper =
    HandLandmarkerResultWrapper(null, 0)

fun HandLandmarkerResultWrapper.withNewLandmark(
    handResultBundle: HandLandmarkerHelper.HandResultBundle,
): HandLandmarkerResultWrapper {
    val nextNumber = this.frameNumber.plus(1)
    return this.copy(
        handResultBundle = handResultBundle,
        frameNumber = nextNumber,
    )
}

