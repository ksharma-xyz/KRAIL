package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

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
