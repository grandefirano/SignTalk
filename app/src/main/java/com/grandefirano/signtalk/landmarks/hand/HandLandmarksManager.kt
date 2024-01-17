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
import com.grandefirano.signtalk.landmarks.LandmarksManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class HandLandmarksManager@Inject constructor(
    @ApplicationContext val context: Context,
):LandmarksManager<HandLandmarksResult> {
    private val _landmarks: MutableStateFlow<HandLandmarksResult> =
        MutableStateFlow(initHandLandmarkerResultWrapper())
    override val landmarks: StateFlow<HandLandmarksResult> = _landmarks
    private var handLandmarker: HandLandmarker? = null

    fun clearHandLandmarker() {
        handLandmarker?.close()
        handLandmarker = null
    }
    fun isClose(): Boolean {
        return handLandmarker == null
    }

    override fun setupLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setDelegate(Delegate.CPU)
        baseOptionBuilder.setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        try {
            val baseOptions = baseOptionBuilder.build()
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
                "AHand Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG, "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            onError(
                "BBHand Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG,
                "Image classifier failed to load model with error: " + e.message
            )
        }
    }

    @VisibleForTesting
    override fun detectAsync(mpImage: MPImage, frameTime: Long) {
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: HandLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()
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
        _landmarks.update {
            it.withNewLandmark(
                handResultBundle
            )
        }
    }

    private fun returnLivestreamError(error: RuntimeException) {
        onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    private fun onError(error: String) {
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
