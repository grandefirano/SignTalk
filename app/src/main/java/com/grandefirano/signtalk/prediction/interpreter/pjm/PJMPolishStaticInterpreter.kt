package com.grandefirano.signtalk.prediction.interpreter.pjm

import android.content.Context
import com.grandefirano.signtalk.ml.Pjm21HandFnnModel
import com.grandefirano.signtalk.prediction.interpreter.StaticInterpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class PJMPolishStaticInterpreter(context: Context) : StaticInterpreter {
    private val model = Pjm21HandFnnModel.newInstance(context)

    val times = mutableListOf<Long>()

    val averageTime:Double
        get() = times.average()


    @OptIn(ExperimentalTime::class)
    override fun interpret(input: TensorBuffer): TensorBuffer {
        var value: TensorBuffer
        val time = measureTime {
            value = model.process(input).outputFeature0AsTensorBuffer
        }
        times.add(time.inWholeMicroseconds)
        //println("ST times actual $time average $averageTime")
        //3.151
        if(times.size == 300) println("ST test of 300 average $averageTime")
        return value
    }
}