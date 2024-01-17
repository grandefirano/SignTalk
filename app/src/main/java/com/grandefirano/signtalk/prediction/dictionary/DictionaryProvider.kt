package com.grandefirano.signtalk.prediction.dictionary

import com.grandefirano.signtalk.prediction.TranslationChoice
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






