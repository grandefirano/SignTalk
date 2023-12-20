package com.grandefirano.signtalk.recognition.dictionary

import android.content.Context
import com.grandefirano.signtalk.ml.Pjm10ModelV2
import com.grandefirano.signtalk.ml.PjmLeftHandV1
import com.grandefirano.signtalk.recognition.TranslationChoice
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject

class DictionaryProvider @Inject constructor() {
    fun getDictionary(translationChoice: TranslationChoice, isAction: Boolean): List<String> {
        //TODO: Add longer dictionary - at least 30 words
        return when (isAction) {
            true -> when (translationChoice) {
                TranslationChoice.PJM_POLISH -> getPolishActionDictionary()
            }
            false -> when (translationChoice) {
                TranslationChoice.PJM_POLISH -> getPolishStaticDictionary()
            }
        }
    }
}

class PredictionInterpreterProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getPredictionInterpreter(
        translationChoice: TranslationChoice,
        isAction: Boolean
    ): Interpreter {
        return when (isAction) {
            true -> when (translationChoice) {
                TranslationChoice.PJM_POLISH -> PJMPolishActionInterpreter(context)
            }
            false -> when (translationChoice) {
                TranslationChoice.PJM_POLISH -> PJMPolishStaticInterpreter(context)
            }
        }
    }
}

interface Interpreter {
    fun interpret(input: TensorBuffer): TensorBuffer
}

class PJMPolishActionInterpreter(context: Context) : Interpreter {
    private val model = Pjm10ModelV2.newInstance(context)

    override fun interpret(input: TensorBuffer): TensorBuffer {
        return model.process(input).outputFeature0AsTensorBuffer
    }
}

class PJMPolishStaticInterpreter(context: Context) : Interpreter {
    private val model = PjmLeftHandV1.newInstance(context)
    override fun interpret(input: TensorBuffer): TensorBuffer {
        return model.process(input).outputFeature0AsTensorBuffer
    }
}