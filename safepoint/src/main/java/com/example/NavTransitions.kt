package com.example

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/*
 * How screens move when you change tab or open a sub-screen.
 *
 * The old and new screen drift sideways in the same direction while they fade, so it reads as one
 * continuous motion. The old screen fades out first and the new one fades in just after, so you
 * never see two half-transparent screens on top of each other. Which side the new screen comes
 * from is decided by slideDirection() in TabNavigation.kt: it follows the order of the tabs, so
 * going back to an earlier tab slides the other way.
 */

/** How far the screens drift, as a share of the screen width. */
private const val DRIFT = 0.1f

private const val FADE_OUT_MILLIS = 110
private const val FADE_IN_DELAY_MILLIS = 90
private const val FADE_IN_MILLIS = 230
private const val SLIDE_OUT_MILLIS = 240
private const val SLIDE_IN_MILLIS = 340

/** Fast start and a long, soft landing for the screen that arrives. */
private val ArriveEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideSide(isPop: Boolean): Int =
    slideDirection(initialState.destination.route, targetState.destination.route, isPop)

/** The screen that is arriving. [isPop] is true when the user is going back. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.screenEnter(isPop: Boolean): EnterTransition {
    val side = slideSide(isPop)
    return fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_IN_DELAY_MILLIS, easing = LinearOutSlowInEasing)) +
        slideInHorizontally(tween(SLIDE_IN_MILLIS, easing = ArriveEasing)) { fullWidth ->
            (fullWidth * DRIFT * side).toInt()
        }
}

/** The screen that is leaving. [isPop] is true when the user is going back. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.screenExit(isPop: Boolean): ExitTransition {
    val side = slideSide(isPop)
    return fadeOut(tween(FADE_OUT_MILLIS, easing = LinearEasing)) +
        slideOutHorizontally(tween(SLIDE_OUT_MILLIS, easing = FastOutSlowInEasing)) { fullWidth ->
            (-fullWidth * DRIFT * side).toInt()
        }
}
