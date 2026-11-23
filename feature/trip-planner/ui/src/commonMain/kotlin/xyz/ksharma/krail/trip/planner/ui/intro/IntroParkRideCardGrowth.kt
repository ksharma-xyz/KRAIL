package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize

/**
 * How long the Park & Ride card takes to grow when the skeleton, which holds one car park, becomes the card of a station with
 * several. It is the length of the Intro's other changes of look ([BAR_FILL_CROSSFADE_MS]), so the screen moves at one pace.
 */
internal const val PARK_RIDE_CARD_GROWTH_MS = BAR_FILL_CROSSFADE_MS

/** The way the card's room grows: [PARK_RIDE_CARD_GROWTH_MS] with the Intro's own easing, as the colour of the bar changes. */
internal fun introCardGrowthSpec(): FiniteAnimationSpec<IntSize> =
    tween(durationMillis = PARK_RIDE_CARD_GROWTH_MS, easing = FastOutSlowInEasing)

/**
 * Whether the room of this slot animates its size. Only while a card is in it, the skeleton or the station card: the empty
 * slot has no height to grow from, so the skeleton still appears at its own height at once, as it always did, and a card that
 * leaves still goes at once. Under [reduceMotion] never, so the room takes the height of the station in the frame the station
 * is found, with no frame held at the old height.
 */
internal fun IntroCardSlot.animatesGrowth(reduceMotion: Boolean): Boolean = !reduceMotion && this != IntroCardSlot.Empty
