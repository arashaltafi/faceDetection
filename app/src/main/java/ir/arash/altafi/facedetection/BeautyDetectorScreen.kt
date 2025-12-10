package ir.arash.altafi.facedetection

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.Executors
import kotlin.math.*

@Composable
fun BeautyDetectorScreen(innerPadding: PaddingValues) {
    var beautyPercentage by remember { mutableIntStateOf(0) }
    var faceDetected by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)

                    val cameraProviderFuture =
                        ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build()
                            .also { it.surfaceProvider = previewView.surfaceProvider }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    Executors.newSingleThreadExecutor(),
                                    FaceAnalyzer(
                                        onBeautyScore = {
                                            beautyPercentage = it
                                        },
                                        onFaceDetected = { detected ->
                                            faceDetected = detected
                                        }
                                    )
                                )
                            }

                        val cameraSelector =
                            CameraSelector.DEFAULT_FRONT_CAMERA

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )

                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        }

        Button(
            onClick = { /* No action needed – real-time value updates */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Calculate Beauty")
        }

        Text(
            text = if (faceDetected)
                "Your facial beauty percentage: $beautyPercentage%"
            else
                "No face detected",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(16.dp)
        )
    }
}

class FaceAnalyzer(
    private val onBeautyScore: (Int) -> Unit,
    private val onFaceDetected: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector =
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        val inputImage =
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    onFaceDetected(true)
                    val score = calculateBeauty(faces[0])
                    onBeautyScore(score)
                } else {
                    onFaceDetected(false)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun calculateBeauty(face: Face): Int {

        fun dist(l1: FaceLandmark?, l2: FaceLandmark?): Float {
            if (l1 == null || l2 == null) return 0f
            val dx = l1.position.x - l2.position.x
            val dy = l1.position.y - l2.position.y
            return sqrt(dx * dx + dy * dy)
        }

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

        val eyeDistance = dist(leftEye, rightEye)
        val eyeNose = dist(nose, leftEye) + dist(nose, rightEye)
        val mouthWidth = dist(mouthLeft, mouthRight)

        // Simple demo formula
        var score = (eyeDistance + mouthWidth) / (eyeNose + 1f) * 30f

        score = min(100f, max(0f, score))

        return score.toInt()
    }
}