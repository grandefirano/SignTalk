package com.grandefirano.signtalk.landmarks.face

data class FaceLandmarkerResultWrapper(
    val faceResultBundle: FaceLandmarksManager.FaceResultBundle?,
    val frameNumber: Int,
)

fun initFaceLandmarkerResultWrapper(): FaceLandmarkerResultWrapper = FaceLandmarkerResultWrapper(null, 0)

fun FaceLandmarkerResultWrapper.withNewLandmark(
    faceResultBundle: FaceLandmarksManager.FaceResultBundle?
): FaceLandmarkerResultWrapper {
    val nextNumber = this.frameNumber.plus(1)
    return this.copy(
        faceResultBundle = faceResultBundle,
        frameNumber = nextNumber,
    )
}

