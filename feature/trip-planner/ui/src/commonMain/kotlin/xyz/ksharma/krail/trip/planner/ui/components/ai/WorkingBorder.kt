package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private const val FADE_IN_MILLIS = 220
private const val FADE_OUT_MILLIS = 420

private const val KEEP_TURNING_AFTER_THE_ANSWER_MILLIS = 1_100L
private const val DECELERATION_MILLIS = 1_000L

@Immutable
internal data class WorkingBorder(val spinning: Boolean, val alpha: Float)

/**
 * Drives the field's gradient border across a whole piece of work rather than only while the
 * flag is true.
 *
 * The border fades in when work starts, keeps its speed for a beat after the work is done,
 * decelerates through the shared spin driver, then fades out. A rider who blinks still sees a
 * border that arrived, ran, and left.
 */
@Composable
internal fun rememberWorkingBorder(isWorking: Boolean): WorkingBorder {
    val alpha = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }

    LaunchedEffect(isWorking) {
        if (isWorking) {
            spinning = true
            alpha.animateTo(1f, animationSpec = tween(durationMillis = FADE_IN_MILLIS))
        } else if (spinning) {
            // Extraction can come back in a few hundred milliseconds, and a border that
            // appears and stops inside one second reads as a glitch rather than as work.
            // Carrying on briefly, then slowing, gives it a beginning, a middle and an end.
            delay(KEEP_TURNING_AFTER_THE_ANSWER_MILLIS)
            spinning = false
            // Matches the spin's own deceleration, so the fade starts as rotation finishes.
            delay(DECELERATION_MILLIS)
            alpha.animateTo(0f, animationSpec = tween(durationMillis = FADE_OUT_MILLIS))
        }
    }

    return WorkingBorder(spinning = spinning, alpha = alpha.value)
}
