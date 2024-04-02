package com.grandefirano.signtalk.prediction.interpreter.pjm

import android.content.Context
import com.grandefirano.signtalk.ml.Pjm21CompleteLstmModel
import com.grandefirano.signtalk.prediction.interpreter.ActionInterpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class PJMPolishActionInterpreter(context: Context) : ActionInterpreter {
    private val model = Pjm21CompleteLstmModel.newInstance(context)

    override fun interpret(input: TensorBuffer): TensorBuffer {
        return model.process(input).outputFeature0AsTensorBuffer
    }
}