package com.grandefirano.signtalk.recognition.dictionary

sealed class DictionaryLanguage(val dictionary: List<String>) {
    object English : DictionaryLanguage(getEnglishDictionary())
    object Polish : DictionaryLanguage(getPolishDictionary())
}

fun getEnglishDictionary(): List<String> {
    return listOf("Hi", "Thank you", "Sorry")
}

fun getPolishDictionary(): List<String> {
    return listOf("Dziękuję", "Proszę", "Przepraszam","Do","Od","Jest","Który","Ja","Gdzie","Mam")
}
