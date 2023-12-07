package com.grandefirano.signtalk.recognition

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RecognitionViewModel @Inject constructor(
    private val predictionManager: PredictionManager
):ViewModel() {

//    val recognizedSentences: StateFlow<List<String>> = predictionManager.recognizedSentences
//    val translationChoice: StateFlow<TranslationChoice> = predictionManager.translationChoice
//
//
//    fun switchTranslation(translationChoice: TranslationChoice) {
//        predictionManager.switchTranslation(translationChoice)
//        //To prevent mixing languages guesses when language is changed
//        //lastSequence.clear()
//    }
}