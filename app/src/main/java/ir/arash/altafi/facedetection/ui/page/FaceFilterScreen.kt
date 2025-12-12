package ir.arash.altafi.facedetection.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.arash.altafi.facedetection.ui.component.CameraWithFilters
import ir.arash.altafi.facedetection.ui.component.FaceFilter
import ir.arash.altafi.facedetection.ui.component.FilterButton

@Composable
fun FaceFilterScreen() {
    var selectedFilter by remember { mutableStateOf(FaceFilter.NONE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Camera + Filters
        Box(modifier = Modifier.weight(1f)) {
            CameraWithFilters(selectedFilter)
        }

        // Filter Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            FilterButton("None") { selectedFilter = FaceFilter.NONE }
            FilterButton("Glasses") { selectedFilter = FaceFilter.GLASSES }
//            FilterButton("SunGlasses") { selectedFilter = FaceFilter.SUNGLASSES }
//            FilterButton("Hat") { selectedFilter = FaceFilter.HAT }
//            FilterButton("Crown") { selectedFilter = FaceFilter.CROWN }
//            FilterButton("Cap") { selectedFilter = FaceFilter.CAP }
        }
    }
}
