package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
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
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// Each line is a spring with a small overshoot that settles in about 300 ms, so a headline of three lines is
// in place well inside half a second. The emoji bounces more, as a sticker would.
private const val LINE_DAMPING = 0.7f
private const val EMOJI_DAMPING = 0.5f
private val LineRiseDistance = 24.dp

/**
 * The big, bold headline at the top of an Intro screen. [lines] is two to four short lines, one idea on
 * each, and [emphasisIndex] is the line with the key word, which takes the heaviest weight. Emphasis is
 * weight only, so no colour competes with the theme colour on the primary button. The lines are left aligned,
 * wrap rather than shrink or truncate at a large text size, and use the app's own boldest display style.
 *
 * Each line is its own animated unit and enters one after another: it rises with a small spring overshoot,
 * grows a little and fades in, [LINE_STAGGER_MS] after the one before. The [emoji] pops with a bounce and
 * one wobble, then rests, so every animation is finite. With reduced motion, in a preview and after a turn
 * of the screen the headline is simply in place. Only transforms are animated, through `graphicsLayer`, so
 * nothing is laid out again on a frame.
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
) {
    val phrase = introHeadlinePhrase(lines)
    val reduceMotion = isReduceMotionEnabled()
    val inspection = LocalInspectionMode.current
    // Saved, so a turn of the screen does not play it again. Keyed on the phrase, so a new headline does.
    var played by rememberSaveable(phrase) { mutableStateOf(false) }
    val animate = remember(phrase) { !headlineStartsSettled(reduceMotion, inspection, played) }

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

    Column(
        modifier = modifier
            .heightIn(min = reservedHeadlineHeight(minLines, hasEmoji = emoji.isNotEmpty()))
            .clearAndSetSemantics {
                heading()
                text = AnnotatedString(phrase)
            },
        verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingS),
        horizontalAlignment = Alignment.Start,
    ) {
        if (emoji.isNotEmpty()) {
            Text(
                text = emoji,
                style = KrailTheme.typography.emoji,
                modifier = Modifier.graphicsLayer {
                    scaleX = emojiScale.value
                    scaleY = emojiScale.value
                    rotationZ = emojiTilt.value
                },
            )
        }
        val display = KrailTheme.typography.introTagline
        lines.forEachIndexed { index, line ->
            val style = if (index == emphasisIndex) {
                display
            } else {
                display.copy(fontWeight = leadLineWeight(display.fontWeight))
            }
            Text(
                text = line,
                style = style,
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

/**
 * The height of the emoji and [minLines] lines of the display style, with the gaps between them, at the rider's
 * text size. A headline that can change between a few lines and fewer keeps this much room, so what sits
 * under it never moves when the words change. It grows with the text, because the lines do.
 */
@Composable
private fun reservedHeadlineHeight(minLines: Int, hasEmoji: Boolean): Dp {
    if (minLines <= 0) return 0.dp
    val typography = KrailTheme.typography
    val gap = KrailTheme.dimensions.spacingS
    return with(LocalDensity.current) {
        val emojiHeight = if (hasEmoji) typography.emoji.lineHeight.toDp() + gap else 0.dp
        emojiHeight + (typography.introTagline.lineHeight.toDp() + gap) * minLines - gap
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
            emphasisIndex = PARK_RIDE_HEADLINE.lastIndex,
            emoji = introEmojiFor(IntroStep.PARK_RIDE),
        )
    }
}
