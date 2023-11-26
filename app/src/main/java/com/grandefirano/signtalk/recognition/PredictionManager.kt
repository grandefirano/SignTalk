package com.grandefirano.signtalk.recognition

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.grandefirano.signtalk.ml.ImageModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
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


    fun predict(sequence: List<List<Float>>) {
        val threshold = 0.5
        val floatArray = sequence.toFloatArray()
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 30, 1662), DataType.FLOAT32)
        inputFeature.loadArray(floatArray, intArrayOf(1, 30, 1662))
        val outputs = mlModel.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer
        val list = outputFeature.floatArray.toList()
        val maxIndex = list.argmax()
        maxIndex?.let {
            if (list[maxIndex] > threshold) {
                println("GUESS ${dictionary[maxIndex]}")
                //TODO:
            }
        }
    }
    suspend fun generate(){
        for (index in 1.. 15){
            delay(500)
            _recognizedSentences.update { it.apply { add("Item: $index")} }

        }
    }

}
