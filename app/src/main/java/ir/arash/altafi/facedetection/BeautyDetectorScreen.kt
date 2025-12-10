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
import androidx.compose.ui.res.stringResource
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
    var goldenRatio by remember { mutableIntStateOf(0) }
    var symmetry by remember { mutableIntStateOf(0) }
    var proportion by remember { mutableIntStateOf(0) }
    var totalScore by remember { mutableIntStateOf(0) }
    var faceDetected by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        // CAMERA PREVIEW
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        val preview = Preview.Builder().build()
                            .also { it.surfaceProvider = previewView.surfaceProvider }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    Executors.newSingleThreadExecutor(),
                                    FaceAnalyzer(
                                        onResults = { g, s, p, t, detected ->
                                            goldenRatio = g
                                            symmetry = s
                                            proportion = p
                                            totalScore = t
                                            faceDetected = detected
                                        }
                                    )
                                )
                            }

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

        Column(modifier = Modifier.padding(16.dp)) {
            if (!faceDetected) {
                Text(
                    text = stringResource(R.string.no_face_detected),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = stringResource(R.string.golden_ratio) + " " + goldenRatio + "%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.facial_symmetry) + " " + symmetry + "%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.proportion_score) + " " + proportion + "%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.total_beauty_score) + " " + totalScore + "%",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

class FaceAnalyzer(
    private val onResults: (
        Int,   // Golden Ratio
        Int,   // Symmetry
        Int,   // Proportion
        Int,   // Total Beauty
        Boolean  // detected
    ) -> Unit
) : ImageAnalysis.Analyzer {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]

                    val golden = calcGoldenRatio(face)
                    val symmetry = calcSymmetry(face)
                    val proportion = calcProportion(face)

                    val total = ((golden + symmetry + proportion) / 3f).toInt().coerceIn(0, 100)

                    onResults(golden, symmetry, proportion, total, true)
                } else {
                    onResults(0, 0, 0, 0, false)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
            .addOnFailureListener {
                onResults(0, 0, 0, 0, false)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    // Helper distance function
    private fun dist(a: FaceLandmark?, b: FaceLandmark?): Float {
        if (a == null || b == null) return 0f
        val dx = a.position.x - b.position.x
        val dy = a.position.y - b.position.y
        return sqrt(dx * dx + dy * dy)
    }

    // 1. Golden Ratio Calculation
    private fun calcGoldenRatio(face: Face): Int {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

        val eyeDistance = dist(leftEye, rightEye)
        val mouthWidth = dist(mouthLeft, mouthRight)

        val faceWidth = face.boundingBox.width().toFloat()

        val eyeRatio = eyeDistance / faceWidth
        val mouthRatio = mouthWidth / (eyeDistance + 1f)

        val idealEye = 0.46f
        val idealMouth = 1.5f

        val error =
            abs(eyeRatio - idealEye) * 100 + abs(mouthRatio - idealMouth) * 20

        return max(0, (100 - error).toInt())
    }

    // 2. Facial Symmetry Calculation
    private fun calcSymmetry(face: Face): Int {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)

        if (leftEye == null || rightEye == null) return 0

        val faceCenterX = face.boundingBox.centerX().toFloat()

        val leftDist = abs(faceCenterX - leftEye.position.x)
        val rightDist = abs(rightEye.position.x - faceCenterX)

        val diff = abs(leftDist - rightDist)

        return max(0, (100 - diff * 2).toInt())
    }

    // 3. Proportion of Eye–Nose–Mouth
    private fun calcProportion(face: Face): Int {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)

        if (leftEye == null || rightEye == null || nose == null || mouthBottom == null)
            return 0

        // Correct eye center
        val eyeCenterY = (leftEye.position.y + rightEye.position.y) / 2f

        // Distances
        val eyeToNose = abs(nose.position.y - eyeCenterY)
        val noseToMouth = abs(mouthBottom.position.y - nose.position.y)

        if (eyeToNose <= 0f || noseToMouth <= 0f) return 0

        // Real-world facial ideal ratio ~ 1.0
        val ratio = eyeToNose / noseToMouth
        val idealRatio = 1.0f

        // Difference percentage
        val diff = abs(ratio - idealRatio)

        // Convert to score
        val score = (100 - diff * 40f).toInt() // controlled decay

        return score.coerceIn(0, 100)
    }
}