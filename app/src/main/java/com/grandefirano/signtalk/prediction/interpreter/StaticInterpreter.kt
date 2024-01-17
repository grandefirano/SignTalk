package com.grandefirano.signtalk.prediction.interpreter

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

interface StaticInterpreter:Interpreter {
    override fun interpret(input: TensorBuffer): TensorBuffer
}