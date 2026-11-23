package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.theme.KrailDimensions

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
