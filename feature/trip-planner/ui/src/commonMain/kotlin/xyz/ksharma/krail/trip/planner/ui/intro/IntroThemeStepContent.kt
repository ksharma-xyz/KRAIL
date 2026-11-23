package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIconSize
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.roundToInt

// How long a badge takes to ease from floating into dancing, and back.
private const val DANCE_BLEND_MS = 250

/**
 * The theme screen: the headline on top, the four badges in the middle and, below, the decision bar that the
 * shell draws. The headline asks for a colour until the rider picks one, and from then on it is the tagline of the
 * theme that is selected. Inside the Intro's shell a tap on a badge plays one choreographed sequence on one clock
 * (`IntroThemeChange`, `IntroThemeChangeTimeline.kt`): the badge presses, the new ground washes out from it, the old headline
 * leaves, the emoji pops as the new one, the new tagline comes in line by line and the button shows last. Outside the shell, in a
 * preview, the headline changes with the plain fade and rise of the other screens. The room for it is kept at
 * the longest it can be, so the badges never move when the words do. The badges are centred on the middle of the
 * window, at half the screen height, and only moved as far as it takes to keep a gap under the headline and a gap above
 * the bar (`IntroStepFrame`, [MiddlePlacement.CentredOnWindow]).
 */
@Composable
internal fun IntroThemeStepContent(
    selectedThemeId: Int?,
    onEvent: (IntroUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val headline = introThemeHeadline(selectedThemeId)
    val change = LocalIntroThemeChange.current
    val accent = introAccentColor(IntroStep.THEME)
    IntroStepFrame(
        headline = {
            // The room is that of the tallest headline at this width and text size, measured as it is drawn, so the
            // badges below never move when a choice changes the words.
            BoxWithConstraints {
                val linesHeight = rememberThemeHeadlineHeight(maxWidth)
                if (change == null) {
                    IntroHeadline(
                        lines = headline.lines,
                        emphasisIndex = headline.emphasisIndex,
                        emoji = introEmojiFor(IntroStep.THEME, selectedThemeId),
                        reservedLinesHeight = linesHeight,
                        spoken = headline.spoken,
                        heroLayout = headline.heroLayout,
                        accent = accent,
                    )
                } else {
                    // Inside the Intro's shell the words, the emoji and the colours are all played on the one clock of the sequence.
                    IntroThemeSequenceHeadline(
                        change = change,
                        reservedLinesHeight = linesHeight,
                        accent = accent,
                    )
                }
            }
        },
        modifier = modifier,
        centreOnWindow = true,
    ) {
        IntroThemeBadges(selectedThemeId = selectedThemeId, onPick = { onEvent(IntroUiEvent.PickTheme(it)) })
    }
}

/**
 * The four badges in a wave, one row spread across the whole room, alternately high and low ([themeBadgeGrid]), centred across it. In a
 * room too narrow for that they are in two staggered rows, and in a window too narrow for those in one staggered column, so no
 * two badges are ever closer than the float allows. Every badge that is not selected floats slowly round its own small ellipse and
 * the selected one dances, both from one clock that runs only while the screen is resumed. How far a badge floats is the layout's own
 * (`floatAmplitudeDp`), less where the room is smaller. With reduced motion, in a preview and in a screenshot nothing moves: the
 * selected badge sits lifted 4 dp. The grid is as tall as it is, and the middle zone scrolls with the headline when that does not fit
 * above the bar, so no badge is squeezed, cut or hidden.
 */
@Composable
internal fun IntroThemeBadges(
    selectedThemeId: Int?,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val motionRuns = themeMotionRuns(reduceMotion = isReduceMotionEnabled(), inspection = LocalInspectionMode.current)
    val clock = rememberThemeClock(running = motionRuns)
    val change = LocalIntroThemeChange.current
    val choices = introThemeChoices()

    // The room is the window's width, at most the Intro's content width, less the page padding on both sides. The badge is a fixed
    // size at every text size, so the layout is that room alone.
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = dim.pageHorizontalPadding)) {
        val grid = remember(maxWidth, choices.size) {
            themeBadgeGrid(
                count = choices.size,
                availableDp = maxWidth.value,
                badgeDp = TransportModeIconSize.XLarge.dpSize.value,
            )
        }
        IntroThemeBadgeGrid(grid = grid) {
            choices.forEachIndexed { index, choice ->
                val selected = choice.style.id == selectedThemeId
                val danceWeight = animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(durationMillis = DANCE_BLEND_MS),
                    label = "dance-weight-${choice.name}",
                )
                IntroThemeBadge(
                    choice = choice,
                    selected = selected,
                    frame = pressed(
                        badgeFrameSource(index, selected, motionRuns, clock, danceWeight, grid.floatAmplitudeDp),
                        change,
                        index,
                    ),
                    // The sequence is told which badge it was and where, before the pick, so the wash can grow from it.
                    onClick = {
                        change?.noteTap(index)
                        onPick(choice.style.id)
                    },
                    modifier = Modifier.onGloballyPositioned { coordinates -> change?.attachBadge(index, coordinates) },
                )
            }
        }
    }
}

