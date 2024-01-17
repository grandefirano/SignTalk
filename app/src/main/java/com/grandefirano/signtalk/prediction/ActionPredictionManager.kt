package com.grandefirano.signtalk.prediction

import androidx.compose.runtime.mutableStateListOf
import com.grandefirano.signtalk.prediction.dictionary.DictionaryProvider
import com.grandefirano.signtalk.prediction.interpreter.ActionInterpreter
import com.grandefirano.signtalk.prediction.interpreter.PredictionInterpreterProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionPredictionManager @Inject constructor(
    private val dictionaryProvider: DictionaryProvider,
    private val interpreterProvider: PredictionInterpreterProvider,
):PredictionManager<List<Float>> {

    private var interpreter: ActionInterpreter =
        interpreterProvider.getActionInterpreter(TranslationChoice.PJM_POLISH)

    private var dictionaryLanguage: List<String> =
        dictionaryProvider.getDictionary(TranslationChoice.PJM_POLISH,true)

    private val _recognizedSentences: MutableStateFlow<MutableList<String>> =
        MutableStateFlow(mutableStateListOf())
    val recognizedSentences: StateFlow<List<String>> = _recognizedSentences

    override fun predict(input: List<List<Float>>): Prediction? {
        val floatArray = input.toFloatArray()
        val inputSize = intArrayOf(1, 23, 195)
        val inputFeature = TensorBuffer.createFixedSize(inputSize, DataType.FLOAT32)
        inputFeature.loadArray(floatArray, inputSize)
        val outputFeature = interpreter.interpret(inputFeature)
        val list = outputFeature.floatArray.toList()
        val maxIndex = list.argmax()
        return maxIndex?.let {
            val possibility = list[maxIndex]
            Prediction(dictionaryLanguage[maxIndex], possibility)
        }
    }

/**    This can be used when more languages are implemented
*
*    fun switchTranslation(translationChoice: TranslationChoice) {
*        _translationChoice.value = translationChoice
*        interpreter = interpreterProvider.getPredictionInterpreter(translationChoice)
*        dictionaryLanguage = dictionaryProvider.getDictionary(translationChoice)
*    }
*
*/

}
