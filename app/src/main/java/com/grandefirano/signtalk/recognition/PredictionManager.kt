package com.grandefirano.signtalk.recognition

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.grandefirano.signtalk.ml.ImageModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionManager @Inject constructor(
    dictionaryProvider: DictionaryProvider,
    @ApplicationContext appContext: Context,
) {
    private val mlModel = ImageModel.newInstance(appContext)
    private val dictionary = dictionaryProvider.getDictionary()

    //TODO handle the flow below
    private val _recognizedSentences:MutableStateFlow<MutableList<String>> = MutableStateFlow(mutableStateListOf())
    val recognizedSentences: StateFlow<List<String>> = _recognizedSentences

    private val threshold = 0.5

    private val lastPredictions = mutableListOf<Int>()
    fun predict(sequence: List<List<Float>>) {
        val floatArray = sequence.toFloatArray()
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 30, 1662), DataType.FLOAT32)
        inputFeature.loadArray(floatArray, intArrayOf(1, 30, 1662))
        val outputs = mlModel.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer
        val list = outputFeature.floatArray.toList()
        val maxIndex = list.argmax()
        maxIndex?.let {
            updateLastPrediction(maxIndex)
            checkLastPredictions(maxIndex,list)
        }
    }

    private fun updateLastPrediction(prediction: Int) {
        lastPredictions.add(prediction)
        if (lastPredictions.size > 10) lastPredictions.removeAt(0)
    }

    private fun checkLastPredictions(currentIndex: Int, list: List<Float>) {
        var isEqual = true
        lastPredictions.forEach {
            if(currentIndex != it) {
                isEqual = false
                return@forEach
            }
        }
        if(isEqual){
            if (list[currentIndex] > threshold) {
                val currentSentence = dictionary[currentIndex]
                if(_recognizedSentences.value.size>0){

                    if(currentSentence!=_recognizedSentences.value.last()){
                        addSentenceItem(currentSentence)
                    }
                }else{
                    addSentenceItem(currentSentence)
                }
                println("GUESS ${dictionary[currentIndex]}")
            }
        }
    }

    private fun addSentenceItem(currentSentence: String) {
        _recognizedSentences.update { it.apply { add(currentSentence)} }
    }


    suspend fun generate(){
        for (index in 1.. 15){
            delay(500)
            _recognizedSentences.update { it.apply { add("Item: $index")} }

        }
    }

}
