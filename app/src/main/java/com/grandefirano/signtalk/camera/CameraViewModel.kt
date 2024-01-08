package com.grandefirano.signtalk.camera

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksResult
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksResult
import com.grandefirano.signtalk.prediction.ActionRecognitionManager
import com.grandefirano.signtalk.prediction.StaticRecognitionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private var currentRecognitionJob: Job? = null
    val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    val handLandmarks: StateFlow<HandLandmarksResult> = landmarksManager.handLandmarks
    val poseLandmarks: StateFlow<PoseLandmarksResult> = landmarksManager.poseLandmarks

    //TODO: SWITCH HERE
    //val recognizedActionSentences: StateFlow<List<String>> = actionRecognitionManager.recognizedActionSentences
    private val recognizedActionSentences: Flow<String?> =
        actionRecognitionManager.recognizedElements.map {
            println("ZZZZZ new action item ${it.lastOrNull()}")
            it.lastOrNull()
        }

    //    private val recognizedStaticSentences: Flow<String?> =
//        staticRecognitionManager.recognizedElements.map {
//            println("ZZZZZ new static item ${it.lastOrNull()}")
//            it.lastOrNull()
//        }
    private val lastRecognizedStaticElement = staticRecognitionManager.lastRecognizedElement
    private val lastRecognizedActionElement = actionRecognitionManager.lastRecognizedElement
    val allRecognizedElements = MutableStateFlow(emptyList<String>())

    init {
        viewModelScope.launch {
            launch {
                lastRecognizedStaticElement.collect { element ->
                    updateNewRecognizedItem(element)
                }
            }
            launch {
                lastRecognizedActionElement.collect { element ->
                    updateNewRecognizedItem(element)
                }
            }
        }
    }

    private fun updateNewRecognizedItem(element: String?) {
        element?.let {
            allRecognizedElements.update {
                it.toMutableList().apply {
                    add(element)
                }
            }
        }
    }
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

    fun switchRecognitionModel(isAction: Boolean, scope: CoroutineScope) {
        currentRecognitionJob?.cancel()
        val recognition = if (isAction) ::startActionRecognition else ::startStaticRecognition
        currentRecognitionJob = scope.launch { recognition() }
    }

    suspend fun startActionRecognition() {
        println("NOWYY ACTION RECOGNITION VM")
        actionRecognitionManager.startRecognition()
    }

    suspend fun startStaticRecognition() {
        staticRecognitionManager.startStaticRecognition()
    }
}

