package com.grandefirano.signtalk.prediction

import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.expandDims
import org.jetbrains.kotlinx.multik.ndarray.operations.toFloatArray

fun List<List<Float>>.toFloatArray(): FloatArray {
    val ndArray = toNDArray()
    val expandedDims = ndArray.expandDims(0)
    val expanded2 = expandedDims[0]
    return expanded2.toFloatArray()
}

fun <T : Comparable<T>> Iterable<T>.argmax(): Int? {
    return withIndex().maxByOrNull { it.value }?.index
}

