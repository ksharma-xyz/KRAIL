package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * How much of the rider's own text setting the fitted headline may shrink to at the least: 0.65 of it, so at 100 percent text a
 * headline may be drawn at 0.65 of the normal Intro headline size, and at 110 percent at 0.715 of it.
 */
internal const val HEADLINE_FIT_FLOOR_SHARE = 0.65f

/**
 * The floor of the floor: from about 115 percent text up, a fitted headline is never drawn below 0.75 of the normal size.
 * A rider who asked for larger text keeps a larger headline, and a headline that would need to be smaller than this wraps a
 * line at a word instead (four lines, never a word split), so it never gets small.
 */
internal const val HEADLINE_FIT_FLOOR_MAX = 0.75f

// The scale is searched in steps of a hundredth, which is under a pixel for a phone-width line.
private const val FIT_STEP = 0.01f
private const val FIT_STEPS_PER_UNIT = 100f

/**
 * The smallest scale a headline that is fitted to its room is drawn at, for the rider's text setting [systemScale]: the
 * share of that setting in [HEADLINE_FIT_FLOOR_SHARE], at most [HEADLINE_FIT_FLOOR_MAX]. The scale is the one of
 * [headlineFontScale], where 1.0 is the Intro headline's normal size at 100 percent text.
 */
internal fun headlineFitFloor(systemScale: Float): Float =
    (HEADLINE_FIT_FLOOR_SHARE * systemScale).coerceAtMost(HEADLINE_FIT_FLOOR_MAX)

/**
 * The font scale a headline of lines that each stay whole is drawn at: [normal] (the scale the other Intro headlines are drawn
 * at) when its widest line fits [roomPx], and otherwise the largest scale at which the widest line does, never below [floor]
 * (or below [normal] when that is smaller). [widestPxAt] is the width in pixels of the widest line at a scale, measured from the
 * real text, so a font that is a little wider or narrower is allowed for. The first guess is linear, a hundredth of a scale
 * is taken off until the measured line fits, and at the floor the lines that still do not fit wrap at a word, so the scale
 * is never lower than [floor].
 */
internal fun fitHeadlineScale(normal: Float, floor: Float, roomPx: Int, widestPxAt: (Float) -> Int): Float {
    val widest = widestPxAt(normal)
    if (widest <= roomPx) return normal
    val low = floor.coerceAtMost(normal)
    var scale = stepDown(normal * roomPx / widest)
    while (scale > low && widestPxAt(scale) > roomPx) scale -= FIT_STEP
    return scale.coerceAtLeast(low)
}

private fun stepDown(scale: Float): Float = (scale * FIT_STEPS_PER_UNIT).toInt() / FIT_STEPS_PER_UNIT

/**
 * The density a headline is drawn in. For a headline that is not [fit] it is [headlineDensity]'s: the screen's own, with the
 * font scale held at the ceiling and, in the narrowest rooms, at what keeps the widest word whole. For one that is [fit], the
 * question on the colour screen, its [lines] are kept to as many lines, so the scale is the largest at which the widest of them
 * fits [room] ([fitHeadlineScale], measured from the text as drawn), at most the normal one and at least [headlineFitFloor].
 *
 * It reads the screen's density, so it is called outside the headline's own [LocalDensity].
 */
@Composable
internal fun fittedHeadlineDensity(
    lines: List<String>,
    fit: Boolean,
    room: Dp = LocalHeadlineRoom.current,
): Density {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = KrailTheme.typography.introTagline.asHeadlineStyle()
    return remember(density, room, lines, fit, style) {
        val normal = headlineFontScale(density.fontScale, room)
        if (!fit || room == Dp.Unspecified || lines.isEmpty()) {
            Density(density.density, normal)
        } else {
            val roomPx = with(density) { room.roundToPx() }
            val scale = fitHeadlineScale(normal, headlineFitFloor(density.fontScale), roomPx) { at ->
                val drawn = Density(density.density, at)
                lines.maxOf { line ->
                    measurer.measure(
                        text = AnnotatedString(introHeadlineDisplay(line)),
                        style = style,
                        softWrap = false,
                        constraints = Constraints(),
                        density = drawn,
                    ).size.width
                }
            }
            Density(density.density, scale)
        }
    }
}

/** The density of a headline that is [HeadlineContent]: fitted when it says so, and the ordinary one when there is none. */
@Composable
internal fun fittedHeadlineDensity(content: HeadlineContent?): Density =
    fittedHeadlineDensity(content?.lines.orEmpty(), fit = content?.fitsRoom == true)
