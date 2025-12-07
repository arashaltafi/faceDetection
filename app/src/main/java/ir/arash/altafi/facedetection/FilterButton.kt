package ir.arash.altafi.facedetection

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterButton(text: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(4.dp),
        onClick = onClick
    ) {
        Text(text)
    }
}
