@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")

package ir.arash.altafi.facedetection.ui.component

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraWithFilters(selectedFilter: FaceFilter) {
    val context = LocalContext.current

    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()

        FaceDetection.getClient(options)
    }

    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }
    var facesDetected by remember { mutableStateOf<List<Face>>(emptyList()) }
    val facesState = remember { mutableStateOf<List<Face>>(emptyList()) }

    // track image sizes / rotation from analyzer
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var imageRotation by remember { mutableIntStateOf(0) }

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
                    val selector = CameraSelector.DEFAULT_FRONT_CAMERA

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val mediaImage = imageProxy.image ?: return@setAnalyzer

                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val input = InputImage.fromMediaImage(mediaImage, rotation)

                        faceDetector.process(input)
                            .addOnSuccessListener { faces ->
                                facesDetected = faces
                                facesState.value = faces
                            }
                            .addOnFailureListener { it.printStackTrace() }
                            .addOnCompleteListener { imageProxy.close() }
                    }

                    preview.surfaceProvider = previewView.surfaceProvider
                    cameraProvider.bindToLifecycle(
                        context as LifecycleOwner,
                        selector,
                        preview,
                        analysis
                    )

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
            val face = facesState.value.firstOrNull()
            if (face != null) {
                Text(
                    text = "Emotion: ${detectEmotion(face)}",
                    color = Color.White,
                )
            } else {
                Text(
                    text = "No face",
                    color = Color.White
                )
            }
        }
    }
}