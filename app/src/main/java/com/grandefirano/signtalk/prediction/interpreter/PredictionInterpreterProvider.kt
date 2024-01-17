package com.grandefirano.signtalk.prediction.interpreter

import android.content.Context
import com.grandefirano.signtalk.prediction.TranslationChoice
import com.grandefirano.signtalk.prediction.interpreter.pjm.PJMPolishActionInterpreter
import com.grandefirano.signtalk.prediction.interpreter.pjm.PJMPolishStaticInterpreter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PredictionInterpreterProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getStaticInterpreter(
        translationChoice: TranslationChoice,
    ): StaticInterpreter {
        return when (translationChoice) {
            TranslationChoice.PJM_POLISH -> PJMPolishStaticInterpreter(context)
        }
    }

    fun getActionInterpreter(
        translationChoice: TranslationChoice,
    ): ActionInterpreter {
        return when (translationChoice) {
            TranslationChoice.PJM_POLISH -> PJMPolishActionInterpreter(context)
        }
    }
}