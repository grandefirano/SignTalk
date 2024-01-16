package com.grandefirano.signtalk.landmarks.pose

data class PoseLandmarksResult(
    val poseResultBundle: PoseLandmarksManager.PoseResultBundle?,
    val frameNumber: Int,
)

fun initPoseLandmarkerResultWrapper(): PoseLandmarksResult =
    PoseLandmarksResult(null, 0)

fun PoseLandmarksResult.withNewLandmark(
    poseResultBundle: PoseLandmarksManager.PoseResultBundle
): PoseLandmarksResult {
    val nextNumber = this.frameNumber.plus(1)
    return this.copy(
        poseResultBundle = poseResultBundle,
        frameNumber = nextNumber,
    )
}

