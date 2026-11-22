package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailTheme
import kotlin.math.max

/**
 * The vertical rhythm every Intro screen shares, all from the dimension tokens so no screen has a number of its own.
 * [top] is the room above the headline, below the status bar. [titleToMiddle] is between the headline and what the screen
 * asks. [middleBottom] is below the middle zone, above the bar. [fieldToList] is between the search field and the first row
 * of a list. The room between the emoji and the title grows with the text and is the headline's own (`introEmojiTitleGap`),
 * and the room between two lines of the headline is `spacingS`.
 */
internal data class IntroRhythm(val top: Dp, val titleToMiddle: Dp, val middleBottom: Dp, val fieldToList: Dp)

/** The rhythm for [dim]: 16 dp above the headline, 24 dp under it, 16 dp under the middle zone and under a search field. */
internal fun introRhythm(dim: KrailDimensions): IntroRhythm = IntroRhythm(
    top = dim.spacingXL,
    titleToMiddle = dim.spacingXXXL,
    middleBottom = dim.spacingXL,
    fieldToList = dim.spacingXL,
)

/**
 * Where the middle zone starts, in pixels from the top. It sits just under the headline, and when there is
 * room to spare in the [zone] below the headline it is centred in that room. When there is none, because
 * the headline and the middle fill the zone or more, it starts straight under the headline.
 */
internal fun introMiddleTop(zone: Int, headline: Int, middle: Int): Int =
    headline + max(0, zone - headline - middle) / 2

/**
 * The shape every Intro question screen shares: the headline at the top, the thing to decide on in the
 * middle, centred in the space under the headline, and the decision bar, which the shell lays over the bottom of the
 * screen and this does not hold. Both zones sit in one scrolling column that goes full height, edge to edge down to the
 * bottom of the screen's room, so nothing is clipped by the bar's slot. When the text is large or the screen is short
 * the headline and the middle scroll together and nothing is out of reach. With room to spare the headline stays at the
 * top and the middle is centred.
 *
 * The bar is read from [LocalIntroBarInset]. Its slot is always kept out of the room the middle is centred in, so the
 * middle does not move when the bar appears. Only while the bar is shown does the column get room at its end for it
 * (the height of the bar), so the last row can always scroll clear of it, and that changes only how far the column
 * scrolls, never where a row on screen is.
 *
 * A screen whose middle changes height while the rider uses it, such as a list that is searched, passes
 * [centreMiddle] false: the middle then starts straight under the headline, so it never moves when its own
 * height does.
 */
@Composable
internal fun IntroStepFrame(
    headline: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    centreMiddle: Boolean = true,
    middle: @Composable () -> Unit,
) {
    val dim = KrailTheme.dimensions
    val rhythm = introRhythm(dim)
    val bar = LocalIntroBarInset.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val zone = maxHeight
        // The headline's room, which it reads to keep its widest word whole in the narrowest windows.
        val headlineRoom = maxWidth - dim.pageHorizontalPadding * 2
        CompositionLocalProvider(LocalHeadlineRoom provides headlineRoom) {
            IntroFrameLayout(
                zone = zone,
                bar = bar,
                centreMiddle = centreMiddle,
                headline = {
                    Box(
                        modifier = Modifier.padding(
                            start = dim.pageHorizontalPadding,
                            end = dim.pageHorizontalPadding,
                            top = rhythm.top,
                            bottom = rhythm.titleToMiddle,
                        ),
                    ) {
                        headline()
                    }
                },
                middle = {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = rhythm.middleBottom)) { middle() }
                },
            )
        }
    }
}

// The two zones in one scrolling column: the headline at the top, and the middle just under it or centred in the room
// that is left once the bar's slot is taken out. The column is as tall as the zone or its content plus, while the bar
// is shown, the bar's height, so the end of the scroll clears the bar and nothing above it moves.
@Composable
private fun IntroFrameLayout(
    zone: Dp,
    bar: IntroBarInset,
    centreMiddle: Boolean,
    headline: @Composable () -> Unit,
    middle: @Composable () -> Unit,
) {
    Layout(
        content = {
            headline()
            middle()
        },
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) { measurables, constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth)
        val headlinePlaceable = measurables[0].measure(loose)
        val middlePlaceable = measurables[1].measure(loose)
        val zonePx = zone.roundToPx()
        val middleTop = if (centreMiddle) {
            introMiddleTop(max(0, zonePx - bar.reservedPx), headlinePlaceable.height, middlePlaceable.height)
        } else {
            headlinePlaceable.height
        }
        layout(constraints.maxWidth, max(zonePx, middleTop + middlePlaceable.height + bar.scrollEndPx)) {
            headlinePlaceable.place(0, 0)
            middlePlaceable.place(0, middleTop)
        }
    }
}
