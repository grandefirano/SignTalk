package com.grandefirano.signtalk.prediction.interpreter.pjm

import android.content.Context
import com.grandefirano.signtalk.ml.Pjm21FnnModel
import com.grandefirano.signtalk.prediction.interpreter.StaticInterpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class PJMPolishStaticInterpreter(context: Context) : StaticInterpreter {
    private val model = Pjm21FnnModel.newInstance(context)
    override fun interpret(input: TensorBuffer): TensorBuffer {
        return model.process(input).outputFeature0AsTensorBuffer
    }
}