package com.grandefirano.signtalk.landmarks

import com.google.mediapipe.framework.image.MPImage
import kotlinx.coroutines.flow.StateFlow

interface LandmarksManager<T> {

    val landmarks: StateFlow<T>

    fun setupLandmarker()
    fun detectAsync(mpImage: MPImage, frameTime: Long)
}