package com.grandefirano.signtalk.prediction

import androidx.compose.runtime.mutableStateListOf
import com.grandefirano.signtalk.recognition.TranslationChoice
import com.grandefirano.signtalk.recognition.argmax
import com.grandefirano.signtalk.recognition.dictionary.DictionaryProvider
import com.grandefirano.signtalk.recognition.dictionary.Interpreter
import com.grandefirano.signtalk.recognition.dictionary.PredictionInterpreterProvider
import com.grandefirano.signtalk.recognition.toFloatArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionPredictionManager @Inject constructor(
    private val dictionaryProvider: DictionaryProvider,
    private val interpreterProvider: PredictionInterpreterProvider,
) {

//    private val _translationChoice: MutableStateFlow<TranslationChoice> =
//        MutableStateFlow(TranslationChoice.PJM_POLISH)
//    val translationChoice: StateFlow<TranslationChoice> = _translationChoice

    private var interpreter: Interpreter =
        interpreterProvider.getPredictionInterpreter(TranslationChoice.PJM_POLISH,true)

    private var dictionaryLanguage: List<String> =
        dictionaryProvider.getDictionary(TranslationChoice.PJM_POLISH,true)

    //TODO handle the flow below
    private val _recognizedSentences: MutableStateFlow<MutableList<String>> =
        MutableStateFlow(mutableStateListOf())
    val recognizedSentences: StateFlow<List<String>> = _recognizedSentences

    private val threshold = 0.8

    private val lastPredictions = mutableListOf<Int>()
    fun predict(sequence: List<List<Float>>) {
        println("NOWYY PREDICT ACTION")
        val floatArray = sequence.toFloatArray()
        val inputSize = intArrayOf(1, 23, 195)
        val inputFeature = TensorBuffer.createFixedSize(inputSize, DataType.FLOAT32)
        inputFeature.loadArray(floatArray, inputSize)
        val outputFeature = interpreter.interpret(inputFeature)
        val list = outputFeature.floatArray.toList()
        val maxIndex = list.argmax()
        maxIndex?.let {
            println("NOWYY ${dictionaryLanguage[it]}")
            updateLastPrediction(maxIndex)
            checkLastPredictions(maxIndex, list)
        }
    }

//    fun switchTranslation(translationChoice: TranslationChoice) {
//        _translationChoice.value = translationChoice
//        interpreter = interpreterProvider.getPredictionInterpreter(translationChoice)
//        dictionaryLanguage = dictionaryProvider.getDictionary(translationChoice)
//    }

    private fun updateLastPrediction(prediction: Int) {

        lastPredictions.add(prediction)
        if (lastPredictions.size > 5) lastPredictions.removeAt(0)
    }

    private fun checkLastPredictions(currentIndex: Int, list: List<Float>) {
        var isEqual = true
        lastPredictions.forEach {
            if (currentIndex != it) {
                isEqual = false
                return@forEach
            }
        }
        if (isEqual) {
            if (list[currentIndex] > threshold) {
                val currentSentence = dictionaryLanguage[currentIndex]
                if (_recognizedSentences.value.size > 0) {

                    if (currentSentence != _recognizedSentences.value.last()) {
                        addSentenceItem(currentSentence)
                    }
                } else {
                    addSentenceItem(currentSentence)
                }
                println("GUESS ${dictionaryLanguage[currentIndex]}")
            }
        }
    }

    private fun addSentenceItem(currentSentence: String) {
        _recognizedSentences.update { it.apply { add(currentSentence) } }
    }
}
