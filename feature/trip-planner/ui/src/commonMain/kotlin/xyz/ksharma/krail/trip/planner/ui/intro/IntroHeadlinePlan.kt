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
import kotlin.math.max
import kotlin.math.min

/**
 * The size rules of the hero and whisper headline. One line of a headline is the hero, the key phrase, drawn as large as the
 * room lets it be and never above the normal headline size. Every other line is a whisper, [WHISPER_SHARE] of the hero's size.
 * A line is a whole phrase and is never merged with another or split, so a headline has the lines it was written with,
 * and only a hero that would have to go below [HERO_FLOOR_SHARE] of the normal size stacks, word by word.
 *
 * All the sizes here are font scales: 1.0 is [HEADLINE_SIZE_FACTOR] times the display style, so the normal headline is 47.6 sp
 * on a phone at 100 percent text, and a line at scale 0.7 is 33.3 sp. They are pure functions of the room and of a width
 * measuring function, so a table holds them and the drawing only reads the result.
 */
internal const val WHISPER_SHARE = 0.70f

/**
 * The smallest a whisper line is ever drawn, as a share of the hero. A whisper that does not fit its room at [WHISPER_SHARE]
 * takes the largest size that does, and no less than this. At this floor it wraps at a word, which it never has to at any
 * window the Intro runs in (the widest whisper, `NEAREST STATION?`, fits 288 dp at 0.62 of the hero or more).
 */
internal const val WHISPER_FLOOR_SHARE = 0.55f

/** A hero that would be drawn below this share of the normal size stacks word by word, at the largest size that fits. */
internal const val HERO_FLOOR_SHARE = 0.75f

// The scale is searched in steps of half a hundredth, which is a quarter of a point for the headline's 47.6 sp.
private const val FIT_STEP = 0.005f
private const val FIT_STEPS_PER_UNIT = 200f

// A line is never fitted to nothing: the search stops here and the line wraps at a word, which no room the app runs in reaches.
private const val FIT_LOWEST = 0.1f

/** What one line is for: the hero in the accent colour, a whisper in the ink, or a plain line of a tagline, in the ink. */
internal enum class HeadlineRole { Hero, Whisper, Plain }

/**
 * One animated unit of a headline, which is one line as it was written. [rows] is that line as drawn, one row, except for a
 * stacked hero, which is one row for each of its words. Every row is drawn at font scale [scale].
 */
internal data class HeadlineUnit(val role: HeadlineRole, val rows: List<String>, val scale: Float)

/** What the headline is drawn as. [heroStacked] and [whisperFitted] say what the plan had to do to fit the room. */
internal data class HeadlinePlan(
    val units: List<HeadlineUnit>,
    val heroStacked: Boolean = false,
    val whisperFitted: Boolean = false,
)

/** The scale of the hero of [this] plan, or null for a plain headline. */
internal val HeadlinePlan.heroScale: Float? get() = units.firstOrNull { it.role == HeadlineRole.Hero }?.scale

/**
 * The plan of a headline whose lines are all drawn the same, as a tagline is: every line at [normal], and a line that does
 * not fit wraps at a word inside its own block.
 */
internal fun planPlainHeadline(lines: List<String>, normal: Float): HeadlinePlan =
    HeadlinePlan(lines.map { HeadlineUnit(HeadlineRole.Plain, listOf(it), normal) })

/**
 * The plan of a hero and whisper headline. [heroIndex] is the hero line of [lines]; [normal] is the largest scale a headline is
 * drawn at here ([headlineFontScale]); [roomPx] is the width every line must fit; [widthAt] is the width in pixels of a line, in
 * capitals, drawn on one line at a scale.
 *
 * - The hero is drawn at the largest scale at which it fits, never above [normal]. Below [HERO_FLOOR_SHARE] of [normal] it stacks
 *   word by word at the largest scale at which its widest word fits, never above [normal], so no word is split.
 * - Each whisper is [WHISPER_SHARE] of the hero's scale. One that does not fit at that scale takes the largest that does, never
 *   below [WHISPER_FLOOR_SHARE] of the hero, and [HeadlinePlan.whisperFitted] says so.
 */
