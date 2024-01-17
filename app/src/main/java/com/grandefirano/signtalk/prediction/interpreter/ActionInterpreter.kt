package com.grandefirano.signtalk.prediction.interpreter

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

interface ActionInterpreter:Interpreter {
    override fun interpret(input: TensorBuffer): TensorBuffer
}