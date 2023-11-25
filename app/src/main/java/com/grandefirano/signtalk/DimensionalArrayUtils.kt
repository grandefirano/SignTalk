package com.grandefirano.signtalk

import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.expandDims
import org.jetbrains.kotlinx.multik.ndarray.operations.toFloatArray

fun List<List<Float>>.toFloatArray(): FloatArray {
    println("JAAAKUB1 GLOB SIZE ${this.size}")
    this.forEach {
        println("JAAAKUB1 INSIDE SIZE ${it.size}")
    }

    val ndArray = toNDArray()
    println("JAAAKUB111 ${ndArray.shape[0]},${ndArray.shape[1]}")
    val expandedDims = ndArray.expandDims(0)
    println("JAAAKUB2 ${expandedDims.shape[0]},${expandedDims.shape[1]},${expandedDims.shape[2]}")
    val expanded2 = expandedDims[0]
    println("JAAAKUB3 ${expanded2.shape[0]},${expanded2.shape[1]}")
    return expanded2.toFloatArray()
}

fun <T : Comparable<T>> Iterable<T>.argmax(): Int? {
    return withIndex().maxByOrNull { it.value }?.index
}

