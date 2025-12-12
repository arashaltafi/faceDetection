package ir.arash.altafi.facedetection.ui.page

import androidx.annotation.OptIn
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import ir.arash.altafi.facedetection.R
import java.util.concurrent.Executors
import kotlin.math.*

@Composable
fun BeautyDetectorScreen() {
    var goldenRatio by remember { mutableIntStateOf(0) }
    var symmetry by remember { mutableIntStateOf(0) }
    var proportion by remember { mutableIntStateOf(0) }
    var totalScore by remember { mutableIntStateOf(0) }
    var faceDetected by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (!faceDetected) {
                    Text(
                        text = stringResource(R.string.no_face_detected),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = "${stringResource(R.string.golden_ratio)} %$goldenRatio",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "${stringResource(R.string.facial_symmetry)} %$symmetry",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${stringResource(R.string.proportion_score)} %$proportion",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${stringResource(R.string.total_beauty_score)} %$totalScore",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
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

    @OptIn(ExperimentalGetImage::class)
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
    val lEye = face.getLandmark(FaceLandmark.LEFT_EYE)
    val rEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
    val mLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
    val mRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
    val nose = face.getLandmark(FaceLandmark.NOSE_BASE)

    if (lEye == null || rEye == null || mLeft == null || mRight == null || nose == null)
        return 0

    val faceW = face.boundingBox.width().toFloat().coerceAtLeast(1f)
    val faceH = face.boundingBox.height().toFloat().coerceAtLeast(1f)

    val eyeDist = dist(lEye, rEye)
    val mouthW = dist(mLeft, mRight)
    val eyeCenterY = (lEye.position.y + rEye.position.y) / 2f
    val noseToEye = abs(nose.position.y - eyeCenterY)
    val noseToChin = abs(face.boundingBox.bottom - nose.position.y)

    // Ratios
    val r1 = faceH / faceW           // ideal ~1.5
    val r2 = eyeDist / faceW         // ideal ~0.46
    val r3 = mouthW / eyeDist        // ideal ~1.5
    val r4 = noseToEye / faceH       // ideal ~0.33
    val r5 = noseToChin / faceH      // ideal ~0.25

    fun err(a: Float, ideal: Float) = abs(a - ideal) / ideal

    val e = (
            err(r1, 1.5f) * 0.25f +
                    err(r2, 0.46f) * 0.25f +
                    err(r3, 1.5f) * 0.20f +
                    err(r4, 0.33f) * 0.15f +
                    err(r5, 0.25f) * 0.15f
            )

    val score = (100 - e * 120).coerceIn(0f, 100f)

    return score.roundToInt()
}

// 2. Facial Symmetry Calculation
private fun calcSymmetry(face: Face): Int {
    val cx = face.boundingBox.centerX().toFloat()
    val fw = face.boundingBox.width().toFloat().coerceAtLeast(1f)

    fun dx(l: FaceLandmark?, r: FaceLandmark?): Float {
        if (l == null || r == null) return 0.5f
        return abs(abs(cx - l.position.x) - abs(cx - r.position.x)) / fw
    }

    val eEye = dx(
        face.getLandmark(FaceLandmark.LEFT_EYE),
        face.getLandmark(FaceLandmark.RIGHT_EYE)
    )
    val eCheek = dx(
        face.getLandmark(FaceLandmark.LEFT_CHEEK),
        face.getLandmark(FaceLandmark.RIGHT_CHEEK)
    )
    val eMouth = dx(
        face.getLandmark(FaceLandmark.MOUTH_LEFT),
        face.getLandmark(FaceLandmark.MOUTH_RIGHT)
    )

    val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
    val noseOffset = if (nose != null) abs(cx - nose.position.x) / fw else 0.1f

    val totalErr =
        eEye * 0.40f +
                eCheek * 0.30f +
                eMouth * 0.20f +
                noseOffset * 0.10f

    val score = (100 - totalErr * 150).coerceIn(0f, 100f)

    return score.roundToInt()
}

// 3. Proportion of Eye–Nose–Mouth
private fun calcProportion(face: Face): Int {
    val lEye = face.getLandmark(FaceLandmark.LEFT_EYE)
    val rEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
    val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
    val mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
    val mLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
    val mRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

    if (lEye == null || rEye == null || nose == null || mouth == null || mLeft == null || mRight == null)
        return 0

    val faceH = face.boundingBox.height().toFloat().coerceAtLeast(1f)
    val faceW = face.boundingBox.width().toFloat().coerceAtLeast(1f)

    val eyeCenterY = (lEye.position.y + rEye.position.y) / 2f

    val eyeToNose = abs(nose.position.y - eyeCenterY)
    val noseToMouth = abs(mouth.position.y - nose.position.y)
    val noseToChin = abs(face.boundingBox.bottom - nose.position.y)

    val eyeDist = dist(lEye, rEye)
    val mouthW = dist(mLeft, mRight)

    val r1 = eyeToNose / noseToMouth       // ideal ~1
    val r2 = eyeDist / faceW               // ideal ~0.46
    val r3 = mouthW / faceW                // ideal ~0.40
    val r4 = noseToChin / faceH            // ideal ~0.25

    fun err(a: Float, ideal: Float) = abs(a - ideal) / ideal

    val e = (
            err(r1, 1f) * 0.40f +
                    err(r2, 0.46f) * 0.30f +
                    err(r3, 0.40f) * 0.20f +
                    err(r4, 0.25f) * 0.10f
            )

    val score = (100 - e * 130).coerceIn(0f, 100f)

    return score.roundToInt()
}