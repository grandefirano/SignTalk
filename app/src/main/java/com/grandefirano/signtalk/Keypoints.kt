package com.grandefirano.signtalk

data class XYZVKeypoints(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
)

data class XYZKeypoints(
    val x: Float,
    val y: Float,
    val z: Float,
)

fun List<XYZKeypoints>.flattenXYZ(): List<Float> {
    return flatMap {
        listOf(it.x, it.y, it.z)
    }
}

fun List<XYZVKeypoints>.flattenXYZV(): List<Float> {
    return flatMap {
        listOf(it.x, it.y, it.z, it.visibility)
    }
}
