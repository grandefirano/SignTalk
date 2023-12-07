package com.grandefirano.signtalk.recognition.dictionary

import android.content.Context
import com.grandefirano.signtalk.ml.ImageModel
import com.grandefirano.signtalk.recognition.TranslationChoice
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject

class DictionaryProvider @Inject constructor() {
    fun getDictionary(translationChoice: TranslationChoice): List<String> {
        //TODO: Add longer dictionary - at least 30 words
        return when (translationChoice) {
           TranslationChoice.ASL_ENGLISH -> getEnglishDictionary()
           TranslationChoice.PJM_POLISH -> getPolishDictionary()
        }
    }
}

class PredictionInterpreterProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getPredictionInterpreter(
        translationChoice: TranslationChoice,
        //isHandOnly:Boolean
    ): Interpreter {
        return when (translationChoice) {
            TranslationChoice.ASL_ENGLISH -> AslEnglishPoseInterpreter(context)
            TranslationChoice.PJM_POLISH -> AslEnglishPoseInterpreter(context)
        }
    }
}

interface Interpreter {
    fun interpret(input: TensorBuffer): TensorBuffer
}

class AslEnglishPoseInterpreter(context: Context) : Interpreter {
    private val model = ImageModel.newInstance(context)
    override fun interpret(input: TensorBuffer): TensorBuffer {
        return model.process(input).outputFeature0AsTensorBuffer
    }
}
