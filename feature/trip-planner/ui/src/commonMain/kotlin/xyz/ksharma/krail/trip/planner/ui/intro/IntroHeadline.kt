package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.roundToInt

/**
 * How much bigger the headline is than the app's `introTagline` style: the style's size and line height times this.
 * The headline is meant to be the loudest thing on the screen. 1.7 is 47.6 sp.
 */
internal const val HEADLINE_SIZE_FACTOR = 1.7f

/**
 * The height of a line of the headline as a share of that line's own font size: 1.12, which is 53.3 sp for the 47.6 sp line and
 * 37.3 sp for a whisper of 33.3 sp. Every line takes it from its own size, so a whisper is as snug as the hero.
 */
internal const val HEADLINE_LINE_HEIGHT_FACTOR = 1.12f

/**
 * The weight of every headline line: the heaviest the display style's family has. The style is the system font at
 * [FontWeight.Black], and the app's only bundled family, the alternative typeface, has no heavier weight and is not
 * the Intro's. Every line takes it, so the title is as bold as the font goes.
 */
internal val HEADLINE_WEIGHT = FontWeight.Black

/**
 * The most the headline grows with the system font scale. Above it the headline stops growing, because a single word
 * in capitals at this size does not fit a 320 dp window at 200 percent text and would be split or cut. The rest of the
 * screen still follows the system scale in full. It is the largest scale at which the longest word of any headline
 * fits the narrowest window with a twelfth to spare, held by `IntroHeadlineLayoutTest`. On the test font that is 1.09
 * for CONCRETE (about 243 dp for each 1.0 of scale at [HEADLINE_SIZE_FACTOR]), and 1.05 leaves another 4 percent for a
 * font that is wider, which is a guess for the system font on iOS. TRANSPORT, in the theme question, is wider still (about
 * 278 dp for each 1.0), so it does not fit the narrowest window at this ceiling: [headlineFontScale] holds the scale lower
 * there, and only there, from the room the screen frame gives the headline.
 */
internal const val HEADLINE_FONT_SCALE_CEILING = 1.05f

/**
 * The width of the widest word of any headline, in capitals at [HEADLINE_SIZE_FACTOR] and a font scale of 1.0, in dp:
 * TRANSPORT, which is 278.5 dp on the test font, rounded up to leave a little for type that does not scale exactly with its
 * size. It is a number of its own and not measured while drawing, and `IntroHeadlineLayoutTest` fails if any word of any
 * headline is wider, so new copy cannot outgrow it unseen.
 */
internal const val WIDEST_HEADLINE_WORD_DP = 285f

/** The room a font a little wider than the test font needs beyond the word: a twelfth, as the ceiling above keeps. */
internal const val HEADLINE_WORD_MARGIN = 1.08f

/**
 * The room a headline has, in dp, which the screen frame sets so the headline can keep its widest word whole. Unspecified
 * outside a frame, such as in a preview, where only [HEADLINE_FONT_SCALE_CEILING] applies.
 */
internal val LocalHeadlineRoom = compositionLocalOf { Dp.Unspecified }

/** The text a headline line is drawn as: ALL UPPER CASE, with the invariant rules, so a locale never changes a letter. */
internal fun introHeadlineDisplay(line: String): String = line.uppercase()

/**
 * The font scale the headline is drawn at for the system scale [systemScale]: the same, up to the ceiling. In a [room]
 * too narrow for the widest word at that scale with a twelfth to spare, which only the narrowest windows are (288 dp
 * is 0.95 here, and a 360 dp phone's 328 dp is above the ceiling), it is held lower, so the widest word is whole and
 * never split. Every other width is the ceiling alone, so a headline is never drawn smaller than its style where it
 * has the room.
 */
internal fun headlineFontScale(systemScale: Float, room: Dp = Dp.Unspecified): Float {
    val capped = systemScale.coerceAtMost(HEADLINE_FONT_SCALE_CEILING)
    if (room == Dp.Unspecified) return capped
    return capped.coerceAtMost(room.value / (WIDEST_HEADLINE_WORD_DP * HEADLINE_WORD_MARGIN))
}

/**
 * The display style of the headline: the app's `introTagline` style, [HEADLINE_SIZE_FACTOR] times as large, at
 * [HEADLINE_WEIGHT], with a line [HEADLINE_LINE_HEIGHT_FACTOR] of its own size.
 */
internal fun TextStyle.asHeadlineStyle(): TextStyle = (fontSize * HEADLINE_SIZE_FACTOR).let { size ->
    copy(fontSize = size, lineHeight = size * HEADLINE_LINE_HEIGHT_FACTOR, fontWeight = HEADLINE_WEIGHT)
}

