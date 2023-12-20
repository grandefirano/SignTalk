package com.grandefirano.signtalk.camera

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.grandefirano.signtalk.ActionRecognizer
import com.grandefirano.signtalk.StaticRecognizer
import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.hand.HandLandmarkerResultWrapper
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarkerResultWrapper
import com.grandefirano.signtalk.recognition.ActionPredictionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val landmarksManager: LandmarksManager,
    private val actionRecognizer: ActionRecognizer,
    private val staticRecognizer: StaticRecognizer,
) : ViewModel() {

    val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    val handLandmarks: StateFlow<HandLandmarkerResultWrapper> = landmarksManager.handLandmarks
    val poseLandmarks: StateFlow<PoseLandmarkerResultWrapper> = landmarksManager.poseLandmarks
    val recognizedActionSentences: StateFlow<List<String>> = actionRecognizer.recognizedActionSentences
    //val translationChoice: StateFlow<TranslationChoice> = actionPredictionManager.translationChoice

//    fun switchTranslation(translationChoice: TranslationChoice) {
//        //TODO: To prevent mixing languages guesses when language is changed
//        actionRecognizer.clearLastSequence()
//        predictionManager.switchTranslation(translationChoice)
//    }

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

    suspend fun startActionRecognition() {
        println("NOWYY ACTION RECOGNITION VM")
        actionRecognizer.startActionRecognition()
    }
}

