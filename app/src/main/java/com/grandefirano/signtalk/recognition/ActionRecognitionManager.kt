package com.grandefirano.signtalk.recognition

import androidx.compose.runtime.mutableStateListOf
import com.grandefirano.signtalk.landmarks.XYZKeypoints
import com.grandefirano.signtalk.landmarks.flattenXYZ
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksResult
import com.grandefirano.signtalk.landmarks.hand.extractHandsXYZKeypoints
import com.grandefirano.signtalk.landmarks.normalize
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksResult
import com.grandefirano.signtalk.landmarks.toXYZ
import com.grandefirano.signtalk.prediction.ActionPredictionManager
import com.grandefirano.signtalk.prediction.UpperBodyKeypointsAggregator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionRecognitionManager @Inject constructor(
    private val actionPredictionManager: ActionPredictionManager,
    private val upperBodyKeypointsAggregator: UpperBodyKeypointsAggregator,
) {

    private val _recognizedElements: MutableStateFlow<MutableList<String>> =
        MutableStateFlow(mutableStateListOf())
    private val lastSequence = mutableListOf<List<Float>>()
    private val lastPredictions = mutableListOf<String>()

    private val _lastRecognizedElement: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val lastRecognizedElement: StateFlow<String?> = _lastRecognizedElement

    private val threshold = 0.8

    suspend fun startRecognition() {
        upperBodyKeypointsAggregator().collect {
            onLandmarkUpdated(it)
        }
    }

    private fun onLandmarkUpdated(combinedLandmarks: List<XYZKeypoints>) {

        updateLastSequence(combinedLandmarks)
        if (lastSequence.size == 23) {
            val prediction = actionPredictionManager.predict(lastSequence)
            prediction?.let {
                updateLastPrediction(it.value)
                checkLastPredictions(it.value, it.possibility)
            }
        }
    }

    private fun updateLastPrediction(prediction: String) {
        lastPredictions.add(prediction)
        if (lastPredictions.size > 5) lastPredictions.removeAt(0)
    }

    private fun checkLastPredictions(currentSign:String, possibility: Float) {
        var isEqual = true
        lastPredictions.forEach {
            if (currentSign != it) {
                isEqual = false
                return@forEach
            }
        }
        if (isEqual) {
            if (possibility > threshold) {
                if (_recognizedElements.value.size > 0) {

                    if (currentSign != _recognizedElements.value.last()) {
                        addRecognizedItem(currentSign)
                    }
                } else {
                    addRecognizedItem(currentSign)
                }
            }
        }
    }
    private fun addRecognizedItem(currentSentence: String) {
        _recognizedElements.update { it.apply { add(currentSentence) } }
        _lastRecognizedElement.update { currentSentence }
    }

    private fun updateLastSequence(landmarks: List<XYZKeypoints>) {
        val keypointArray = landmarks.normalize().flattenXYZ()
        lastSequence.add(keypointArray)
        if (lastSequence.size > 23) lastSequence.removeAt(0)

    }
}

data class ConsumedFrames(
    val handConsumed: Int,
    val poseConsumed: Int
)

data class CombinedLandmarks(
    val hand: HandLandmarksResult,
    val pose: PoseLandmarksResult
)

fun extractUpperBodyKeypoints(landmarks: CombinedLandmarks): List<XYZKeypoints> {
    val filterPoseLandmarks: List<Int> = listOf(23, 24, 25, 26, 27, 28, 29, 30, 31, 32)
    val poseBefore =
        landmarks.pose.poseResultBundle?.result?.landmarks()?.flatten()?.toXYZ()
    val pose = poseBefore?.filterIndexed { index, _ ->
        filterPoseLandmarks.contains(index).not()
    }.let { if (it.isNullOrEmpty()) List(23) { XYZKeypoints(0f, 0f, 0f) } else it }
    val handResult = landmarks.hand.handResultBundle?.results.extractHandsXYZKeypoints()
    val leftHand = handResult.first
    val rightHand = handResult.second
    return pose + leftHand + rightHand
}
