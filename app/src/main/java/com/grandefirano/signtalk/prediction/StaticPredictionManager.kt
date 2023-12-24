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
    dictionaryProvider: DictionaryProvider,
    interpreterProvider: PredictionInterpreterProvider,
) : PredictionManager {
    private val translationChoice: TranslationChoice = TranslationChoice.PJM_POLISH
    private val isAction: Boolean = false
    private val interpreter: Interpreter =
        interpreterProvider.getPredictionInterpreter(translationChoice, isAction)
    private val dictionaryLanguage: List<String> =
        dictionaryProvider.getDictionary(translationChoice, isAction)


    override fun predict(input: List<Float>): Prediction? {
        val floatArray = input.toFloatArray()
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 63), DataType.FLOAT32)
        inputFeature.loadArray(floatArray, intArrayOf(1, 63))
        val outputFeature = interpreter.interpret(inputFeature)
        val list = outputFeature.floatArray.toList()
        val maxIndex = list.argmax()
        return maxIndex?.let {
            val possibility = list[maxIndex]
            Prediction(dictionaryLanguage[maxIndex], possibility)
        }
    }
}

interface PredictionManager {
    fun predict(input: List<Float>): Prediction?
}

data class Prediction(
    val value: String,
    val possibility: Float
)


