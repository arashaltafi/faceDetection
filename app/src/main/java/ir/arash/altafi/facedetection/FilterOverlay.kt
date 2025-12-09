package ir.arash.altafi.facedetection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun FilterOverlay(
    faces: List<Face>,
    selectedFilter: FaceFilter,
    previewView: PreviewView?,
    imgW: Int,
    imgH: Int,
    rotation: Int,
    verticalNudgeRatio: Float = 0.06f,
    horizontalNudgeRatio: Float = 0.00f,
    isFrontCamera: Boolean = true
) {
    if (previewView == null || selectedFilter == FaceFilter.NONE) return
    val resId = selectedFilter.resId ?: return
    val filterBitmap: ImageBitmap = ImageBitmap.imageResource(resId)

    val viewW = previewView.width.toFloat()
    val viewH = previewView.height.toFloat()
    if (viewW == 0f || viewH == 0f) return
    if (imgW == 0 || imgH == 0) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        fun rotatePoint(
            x: Float,
            y: Float,
            rotation: Int,
            srcW: Int,
            srcH: Int
        ): Pair<Float, Float> {
            return when (rotation) {
                0 -> x to y
                90 -> y to (srcW - x)
                180 -> (srcW - x) to (srcH - y)
                270 -> (srcH - y) to x
                else -> x to y
            }
        }

        // rotated image logical size
        val (imgRotW, imgRotH) = when (rotation) {
            90, 270 -> imgH to imgW
            else -> imgW to imgH
        }

        // center-crop scale (PreviewView commonly uses center-crop / fill)
        val scale = max(viewW / imgRotW.toFloat(), viewH / imgRotH.toFloat())
        val scaledImgW = imgRotW * scale
        val scaledImgH = imgRotH * scale
        val offsetX = (viewW - scaledImgW) / 2f
        val offsetY = (viewH - scaledImgH) / 2f

        // Determine if we should mirror X:
        // Many apps set previewView.scaleX = -1f to mirror the preview for front camera.
        // If previewView.scaleX < 0 -> preview already mirrored, so DON'T mirror again.
        val previewAlreadyMirrored = previewView.scaleX < 0f
        val shouldApplyMirror = isFrontCamera && !previewAlreadyMirrored

        fun mapToView(x: Float, y: Float): Pair<Float, Float> {
            // rotate MLKit coordinate into rotated image space
            var (rx, ry) = rotatePoint(x, y, rotation, imgW, imgH)

            // apply mirror only if needed
            if (shouldApplyMirror) {
                rx = imgRotW.toFloat() - rx
            }

            // scale & translate to view coordinates
            val vx = rx * scale + offsetX
            val vy = ry * scale + offsetY
            return vx to vy
        }

        faces.forEach { face ->
            val left = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return@forEach
            val right = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return@forEach

            val (lx, ly) = mapToView(left.x, left.y)
            val (rx, ry) = mapToView(right.x, right.y)

            val cx = (lx + rx) / 2f
            val cy = (ly + ry) / 2f

            val eyeDistance = hypot(rx - lx, ry - ly)
            // scale factor — tweak multiplier if glasses look too large/small
            val bitScale = (eyeDistance / filterBitmap.width) * 2.2f

            val dstW = (filterBitmap.width * bitScale).toInt()
            val dstH = (filterBitmap.height * bitScale).toInt()

            // apply small nudges to center the glasses better on eyes:
            val offX = (cx - dstW / 2f + dstW * horizontalNudgeRatio).toInt()
            val offY = (cy - dstH / 2f + dstH * verticalNudgeRatio).toInt()

            drawImage(
                image = filterBitmap,
                dstOffset = IntOffset(offX, offY),
                dstSize = IntSize(dstW, dstH)
            )
        }
    }
}