package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