internal fun planHeroHeadline(
    lines: List<String>,
    heroIndex: Int,
    normal: Float,
    roomPx: Int,
    widthAt: (line: String, scale: Float) -> Int,
): HeadlinePlan {
    val hero = lines[heroIndex]
    var heroScale = fitLineScale(hero, normal, roomPx, widthAt)
    val stacked = heroScale < normal * HERO_FLOOR_SHARE
    val heroRows = if (stacked) hero.split(' ') else listOf(hero)
    if (stacked) {
        val widest = heroRows.maxBy { widthAt(it, 1f) }
        heroScale = fitLineScale(widest, normal, roomPx, widthAt)
    }
    val whisper = heroScale * WHISPER_SHARE
    var fitted = false
    val units = lines.mapIndexed { index, line ->
        if (index == heroIndex) {
            HeadlineUnit(HeadlineRole.Hero, heroRows, heroScale)
        } else {
            val scale = max(fitLineScale(line, whisper, roomPx, widthAt), heroScale * WHISPER_FLOOR_SHARE)
            if (scale < whisper) fitted = true
            HeadlineUnit(HeadlineRole.Whisper, listOf(line), scale)
        }
    }
    return HeadlinePlan(units, heroStacked = stacked, whisperFitted = fitted)
}

/**
 * The largest scale, at most [ceiling], at which [line] is no wider than [roomPx]: [ceiling] when it fits, else a first
 * linear guess and then half a hundredth at a time until the measured width fits, so a font that is not exactly linear in its
 * size is allowed for.
 */
internal fun fitLineScale(line: String, ceiling: Float, roomPx: Int, widthAt: (String, Float) -> Int): Float {
    val widest = widthAt(line, ceiling)
    if (widest <= roomPx) return ceiling
    var scale = min(ceiling, stepDown(ceiling * roomPx / widest))
    while (scale > FIT_LOWEST && widthAt(line, scale) > roomPx) scale -= FIT_STEP
    return max(scale, FIT_LOWEST)
}

private fun stepDown(scale: Float): Float = (scale * FIT_STEPS_PER_UNIT).toInt() / FIT_STEPS_PER_UNIT

/**
 * The height of [plan] in dp, with [lineGap] between units and between the rows of a stacked hero, [heroPad] above and below the
 * hero unit, and a row [rowHeightDp] at font scale 1.0, which is the headline style's line height in sp. A tagline line may wrap,
 * so [rowsOf] says how many rows a unit really takes, one by default.
 */
internal fun headlineHeightDp(
    plan: HeadlinePlan,
    rowHeightDp: Float,
    lineGap: Float,
    heroPad: Float,
    rowsOf: (HeadlineUnit) -> Int = { it.rows.size },
): Float {
    if (plan.units.isEmpty()) return 0f
    val units = plan.units.sumOf { unit ->
        val rows = rowsOf(unit)
        val pad = if (unit.role == HeadlineRole.Hero) heroPad * 2 else 0f
        (rows * rowHeightDp * unit.scale + (rows - 1) * lineGap + pad).toDouble()
    }.toFloat()
    return units + (plan.units.size - 1) * lineGap
}

/**
 * The plan for [content] in the room [room], measured as it is drawn. A plain headline is every line at the normal scale and
 * a hero headline is [planHeroHeadline]. It reads the screen's own density, so it is called outside the headline's own, and it is
 * remembered for the density, the room and the words, so nothing is measured on a frame.
 */
@Composable
internal fun rememberHeadlinePlan(content: HeadlineContent, room: Dp = LocalHeadlineRoom.current): HeadlinePlan {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = KrailTheme.typography.introTagline.asHeadlineStyle()
    return remember(density, room, content.lines, content.emphasisIndex, content.heroLayout, style) {
        val normal = headlineFontScale(density.fontScale, room)
        if (!content.heroLayout || content.lines.isEmpty()) {
            planPlainHeadline(content.lines, normal)
        } else {
            val roomPx = if (room == Dp.Unspecified) Int.MAX_VALUE else with(density) { room.roundToPx() }
            val hero = content.emphasisIndex.coerceIn(content.lines.indices)
            planHeroHeadline(content.lines, hero, normal, roomPx) { line, scale ->
                measurer.measure(
                    text = AnnotatedString(introHeadlineDisplay(line)),
                    style = style,
                    softWrap = false,
                    constraints = Constraints(),
                    density = Density(density.density, scale),
                ).size.width
            }
        }
    }
}
