package ir.arash.altafi.facedetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import ir.arash.altafi.facedetection.ui.theme.FaceDetectionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FaceDetectionScreen(innerPadding)
                }
            }
        }
    }
}

@Composable
fun FaceDetectionScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    //---------------------- Permission ---------------------------------
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required")
        }
        return
    }
    //-------------------------------------------------------------------

    val facesState = remember { mutableStateOf<List<Face>>(emptyList()) }
    val previewView = remember { PreviewView(context) }

    Box(Modifier.fillMaxSize()) {

        // CAMERA PREVIEW
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            startCamera(view, lifecycleOwner, facesState)
        }

        // FACE OVERLAY CANVAS
        FaceOverlay(
            faces = facesState.value,
            previewView = previewView
        )

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

@SuppressLint("DefaultLocale")
fun formatProb(p: Float?): String =
    if (p == null || p.isNaN()) "N/A" else String.format("%.2f", p)

private fun startCamera(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    facesState: MutableState<List<Face>>
) {
    val context = previewView.context
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .enableTracking()
            .build()

        val detector = FaceDetection.getClient(detectorOptions)

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            processImageProxy(detector, imageProxy, facesState)
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            analyzer
        )

    }, ContextCompat.getMainExecutor(context))
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    detector: FaceDetector,
    imageProxy: ImageProxy,
    facesState: MutableState<List<Face>>
) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    detector.process(image)
        .addOnSuccessListener { faces ->
            facesState.value = faces
        }
        .addOnFailureListener { it.printStackTrace() }
        .addOnCompleteListener { imageProxy.close() }
}

@Composable
fun FaceOverlay(
    faces: List<Face>,
    previewView: PreviewView
) {
    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val previewWidth = previewView.width.toFloat()
        val previewHeight = previewView.height.toFloat()

        if (previewWidth == 0f || previewHeight == 0f) return@Canvas

        for (face in faces) {
            val box = face.boundingBox

            // Convert to Canvas coordinate
            val left = box.left / previewWidth * size.width
            val top = box.top / previewHeight * size.height
            val right = box.right / previewWidth * size.width
            val bottom = box.bottom / previewHeight * size.height

            val centerX = (left + right) / 2
            val centerY = (top + bottom) / 2
            val radius = ((right - left).coerceAtLeast(bottom - top)) / 2

            drawCircle(
                color = androidx.compose.ui.graphics.Color.Cyan,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 6.dp.toPx())
            )
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