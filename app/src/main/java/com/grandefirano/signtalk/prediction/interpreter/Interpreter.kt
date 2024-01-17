package com.grandefirano.signtalk.prediction.interpreter

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

interface Interpreter {
    fun interpret(input: TensorBuffer): TensorBuffer
}
