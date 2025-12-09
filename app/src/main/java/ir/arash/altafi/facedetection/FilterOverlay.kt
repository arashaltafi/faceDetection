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
    previewView: PreviewView?,
    imgW: Int,
    imgH: Int,
    rotation: Int,
) {
    if (selectedFilter == FaceFilter.NONE || previewView == null) return
    val filterBitmap = selectedFilter.resId?.let { ImageBitmap.imageResource(it) }

    val viewW = previewView.width.toFloat()
    val viewH = previewView.height.toFloat()
    if (viewW == 0f || viewH == 0f || imgW == 0 || imgH == 0) return

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        if (filterBitmap == null) return@Canvas

        fun mapPoint(x: Float, y: Float): Pair<Float, Float> {
            var px = x
            var py = y

            when (rotation) {
                0 -> {} // no change
                90 -> {
                    px = y
                    py = imgW - x
                }
                180 -> {
                    px = imgW - x
                    py = imgH - y
                }
                270 -> {
                    px = imgH - y
                    py = x
                }
            }

            // Front camera mirror
            px = imgW - px

            val scaleX = viewW / imgW
            val scaleY = viewH / imgH

            return px * scaleX to py * scaleY
        }

        faces.forEach { face ->
            val left = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return@forEach
            val right = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return@forEach

            val (lx, ly) = mapPoint(left.x, left.y)
            val (rx, ry) = mapPoint(right.x, right.y)

            val cx = (lx + rx) / 2f
            val cy = (ly + ry) / 2f

            val eyeDistance = hypot((rx - lx), (ry - ly))
            val scale = eyeDistance / filterBitmap.width * 2.2f

            val dstW = (filterBitmap.width * scale).toInt()
            val dstH = (filterBitmap.height * scale).toInt()

            val offX = cx - dstW / 2
            val offY = cy - dstH / 2

            drawImage(
                image = filterBitmap,
                dstOffset = IntOffset(offX.toInt(), offY.toInt()),
                dstSize = IntSize(dstW, dstH)
            )
        }
    }
}