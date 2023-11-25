package com.grandefirano.signtalk.recognition

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.grandefirano.signtalk.XYZKeypoints
import com.grandefirano.signtalk.XYZVKeypoints

fun List<NormalizedLandmark>.dropIrises(): List<NormalizedLandmark> {
    println("WRONGGGGGGGGG ${this.size}")
    val ren = take(468)
    println("WRONGGGGGGGGG ${ren.size}")
    return ren
}

fun List<NormalizedLandmark>.toXYZVisibility(): List<XYZVKeypoints> {
    return map {
        XYZVKeypoints(it.x(), it.y(), it.z(), it.visibility().get())
    }
}

fun List<NormalizedLandmark>.toXYZ(): List<XYZKeypoints> {
    return map {
        XYZKeypoints(it.x(), it.y(), it.z())
    }
}
