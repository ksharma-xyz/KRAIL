package xyz.ksharma.krail.trip.planner.ui.intro

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * How much bigger the headline is than the app's `introTagline` style: the style's size and line height times this.
 * The headline is meant to be the loudest thing on the screen. 1.7 is 47.6 sp on a 61.2 sp line.
 */
internal const val HEADLINE_SIZE_FACTOR = 1.7f

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
 * (CONCRETE is about 243 dp for each 1.0 of scale at [HEADLINE_SIZE_FACTOR]), and 1.05 leaves another 4 percent for a
 * font that is wider, which is a guess for the system font on iOS.
 */
internal const val HEADLINE_FONT_SCALE_CEILING = 1.05f

/** The text a headline line is drawn as: ALL UPPER CASE, with the invariant rules, so a locale never changes a letter. */
internal fun introHeadlineDisplay(line: String): String = line.uppercase()

/** The font scale the headline is drawn at for the system scale [systemScale]: the same, up to the ceiling. */
internal fun headlineFontScale(systemScale: Float): Float = systemScale.coerceAtMost(HEADLINE_FONT_SCALE_CEILING)

/**
 * The display style of the headline: the app's `introTagline` style, [HEADLINE_SIZE_FACTOR] times as large, at
 * [HEADLINE_WEIGHT].
 */
internal fun TextStyle.asHeadlineStyle(): TextStyle = copy(
    fontSize = fontSize * HEADLINE_SIZE_FACTOR,
    lineHeight = lineHeight * HEADLINE_SIZE_FACTOR,
    fontWeight = HEADLINE_WEIGHT,
)

/** The room between the emoji and the first line, which grows with the text: [base] at 100 percent and up to twice that. */
internal fun introEmojiTitleGap(base: Dp, fontScale: Float): Dp = base * fontScale.coerceIn(1f, 2f)

/** The density the headline is drawn in: the screen's own, with the font scale held at [HEADLINE_FONT_SCALE_CEILING]. */
@Composable
internal fun headlineDensity(): Density {
    val density = LocalDensity.current
    return remember(density) { Density(density.density, headlineFontScale(density.fontScale)) }
}

// Each line is a spring with a small overshoot that settles in about 300 ms, so a headline of three lines is
// in place well inside half a second. The emoji bounces more, as a sticker would.
private const val LINE_DAMPING = 0.7f
private const val EMOJI_DAMPING = 0.5f
private val LineRiseDistance = 24.dp

/**
 * The big, bold headline at the top of an Intro screen. [lines] is two to four short lines, one idea on
 * each, and [emphasisIndex] is the line with the key word, which lands last in the entrance with a beat more. Every
 * line is at the same weight, the heaviest there is, and no colour competes with the theme colour on the primary
 * button. The lines are left aligned, wrap rather than shrink or truncate at a large text size, and use the app's own
 * boldest display style, drawn ALL UPPER CASE ([introHeadlineDisplay]), [HEADLINE_SIZE_FACTOR] times its size and at
 * [HEADLINE_WEIGHT]. The case is a display matter only: the lines passed in, the screen reader's heading and every test
 * that reads the copy keep the case the words are written in. The headline grows with the system text size only up to
 * [HEADLINE_FONT_SCALE_CEILING], so its longest word always fits. The emoji sits [introEmojiTitleGap] above the first
 * line, further as the text grows.
 *
 * Each line is its own animated unit and enters one after another: it rises with a small spring overshoot,
 * grows a little and fades in, [LINE_STAGGER_MS] after the one before. The [emoji] pops with a bounce and
 * one wobble, then rests, so every animation is finite. With reduced motion, in a preview and after a turn
 * of the screen the headline is simply in place. Only transforms are animated, through `graphicsLayer`, so
 * nothing is laid out again on a frame.
 *
 * [celebrate] is for the confirm screen's star. With it the star also throws a burst of plain shapes in the theme
 * colour, drawn behind it, which flies out and fades in [BURST_DURATION_MS]. With reduced motion the star fades in at
 * rest instead and nothing flies, and in a preview, a screenshot or after a turn of the screen it is simply there.
 *
 * [minLines] is the least number of lines of room to keep, for a screen whose headline changes length: with it
 * the headline takes the height of the emoji and that many lines whatever it says, so nothing below it moves.
 *
 * For a screen reader the whole headline is one heading whose text is the full phrase, with no pause
 * between lines, and the emoji is hidden from it.
 */
