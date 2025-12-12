package ir.arash.altafi.facedetection.ui.page

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.arash.altafi.facedetection.ui.navigation.Route

@Composable
fun HomeScreen(navController: NavHostController) {

    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    navController.navigate(Route.BeautyDetector)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BeautyDetector")


        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    navController.navigate(Route.FaceFilter)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("FaceFilter")
        }
    }
}