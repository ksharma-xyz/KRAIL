package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** The two values the star's celebration animates: the burst's progress and the star's own opacity. */
internal class Celebration(val burst: Animatable<Float, *>, val fade: Animatable<Float, *>)

/**
 * Plays the celebration for [mode] once and rests: the burst runs 0 to 1 over [BURST_DURATION_MS], the fade runs 0 to 1
 * over [CELEBRATION_FADE_MS], and a settled star has neither. It is saved as played, so a turn of the screen does
 * not play it again.
 */
@Composable
internal fun rememberCelebration(
    phrase: String,
    celebrate: Boolean,
    reduceMotion: Boolean,
    inspection: Boolean,
): Celebration {
    var played by rememberSaveable(phrase) { mutableStateOf(false) }
    val mode = remember(phrase) { celebrationMode(celebrate, reduceMotion, inspection, played) }
    val celebration = remember(phrase) {
        Celebration(burst = Animatable(0f), fade = Animatable(if (mode == CelebrationMode.Fade) 0f else 1f))
    }
    LaunchedEffect(phrase) {
        if (mode == CelebrationMode.Settled) return@LaunchedEffect
        played = true
        when (mode) {
            CelebrationMode.Burst -> celebration.burst.animateTo(1f, tween(durationMillis = BURST_DURATION_MS))
            CelebrationMode.Fade -> celebration.fade.animateTo(1f, tween(durationMillis = CELEBRATION_FADE_MS))
            CelebrationMode.Settled -> Unit
        }
    }
    return celebration
}
