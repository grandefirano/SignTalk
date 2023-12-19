package com.grandefirano.signtalk.landmarks.hand

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.grandefirano.signtalk.landmarks.LandmarksManager.Companion.GPU_ERROR
import com.grandefirano.signtalk.landmarks.LandmarksManager.Companion.OTHER_ERROR
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class HandLandmarkerHelper@Inject constructor(
    @ApplicationContext val context: Context,
) {
    val handLandmarks: MutableStateFlow<HandLandmarkerResultWrapper> =
        MutableStateFlow(initHandLandmarkerResultWrapper())

    // For this example this needs to be a var so it can be reset on changes.
    // If the Hand Landmarker will not change, a lazy val would be preferable.
    private var handLandmarker: HandLandmarker? = null

    fun clearHandLandmarker() {
        handLandmarker?.close()
        handLandmarker = null
    }

    // Return running status of HandLandmarkerHelper
    fun isClose(): Boolean {
        return handLandmarker == null
    }

    // Initialize the Hand landmarker using current settings on the
    // thread that is using it. CPU can be used with Landmarker
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the
    // Landmarker
    fun setupHandLandmarker() {
        // Set general hand landmarker options
        val baseOptionBuilder = BaseOptions.builder()

        // Use the specified hardware for running the model. Default to CPU
        baseOptionBuilder.setDelegate(Delegate.CPU)
        baseOptionBuilder.setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        try {
            val baseOptions = baseOptionBuilder.build()
            // Create an option builder with base options and specific
            // options only use for Hand Landmarker.
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(DEFAULT_HAND_DETECTION_CONFIDENCE)
                .setMinTrackingConfidence(DEFAULT_HAND_TRACKING_CONFIDENCE)
                .setMinHandPresenceConfidence(DEFAULT_HAND_PRESENCE_CONFIDENCE)
                .setNumHands(DEFAULT_NUM_HANDS)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
                .build()
            handLandmarker =
                HandLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            onError(
                "AAHand Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG, "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            onError(
                "BBHand Landmarker failed to initialize. See error logs for " +
                        "details", GPU_ERROR
            )
            Log.e(
                TAG,
                "Image classifier failed to load model with error: " + e.message
            )
        }
    }


    // Run hand hand landmark using MediaPipe Hand Landmarker API
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        handLandmarker?.detectAsync(mpImage, frameTime)
        // As we're using running mode LIVE_STREAM, the landmark result will
        // be returned in returnLivestreamResult function
    }


    // Return the landmark result to this HandLandmarkerHelper's caller
    private fun returnLivestreamResult(
        result: HandLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()
        println("INFERENCE TIME HAND: $inferenceTime")
        updateHands(
            HandResultBundle(
                result,
                inferenceTime,
                input.height,
                input.width,
                finishTimeMs
            )
        )
    }

    private fun updateHands(handResultBundle: HandResultBundle) {
        handLandmarks.update {
            it.withNewLandmark(
                handResultBundle
            )
        }
    }

    // Return errors thrown during detection to this HandLandmarkerHelper's
    // caller
    private fun returnLivestreamError(error: RuntimeException) {
        onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    private fun onError(error: String, errorCode: Int = OTHER_ERROR) {
        println("ERROR APP: $error")
    }


    companion object {
        const val TAG = "HandLandmarkerHelper"
        private const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"

        const val DEFAULT_HAND_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_HANDS = 2
    }

    data class HandResultBundle(
        val results: HandLandmarkerResult,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
        val timeStampFinish: Long
    )
}
