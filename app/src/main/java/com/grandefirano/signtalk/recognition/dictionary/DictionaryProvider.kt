package com.grandefirano.signtalk.recognition.dictionary

import android.content.Context
import com.grandefirano.signtalk.ml.Pjm10ModelNorm
import com.grandefirano.signtalk.ml.Pjm10ModelNormV3
import com.grandefirano.signtalk.ml.Pjm21ModelGruV1
import com.grandefirano.signtalk.ml.Pjm21ModelLstmV294
import com.grandefirano.signtalk.ml.PjmLeftHandV3
import com.grandefirano.signtalk.recognition.TranslationChoice
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject

class DictionaryProvider @Inject constructor() {
    fun getDictionary(translationChoice: TranslationChoice, isAction: Boolean): List<String> {
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
    private val model = Pjm21ModelLstmV294.newInstance(context)

    override fun interpret(input: TensorBuffer): TensorBuffer {
        return model.process(input).outputFeature0AsTensorBuffer
    }
}

class PJMPolishStaticInterpreter(context: Context) : Interpreter {
    private val model = PjmLeftHandV3.newInstance(context)
    override fun interpret(input: TensorBuffer): TensorBuffer {
        return model.process(input).outputFeature0AsTensorBuffer
    }
}