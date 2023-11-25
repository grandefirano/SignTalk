package com.grandefirano.signtalk.landmarks.pose

data class PoseLandmarkerResultWrapper(
    val poseResultBundle: PoseLandmarkerHelper.PoseResultBundle?,
    val frameNumber: Int,
)

fun initPoseLandmarkerResultWrapper(): PoseLandmarkerResultWrapper =
    PoseLandmarkerResultWrapper(null, 0)

fun PoseLandmarkerResultWrapper.withNewLandmark(
    poseResultBundle: PoseLandmarkerHelper.PoseResultBundle
): PoseLandmarkerResultWrapper {
    val nextNumber = this.frameNumber.plus(1)
    return this.copy(
        poseResultBundle = poseResultBundle,
        frameNumber = nextNumber,
    )
}

