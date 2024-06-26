package com.grandefirano.signtalk.landmarks.face

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class FaceLandmarksManager @Inject constructor(
    @ApplicationContext val context: Context,
) {

    private val _faceLandmarks: MutableStateFlow<FaceLandmarkerResultWrapper> =
        MutableStateFlow(initFaceLandmarkerResultWrapper())
    val faceLandmarks: StateFlow<FaceLandmarkerResultWrapper> = _faceLandmarks

    private var faceLandmarker: FaceLandmarker? = null

    fun clearFaceLandmarker() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    fun isClose(): Boolean {
        return faceLandmarker == null
    }

    fun setupFaceLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setDelegate(Delegate.CPU)
        baseOptionBuilder.setModelAssetPath(MP_FACE_LANDMARKER_TASK)

        try {
            val baseOptions = baseOptionBuilder.build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinFaceDetectionConfidence(DEFAULT_FACE_DETECTION_CONFIDENCE)
                .setMinTrackingConfidence(DEFAULT_FACE_TRACKING_CONFIDENCE)
                .setMinFacePresenceConfidence(DEFAULT_FACE_PRESENCE_CONFIDENCE)
                .setNumFaces(DEFAULT_NUM_FACES)
                .setOutputFaceBlendshapes(true)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
                .build()
            faceLandmarker =
                FaceLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            onError(
                "CCFace Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG, "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            onError(
                "DDFace Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG,
                "Face Landmarker failed to load model with error: " + e.message
            )
        }
    }

    private fun onError(error: String) {
        println("ERROR APP: $error")
    }

    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        faceLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: FaceLandmarkerResult,
        input: MPImage
    ) {
        if (result.faceLandmarks().size > 0) {
            val finishTimeMs = SystemClock.uptimeMillis()
            val inferenceTime = finishTimeMs - result.timestampMs()
            updateFace(
                FaceResultBundle(
                    result,
                    inferenceTime,
                    input.height,
                    input.width,
                    finishTimeMs
                )
            )

        } else {
            updateEmptyFace()
        }
    }

    private fun updateEmptyFace() {
        _faceLandmarks.update {
            it.withNewLandmark(
                faceResultBundle = null
            )
        }
    }

    private fun updateFace(faceResultBundle: FaceResultBundle) {
        _faceLandmarks.update {
            it.withNewLandmark(
                faceResultBundle = faceResultBundle
            )
        }
    }

    private fun returnLivestreamError(error: RuntimeException) {
        onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    companion object {
        const val TAG = "FaceLandmarkerHelper"
        private const val MP_FACE_LANDMARKER_TASK = "face_landmarker.task"
        const val DEFAULT_FACE_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_FACE_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_FACE_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_FACES = 1
    }

    data class FaceResultBundle(
        val result: FaceLandmarkerResult,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
        val timeStampFinish: Long
    )
}
