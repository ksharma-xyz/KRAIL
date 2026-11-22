package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// The reduced motion cross-fade: short, plain, nothing moves.
private const val REDUCED_FADE_OUT_MS = 90
private const val REDUCED_FADE_IN_MS = 120

// The old screen leaves faster than the new one settles, so the rider is never watching two at once.
private const val SLIDE_EXIT_MS = 150
private const val SLIDE_ENTER_FADE_MS = 160
private const val SLIDE_ENTER_DAMPING = 0.82f

// The leaving screen only travels part of the way, which reads as faster than the one arriving.
private const val EXIT_TRAVEL_DIVISOR = 3

/** Which way a screen change moves, by the order of the two screens. */
internal enum class IntroSlide {
    /** The screens are the same one, so nothing moves. */
    None,

    /** Later in the order: the new screen comes in from the right and the old one leaves to the left. */
    Forward,

    /** Earlier in the order: the reverse. */
    Back,
}

/** [IntroSlide] for going from [from] to [to], decided by their place in the order and nothing else. */
internal fun introSlideDirection(from: IntroStep, to: IntroStep): IntroSlide = when {
    to.ordinal > from.ordinal -> IntroSlide.Forward
    to.ordinal < from.ordinal -> IntroSlide.Back
    else -> IntroSlide.None
}

/**
 * How a screen change is drawn, as plain numbers so a table can hold it. [enterFrom] is where the new
 * screen starts and [exitTo] where the old one ends, as a share of the screen width with right positive.
 * [exitMs] is how long the old screen takes to leave. A [slides] false change is the plain cross-fade.
 */
internal data class IntroTransitionSpec(
    val slides: Boolean,
    val enterFrom: Int,
    val exitTo: Int,
    val exitMs: Int,
)

/**
 * The spec for a change in [slide], and a plain short cross-fade with everything settled when the rider
 * has asked for reduced motion. Forward comes in from the right and goes out to the left, back is the
 * mirror image, and the old screen always leaves faster than a spring settles.
 */
internal fun introTransitionSpec(slide: IntroSlide, reduceMotion: Boolean): IntroTransitionSpec = when {
    reduceMotion || slide == IntroSlide.None ->
        IntroTransitionSpec(slides = false, enterFrom = 0, exitTo = 0, exitMs = REDUCED_FADE_OUT_MS)
    slide == IntroSlide.Forward ->
        IntroTransitionSpec(slides = true, enterFrom = 1, exitTo = -1, exitMs = SLIDE_EXIT_MS)
    else ->
        IntroTransitionSpec(slides = true, enterFrom = -1, exitTo = 1, exitMs = SLIDE_EXIT_MS)
}

/**
 * The transition between two screens: a spring slide with a fade, or the plain cross-fade. Only
 * transforms are animated, so nothing is laid out again on a frame.
 */
internal fun introScreenTransition(from: IntroStep, to: IntroStep, reduceMotion: Boolean): ContentTransform {
    val spec = introTransitionSpec(introSlideDirection(from, to), reduceMotion)
    return if (spec.slides) {
        (
            slideInHorizontally(
                animationSpec = spring(dampingRatio = SLIDE_ENTER_DAMPING, stiffness = Spring.StiffnessMediumLow),
                initialOffsetX = { width -> spec.enterFrom * width },
            ) + fadeIn(tween(durationMillis = SLIDE_ENTER_FADE_MS))
            ) togetherWith (
            slideOutHorizontally(
                animationSpec = tween(durationMillis = spec.exitMs, easing = FastOutLinearInEasing),
                targetOffsetX = { width -> spec.exitTo * width / EXIT_TRAVEL_DIVISOR },
            ) + fadeOut(tween(durationMillis = spec.exitMs))
            )
    } else {
        ContentTransform(
            targetContentEnter = if (reduceMotion) {
                fadeIn(tween(durationMillis = REDUCED_FADE_IN_MS, delayMillis = REDUCED_FADE_OUT_MS))
            } else {
                EnterTransition.None
            },
            initialContentExit = if (reduceMotion) {
                fadeOut(tween(durationMillis = REDUCED_FADE_OUT_MS))
            } else {
                ExitTransition.None
            },
            sizeTransform = null,
        )
    }
}
