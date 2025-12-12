package ir.arash.altafi.facedetection.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

enum class TransitionType {
    FADE, SLIDE, SCALE, EXPAND, SHRINK, NONE
}

inline fun <reified T : Route> NavGraphBuilder.dynamicComposable(
    transitionType: TransitionType = TransitionType.FADE,
    duration: Int = 500,
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable (T, NavBackStackEntry) -> Unit
) {
    composable<T>(
        deepLinks = deepLinks
    ) { backStackEntry ->
        val args = backStackEntry.toRoute<T>()

        AnimatedContent(
            targetState = args,
            transitionSpec = {
                when (transitionType) {
                    TransitionType.FADE -> {
                        fadeIn(animationSpec = tween(duration)) togetherWith
                                fadeOut(animationSpec = tween(duration))
                    }

                    TransitionType.SLIDE -> {
                        slideInHorizontally(
                            animationSpec = tween(duration),
                            initialOffsetX = { it }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(duration),
                            targetOffsetX = { -it }
                        )
                    }

                    TransitionType.SCALE -> {
                        scaleIn(animationSpec = tween(duration)) togetherWith
                                scaleOut(animationSpec = tween(duration))
                    }

                    TransitionType.EXPAND -> {
                        expandHorizontally(animationSpec = tween(duration)) togetherWith
                                shrinkHorizontally(animationSpec = tween(duration))
                    }

                    TransitionType.SHRINK -> {
                        expandHorizontally(animationSpec = tween(duration)) togetherWith
                                shrinkHorizontally(animationSpec = tween(duration))
                    }

                    TransitionType.NONE -> {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                }
            }
        ) { state ->
            content(state, backStackEntry)
        }
    }
}