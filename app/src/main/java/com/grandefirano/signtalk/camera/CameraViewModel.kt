package com.grandefirano.signtalk.camera

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandefirano.signtalk.landmarks.flattenXYZ
import com.grandefirano.signtalk.landmarks.flattenXYZV
import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.dropIrises
import com.grandefirano.signtalk.landmarks.face.FaceLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.hand.HandLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.toXYZ
import com.grandefirano.signtalk.landmarks.toXYZVisibility
import com.grandefirano.signtalk.recognition.PredictionManager
import com.grandefirano.signtalk.recognition.TranslationChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val landmarksManager: LandmarksManager,
    private val predictionManager: PredictionManager,
) : ViewModel() {

    val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    val faceLandmarks: StateFlow<FaceLandmarkerResultWrapper> = landmarksManager.faceLandmarks
    val handLandmarks: StateFlow<HandLandmarkerResultWrapper> = landmarksManager.handLandmarks
    val poseLandmarks: StateFlow<PoseLandmarkerResultWrapper> = landmarksManager.poseLandmarks

    val combinedLandmarks =
        combine(faceLandmarks, handLandmarks, poseLandmarks) { face, hand, pose ->
            println("Frame face ${face.frameNumber}")
            println("Frame hand ${hand.frameNumber}")
            println("Frame pose ${pose.frameNumber}")
            CombinedLandmarks(face, hand, pose)
        }
    private var lastConsumedFrame = ConsumedFrames(0, 0, 0)
    private val lastSequence = mutableListOf<List<Float>>()

    val recognizedSentences: StateFlow<List<String>> = predictionManager.recognizedSentences
    val translationChoice: StateFlow<TranslationChoice> = predictionManager.translationChoice


    fun switchTranslation(translationChoice: TranslationChoice) {
        //TODO: To prevent mixing languages guesses when language is changed
        lastSequence.clear()
        predictionManager.switchTranslation(translationChoice)

    }

    init {
        viewModelScope.launch {
        }
    }

    fun onLandmarkUpdated(combinedLandmarks: CombinedLandmarks) {
        combinedLandmarks.doWhenAllUpdated {
            println(
                "Frame Combined: \n" +
                        "Combined Frame Hand: ${it.hand.frameNumber}: ${it.hand.handResultBundle?.timeStampFinish} \n" +
                        "Combined Frame Face: ${it.face.frameNumber}: ${it.face.faceResultBundle?.timeStampFinish} \n" +
                        "Combined Frame Pose: ${it.pose.frameNumber}: ${it.pose.poseResultBundle?.timeStampFinish}"
            )
            updateLastSequence(it)
            if (lastSequence.size == 30) {
                predictionManager.predict(lastSequence)
            }
        }
    }

    private fun updateLastSequence(landmarks: CombinedLandmarks) {
        val keypointArray = extractKeypoints(landmarks)
        println("TESTTT KEYPOINT ARRAY ${keypointArray.size} $keypointArray")

        lastSequence.add(keypointArray)
        if (lastSequence.size > 30) lastSequence.removeAt(0)

    }

    private fun extractKeypoints(landmarks: CombinedLandmarks): List<Float> {
        //TODO: CHANGE LOGIC OF OBSERVING SO IT DOESNT STOP HERE FOR THESE, NOW IT DOESNT STOP FOR NON EXISTING HANDS ONLY

        val pose =
            landmarks.pose.poseResultBundle?.result?.landmarks()?.flatten()?.toXYZVisibility()
                ?.flattenXYZV().let { if (it.isNullOrEmpty()) List(33 * 4) { 0f } else it }
        val face =
            landmarks.face.faceResultBundle?.result?.faceLandmarks()?.flatten()?.dropIrises()
                ?.toXYZ()
                ?.flattenXYZ().let { if (it.isNullOrEmpty()) List(468 * 3) { 0f } else it }
        val handResult = landmarks.hand.handResultBundle?.results
        val handedness = handResult?.handedness()?.flatten()
        val rightHandIndex = handedness?.indexOfFirst {
            it.categoryName() == "Left"
        }
        val leftHandIndex = handedness?.indexOfFirst {
            it.categoryName() == "Right"
        }
        val rightHand = rightHandIndex?.let {
            handResult.landmarks()?.getOrNull(rightHandIndex)?.toXYZ()
                ?.flattenXYZ()
        } ?: List(21 * 3) { 0f }
        val leftHand = leftHandIndex?.let {
            handResult.landmarks()?.getOrNull(leftHandIndex)?.toXYZ()
                ?.flattenXYZ()
        } ?: List(21 * 3) { 0f }
        if (leftHand.size != 21 * 3) {
            println("WRONGGGG left hand ${leftHand.size}")
        }
        if (rightHand.size != 21 * 3) {
            println("WRONGGGG right hand ${rightHand.size}")
        }
        if (pose.size != 33 * 4) {
            println("WRONGGGG pose ${pose.size}")
        }
        if (face.size != 468 * 3) {
            println("WRONGGGG face ${face.size}")
        }
        println("TESTTT hand RIGHT $rightHand")
        println("TESTTT hand LEFT $leftHand")
        return pose + face + leftHand + rightHand
    }

    fun CombinedLandmarks.doWhenAllUpdated(
        callback: (CombinedLandmarks) -> Unit
    ) {
        if (lastConsumedFrame.faceConsumed == face.frameNumber) return
        if (lastConsumedFrame.handConsumed == hand.frameNumber) return
        if (lastConsumedFrame.poseConsumed == pose.frameNumber) return
        lastConsumedFrame = ConsumedFrames(
            handConsumed = hand.frameNumber,
            faceConsumed = face.frameNumber,
            poseConsumed = pose.frameNumber
        )
        callback(this)
    }

    fun setupLandmarkerIfClosed() {
        backgroundExecutor.execute {
            landmarksManager.setupLandmarkerIfClosed()
        }
    }

    fun cleanLandmarker() {
        backgroundExecutor.execute { landmarksManager.cleanLandmarker() }
    }

    fun detectCombined(imageProxy: ImageProxy) {
        landmarksManager.detectCombined(imageProxy)
    }

    fun shutDownExecutor() {
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }
}

data class ConsumedFrames(
    val handConsumed: Int,
    val faceConsumed: Int,
    val poseConsumed: Int
)

data class CombinedLandmarks(
    val face: FaceLandmarkerResultWrapper,
    val hand: HandLandmarkerResultWrapper,
    val pose: PoseLandmarkerResultWrapper
)
