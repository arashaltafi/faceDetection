package ir.arash.altafi.facedetection

import android.annotation.SuppressLint
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraWithFilters(selectedFilter: FaceFilter) {
    val context = LocalContext.current

    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }
    var facesDetected by remember { mutableStateOf<List<Face>>(emptyList()) }

    // track image sizes / rotation from analyzer
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }
    var imageRotation by remember { mutableStateOf(0) }

    // choose front camera
    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
        )
    }

    Box(Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewViewState.value = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        // Save image info (width/height/rotation) for overlay mapping
                        imageWidth = mediaImage.width
                        imageHeight = mediaImage.height
                        imageRotation = imageProxy.imageInfo.rotationDegrees

                        val inputImage = InputImage.fromMediaImage(mediaImage, imageRotation)
                        faceDetector.process(inputImage)
                            .addOnSuccessListener { faces ->
                                facesDetected = faces
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as LifecycleOwner,
                            cameraSelector,
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // overlay: pass imageWidth/imageHeight/rotation
        FilterOverlay(
            faces = facesDetected,
            selectedFilter = selectedFilter,
            previewView = previewViewState.value,
            imgW = imageWidth,
            imgH = imageHeight,
            rotation = imageRotation,
            verticalNudgeRatio = 0.06f,
            horizontalNudgeRatio = 0.00f,
            isFrontCamera = true
        )

        // Simple status text
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            val face = facesDetected.firstOrNull()
            Text(
                text = face?.let { detectEmotion(it) } ?: "No face",
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}