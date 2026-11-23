package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color

// What each element of the theme change sequence is at this moment, read from the one clock of [IntroThemeChange]. They are read
// inside draw and layer lambdas, so a frame redraws and does not compose or lay out, and none of them allocates.

/** The theme colour now, blended on the one clock. */
internal fun IntroThemeChange.colourNow(): Color = spec.colourAt(clock.value)

/** The headline's text colour now, on the same blend. */
internal fun IntroThemeChange.textNow(): Color = spec.textAt(clock.value)

/** The emoji's scale now. */
internal fun IntroThemeChange.emojiScaleNow(): Float = themeEmojiScale(
    clock.value,
    spec.headline.emojiScaleAtStart,
    spec.reduceMotion,
)

/** The scale of badge [index] from its press now. */
internal fun IntroThemeChange.pressNow(index: Int): Float = spec.pressAt(index, clock.value)

/** Opacity of line [index] of the headline that is leaving, now. */
internal fun IntroThemeChange.outgoingAlphaNow(index: Int): Float {
    val s = spec
    return themeOutgoingAlpha(
        s.headline.outgoingLooks.alphas.getOrElse(index) { 0f },
        themeOutgoing(clock.value, s.reduceMotion),
    )
}

/** How far line [index] of the headline that is leaving is moved now, in dp. */
internal fun IntroThemeChange.outgoingRiseNowDp(index: Int): Float {
    val s = spec
    return themeOutgoingRiseDp(
        s.headline.outgoingLooks.risesDp.getOrElse(index) { 0f },
        themeOutgoing(clock.value, s.reduceMotion),
        s.reduceMotion,
    )
}

/** Opacity of line [index] of the headline that is arriving, now. */
internal fun IntroThemeChange.incomingAlphaNow(
    index: Int,
): Float = themeIncomingLine(clock.value, index, spec.reduceMotion)

/** How far line [index] of the headline that is arriving is moved now, in dp. */
internal fun IntroThemeChange.incomingRiseNowDp(
    index: Int,
): Float = themeIncomingRiseDp(incomingAlphaNow(index), spec.reduceMotion)
