@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")

package ir.arash.altafi.facedetection

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

    var facesDetected by remember { mutableStateOf<List<Face>>(emptyList()) }
    val facesState = remember { mutableStateOf<List<Face>>(emptyList()) }

    Box(Modifier.fillMaxSize()) {
        // CAMERA PREVIEW
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)

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

        // FACE OVERLAY CANVAS
        FilterOverlay(facesDetected, selectedFilter)

        // First face emotion
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            val face = facesState.value.firstOrNull()
            if (face != null) {
                Text(
                    text = "Emotion: ${detectEmotion(face)}",
                    color = androidx.compose.ui.graphics.Color.White,
                )
            } else {
                Text(
                    text = "No face",
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}

fun detectEmotion(
    face: Face
): String {
    val smile = face.smilingProbability ?: -1f
    val leftEye = face.leftEyeOpenProbability ?: -1f
    val rightEye = face.rightEyeOpenProbability ?: -1f

    return when {
        smile > 0.7f -> "Happy 😊"
        leftEye < 0.3f && rightEye < 0.3f -> "Sleepy 😴"
        leftEye > 0.7f && rightEye > 0.7f -> "Surprised 😮"
        smile < 0.2f -> "Angry 😠"
        else -> "Neutral 😐"
    }
}