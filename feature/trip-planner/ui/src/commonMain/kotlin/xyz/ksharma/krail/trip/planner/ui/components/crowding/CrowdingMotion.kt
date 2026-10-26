package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.feature.crowding.SeatLevel

/**
 * Motion shared by every crowding surface, so a 30 second forecast refresh never jumps: fills
 * glide to their new level and changed words cross-fade. Nothing animates on first draw, only
 * when a value changes.
 */
internal const val LEVEL_CHANGE_MS = 600
internal const val TEXT_FADE_MS = 250

/** How full a car is drawn: a third, two thirds, all of it. Shape, not colour, carries the level. */
internal fun SeatLevel.fillFraction(): Float = (ordinal + 1f) / SeatLevel.entries.size

/** One fill fraction per car, each easing to its new level when the forecast changes. */
@Composable
internal fun animatedFills(levels: List<SeatLevel>): List<Float> = levels.mapIndexed { index, level ->
    key(index) {
        animateFloatAsState(
            targetValue = level.fillFraction(),
            animationSpec = tween(LEVEL_CHANGE_MS, easing = FastOutSlowInEasing),
            label = "carFill",
        ).value
    }
}

/** Text that cross-fades when it changes, and resizes its slot instead of snapping. */
@Composable
internal fun CrossfadeText(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit,
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(TEXT_FADE_MS)) togetherWith fadeOut(tween(TEXT_FADE_MS)) using
                SizeTransform(clip = false) { _, _ -> tween(TEXT_FADE_MS, easing = FastOutSlowInEasing) }
        },
        label = "crowdingText",
    ) { content(it) }
}
