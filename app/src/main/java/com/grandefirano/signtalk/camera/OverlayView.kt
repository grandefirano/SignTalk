package com.grandefirano.signtalk.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.grandefirano.signtalk.landmarks.hand.HandLandmarksRepository
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.grandefirano.signtalk.R
import com.grandefirano.signtalk.landmarks.pose.PoseLandmarksRepository
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    //private var resultsFace: FaceLandmarkerResult? = null
    private var resultsPose: PoseLandmarkerResult? = null
    private var resultsHand: HandLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    fun clear() {
        //resultsFace = null
        resultsHand = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    fun clearFace() {
        //resultsFace = null
        invalidate()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color =  ContextCompat.getColor(context!!, R.color.mp_color_2)
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        //drawFace(canvas)
        drawHand(canvas)
        drawPose(canvas)
    }

    private fun drawPose(canvas: Canvas) {
        resultsPose?.let { poseLandmarkerResult ->
            val visibilityThreshold = 0.8
            for (landmark in poseLandmarkerResult.landmarks()) {
                for (normalizedLandmark in landmark) {
                    if (normalizedLandmark.visibility().get() > visibilityThreshold) {
                        canvas.drawPoint(
                            normalizedLandmark.x() * imageWidth * scaleFactor,
                            normalizedLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                        )
                    }
                }

                PoseLandmarker.POSE_LANDMARKS.forEach {
                    val start = landmark.get(it!!.start())
                    val end = landmark.get(it.end())

                    if (start.visibility().get() > visibilityThreshold && end.visibility().get() > visibilityThreshold) {
                        canvas.drawLine(
                            start.x() * imageWidth * scaleFactor,
                            start.y() * imageHeight * scaleFactor,
                            end.x() * imageWidth * scaleFactor,
                            end.y() * imageHeight * scaleFactor,
                            linePaint
                        )
                    }
                }
            }
        }
    }

    private fun drawHand(canvas: Canvas) {
        resultsHand?.let { handLandmarkerResult ->
            for (landmark in handLandmarkerResult.landmarks()) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }
                HandLandmarker.HAND_CONNECTIONS.forEach {
                    canvas.drawLine(
                        landmark.get(it!!.start())
                            .x() * imageWidth * scaleFactor,
                        landmark.get(it.start())
                            .y() * imageHeight * scaleFactor,
                        landmark.get(it.end())
                            .x() * imageWidth * scaleFactor,
                        landmark.get(it.end())
                            .y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }
        }
    }

//    private fun drawFace(canvas: Canvas) {
//
//        resultsFace?.let { faceLandmarkerResult ->
//            if (faceLandmarkerResult.faceBlendshapes().isPresent) {
//                faceLandmarkerResult.faceBlendshapes().get().forEach {
//                    it.forEach {
//                        Log.e(TAG, it.displayName() + " " + it.score())
//                    }
//                }
//            }
//
//            for (landmark in faceLandmarkerResult.faceLandmarks()) {
//                for (normalizedLandmark in landmark) {
//                    canvas.drawPoint(
//                        normalizedLandmark.x() * imageWidth * scaleFactor,
//                        normalizedLandmark.y() * imageHeight * scaleFactor,
//                        pointPaint
//                    )
//                }
//
//
//                FaceLandmarker.FACE_LANDMARKS_CONNECTORS.forEach {
//                    canvas.drawLine(
//                        landmark.get(it!!.start())
//                            .x() * imageWidth * scaleFactor,
//                        landmark.get(it.start())
//                            .y() * imageHeight * scaleFactor,
//                        landmark.get(it.end())
//                            .x() * imageWidth * scaleFactor,
//                        landmark.get(it.end())
//                            .y() * imageHeight * scaleFactor,
//                        linePaint
//                    )
//                }
//            }
//        }
//    }

//    fun setFaceResults(faceResultBundle: FaceLandmarkerHelper.FaceResultBundle?) {
//        println("HANDYY face result")
//        resultsFace = faceResultBundle?.result
//        faceResultBundle?.let {
//            setResultsCommon(it.inputImageHeight, it.inputImageWidth)
//        }
//    }

    fun setPoseResults(poseResultBundle: PoseLandmarksRepository.PoseResultBundle?) {
        resultsPose = poseResultBundle?.result
        poseResultBundle?.let {
            setResultsCommon(it.inputImageHeight, it.inputImageWidth)
        }
    }

    fun setHandResults(handResultBundle: HandLandmarksRepository.HandResultBundle?) {
        println("HANDYY hand result")
        resultsHand = handResultBundle?.results
        handResultBundle?.let {
            setResultsCommon(it.inputImageHeight, it.inputImageWidth)
        }
    }

    private fun setResultsCommon(imageHeight: Int, imageWidth: Int) {
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 4F
        private const val TAG = "Face Landmarker Overlay"
    }
}