@Composable
internal fun IntroHeadline(
    lines: List<String>,
    emphasisIndex: Int,
    emoji: String,
    modifier: Modifier = Modifier,
    minLines: Int = 0,
    celebrate: Boolean = false,
) {
    val phrase = introHeadlinePhrase(lines)
    val reduceMotion = isReduceMotionEnabled()
    val inspection = LocalInspectionMode.current
    // Saved, so a turn of the screen does not play it again. Keyed on the phrase, so a new headline does.
    var played by rememberSaveable(phrase) { mutableStateOf(false) }
    val animate = remember(phrase) { !headlineStartsSettled(reduceMotion, inspection, played) }
    val celebration = rememberCelebration(phrase, celebrate, reduceMotion, inspection)

    val progress = remember(phrase) { lines.map { Animatable(if (animate) 0f else 1f) } }
    val emojiScale = remember(phrase) { Animatable(if (animate) 0f else 1f) }
    val emojiTilt = remember(phrase) { Animatable(0f) }

    LaunchedEffect(phrase) {
        if (!animate) return@LaunchedEffect
        played = true
        val timeline = headlineTimeline(lines.size, emphasisIndex)
        launch { popEmoji(emojiScale, emojiTilt, timeline.emojiDelayMs) }
        progress.forEachIndexed { index, line ->
            launch {
                delay(timeline.lineDelaysMs[index])
                line.animateTo(1f, spring(dampingRatio = LINE_DAMPING, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    // The headline grows with the system text size only up to the ceiling, so a word in capitals always fits.
    val lineGap = KrailTheme.dimensions.spacingS
    val emojiGap = introEmojiTitleGap(KrailTheme.dimensions.spacingXXL, LocalDensity.current.fontScale)
    CompositionLocalProvider(LocalDensity provides headlineDensity()) {
        Column(
            modifier = modifier
                .heightIn(min = reservedHeadlineHeight(minLines, hasEmoji = emoji.isNotEmpty(), emojiGap = emojiGap))
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
                        },
                )
            }
            val display = KrailTheme.typography.introTagline.asHeadlineStyle()
            lines.forEachIndexed { index, line ->
                Text(
                    text = introHeadlineDisplay(line),
                    style = display,
                    color = KrailTheme.colors.onSurface,
                    modifier = Modifier.graphicsLayer {
                        val frame = lineFrame(progress[index].value)
                        alpha = frame.alpha
                        translationY = frame.rise * LineRiseDistance.toPx()
                        scaleX = frame.scale
                        scaleY = frame.scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
                )
            }
        }
    }
}

/**
 * The height of the emoji, the [emojiGap] under it and [minLines] lines of the display style with the gaps between them,
 * all in the density the headline is drawn in, so the room is exactly what the lines take and no more. A headline that
 * can change between a few lines and fewer keeps this much room, so what sits under it never moves when the words
 * change. It grows with the text up to the ceiling, because the lines do.
 */
@Composable
private fun reservedHeadlineHeight(minLines: Int, hasEmoji: Boolean, emojiGap: Dp): Dp {
    if (minLines <= 0) return 0.dp
    val typography = KrailTheme.typography
    val gap = KrailTheme.dimensions.spacingS
    return with(headlineDensity()) {
        val emojiHeight = if (hasEmoji) typography.emoji.lineHeight.toDp() + emojiGap else 0.dp
        emojiHeight + (typography.introTagline.asHeadlineStyle().lineHeight.toDp() + gap) * minLines - gap
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
            emphasisIndex = PARK_RIDE_HEADLINE.lastIndex,
            emoji = introEmojiFor(IntroStep.PARK_RIDE),
        )
    }
}
