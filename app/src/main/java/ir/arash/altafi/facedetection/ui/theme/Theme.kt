package ir.arash.altafi.facedetection.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue300,
    secondary = Sky400,
    tertiary = Teal200,
    background = Gray950,
    surface = Gray975,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = Gray300,
    onSurface = Gray500,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    secondary = Sky500,
    tertiary = Teal300,
    background = White,
    surface = Gray100,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = Gray900,
    onSurface = Gray700,
)

@Composable
fun FaceDetectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}