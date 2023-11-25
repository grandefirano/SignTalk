package com.grandefirano.signtalk.recognition

import javax.inject.Inject

class DictionaryProvider @Inject constructor() {
    fun getDictionary(): List<String> {
        //TODO: Add longer dictionary - at least 30 words
        return listOf("hello", "thanks", "iloveyou")
    }
}
