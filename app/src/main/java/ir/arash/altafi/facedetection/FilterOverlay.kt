package ir.arash.altafi.facedetection

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.hypot

@SuppressLint("RestrictedApi")
@OptIn(TransformExperimental::class)
@Composable
fun FilterOverlay(
    faces: List<Face>,
    selectedFilter: FaceFilter,
    previewView: PreviewView?
) {
    if (selectedFilter == FaceFilter.NONE || previewView == null) return

    val filterBitmap = selectedFilter.resId?.let { ImageBitmap.imageResource(it) }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        if (filterBitmap == null) return@Canvas

        val transform = previewView.outputTransform ?: return@Canvas
        val matrix = transform.matrix

        fun mapPoint(x: Float, y: Float): Pair<Float, Float> {
            val pts = floatArrayOf(x, y)
            matrix.mapPoints(pts)
            return pts[0] to pts[1]
        }

        faces.forEach { face ->
            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return@forEach
            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return@forEach

            // Map MLKit -> View coordinates
            val (lx, ly) = mapPoint(leftEye.x, leftEye.y)
            val (rx, ry) = mapPoint(rightEye.x, rightEye.y)

            val centerX = (lx + rx) / 2f
            val centerY = (ly + ry) / 2f

            val eyeDistance = hypot((rx - lx), (ry - ly))
            val scale = (eyeDistance / filterBitmap.width) * 2.2f

            val dstWidth = (filterBitmap.width * scale).toInt()
            val dstHeight = (filterBitmap.height * scale).toInt()

            val offsetX = centerX - dstWidth / 2
            val offsetY = centerY - dstHeight / 2

            drawImage(
                image = filterBitmap,
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(dstWidth, dstHeight)
            )
        }
    }
}