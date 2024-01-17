package com.grandefirano.signtalk.prediction

interface PredictionManager<T> {

    fun predict(input:List<T>):Prediction?
}