package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.compositionLocalOf

/**
 * What the decision bar takes of the screen, which the frame of each Intro screen reads. [reservedPx] is the height of
 * the bar's slot, which is there whether or not the bar is [shown], so a screen that centres its middle zone keeps it
 * where it is when the bar appears. [shown] is whether the rider can see the bar. Only a bar that is shown gets room at
 * the end of the scroll, because only then is it over the last of the content.
 */
internal data class IntroBarInset(val reservedPx: Int, val shown: Boolean) {
    /** The room a scrolling middle zone leaves at its end so its last row can scroll clear of the bar: none while hidden. */
    val scrollEndPx: Int get() = if (shown) reservedPx else 0
}

/** No bar over the content: a frame drawn outside the Intro shell, such as in a preview, or a screen that has none. */
internal val LocalIntroBarInset = compositionLocalOf { IntroBarInset(reservedPx = 0, shown = false) }
