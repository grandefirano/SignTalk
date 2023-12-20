package com.grandefirano.signtalk.prediction

import androidx.compose.runtime.mutableStateListOf
import com.grandefirano.signtalk.recognition.TranslationChoice
import com.grandefirano.signtalk.recognition.argmax
import com.grandefirano.signtalk.recognition.dictionary.DictionaryProvider
import com.grandefirano.signtalk.recognition.dictionary.Interpreter
import com.grandefirano.signtalk.recognition.dictionary.PredictionInterpreterProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticPredictionManager @Inject constructor(
    private val dictionaryProvider: DictionaryProvider,
    private val interpreterProvider: PredictionInterpreterProvider,
) {

    private var interpreter: Interpreter =
        interpreterProvider.getPredictionInterpreter(TranslationChoice.PJM_POLISH,false)

    private var dictionaryLanguage: List<String> =
        dictionaryProvider.getDictionary(TranslationChoice.PJM_POLISH,false)

    private val _recognizedSigns: MutableStateFlow<MutableList<String>> =
        MutableStateFlow(mutableStateListOf())
    val recognizedSigns: StateFlow<List<String>> = _recognizedSigns

    private val threshold = 0.8

    private val lastPredictions = mutableListOf<Int>()
    fun predict(sign:List<Float>) {
        val floatArray = sign.toFloatArray()
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 63), DataType.FLOAT32)
        inputFeature.loadArray(floatArray, intArrayOf(1, 63))
        val outputFeature = interpreter.interpret(inputFeature)
        val list = outputFeature.floatArray.toList()
        val maxIndex = list.argmax()
        maxIndex?.let {
            updateLastPrediction(maxIndex)
            checkLastPredictions(maxIndex, list)
        }
    }

    private fun updateLastPrediction(prediction: Int) {
        lastPredictions.add(prediction)
        if (lastPredictions.size > 10) lastPredictions.removeAt(0)
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
                if (_recognizedSigns.value.size > 0) {

                    if (currentSentence != _recognizedSigns.value.last()) {
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
        _recognizedSigns.update { it.apply { add(currentSentence) } }
    }
}
