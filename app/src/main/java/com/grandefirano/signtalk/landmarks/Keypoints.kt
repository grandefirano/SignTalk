package com.grandefirano.signtalk.landmarks

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

fun List<XYZKeypoints>.normalize(): List<XYZKeypoints> {
    var smallestX = 1f
    var biggestX = 0f
    var smallestY = 1f
    var biggestY = 0f
    var smallestZ = 1f
    var biggestZ = 0f
    this.forEach {
        if (smallestX > it.x) smallestX = it.x
        if (biggestX < it.x) biggestX = it.x
        if (smallestY > it.y) smallestY = it.y
        if (biggestY < it.y) biggestY = it.y
        if (smallestZ > it.z) smallestZ = it.z
        if (biggestZ < it.z) biggestZ = it.z
    }
    val deltaX = biggestX - smallestX
    val deltaY = biggestY - smallestY
    val deltaZ = biggestZ - smallestZ
    if (deltaX == 0f || deltaY == 0f || deltaZ == 0f) return List(21) { XYZKeypoints(0f, 0f, 0f) }
    return this.map {
        val normX = (it.x - smallestX) / deltaX
        val normY = (it.y - smallestY) / deltaY
        val normZ = (it.z - smallestZ) / deltaZ
        XYZKeypoints(normX, normY, normZ)
    }
}
