package com.grandefirano.signtalk.prediction

import android.os.SystemClock
import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.XYZKeypoints
import com.grandefirano.signtalk.landmarks.flattenXYZ
import com.grandefirano.signtalk.landmarks.flattenXYZV
import com.grandefirano.signtalk.landmarks.hand.HandLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.hand.extractHandsXYZKeypoints
import com.grandefirano.signtalk.landmarks.normalize
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.toXYZ
import com.grandefirano.signtalk.landmarks.toXYZVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionRecognizer @Inject constructor(
    private val landmarksManager: LandmarksManager,
    private val actionPredictionManager: ActionPredictionManager,
) {
    val recognizedActionSentences: StateFlow<List<String>> = actionPredictionManager.recognizedSentences
    //val faceLandmarks: StateFlow<FaceLandmarkerResultWrapper> = landmarksManager.faceLandmarks
    private val handLandmarks: StateFlow<HandLandmarkerResultWrapper> = landmarksManager.handLandmarks
    private val poseLandmarks: StateFlow<PoseLandmarkerResultWrapper> = landmarksManager.poseLandmarks
    private val filterPoseLandmarks
        get() = listOf(23, 24, 25, 26, 27, 28, 29, 30, 31, 32)

    private var lastConsumedFrame = ConsumedFrames(0, 0)
    private val lastSequence = mutableListOf<List<Float>>()

    private var counterXX = 0
    private var counterXX2222 = 0
    private var counterXXSaved = 0L
    private val combinedLandmarks: Flow<CombinedLandmarks> =
        combine(
            //faceLandmarks,
            handLandmarks, poseLandmarks
        ) { hand, pose ->
            //println("Frame face ${face.frameNumber}")
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
        }

    suspend fun startActionRecognition() {
        println("NOWYY ACTION RECOGNITION")
        combinedLandmarks.collect {
            println("NOWYY ONLANDMARK")
            onLandmarkUpdated(it)
        }
    }

    fun onLandmarkUpdated(combinedLandmarks: CombinedLandmarks) {
        combinedLandmarks.doWhenAllUpdated {
            println("NOWYY ONLANDMARK UPDATED")
            println(
                "Frame Combined: \n" +
                        "Combined Frame Hand: ${it.hand.frameNumber}: ${it.hand.handResultBundle?.timeStampFinish} \n" +
                        //"Combined Frame Face: ${it.face.frameNumber}: ${it.face.faceResultBundle?.timeStampFinish} \n" +
                        "Combined Frame Pose: ${it.pose.frameNumber}: ${it.pose.poseResultBundle?.timeStampFinish}"
            )
            updateLastSequence(it)
            if (lastSequence.size == 23) {
                println("JAKUB1222 ${lastSequence.size}")
                actionPredictionManager.predict(lastSequence)
            }
        }
    }

    private fun updateLastSequence(landmarks: CombinedLandmarks) {

        val keypointArray = extractKeypoints(landmarks)
        println("TESTTT KEYPOINT ARRAY ${keypointArray.size} $keypointArray")

        lastSequence.add(keypointArray)
        if (lastSequence.size > 23) lastSequence.removeAt(0)

    }

    private fun extractKeypoints(landmarks: CombinedLandmarks): List<Float> {
        //TODO: CHANGE LOGIC OF OBSERVING SO IT DOESNT STOP HERE FOR THESE, NOW IT DOESNT STOP FOR NON EXISTING HANDS ONLY

        val poseBefore =
            landmarks.pose.poseResultBundle?.result?.landmarks()?.flatten()?.toXYZ()
        val pose = poseBefore?.filterIndexed { index, _ ->
            filterPoseLandmarks.contains(index).not()
        }.let { if (it.isNullOrEmpty()) List(23) { XYZKeypoints(0f,0f,0f) } else it }
        val handResult = landmarks.hand.handResultBundle?.results.extractHandsXYZKeypoints()
        val leftHand = handResult.first//.flattenXYZ()
        val rightHand = handResult.second//.flattenXYZ()
        val combinedLandmarks = pose+leftHand+rightHand

        if (leftHand.size != 21 ) {
            println("WRONGGGG left hand ${leftHand.size}")
        }
        if (rightHand.size != 21 ) {
            println("WRONGGGG right hand ${rightHand.size}")
        }
        if (pose.size != 23) {
            println("WRONGGGG pose ${pose.size}")
        }
//        if (face.size != 468 * 3) {
//            println("WRONGGGG face ${face.size}")
//        }
        println("TESTTT hand RIGHT $rightHand")
        println("TESTTT hand LEFT $leftHand")
        return combinedLandmarks.normalize().flattenXYZ()
    }

    fun CombinedLandmarks.doWhenAllUpdated(
        callback: (CombinedLandmarks) -> Unit
    ) {
        //if (lastConsumedFrame.faceConsumed == face.frameNumber) return
        if (lastConsumedFrame.handConsumed == hand.frameNumber) return
        if (lastConsumedFrame.poseConsumed == pose.frameNumber) return
        lastConsumedFrame = ConsumedFrames(
            handConsumed = hand.frameNumber,
            //faceConsumed = face.frameNumber,
            poseConsumed = pose.frameNumber
        )
        callback(this)
    }

    fun clearLastSequence() {
        lastSequence.clear()
    }
}

data class ConsumedFrames(
    val handConsumed: Int,
    //val faceConsumed: Int,
    val poseConsumed: Int
)

data class CombinedLandmarks(
    //val face: FaceLandmarkerResultWrapper,
    val hand: HandLandmarkerResultWrapper,
    val pose: PoseLandmarkerResultWrapper
)

