package com.grandefirano.signtalk.landmarks.pose

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.grandefirano.signtalk.landmarks.LandmarksManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class PoseLandmarksManager @Inject constructor(
    @ApplicationContext val context: Context,
):LandmarksManager<PoseLandmarksResult> {

    private val _landmarks: MutableStateFlow<PoseLandmarksResult> =
        MutableStateFlow(initPoseLandmarkerResultWrapper())
    override val landmarks:StateFlow<PoseLandmarksResult> = _landmarks
    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupLandmarker()
    }

    fun clearPoseLandmarker() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    fun isClose(): Boolean {
        return poseLandmarker == null
    }

    override fun setupLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setDelegate(Delegate.CPU)

        val modelName = "pose_landmarker_lite.task"

        baseOptionBuilder.setModelAssetPath(modelName)

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder =
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(DEFAULT_POSE_DETECTION_CONFIDENCE)
                    .setMinTrackingConfidence(DEFAULT_POSE_TRACKING_CONFIDENCE)
                    .setMinPosePresenceConfidence(DEFAULT_POSE_PRESENCE_CONFIDENCE)
                    .setRunningMode(RunningMode.LIVE_STREAM)
            optionsBuilder
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)

            val options = optionsBuilder.build()
            poseLandmarker =
                PoseLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            onError(
                "Pose Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG, "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            onError(
                "Pose Landmarker failed to initialize. See error logs for " +
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
        poseLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {

        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()
        println("INFERENCE TIME POSE: $inferenceTime")
        updatePose(
            PoseResultBundle(
                result,
                inferenceTime,
                input.height,
                input.width,
                finishTimeMs
            )
        )
    }

    private fun updatePose(poseResultBundle: PoseResultBundle) {
        _landmarks.update {
            it.withNewLandmark(
                poseResultBundle = poseResultBundle
            )
        }
    }

    private fun onError(error: String) {
        println("ERROR APP: $error")
    }

    private fun returnLivestreamError(error: RuntimeException) {
        onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    companion object {
        const val TAG = "PoseLandmarkerHelper"
        const val DEFAULT_POSE_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_POSE_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_POSES = 1
    }

    data class PoseResultBundle(
        val result: PoseLandmarkerResult,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
        val timeStampFinish: Long
    )
}