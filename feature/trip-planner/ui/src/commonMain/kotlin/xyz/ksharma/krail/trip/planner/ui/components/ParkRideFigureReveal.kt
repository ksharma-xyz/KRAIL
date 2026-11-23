package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled

// One line of a reading fades in over this long, easing out, and rises into its place.
internal const val FIGURE_BEAT_MS = 250

// Each line starts this long after the one before it, in the order they read.
internal const val FIGURE_STAGGER_MS = 80

// A line starts this far below its place.
internal const val FIGURE_RISE_DP = 8

// The spots, the share full and the time of the reading.
internal const val FIGURE_BEAT_COUNT = 3

// Reduced motion: one plain fade of the whole reading, nothing rises and nothing waits.
internal const val FIGURE_REDUCED_FADE_MS = 150

/** From the first line starting to the last one landing: the beat, plus a stagger for every line after the first. */
internal const val FIGURE_TOTAL_MS = FIGURE_BEAT_MS + (FIGURE_BEAT_COUNT - 1) * FIGURE_STAGGER_MS

/** How long a reading takes to arrive: [FIGURE_TOTAL_MS], or the one plain fade under reduced motion. */
internal fun figureRevealMs(reduceMotion: Boolean): Int = if (reduceMotion) FIGURE_REDUCED_FADE_MS else FIGURE_TOTAL_MS

/** What a line of the reading looks like: [alpha] is 0 for clear and 1 for drawn, and [rise] is the share of the rise still to go. */
internal data class FigureFrame(val alpha: Float, val rise: Float)

/**
 * The frame of line [beat], counting from 0 for the spots, [elapsedMs] after the numbers arrived. Each line starts [FIGURE_STAGGER_MS]
 * after the one before it and takes [FIGURE_BEAT_MS], easing out, so most of the fade and the rise happen in the first part of it. Under
 * [reduceMotion] every line is the same one plain fade over [FIGURE_REDUCED_FADE_MS] with no rise.
 */
internal fun figureFrame(elapsedMs: Float, beat: Int, reduceMotion: Boolean): FigureFrame {
    if (reduceMotion) {
        val fade = (elapsedMs / FIGURE_REDUCED_FADE_MS).coerceIn(0f, 1f)
        return FigureFrame(alpha = FastOutSlowInEasing.transform(fade), rise = 0f)
    }
    val local = ((elapsedMs - beat * FIGURE_STAGGER_MS) / FIGURE_BEAT_MS).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(local)
    return FigureFrame(alpha = eased, rise = 1f - eased)
}

/**
 * The arrival of a card's numbers, as the progress from 0, when they arrived, to 1, when the last line has landed. The three lines
 * each read it in the draw phase ([beats]), so a frame of the arrival moves and fades them without composing or laying anything out.
 */
internal class FigureReveal(
    private val progress: Animatable<Float, AnimationVector1D>,
    private val reduceMotion: Boolean,
) {
    /** One draw modifier for each line of a reading, indexed from 0, that fades and lifts it as [progress] says. */
    val beats: List<Modifier> = List(FIGURE_BEAT_COUNT) { beat ->
        Modifier.graphicsLayer {
            val frame = figureFrame(progress.value * figureRevealMs(reduceMotion), beat, reduceMotion)
            alpha = frame.alpha
            translationY = frame.rise * FIGURE_RISE_DP.dp.toPx()
        }
    }
}

/**
 * Plays the arrival of a card's numbers, or null when the card does not stage them. It plays once, in the frame the card first has
 * numbers after it has been drawn without any, and a later value just replaces the text. A card drawn with its numbers already there,
 * which is a restored one, a preview or a screenshot, is complete from the first frame. It starts at once: no delay, no gate, no
 * timer. [reduceMotion] is the rider's setting unless a caller says otherwise.
 */
@Composable
internal fun rememberFigureReveal(figuresShown: Boolean, staged: Boolean, reduceMotion: Boolean?): FigureReveal? {
    if (!staged) return null
    val reduce = reduceMotion ?: isReduceMotionEnabled()
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (figuresShown || inspection) 1f else 0f) }
    LaunchedEffect(figuresShown) {
        if (figuresShown && progress.value < 1f) {
            progress.animateTo(1f, tween(durationMillis = figureRevealMs(reduce), easing = LinearEasing))
        }
    }
    return remember(progress, reduce) { FigureReveal(progress, reduce) }
}
