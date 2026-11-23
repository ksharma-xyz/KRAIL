package xyz.ksharma.krail.trip.planner.ui.intro

// A line starts this much smaller and this far down, and rises and grows into place.
internal const val LINE_START_SCALE = 0.92f
internal const val LINE_RISE_FRACTION = 0.5f

// One line enters this long after the one before it, which is the stagger. The emphasised line takes a short
// beat more, so the key word lands last.
internal const val LINE_STAGGER_MS = 70L
internal const val EMPHASIS_BEAT_MS = 40L

/**
 * What a headline says, as a value: its [lines], the one with the key word and the [emoji] above them. [spoken] is the text
 * a screen reader reads as the heading, which is the lines joined unless the drawn lines are a shortened form of what is said.
 * [fitsRoom] is for the question on the colour screen, whose lines are kept whole by choosing the size they are drawn at
 * ([fittedHeadlineDensity]). Two headlines are the same headline when all of these are equal, so the screen can tell a change
 * of words from a redraw.
 */
internal data class HeadlineContent(
    val lines: List<String>,
    val emphasisIndex: Int,
    val emoji: String,
    val spoken: String = introHeadlinePhrase(lines),
    val fitsRoom: Boolean = false,
)

/** The words of a headline as one phrase, which is what a screen reader reads and what a test finds. */
internal fun introHeadlinePhrase(lines: List<String>): String = lines.joinToString(separator = " ")

/** When each line, and the emoji, starts, in milliseconds after the screen appears. */
internal data class HeadlineTimeline(val lineDelaysMs: List<Long>, val emojiDelayMs: Long)

/**
 * The lines enter one after another, [LINE_STAGGER_MS] apart, in the order they read, and the line at
 * [emphasisIndex] takes [EMPHASIS_BEAT_MS] more so it arrives with weight. The emoji pops first, above them.
 * An [emphasisIndex] outside the lines gives the plain stagger.
 */
internal fun headlineTimeline(lineCount: Int, emphasisIndex: Int): HeadlineTimeline = HeadlineTimeline(
    lineDelaysMs = List(lineCount) { index ->
        index * LINE_STAGGER_MS + if (index == emphasisIndex) EMPHASIS_BEAT_MS else 0L
    },
    emojiDelayMs = 0L,
)

/** What one line looks like at [progress] of its entrance: 0 is not started and 1 is in place. */
internal data class LineFrame(val alpha: Float, val rise: Float, val scale: Float)

/**
 * A line's frame at [progress]. The spring that drives [progress] overshoots 1 a little, and the frame
 * follows: the line rises a touch past its place and grows a touch past its size, then settles. Alpha is
 * held between 0 and 1. [LineFrame.rise] is a share of the rise distance, down while positive.
 */
internal fun lineFrame(progress: Float): LineFrame = LineFrame(
    alpha = progress.coerceIn(0f, 1f),
    rise = (1f - progress) * LINE_RISE_FRACTION,
    scale = LINE_START_SCALE + (1f - LINE_START_SCALE) * progress,
)

/**
 * The emoji's one wobble after it has popped, as time in milliseconds and tilt in degrees. It starts and
 * ends at rest, swings out and back once with a smaller swing on the way home, and is over in half a second.
 */
internal val EmojiWobble: List<Pair<Int, Float>> = listOf(
    0 to 0f,
    120 to -14f,
    260 to 10f,
    380 to -5f,
    480 to 0f,
)

/**
 * Whether the headline is drawn in place with no animation: reduced motion, a preview or a screenshot, or
 * a headline that already played, such as after a turn of the screen.
 */
internal fun headlineStartsSettled(reduceMotion: Boolean, inspection: Boolean, played: Boolean): Boolean =
    reduceMotion || inspection || played

// How a headline that is already on screen is replaced by another: one fade and one gentle rise, with no spring.
internal const val HEADLINE_CHANGE_ENTER_MS = 350
internal const val HEADLINE_CHANGE_EXIT_MS = 175
internal const val HEADLINE_CHANGE_RISE_DP = 16

/**
 * How a headline is replaced by another once it is on screen. [animated] false is the plain swap with nothing moving.
 * Otherwise the new headline fades in over [enterMs] while it rises [riseDp] from below to its place, and the old one
 * fades out over [exitMs], at the same time, so the old words are gone before the new ones settle. Nothing
 * overshoots and no line starts on its own timer, so a change that comes while another is running just carries on.
 */
internal data class HeadlineChangeSpec(val animated: Boolean, val enterMs: Int, val exitMs: Int, val riseDp: Int)

/** The change for the rider's motion setting: instant under reduced motion, and [HEADLINE_CHANGE_ENTER_MS] otherwise. */
internal fun headlineChangeSpec(reduceMotion: Boolean): HeadlineChangeSpec = if (reduceMotion) {
    HeadlineChangeSpec(animated = false, enterMs = 0, exitMs = 0, riseDp = 0)
} else {
    HeadlineChangeSpec(
        animated = true,
        enterMs = HEADLINE_CHANGE_ENTER_MS,
        exitMs = HEADLINE_CHANGE_EXIT_MS,
        riseDp = HEADLINE_CHANGE_RISE_DP,
    )
}
