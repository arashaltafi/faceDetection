package ir.arash.altafi.facedetection.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.arash.altafi.facedetection.R
import ir.arash.altafi.facedetection.ui.component.TypewriterText
import ir.arash.altafi.facedetection.ui.navigation.Route
import ir.arash.altafi.facedetection.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
) {
    val context = LocalContext.current

    val versionName =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"

    LaunchedEffect(Unit) {
        delay(3000)

        try {
            navController.navigate(Route.Home) {
                popUpTo(Route.Splash) {
                    saveState = true
                    inclusive = true
                }
                launchSingleTop = true
                restoreState = true
            }
        } catch (_: Exception) {
            navController.navigate(Route.Home) {
                popUpTo(Route.Splash) {
                    saveState = true
                    inclusive = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val gradientColors = if (isSystemInDarkTheme()) {
        listOf(Blue300, Blue400, Blue500)
    } else {
        listOf(Blue700, Blue500, Blue300)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TypewriterText(
            modifier = Modifier.padding(top = 32.dp),
            text = context.getString(R.string.app_name),
            color = White,
            fontWeight = FontWeight.Bold
        )


        Text(
            modifier = Modifier.padding(bottom = 32.dp),
            text = versionName,
            color = White,
        )
    }
}