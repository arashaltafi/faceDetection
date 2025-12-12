package ir.arash.altafi.facedetection.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {

    val route: String

    @Serializable
    data object Splash : Route {
        override val route: String = ".navigation.Route.SplashScreen"
    }

    @Serializable
    data object Home : Route {
        override val route: String = ".navigation.Route.HomeScreen"
    }

    @Serializable
    data object BeautyDetector : Route {
        override val route: String = ".navigation.Route.BeautyDetectorScreen"
    }

    @Serializable
    data object FaceFilter : Route {
        override val route: String = ".navigation.Route.FaceFilterScreen"
    }

}