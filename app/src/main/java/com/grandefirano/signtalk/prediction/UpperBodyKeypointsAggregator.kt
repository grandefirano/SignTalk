package com.grandefirano.signtalk.prediction

import com.grandefirano.signtalk.landmarks.LandmarksManager
import com.grandefirano.signtalk.landmarks.XYZKeypoints
import com.grandefirano.signtalk.recognition.CombinedLandmarks
import com.grandefirano.signtalk.recognition.ConsumedFrames
import com.grandefirano.signtalk.recognition.extractUpperBodyKeypoints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class UpperBodyKeypointsAggregator @Inject constructor(
    private val landmarksManager: LandmarksManager
) {
    private var lastConsumedFrame = ConsumedFrames(0, 0)

    operator fun invoke(): Flow<List<XYZKeypoints>> {
        return combine(
            landmarksManager.handLandmarks,
            landmarksManager.poseLandmarks
        ) { hand, pose ->
            CombinedLandmarks(hand, pose)
        }.filter {
            val isEveryFrameUpdated = isEveryFrameUpdated(it)
            if (isEveryFrameUpdated) updateLastConsumed(it)
            isEveryFrameUpdated
        }.map { combinedLandmarks ->
            extractUpperBodyKeypoints(combinedLandmarks)
        }
    }

    private fun isEveryFrameUpdated(combinedLandmarks: CombinedLandmarks): Boolean {
        return combinedLandmarks.hand.frameNumber != lastConsumedFrame.handConsumed && combinedLandmarks.pose.frameNumber != lastConsumedFrame.poseConsumed
    }

    private fun updateLastConsumed(combinedLandmarks: CombinedLandmarks) {
        combinedLandmarks.let {
            lastConsumedFrame = ConsumedFrames(
                handConsumed = it.hand.frameNumber,
                poseConsumed = it.pose.frameNumber
            )
        }
    }
}
