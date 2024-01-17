package com.grandefirano.signtalk.camera

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
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
import androidx.navigation.Navigation
import com.grandefirano.signtalk.PermissionsFragment
import com.grandefirano.signtalk.R
import com.grandefirano.signtalk.databinding.FragmentCameraBinding
import com.grandefirano.signtalk.recognition.RecognitionPanel
import com.grandefirano.signtalk.recognition.RecognitionViewModel
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

    private val viewModel: RecognitionViewModel by activityViewModels()

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.fragment_container
            ).navigate(R.id.action_camera_to_permissions)
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
                    RecognitionPanel()
                }
            }
        }
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }
        setUpOverlayDrawing()
    }

    private fun setUpOverlayDrawing() {
        lifecycleScope.launch {
            launch {

                viewModel.handLandmarks.collect {
                    fragmentCameraBinding.overlay.setHandResults(it.handResultBundle)
                    fragmentCameraBinding.overlay.invalidate()
                }
            }
            launch {
                viewModel.poseLandmarks.collect {
                    fragmentCameraBinding.overlay.setPoseResults(it.poseResultBundle)
                    fragmentCameraBinding.overlay.invalidate()
                }
            }
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

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
        imageAnalyzer = imageAnalyzerBuilder.build()
            .also {
                it.setAnalyzer(viewModel.backgroundExecutor) { image ->
                    viewModel.detectCombinedLandmarks(image)
                }
            }
        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
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
