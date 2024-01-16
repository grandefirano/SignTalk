package com.grandefirano.signtalk.prediction

import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.XYZKeypoints
import com.grandefirano.signtalk.landmarks.flattenXYZ
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksResult
import com.grandefirano.signtalk.landmarks.hand.extractHandsXYZKeypoints
import com.grandefirano.signtalk.landmarks.normalize
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksResult
import com.grandefirano.signtalk.landmarks.toXYZ
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

class UpperBodyKeypointsAggregator @Inject constructor(
    private val landmarksManager: LandmarksManager
) {
    private var lastConsumedFrame = ConsumedFrames(0, 0)

    operator fun invoke(): Flow<List<XYZKeypoints>> {
        return combine(
            landmarksManager.handLandmarks,
            landmarksManager.poseLandmarks
        ) { hand, pose ->
            CombinedLandmarks(hand, pose)
        }.filter {
            val isEveryFrameUpdated = isEveryFrameUpdated(it)
            if (isEveryFrameUpdated) updateLastConsumed(it)
            isEveryFrameUpdated
        }.map { combinedLandmarks ->
            extractUpperBodyKeypoints(combinedLandmarks)
        }
    }

    private fun isEveryFrameUpdated(combinedLandmarks: CombinedLandmarks): Boolean {
        return combinedLandmarks.hand.frameNumber != lastConsumedFrame.handConsumed && combinedLandmarks.pose.frameNumber != lastConsumedFrame.poseConsumed
    }

    private fun updateLastConsumed(combinedLandmarks: CombinedLandmarks) {
        combinedLandmarks.let {
            lastConsumedFrame = ConsumedFrames(
                handConsumed = it.hand.frameNumber,
                poseConsumed = it.pose.frameNumber
            )
        }
    }
}

@Singleton
class ActionRecognitionManager @Inject constructor(
    private val actionPredictionManager: ActionPredictionManager,
    private val upperBodyKeypointsAggregator: UpperBodyKeypointsAggregator,
) {
    val recognizedElements: StateFlow<List<String>> =
        actionPredictionManager.recognizedSentences
    val lastRecognizedElement: StateFlow<String?> = actionPredictionManager.lastRecognizedElement
    private val lastSequence = mutableListOf<List<Float>>()

    suspend fun startRecognition() {
        upperBodyKeypointsAggregator().collect {
            onLandmarkUpdated(it)
        }
    }

    private fun onLandmarkUpdated(combinedLandmarks: List<XYZKeypoints>) {

        updateLastSequence(combinedLandmarks)
        if (lastSequence.size == 23) {
            actionPredictionManager.predict(lastSequence)
        }
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
