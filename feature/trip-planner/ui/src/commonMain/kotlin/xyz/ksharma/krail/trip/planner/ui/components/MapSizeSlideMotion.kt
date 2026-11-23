package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

// The arrivals and exits that change a size or slide, read from the numbers in [MapMotion].

/** A notice that comes and goes in place: it fades while its height grows or shrinks, on the arrival and leaving curves; plain with reduced motion. */
internal fun mapSizeFadeIn(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(MapMotion.FADE_IN_MS, easing = MapMotion.FADE_IN_EASING)) +
            expandVertically(animationSpec = tween(MapMotion.ENTER_MS, easing = MapMotion.ENTER_EASING))
    }

/** And leaves in place: it fades while its height shrinks to nothing, a little quicker than it came. */
internal fun mapSizeFadeOut(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING)) +
            shrinkVertically(animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING))
    }

/** A card that arrives from below: it slides up by its own height while it fades in, on the arrival curve; plain with reduced motion. */
internal fun mapSlideUpIn(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(MapMotion.FADE_IN_MS, easing = MapMotion.FADE_IN_EASING)) +
            slideInVertically(
                animationSpec = tween(MapMotion.ENTER_MS, easing = MapMotion.ENTER_EASING),
                initialOffsetY = { it },
            )
    }

/** And leaves downward the way it came, a little quicker: it slides down by its own height while it fades out. */
internal fun mapSlideDownOut(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING)) +
            slideOutVertically(
                animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING),
                targetOffsetY = { it },
            )
    }
