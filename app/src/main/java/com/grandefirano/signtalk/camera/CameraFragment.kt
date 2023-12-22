package com.grandefirano.signtalk.camera

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.grandefirano.signtalk.PermissionsFragment
import com.grandefirano.signtalk.databinding.FragmentCameraBinding
import com.grandefirano.signtalk.recognition.RecognizedSentences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CameraFragment : Fragment() {

    companion object {
        private const val TAG = "Face Landmarker"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private val viewModel: CameraViewModel by activityViewModels()

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
//            Navigation.findNavController(
//                requireActivity(), R.id.fragment_container
//            ).navigate(R.id.action_camera_to_permissions)
        }
        viewModel.setupLandmarkerIfClosed()
    }

    override fun onPause() {
        super.onPause()
        viewModel.cleanLandmarker()
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        viewModel.shutDownExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)
        fragmentCameraBinding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    RecognizedSentences()
                }
            }
        }
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize our background executor

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }
        lifecycleScope.launch {
            launch {
                println("NOWYY ONVIEW CREATED")
                //TODO:switch here
                viewModel.startActionRecognition()
                //viewModel.startStaticRecognition()
            }
            launch {
                setHandDrawing()
            }
            launch {
                setPoseDrawing()
            }
        }
    }

    private suspend fun setHandDrawing() {
        viewModel.handLandmarks.collect {
            fragmentCameraBinding.overlay.setHandResults(it.handResultBundle)
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    private suspend fun setPoseDrawing() {
        viewModel.poseLandmarks.collect {
            fragmentCameraBinding.overlay.setPoseResults(it.poseResultBundle)
            fragmentCameraBinding.overlay.invalidate()
        }
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

    private var counterXX = 0
    private var counterXX2222 = 0
    private var counterXXSaved = 0L

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()


        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work


        val imageAnalyzerBuilder =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        val ext: Camera2Interop.Extender<*> = Camera2Interop.Extender(imageAnalyzerBuilder)
        ext.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_OFF
        )
        ext.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
            Range<Int>(15, 15)
        )
        //val imageAnalysis = builder.build()
        imageAnalyzer = imageAnalyzerBuilder.build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(viewModel.backgroundExecutor) { image ->
                    //todo here
                    val finishTimeMs = SystemClock.uptimeMillis()
                    //TODO test if frames are not lost here
                    val newXX = finishTimeMs / 1000
                    if (counterXXSaved == newXX) {
                        counterXX++
                    } else {
                        println("FFFF TIME IN SECCCC $counterXXSaved  frames: $counterXX")
                        counterXX = 0
                        counterXXSaved = newXX
                    }
                    //if(counterXX2222 == 1) {
                    viewModel.detectCombined(image)
                    //   counterXX2222=0
                    //}
                    // counterXX2222++
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }
}
