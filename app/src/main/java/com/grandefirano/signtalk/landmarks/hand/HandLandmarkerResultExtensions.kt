package com.grandefirano.signtalk.landmarks.hand

import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.grandefirano.signtalk.landmarks.flattenXYZ
import com.grandefirano.signtalk.landmarks.toXYZ

fun HandLandmarkerResult?.extractHands(): Pair<List<Float>, List<Float>> {
    val handedness = this?.handedness()?.flatten()
    val rightHandIndex = handedness?.indexOfFirst {
        it.categoryName() == "Left"
    }
    val leftHandIndex = handedness?.indexOfFirst {
        it.categoryName() == "Right"
    }
    val rightHand = rightHandIndex?.let {
        this?.landmarks()?.getOrNull(rightHandIndex)?.toXYZ()
            ?.flattenXYZ()
    } ?: List(21 * 3) { 0f }
    val leftHand = leftHandIndex?.let {
        this?.landmarks()?.getOrNull(leftHandIndex)?.toXYZ()
            ?.flattenXYZ()
    } ?: List(21 * 3) { 0f }
    return Pair(leftHand,rightHand)
}