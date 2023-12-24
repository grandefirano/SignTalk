package com.grandefirano.signtalk.camera

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksResult
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksResult
import com.grandefirano.signtalk.prediction.ActionRecognitionManager
import com.grandefirano.signtalk.prediction.StaticRecognitionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val landmarksManager: LandmarksManager,
    private val actionRecognitionManager: ActionRecognitionManager,
    private val staticRecognitionManager: StaticRecognitionManager,
) : ViewModel() {

    val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    val handLandmarks: StateFlow<HandLandmarksResult> = landmarksManager.handLandmarks
    val poseLandmarks: StateFlow<PoseLandmarksResult> = landmarksManager.poseLandmarks
    //TODO: SWITCH HERE
    val recognizedActionSentences: StateFlow<List<String>> = actionRecognitionManager.recognizedActionSentences
    //val recognizedActionSentences: StateFlow<List<String>> = staticRecognizer.recognizedStaticSigns
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
        actionRecognitionManager.startActionRecognition()
    }

    suspend fun startStaticRecognition() {
        staticRecognitionManager.startStaticRecognition()
    }
}

