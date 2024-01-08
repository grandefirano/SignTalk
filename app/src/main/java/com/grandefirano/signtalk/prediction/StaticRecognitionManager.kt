package com.grandefirano.signtalk.prediction

import androidx.compose.runtime.mutableStateListOf
import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.XYZKeypoints
import com.grandefirano.signtalk.landmarks.flattenXYZ
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksResult
import com.grandefirano.signtalk.landmarks.hand.extractHandsXYZKeypoints
import com.grandefirano.signtalk.landmarks.normalize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticRecognitionManager @Inject constructor(
    landmarksManager: LandmarksManager,
    private val staticPredictionManager: StaticPredictionManager,
) {
    private val handLandmarks: StateFlow<HandLandmarksResult> =
        landmarksManager.handLandmarks

    private val _recognizedElements: MutableStateFlow<MutableList<String>> =
        MutableStateFlow(mutableStateListOf())
    val recognizedElements: StateFlow<List<String>> = _recognizedElements

    private val _lastRecognizedElement: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val lastRecognizedElement: StateFlow<String?> = _lastRecognizedElement

    private val threshold = 0.8

    private val lastPredictions = mutableListOf<String>()
    suspend fun startStaticRecognition() {
        handLandmarks.collect {
            onLandmarksUpdated(it)
        }
    }

    private fun onLandmarksUpdated(handLandmarks: HandLandmarksResult) {
        val normalizedKeypoints = extractLeftHandKeypoints(handLandmarks).normalize()
        val flattenList = normalizedKeypoints.flattenXYZ()
        val prediction = staticPredictionManager.predict(flattenList)
        prediction?.let {
            updateLastPrediction(it.value)
            checkLastPredictions(it.value, it.possibility)
        }
    }

    private fun checkLastPredictions(currentSentence:String, possibility: Float) {
        var isEqual = true
        lastPredictions.forEach {
            if (currentSentence != it) {
                isEqual = false
                return@forEach
            }
        }
        if (isEqual) {
            if (possibility > threshold) {
                if (_recognizedElements.value.size > 0) {

                    if (currentSentence != _recognizedElements.value.last()) {
                        addRecognizedItem(currentSentence)
                    }
                } else {
                    addRecognizedItem(currentSentence)
                }
                println("GUESS $currentSentence")
            }
        }
    }

    private fun updateLastPrediction(prediction: String) {
        lastPredictions.add(prediction)
        if (lastPredictions.size > 10) lastPredictions.removeAt(0)
    }

    private fun addRecognizedItem(currentSentence: String) {
        _recognizedElements.update { it.apply { add(currentSentence) } }
        _lastRecognizedElement.update { currentSentence }
    }
}

private fun extractLeftHandKeypoints(landmarks: HandLandmarksResult): List<XYZKeypoints> {
    val handResult = landmarks.handResultBundle?.results.extractHandsXYZKeypoints()
    return handResult.first
}
