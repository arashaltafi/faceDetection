package ir.arash.altafi.facedetection.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlin.text.indices
import kotlin.text.take

@Composable
fun TypewriterText(
    modifier: Modifier,
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.White,
    delay: Long = 200L
) {
    var visibleText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        visibleText = ""
        for (i in text.indices) {
            visibleText = text.take(i + 1)
            delay(delay)
        }
    }

    Text(
        modifier = modifier,
        text = visibleText,
        color = color,
        fontWeight = fontWeight,
    )
}