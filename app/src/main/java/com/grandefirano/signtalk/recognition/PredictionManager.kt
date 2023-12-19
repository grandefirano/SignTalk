package com.grandefirano.signtalk.recognition

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.grandefirano.signtalk.ml.ImageModel
import com.grandefirano.signtalk.recognition.dictionary.DictionaryLanguage
import com.grandefirano.signtalk.recognition.dictionary.DictionaryProvider
import com.grandefirano.signtalk.recognition.dictionary.Interpreter
import com.grandefirano.signtalk.recognition.dictionary.PredictionInterpreterProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionManager @Inject constructor(
    private val dictionaryProvider: DictionaryProvider,
    private val interpreterProvider: PredictionInterpreterProvider,
) {

    private val _translationChoice: MutableStateFlow<TranslationChoice> =
        MutableStateFlow(TranslationChoice.PJM_POLISH)
    val translationChoice: StateFlow<TranslationChoice> = _translationChoice

    private var interpreter: Interpreter =
        interpreterProvider.getPredictionInterpreter(_translationChoice.value)

    private var dictionaryLanguage: List<String> =
        dictionaryProvider.getDictionary(_translationChoice.value)

    //TODO handle the flow below
    private val _recognizedSentences: MutableStateFlow<MutableList<String>> =
        MutableStateFlow(mutableStateListOf())
    val recognizedSentences: StateFlow<List<String>> = _recognizedSentences

    private val threshold = 0.8

    private val lastPredictions = mutableListOf<Int>()
    fun predict(sequence: List<List<Float>>) {
        val floatArray = sequence.toFloatArray()
        // Creates inputs for reference.
        //val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 23, 218), DataType.FLOAT32)

        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 23, 218), DataType.FLOAT32)
        inputFeature.loadArray(floatArray, intArrayOf(1, 23, 218))
        val outputFeature = interpreter.interpret(inputFeature)
        val list = outputFeature.floatArray.toList()
        val maxIndex = list.argmax()
        maxIndex?.let {
            println("NOWYY ${dictionaryLanguage[it]}")
            updateLastPrediction(maxIndex)
            checkLastPredictions(maxIndex, list)
        }
    }

    fun switchTranslation(translationChoice: TranslationChoice) {
        _translationChoice.value = translationChoice
        interpreter = interpreterProvider.getPredictionInterpreter(translationChoice)
        dictionaryLanguage = dictionaryProvider.getDictionary(translationChoice)
    }

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


    suspend fun generate() {
        for (index in 1..15) {
            delay(500)
            _recognizedSentences.update { it.apply { add("Item: $index") } }

        }
    }

}
