package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

// The transition counts from 0 to this many seconds and starts again. The clock is not that number: it keeps
// adding what the transition moved, so it never goes back when the transition starts over.
internal const val CLOCK_LOOP_SECONDS = 60f
private const val MILLIS_PER_SECOND = 1000

/**
 * How far the clock moves when the transition goes from [previous] to [current], where the transition starts
 * over at [loop]. Forward by the difference, and when [current] is behind [previous] the transition has wrapped,
 * so it is the rest of the loop and then [current]. Never negative.
 */
internal fun clockAdvance(previous: Float, current: Float, loop: Float = CLOCK_LOOP_SECONDS): Float {
    val delta = current - previous
    return if (delta < 0f) delta + loop else delta
}

/**
 * The one clock the theme badges' float and dance read, in seconds. It is a state that nothing reads while
 * composing: the badges read it inside `graphicsLayer`, so a tick redraws their layers and recomposes and lays
 * out nothing.
 *
 * It runs only while [running] and only while the screen is RESUMED, the lifecycle gate the app uses for work that
 * must not run in the background: the transition that drives it is composed only then, so it ends when the screen
 * is paused or stopped or leaves composition, and carries on from where the clock stood when the screen comes back.
 * The driver is an infinite transition, which a UI test cancels by itself, so a test is never held up by it.
 */
@Composable
internal fun rememberThemeClock(running: Boolean): FloatState {
    val clock = remember { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val resumed = lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.RESUMED)
    if (running && resumed) ThemeClockDriver(clock)
    return clock
}

@Composable
private fun ThemeClockDriver(clock: MutableFloatState) {
    val transition = rememberInfiniteTransition(label = "intro-theme-clock")
    val position = transition.animateFloat(
        initialValue = 0f,
        targetValue = CLOCK_LOOP_SECONDS,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (CLOCK_LOOP_SECONDS * MILLIS_PER_SECOND).toInt(), easing = LinearEasing),
        ),
        label = "intro-theme-clock-position",
    )
    // The position is read here, in a frame collector, and not in composition, so the driver never recomposes.
    LaunchedEffect(transition) {
        var previous = 0f
        snapshotFlow { position.value }.collect { current ->
            clock.floatValue += clockAdvance(previous, current)
            previous = current
        }
    }
}