/** The room between the emoji and the first line, which grows with the text: [base] at 100 percent and up to twice that. */
internal fun introEmojiTitleGap(base: Dp, fontScale: Float): Dp = base * fontScale.coerceIn(1f, 2f)

/**
 * The density the headline is drawn in: the screen's own, with the font scale held at [HEADLINE_FONT_SCALE_CEILING] and,
 * in the narrowest rooms, at what keeps the widest word whole ([headlineFontScale]).
 */
@Composable
internal fun headlineDensity(): Density {
    val density = LocalDensity.current
    val room = LocalHeadlineRoom.current
    return remember(density, room) { Density(density.density, headlineFontScale(density.fontScale, room)) }
}

// Each line is a spring with a small overshoot that settles in about 300 ms, so a headline of three lines is
// in place well inside half a second. The emoji bounces more, as a sticker would.
private const val LINE_DAMPING = 0.7f
private const val EMOJI_DAMPING = 0.5f
private val LineRiseDistance = 24.dp

/**
 * The big, bold headline at the top of an Intro screen. [lines] is two to four short lines, one idea on each. Every line is at
 * the same weight, the heaviest there is, and the lines are left aligned, drawn ALL UPPER CASE ([introHeadlineDisplay]) in the
 * app's own boldest display style, [HEADLINE_SIZE_FACTOR] times its size and at [HEADLINE_WEIGHT]. The case is a display matter
 * only: the lines passed in, the screen reader's heading and every test that reads the copy keep the case the words are written
 * in. The emoji sits [introEmojiTitleGap] above the first line, further as the text grows.
 *
 * Without [heroLayout] every line is drawn at the same size, [headlineFontScale] (the system text size up to a ceiling and, in
 * the narrowest room, no more than keeps the widest word whole), and a line that does not fit wraps at a word. With it the line at
 * [emphasisIndex] is the hero, in [accent], drawn as large as the room lets it be and never above that size, and every other line
 * is a whisper in the ink at 0.70 of the hero ([planHeroHeadline]). A hero too wide for the room at 0.75 of the normal size
 * stacks word by word, and no line is ever wider than its room.
 *
 * The first time the screen shows its headline, each line is its own animated unit and enters one after another:
 * it rises with a small spring overshoot, grows a little and fades in, [LINE_STAGGER_MS] after the one before. The
 * [emoji] pops with a bounce and one wobble, then rests, so every animation is finite. With reduced motion, in a
 * preview and after a turn of the screen the headline is simply in place. Only transforms are animated, through
 * `graphicsLayer`, so nothing is laid out again on a frame.
 *
 * When the words change after that, as the theme screen's do with each badge, nothing enters again line by line: the
 * old headline fades out while the new one fades in and rises [HeadlineChangeSpec.riseDp] to its place, in one smooth
 * move with no overshoot ([headlineChangeSpec]), and with reduced motion it is simply swapped. Both keep the same room, so
 * nothing below the headline moves.
 *
 * [celebrate] is for the confirm screen's star. With it the star also throws a burst of plain shapes in the theme
 * colour, drawn behind it, which flies out and fades in [BURST_DURATION_MS]. With reduced motion the star fades in at
 * rest instead and nothing flies, and in a preview, a screenshot or after a turn of the screen it is simply there.
 *
 * [reservedLinesHeight] is the height of room to keep for the lines, for a screen whose headline changes: with it the headline
 * takes the height of the emoji and that much for the lines whatever it says, so nothing below it moves.
 *
 * For a screen reader the whole headline is one heading whose text is [spoken], which is the full phrase, the lines joined,
 * unless the lines drawn are a shortened form of it, with no pause between lines, and the emoji is hidden from it. The colour of
 * the hero is never what says anything.
 */
@Composable
internal fun IntroHeadline(
    lines: List<String>,
    emphasisIndex: Int,
    emoji: String,
    modifier: Modifier = Modifier,
    reservedLinesHeight: Dp = 0.dp,
    celebrate: Boolean = false,
    spoken: String = introHeadlinePhrase(lines),
    heroLayout: Boolean = false,
    accent: Color = Color.Unspecified,
) {
    val reduceMotion = isReduceMotionEnabled()
    val inspection = LocalInspectionMode.current
    // Saved, so a turn of the screen does not play the first entrance again. It is read once, at the start.
    var entered by rememberSaveable { mutableStateOf(false) }
    val startsSettled = remember { headlineStartsSettled(reduceMotion, inspection, entered) }
    LaunchedEffect(Unit) { entered = true }

    val content = HeadlineContent(lines, emphasisIndex, emoji, spoken, heroLayout)
    // The headline the screen opened with is the one that enters line by line. Every later one arrives by the change.
    val first = remember { content }
    val spec = headlineChangeSpec(reduceMotion)
    val risePx = with(LocalDensity.current) { spec.riseDp.dp.roundToPx() }
    AnimatedContent(
        targetState = content,
        modifier = modifier,
        transitionSpec = { headlineChangeTransition(spec, risePx) },
        contentAlignment = Alignment.TopStart,
        label = "intro-headline",
    ) { shown ->
        IntroHeadlineBody(
            content = shown,
            kinetic = shown == first && !startsSettled,
            reservedLinesHeight = reservedLinesHeight,
            celebrate = celebrate,
            accent = accent,
        )
    }
}

