package ir.arash.altafi.facedetection

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

@Composable
fun FilterOverlay(faces: List<Face>, selectedFilter: FaceFilter) {
    if (selectedFilter == FaceFilter.NONE) return

    val filterBitmap = selectedFilter.resId?.let { ImageBitmap.imageResource(it) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (filterBitmap == null) return@Canvas

        faces.forEach { face ->
            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

            if (leftEye == null || rightEye == null) return@forEach

            val lx = leftEye.x
            val ly = leftEye.y
            val rx = rightEye.x
            val ry = rightEye.y

            val eyeCenterX = (lx + rx) / 2f
            val eyeCenterY = (ly + ry) / 2f
            val eyeDistance = hypot((rx - lx), (ry - ly))

            // Scale filter relative to eye distance
            val scale = eyeDistance / filterBitmap.width * 2f

            val dstWidth = (filterBitmap.width * scale).toInt()
            val dstHeight = (filterBitmap.height * scale).toInt()

            val offsetX = (eyeCenterX - dstWidth / 2)
            val offsetY = (eyeCenterY - dstHeight / 2)

            val mirroredX = size.width - offsetX - dstWidth

            drawImage(
                image = filterBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(filterBitmap.width, filterBitmap.height),
                dstOffset = IntOffset(mirroredX.toInt(), offsetY.toInt()),
                dstSize = IntSize(dstWidth, dstHeight)
            )
        }
    }
}