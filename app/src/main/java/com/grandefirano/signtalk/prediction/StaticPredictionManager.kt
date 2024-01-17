package com.grandefirano.signtalk.prediction

import com.grandefirano.signtalk.prediction.dictionary.DictionaryProvider
import com.grandefirano.signtalk.prediction.interpreter.PredictionInterpreterProvider
import com.grandefirano.signtalk.prediction.interpreter.StaticInterpreter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticPredictionManager @Inject constructor(
    dictionaryProvider: DictionaryProvider,
    interpreterProvider: PredictionInterpreterProvider,
) : PredictionManager<Float> {
    private val translationChoice: TranslationChoice = TranslationChoice.PJM_POLISH
    private val isAction: Boolean = false
    private val interpreter: StaticInterpreter =
        interpreterProvider.getStaticInterpreter(translationChoice)
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





