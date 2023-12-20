package com.grandefirano.signtalk.prediction

import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.XYZKeypoints
import com.grandefirano.signtalk.landmarks.flattenXYZ
import com.grandefirano.signtalk.landmarks.hand.HandLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.hand.extractHandsXYZKeypoints
import com.grandefirano.signtalk.landmarks.normalize
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticRecognizer @Inject constructor(
    private val landmarksManager: LandmarksManager,
    private val staticPredictionManager: StaticPredictionManager,
) {
    val recognizedStaticSigns: StateFlow<List<String>> =
        staticPredictionManager.recognizedSigns
    private val handLandmarks: StateFlow<HandLandmarkerResultWrapper> =
        landmarksManager.handLandmarks

    suspend fun startStaticRecognition() {
        handLandmarks.collect {
            onLandmarkUpdated(it)
        }
    }

    fun onLandmarkUpdated(handLandmarks: HandLandmarkerResultWrapper) {
        val normalizedKeypoints = extractLeftHandKeypoints(handLandmarks).normalize()
        val flattenList = normalizedKeypoints.flattenXYZ()
        staticPredictionManager.predict(flattenList)
    }

    private fun extractLeftHandKeypoints(landmarks: HandLandmarkerResultWrapper): List<XYZKeypoints> {
        val handResult = landmarks.handResultBundle?.results.extractHandsXYZKeypoints()
        return handResult.first
    }
}
