package ir.arash.altafi.facedetection

import com.google.mlkit.vision.face.Face

fun detectEmotion(face: Face): String {
    val smile = face.smilingProbability ?: -1f
    val leftEye = face.leftEyeOpenProbability ?: -1f
    val rightEye = face.rightEyeOpenProbability ?: -1f

    return when {
        smile > 0.7f -> "Happy"
        leftEye < 0.3f && rightEye < 0.3f -> "Sleepy"
        leftEye > 0.7f && rightEye > 0.7f -> "Surprised"
        smile < 0.2f -> "Angry"
        else -> "Neutral"
    }
}
