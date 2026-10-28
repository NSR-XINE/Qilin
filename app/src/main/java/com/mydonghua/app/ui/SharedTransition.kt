package com.mydonghua.app.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val SharedBoundsSpring = spring<Rect>(dampingRatio = 1f, stiffness = 350f)

/**
 * Key of the poster that launched the current detail screen. Set right before
 * navigating so the detail poster matches exactly one source card.
 */
object SharedPosterKey {
    var current: String? = null
}

@Composable
fun SharedElementDestination(
    scope: AnimatedVisibilityScope,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides scope) {
        content()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.scopedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) {
    composable(route, arguments) { entry ->
        SharedElementDestination(this) { content(entry) }
    }
}
