package com.grandefirano.signtalk.landmarks.hand

import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.grandefirano.signtalk.landmarks.XYZKeypoints
import com.grandefirano.signtalk.landmarks.toXYZ

fun HandLandmarkerResult?.extractHandsXYZKeypoints(): Pair<List<XYZKeypoints>, List<XYZKeypoints>> {
    val handedness = this?.handedness()?.flatten()
    val rightHandIndex = handedness?.indexOfFirst {
        it.categoryName() == "Left"
    }
    val leftHandIndex = handedness?.indexOfFirst {
        it.categoryName() == "Right"
    }
    val rightHand = rightHandIndex?.let {
        this?.landmarks()?.getOrNull(rightHandIndex)?.toXYZ()
    } ?: List(21) { XYZKeypoints(0f,0f,0f) }
    val leftHand = leftHandIndex?.let {
        this?.landmarks()?.getOrNull(leftHandIndex)?.toXYZ()
    } ?: List(21) { XYZKeypoints(0f,0f,0f) }
    return Pair(leftHand,rightHand)
}