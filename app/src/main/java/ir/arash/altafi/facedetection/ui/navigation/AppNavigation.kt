package ir.arash.altafi.facedetection.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import ir.arash.altafi.facedetection.ui.page.BeautyDetectorScreen
import ir.arash.altafi.facedetection.ui.page.FaceFilterScreen
import ir.arash.altafi.facedetection.ui.page.HomeScreen
import ir.arash.altafi.facedetection.ui.page.SplashScreen
import ir.arash.altafi.facedetection.ui.theme.FaceDetectionTheme

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    FaceDetectionTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.Splash,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                dynamicComposable<Route.Splash>(
                    transitionType = TransitionType.NONE
                ) { args, backStackEntry ->
                    SplashScreen(
                        navController = navController,
                    )
                }
                dynamicComposable<Route.Home>(
                    transitionType = TransitionType.NONE
                ) { args, backStackEntry ->
                    HomeScreen(
                        navController = navController,
                    )
                }
                dynamicComposable<Route.FaceFilter>(
                    transitionType = TransitionType.NONE
                ) { args, backStackEntry ->
                    FaceFilterScreen()
                }
                dynamicComposable<Route.BeautyDetector>(
                    transitionType = TransitionType.NONE
                ) { args, backStackEntry ->
                    BeautyDetectorScreen()
                }
            }
        }
    }
}