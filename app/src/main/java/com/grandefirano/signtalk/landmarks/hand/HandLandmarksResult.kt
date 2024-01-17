package com.grandefirano.signtalk.landmarks.hand

data class HandLandmarksResult(
    val handResultBundle: HandLandmarksManager.HandResultBundle?,
    val frameNumber: Int,
)

fun initHandLandmarkerResultWrapper(): HandLandmarksResult =
    HandLandmarksResult(null, 0)

fun HandLandmarksResult.withNewLandmark(
    handResultBundle: HandLandmarksManager.HandResultBundle,
): HandLandmarksResult {
    val nextNumber = this.frameNumber.plus(1)
    return this.copy(
        handResultBundle = handResultBundle,
        frameNumber = nextNumber,
    )
}

