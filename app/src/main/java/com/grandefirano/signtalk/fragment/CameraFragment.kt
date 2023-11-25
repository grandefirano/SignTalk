/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.facelandmarker.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.examples.facelandmarker.MainViewModel
import com.google.mediapipe.examples.facelandmarker.combined.CombineLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.face.FaceLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.face.FaceLandmarkerResultWrapper
import com.google.mediapipe.examples.facelandmarker.face.withNewLandmark
import com.google.mediapipe.examples.facelandmarker.hand.HandLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.hand.HandLandmarkerResultWrapper
import com.google.mediapipe.examples.facelandmarker.hand.withNewLandmark
import com.google.mediapipe.examples.facelandmarker.pose.PoseLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.pose.PoseLandmarkerResultWrapper
import com.google.mediapipe.examples.facelandmarker.pose.withNewLandmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.grandefirano.signtalk.databinding.FragmentCameraBinding
import com.grandefirano.signtalk.ml.ImageModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.expandDims
import org.jetbrains.kotlinx.multik.ndarray.operations.toFloatArray
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment() {

    companion object {
        private const val TAG = "Face Landmarker"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var combineLandmarkerHelper: CombineLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    val model by lazy { ImageModel.newInstance(requireContext()) }


    val faceLandmarks = MutableStateFlow(FaceLandmarkerResultWrapper(null, 0, 0))
    val handLandmarks = MutableStateFlow(HandLandmarkerResultWrapper(null, 0, 0))
    val poseLandmarks = MutableStateFlow(PoseLandmarkerResultWrapper(null, 0, 0))

    data class ConsumedFrames(
        val handConsumed: Int,
        val faceConsumed: Int,
        val poseConsumed: Int
    )

    data class CombinedLandmarks(
        val face: FaceLandmarkerResultWrapper,
        val hand: HandLandmarkerResultWrapper,
        val pose: PoseLandmarkerResultWrapper
    )


    private var lastConsumedFrame = ConsumedFrames(0, 0, 0)

    //TODO later pose
    val combinedLandmarks =
        combine(faceLandmarks, handLandmarks, poseLandmarks) { face, hand, pose ->
            println("Frame face ${face.frameNumber}")
            println("Frame hand ${hand.frameNumber}")
            println("Frame pose ${pose.frameNumber}")
            CombinedLandmarks(face, hand, pose)
        }

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    val poseListener = object : PoseLandmarkerHelper.LandmarkerListener {
        override fun onResults(
            resultBundle: PoseLandmarkerHelper.ResultBundle
        ) {
            activity?.runOnUiThread {
                if (_fragmentCameraBinding != null) {
                    val newLandmark = resultBundle.results.first()
                    poseLandmarks.update {
                        it.withNewLandmark(
                            newLandmark,
                            resultBundle.timeStampFinish
                        )
                    }

                    // Pass necessary information to OverlayView for drawing on the canvas
                    fragmentCameraBinding.overlay.setPoseResults(
                        resultBundle.results.first(),
                        resultBundle.inputImageHeight,
                        resultBundle.inputImageWidth,
                    )

                    // Force a redraw
                    fragmentCameraBinding.overlay.invalidate()
                }
            }
        }

        override fun onError(error: String, errorCode: Int) {
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val faceListener = object : FaceLandmarkerHelper.LandmarkerListener {
        override fun onResults(
            resultBundle: FaceLandmarkerHelper.ResultBundle
        ) {
            activity?.runOnUiThread {
                val newLandmark = resultBundle.result
                if (_fragmentCameraBinding != null) {
                    faceLandmarks.update {
                        it.withNewLandmark(
                            newLandmark,
                            resultBundle.timeStampFinish
                        )
                    }
                    // Pass necessary information to OverlayView for drawing on the canvas
                    fragmentCameraBinding.overlay.setFaceResults(
                        newLandmark,
                        resultBundle.inputImageHeight,
                        resultBundle.inputImageWidth
                    )
                    // Force a redraw
                    fragmentCameraBinding.overlay.invalidate()
                }
            }
        }

        override fun onEmpty() {
            fragmentCameraBinding.overlay.clearFace()
        }

        override fun onError(error: String, errorCode: Int) {
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

    }

    val handListener = object : HandLandmarkerHelper.LandmarkerListener {
        override fun onResults(
            resultBundle: HandLandmarkerHelper.ResultBundle
        ) {
            activity?.runOnUiThread {
                val newLandmark = resultBundle.results
                if (_fragmentCameraBinding != null) {
                    handLandmarks.update {
                        it.withNewLandmark(
                            newLandmark,
                            resultBundle.timeStampFinish
                        )
                    }

                    // Pass necessary information to OverlayView for drawing on the canvas
                    fragmentCameraBinding.overlay.setHandResults(
                        newLandmark,
                        resultBundle.inputImageHeight,
                        resultBundle.inputImageWidth,
                    )

                    // Force a redraw
                    fragmentCameraBinding.overlay.invalidate()
                }
            }
        }

        override fun onError(error: String, errorCode: Int) {
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
//            Navigation.findNavController(
//                requireActivity(), R.id.fragment_container
//            ).navigate(R.id.action_camera_to_permissions)
        }

        // Start the FaceLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if (combineLandmarkerHelper.isFaceClose()) {
                combineLandmarkerHelper.setupFaceLandmarker()
            }
            if (combineLandmarkerHelper.isHandClose()) {
                combineLandmarkerHelper.setupHandLandmarker()
            }
            if (combineLandmarkerHelper.isPoseClose()) {
                combineLandmarkerHelper.setupPoseLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::combineLandmarkerHelper.isInitialized) {
            viewModel.setMaxFaces(combineLandmarkerHelper.faceLandmarkerHelper.maxNumFaces)
            viewModel.setMinFaceDetectionConfidence(combineLandmarkerHelper.faceLandmarkerHelper.minFaceDetectionConfidence)
            viewModel.setMinFaceTrackingConfidence(combineLandmarkerHelper.faceLandmarkerHelper.minFaceTrackingConfidence)
            viewModel.setMinFacePresenceConfidence(combineLandmarkerHelper.faceLandmarkerHelper.minFacePresenceConfidence)

            viewModel.setMaxHands(combineLandmarkerHelper.handLandmarkerHelper.maxNumHands)
            viewModel.setMinHandDetectionConfidence(combineLandmarkerHelper.handLandmarkerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(combineLandmarkerHelper.handLandmarkerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(combineLandmarkerHelper.handLandmarkerHelper.minHandPresenceConfidence)

            viewModel.setMinPoseDetectionConfidence(combineLandmarkerHelper.poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(combineLandmarkerHelper.poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(combineLandmarkerHelper.poseLandmarkerHelper.minPosePresenceConfidence)

            backgroundExecutor.execute { combineLandmarkerHelper.clearLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }
        lifecycleScope.launch {
            combinedLandmarks.collect {
                //TODO maybe change to collect when both hands change
                it.doWhenAllUpdated {

                    println(
                        "Frame Combined: \n" +
                                "Combined Frame Hand: ${it.hand.frameNumber}: ${it.hand.finishTimeMilis} \n" +
                                "Combined Frame Face: ${it.face.frameNumber}: ${it.face.finishTimeMilis} \n" +
                                "Combined Frame Pose: ${it.pose.frameNumber}: ${it.pose.finishTimeMilis}"
                    )

                    updateLastSequence(it)
                    if (lastSequence.size == 30) {
                        predict(lastSequence)
                    }
                }
            }
        }

        // Create the FaceLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            val faceLandmarkerHelper = FaceLandmarkerHelper(
                context = requireContext(),
                //runningMode = RunningMode.LIVE_STREAM,
                minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                maxNumFaces = viewModel.currentMaxFaces,
                faceLandmarkerHelperListener = faceListener
            )
            val handLandmarkerHelper = HandLandmarkerHelper(
                context = requireContext(),
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                maxNumHands = viewModel.currentMaxHands,
                handLandmarkerHelperListener = handListener
            )
            val poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                poseLandmarkerHelperListener = poseListener
            )
            combineLandmarkerHelper =
                CombineLandmarkerHelper(
                    faceLandmarkerHelper,
                    handLandmarkerHelper,
                    poseLandmarkerHelper
                )
        }
    }

    val lastSequence = mutableListOf<List<Float>>()

    private fun updateLastSequence(landmarks: CombinedLandmarks) {
        val keypointArray = extractKeypoints(landmarks)
        println("TESTTT KEYPOINT ARRAY ${keypointArray.size} $keypointArray")

        lastSequence.add(keypointArray)
        if (lastSequence.size > 30) lastSequence.removeAt(0)

    }

    private fun extractKeypoints(landmarks: CombinedLandmarks): List<Float> {
        //TODO: CHANGE LOGIC OF OBSERVING SO IT DOESNT STOP HERE FOR THESE, NOW IT DOESNT STOP FOR NON EXISTING HANDS ONLY
        val pose = landmarks.pose.poseLandmarkerResult?.landmarks()?.flatten()?.toXYZVisibility()
            ?.flattenXYZV() ?: List(33 * 4) { 0f }
        val face =
            landmarks.face.faceLandmarkerResult?.faceLandmarks()?.flatten()?.dropIrises()?.toXYZ()
                ?.flattenXYZ()
                ?: List(468 * 3) { 0f }
        println("TESTTT KK pose $pose")
        println("TESTTT KK face $face")
        val handedness = landmarks.hand.handLandmarkerResult?.handedness()?.flatten()
        val rightHandIndex = handedness?.indexOfFirst {
            it.categoryName() == "Left"
        }
        val leftHandIndex = handedness?.indexOfFirst {
            it.categoryName() == "Right"
        }
        val rightHand = rightHandIndex?.let {
            landmarks.hand.handLandmarkerResult?.landmarks()?.getOrNull(rightHandIndex)?.toXYZ()
                ?.flattenXYZ()
        } ?: List(21 * 3) { 0f }
        val leftHand = leftHandIndex?.let {
            landmarks.hand.handLandmarkerResult?.landmarks()?.getOrNull(leftHandIndex)?.toXYZ()
                ?.flattenXYZ()
        } ?: List(21 * 3) { 0f }
        if (leftHand.size != 21 * 3) {
            println("WRONGGGG left hand ${leftHand.size}")
        }
        if (rightHand.size != 21 * 3) {
            println("WRONGGGG right hand ${rightHand.size}")
        }
        if (pose.size != 33 * 4) {
            println("WRONGGGG pose ${pose.size}")
        }
        if (face.size != 468 * 3) {
            println("WRONGGGG face ${face.size}")
        }
        println("TESTTT hand RIGHT $rightHand")
        println("TESTTT hand LEFT $leftHand")
        return pose + face + leftHand + rightHand
    }

    private fun CombinedLandmarks.doWhenAllUpdated(
        callback: () -> Unit
    ) {
        if (lastConsumedFrame.faceConsumed == face.frameNumber) return
        if (lastConsumedFrame.handConsumed == hand.frameNumber) return
        if (lastConsumedFrame.poseConsumed == pose.frameNumber) return
        lastConsumedFrame =
            ConsumedFrames(
                handConsumed = hand.frameNumber,
                faceConsumed = face.frameNumber,
                poseConsumed = pose.frameNumber
            )
        callback()
    }


    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectCombined(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectCombined(imageProxy: ImageProxy) {
        combineLandmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }
    val actions = listOf("hello", "thanks", "iloveyou")
    fun predict(sequence: List<List<Float>>) {
        val threshold = 0.5

        val floatArray = sequence.toFloatArray()
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 30, 1662), DataType.FLOAT32)
        inputFeature.loadArray(floatArray, intArrayOf(1, 30, 1662))
// Runs model inference and gets result.
        val outputs = model.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer
        val list = outputFeature.floatArray.toList()
        val maxIndex = list.argmax()
        maxIndex?.let {
            if (list[maxIndex] > threshold) {
                println("GUESS ${actions[maxIndex]}")
            }
        }
//        outputFeature.floatArray.forEach {
//            println("TESTTT KEY OUTPUTTTT ${it}")
//        }
// Releases model resources if no longer used.
        //model.close()
    }

}

fun <T : Comparable<T>> Iterable<T>.argmax(): Int? {
    return withIndex().maxByOrNull { it.value }?.index
}

private fun List<NormalizedLandmark>.dropIrises():List<NormalizedLandmark> {
    println("WRONGGGGGGGGG ${this.size}")
    val ren = take(468)
    println("WRONGGGGGGGGG ${ren.size}")
    return ren
}

private fun List<List<Float>>.toFloatArray(): FloatArray {
//    val array = map{
//        it.toTypedArray()
//    }.toTypedArray()
    println("JAAAKUB1 GLOB SIZE ${this.size}")
    this.forEach {
        println("JAAAKUB1 INSIDE SIZE ${it.size}")
    }

    val ndArray = toNDArray()
    println("JAAAKUB111 ${ndArray.shape[0]},${ndArray.shape[1]}")
    val expandedDims = ndArray.expandDims(0)
    println("JAAAKUB2 ${expandedDims.shape[0]},${expandedDims.shape[1]},${expandedDims.shape[2]}")
    val expanded2 = expandedDims[0]
    println("JAAAKUB3 ${expanded2.shape[0]},${expanded2.shape[1]}")
    return expanded2.toFloatArray()
    //flatten().toFloatArray()
}

private fun List<XYZKeypoints>.flattenXYZ(): List<Float> {
    return flatMap {
        listOf(it.x, it.y, it.z)
    }
}

private fun List<XYZVKeypoints>.flattenXYZV(): List<Float> {
    return flatMap {
        listOf(it.x, it.y, it.z, it.visibility)
    }
}

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

private fun List<NormalizedLandmark>.toXYZVisibility(): List<XYZVKeypoints> {
    return map {
        XYZVKeypoints(it.x(), it.y(), it.z(), it.visibility().get())
    }
}

private fun List<NormalizedLandmark>.toXYZ(): List<XYZKeypoints> {
    return map {
        XYZKeypoints(it.x(), it.y(), it.z())
    }
}
//val interpreter = Interpreter(loadModelFile())
//

//fun predict(sequence:List<List<Float>>){
//    //val results = Array(3){ 0f }
//    //interpreter.run(sequence,results)
//
//    println(results)
//}
