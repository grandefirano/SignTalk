package com.grandefirano.signtalk.fragment

import com.grandefirano.signtalk.recognition.face.FaceLandmarkerHelper
import com.grandefirano.signtalk.recognition.hand.HandLandmarkerHelper
import com.grandefirano.signtalk.recognition.face.FaceLandmarkerResultWrapper
import com.grandefirano.signtalk.recognition.hand.HandLandmarkerResultWrapper
import com.grandefirano.signtalk.recognition.pose.PoseLandmarkerHelper
import com.grandefirano.signtalk.recognition.pose.PoseLandmarkerResultWrapper
import kotlinx.coroutines.flow.StateFlow

class LandmarksManager(
    faceLandmarkerHelper: FaceLandmarkerHelper,
    poseLandmarkerHelper: PoseLandmarkerHelper,
    handLandmarkerHelper: HandLandmarkerHelper
) {
    val faceLandmarks: StateFlow<FaceLandmarkerResultWrapper> =
        faceLandmarkerHelper.faceLandmarks
    val poseLandmarks: StateFlow<PoseLandmarkerResultWrapper> =
        poseLandmarkerHelper.poseLandmarks
    val handLandmarks: StateFlow<HandLandmarkerResultWrapper> =
        handLandmarkerHelper.handLandmarks
}