// The pose of a badge each time it is drawn. With motion it is read from the clock and the blend, and without it,
// it is the settled one. Either way the function itself is only called while drawing.
private fun badgeFrameSource(
    index: Int,
    selected: Boolean,
    motionRuns: Boolean,
    clock: FloatState,
    danceWeight: State<Float>,
    amplitudeDp: Float,
): () -> BadgeFrame {
    if (!motionRuns) {
        val settled = settledFrame(selected)
        return { settled }
    }
    return { badgeFrame(index, clock.floatValue, danceWeight.value, amplitudeDp) }
}

// The pose with the badge's press on top, while the change of theme it started plays: its own scale, times the press's.
private fun pressed(base: () -> BadgeFrame, change: IntroThemeChange?, index: Int): () -> BadgeFrame {
    if (change == null) return base
    return {
        val frame = base()
        val press = change.pressNow(index)
        if (press == 1f) frame else frame.copy(scale = frame.scale * press)
    }
}

/**
 * The height of the room the theme screen's headline keeps for its lines at [width]: the most any of its headlines takes, as drawn.
 * The question is planned for the room, its hero and whispers each at their own size, and each tagline is every line at the
 * ordinary headline size, wrapping inside its own block as it does when it is drawn. It is measured once for the window and the text
 * size, so nothing below the headline moves when a pick changes the words.
 */
@Composable
private fun rememberThemeHeadlineHeight(width: Dp): Dp {
    val density = LocalDensity.current
    val normal = headlineFontScale(density.fontScale, width)
    val question = rememberHeadlinePlan(introThemeHeadline(null).toContent(), room = width)
    val measurer = rememberTextMeasurer()
    val style = KrailTheme.typography.introTagline.asHeadlineStyle()
    val gap = headlineLineGap()
    val heroPad = headlineHeroPad()
    return remember(width, density, question, style, gap, heroPad) {
        val drawn = Density(density.density, normal)
        fun rowsOf(unit: HeadlineUnit): Int = measurer.measure(
            text = AnnotatedString(introHeadlineDisplay(unit.rows.first())),
            style = style,
            constraints = Constraints(maxWidth = with(drawn) { width.roundToPx() }),
            density = drawn,
        ).lineCount
        // The style's line height in sp is its height in dp at font scale 1.0, and a unit draws at its own scale.
        val rowDp = style.lineHeight.value
        val heights = listOf(question) + themeTaglineVariants().map { planPlainHeadline(it, normal) }
        // Only a tagline line can wrap. The question's lines are fitted, so each row of it is one row.
        val rows = { unit: HeadlineUnit -> if (unit.role == HeadlineRole.Plain) rowsOf(unit) else unit.rows.size }
        val snap = { dp: Float -> density.density.let { d -> (dp * d).roundToInt() / d } }
        heights.maxOf { plan -> headlineHeightDp(plan, rowDp, gap.value, heroPad.value, rows, snap) }.dp
    }
}

private fun IntroThemeHeadline.toContent() =
    HeadlineContent(lines, emphasisIndex, emoji = "", spoken = spoken, heroLayout = heroLayout)
