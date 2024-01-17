package com.grandefirano.signtalk.landmarks

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

fun List<NormalizedLandmark>.dropIrises(): List<NormalizedLandmark> {
    return take(468)
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
