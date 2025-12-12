package ir.arash.altafi.facedetection.ui.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.arash.altafi.facedetection.ui.navigation.Route

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                navController.navigate(Route.BeautyDetector)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BeautyDetector")


        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                navController.navigate(Route.FaceFilter)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("FaceFilter")
        }
    }
}