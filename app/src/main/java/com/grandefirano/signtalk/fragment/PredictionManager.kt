package com.grandefirano.signtalk.fragment

import android.content.Context
import com.grandefirano.signtalk.DictionaryProvider
import com.grandefirano.signtalk.argmax
import com.grandefirano.signtalk.ml.ImageModel
import com.grandefirano.signtalk.toFloatArray
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject

class PredictionManager @Inject constructor(
    dictionaryProvider: DictionaryProvider,
    @ApplicationContext appContext: Context,
) {
    private val mlModel = ImageModel.newInstance(appContext)
    private val dictionary = dictionaryProvider.getDictionary()

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
            }
        }
    }

}