/** One headline as it is drawn: the words, the hero line, and the emoji above them. */
@Composable
private fun IntroHeadlineBody(
    content: HeadlineContent,
    kinetic: Boolean,
    reservedLinesHeight: Dp,
    celebrate: Boolean,
    accent: Color,
) {
    val lines = content.lines
    val emphasisIndex = content.emphasisIndex
    val emoji = content.emoji
    val phrase = content.spoken
    val reduceMotion = isReduceMotionEnabled()
    val inspection = LocalInspectionMode.current
    val animate = kinetic
    val celebration = rememberCelebration(phrase, celebrate, reduceMotion, inspection)

    val progress = remember(phrase) { lines.map { Animatable(if (animate) 0f else 1f) } }
    val emojiScale = remember(phrase) { Animatable(if (animate) 0f else 1f) }
    val emojiTilt = remember(phrase) { Animatable(0f) }

    LaunchedEffect(phrase) {
        if (!animate) return@LaunchedEffect
        val timeline = headlineTimeline(lines.size, emphasisIndex)
        launch { popEmoji(emojiScale, emojiTilt, timeline.emojiDelayMs) }
        progress.forEachIndexed { index, line ->
            launch {
                delay(timeline.lineDelaysMs[index])
                line.animateTo(1f, spring(dampingRatio = LINE_DAMPING, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    // The headline grows with the system text size only up to the ceiling, so a word in capitals always fits. The plan is read
    // in the screen's own density, and so is the room kept, so a headline keeps the room of the others and nothing moves on a change.
    val lineGap = headlineLineGap()
    val emojiGap = introEmojiTitleGap(KrailTheme.dimensions.spacingXXL, LocalDensity.current.fontScale)
    val plan = rememberHeadlinePlan(content)
    val emojiDensity = headlineDensity()
    val reservedHeight = reservedHeadlineHeight(reservedLinesHeight, hasEmoji = emoji.isNotEmpty(), emojiGap = emojiGap)
    CompositionLocalProvider(LocalDensity provides emojiDensity) {
        Column(
            modifier = Modifier
                .heightIn(min = reservedHeight)
                .clearAndSetSemantics {
                    heading()
                    text = AnnotatedString(phrase)
                },
            verticalArrangement = Arrangement.spacedBy(lineGap),
            horizontalAlignment = Alignment.Start,
        ) {
            if (emoji.isNotEmpty()) {
                Text(
                    text = emoji,
                    style = KrailTheme.typography.emoji,
                    modifier = Modifier
                        // The column's own gap is the lines', so the emoji adds what makes its gap the larger one.
                        .padding(bottom = emojiGap - lineGap)
                        .introBurst(progress = { celebration.burst.value }, color = themeDecorColor())
                        .graphicsLayer {
                            scaleX = emojiScale.value
                            scaleY = emojiScale.value
                            rotationZ = emojiTilt.value
                            alpha = celebration.fade.value
                        }
                        // Its box is its line height, so the room kept for it is exactly what it takes.
                        .headlineLineBox(KrailTheme.typography.emoji.lineHeight) { 1 },
                )
            }
            HeadlineUnits(plan = plan, accent = accent, ink = KrailTheme.colors.onSurface) { index ->
                Modifier.graphicsLayer {
                    val frame = lineFrame(progress[index].value)
                    alpha = frame.alpha
                    translationY = frame.rise * LineRiseDistance.toPx()
                    scaleX = frame.scale
                    scaleY = frame.scale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
            }
        }
    }
}

/** The room between two lines of a headline, and between the words of a stacked hero: 2 dp. */
@Composable
internal fun headlineLineGap(): Dp = KrailTheme.dimensions.spacingXXS

/** The extra room above and below the hero line, and only that line: 6 dp. */
@Composable
internal fun headlineHeroPad(): Dp = KrailTheme.dimensions.spacingS

/**
 * Makes the box of a text [lines] lines of [lineHeight] tall, and puts the text in the middle of it. The text measures to the height
 * its font has by nature, which is a little taller than the 1.12 of its size this box is, and it is drawn whole: it is not clipped
 * and its glyphs sit where a line box on the web puts them, half the difference above and half below.
 */
internal fun Modifier.headlineLineBox(lineHeight: TextUnit, lines: () -> Int): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val boxHeight = (lines() * lineHeight.toPx()).roundToInt()
        layout(placeable.width, boxHeight) { placeable.place(0, (boxHeight - placeable.height) / 2) }
    }

/**
 * The lines of [plan], each unit its own block drawn at its own font scale, in a column with [headlineLineGap] between them.
 * The hero is in [accent] (the ink when it is unspecified) and every other line in [ink]. [unitModifier] is the modifier of unit
 * [Int], where the entrance and the change of words animate it, through `graphicsLayer` only. It reads the screen's density, so
 * it is called outside the headline's own [LocalDensity].
 */
@Composable
internal fun HeadlineUnits(
    plan: HeadlinePlan,
    accent: Color,
    ink: Color,
    unitModifier: (Int) -> Modifier,
) {
    val display = KrailTheme.typography.introTagline.asHeadlineStyle()
    val density = LocalDensity.current
    val gap = headlineLineGap()
    val heroPad = headlineHeroPad()
    Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.Start) {
        plan.units.forEachIndexed { index, unit ->
            val colour = if (unit.role == HeadlineRole.Hero && accent != Color.Unspecified) accent else ink
            val unitDensity = remember(density, unit.scale) { Density(density.density, unit.scale) }
            CompositionLocalProvider(LocalDensity provides unitDensity) {
                Column(
                    modifier = unitModifier(index)
                        .padding(vertical = if (unit.role == HeadlineRole.Hero) heroPad else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    horizontalAlignment = Alignment.Start,
                ) {
                    unit.rows.forEach { row ->
                        // A row is a box of exactly one line height tall per line, and a line height is under the height the font
                        // has by nature, so the text is not left to make its own box. Its text keeps the font's own height and sits
                        // centred in the box, as a line box does on the web, so the rows are as snug as they were drawn.
                        var lineCount by remember(row) { mutableIntStateOf(1) }
                        Text(
                            text = introHeadlineDisplay(row),
                            style = display,
                            color = colour,
                            onTextLayout = { lineCount = it.lineCount },
                            modifier = Modifier.headlineLineBox(display.lineHeight) { lineCount },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The height of the emoji, the [emojiGap] under it and [linesHeight] for the lines, all in the density the headline is drawn in, so
 * the room is exactly what the lines take and no more. A headline that can change between a few lines and fewer keeps this much
 * room, so what sits under it never moves when the words change. It grows with the text up to the ceiling, because the lines do.
 */
@Composable
internal fun reservedHeadlineHeight(linesHeight: Dp, hasEmoji: Boolean, emojiGap: Dp): Dp {
    if (linesHeight <= 0.dp) return 0.dp
    val typography = KrailTheme.typography
    return with(headlineDensity()) {
        val emojiHeight = if (hasEmoji) typography.emoji.lineHeight.toDp() + emojiGap else 0.dp
        emojiHeight + linesHeight
    }
}

/** The two values the star's celebration animates: the burst's progress and the star's own opacity. */
internal class Celebration(val burst: Animatable<Float, *>, val fade: Animatable<Float, *>)

/**
 * Plays the celebration for [mode] once and rests: the burst runs 0 to 1 over [BURST_DURATION_MS], the fade runs 0 to 1
 * over [CELEBRATION_FADE_MS], and a settled star has neither. It is saved as played, so a turn of the screen does
 * not play it again.
 */
@Composable
private fun rememberCelebration(
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

// The pop and then the one wobble, in that order, and then it rests. Both finish.
private suspend fun popEmoji(scale: Animatable<Float, *>, tilt: Animatable<Float, *>, delayMs: Long) {
    delay(delayMs)
    scale.animateTo(1f, spring(dampingRatio = EMOJI_DAMPING, stiffness = Spring.StiffnessMediumLow))
    tilt.animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = EmojiWobble.last().first
            EmojiWobble.forEach { (timeMs, degrees) -> degrees at timeMs }
        },
    )
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewIntroHeadlineParkRide() {
    PreviewTheme {
        IntroHeadline(
            lines = PARK_RIDE_HEADLINE,
            emphasisIndex = PARK_RIDE_HERO_INDEX,
            emoji = introEmojiFor(IntroStep.PARK_RIDE),
            heroLayout = true,
        )
    }
}
