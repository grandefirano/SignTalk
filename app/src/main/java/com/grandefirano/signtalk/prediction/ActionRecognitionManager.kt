package com.grandefirano.signtalk.prediction

import android.os.SystemClock
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

class GetUpperBodyLandmarks(
    private val landmarksManager: LandmarksManager
) {
    private var counterXX = 0
    private var counterXXSaved = 0L
    private var lastConsumedFrame = ConsumedFrames(0, 0)

    operator fun invoke(): Flow<CombinedLandmarks> {
        return combine(
            landmarksManager.handLandmarks,
            landmarksManager.poseLandmarks
        ) { hand, pose ->

            val finishTimeMs = SystemClock.uptimeMillis()
            //TODO test if frames are not lost here
            val newXX = finishTimeMs / 1000
            if (counterXXSaved == newXX) {
                counterXX++
            } else {
                println("KNOW COMBINE FFFF TIME IN SECCCC $counterXXSaved  frames: $counterXX")
                counterXX = 0
                counterXXSaved = newXX
            }
            //println("NOW COMBINE ")
            println("Frame hand ${hand.frameNumber}")
            println("Frame pose ${pose.frameNumber}")
            CombinedLandmarks(hand, pose)
        }.map {
            if (isEveryFrameUpdated(it)) updateLastConsumed(it)
            it
        }.filter {
            isEveryFrameUpdated(it)
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
    private val getUpperBodyLandmarks: GetUpperBodyLandmarks,
) {
    val recognizedActionSentences: StateFlow<List<String>> =
        actionPredictionManager.recognizedSentences

    private val lastSequence = mutableListOf<List<Float>>()

    suspend fun startActionRecognition() {
        println("NOWYY ACTION RECOGNITION")
        getUpperBodyLandmarks().collect {
            println("NOWYY ONLANDMARK")
            onLandmarkUpdated(it)
        }
    }

    private fun onLandmarkUpdated(combinedLandmarks: CombinedLandmarks) {
        println("NOWYY ONLANDMARK UPDATED")
        println(
            "Frame Combined: \n" +
                    "Combined Frame Hand: ${combinedLandmarks.hand.frameNumber}: ${combinedLandmarks.hand.handResultBundle?.timeStampFinish} \n" +
                    //"Combined Frame Face: ${it.face.frameNumber}: ${it.face.faceResultBundle?.timeStampFinish} \n" +
                    "Combined Frame Pose: ${combinedLandmarks.pose.frameNumber}: ${combinedLandmarks.pose.poseResultBundle?.timeStampFinish}"
        )
        updateLastSequence(combinedLandmarks)
        if (lastSequence.size == 23) {
            println("JAKUB1222 ${lastSequence.size}")
            actionPredictionManager.predict(lastSequence)
        }
    }

    private fun updateLastSequence(landmarks: CombinedLandmarks) {
        val keypointArray = extractUpperBodyKeypoints(landmarks).normalize().flattenXYZ()
        println("TESTTT KEYPOINT ARRAY ${keypointArray.size} $keypointArray")
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
    val leftHand = handResult.first//.flattenXYZ()
    val rightHand = handResult.second//.flattenXYZ()
    val combinedLandmarks = pose + leftHand + rightHand

    if (leftHand.size != 21) {
        println("WRONGGGG left hand ${leftHand.size}")
    }
    if (rightHand.size != 21) {
        println("WRONGGGG right hand ${rightHand.size}")
    }
    if (pose.size != 23) {
        println("WRONGGGG pose ${pose.size}")
    }
    println("TESTTT hand RIGHT $rightHand")
    println("TESTTT hand LEFT $leftHand")
    return combinedLandmarks
}
