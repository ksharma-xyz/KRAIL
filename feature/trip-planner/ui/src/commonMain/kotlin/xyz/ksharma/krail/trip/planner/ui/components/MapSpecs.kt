package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween

// The go circle's, the swap's and the card's animation specs, read from the numbers in [MapMotion].

/**
 * The go circle's scale on its way in is two plain eases, not a spring: it grows from wherever it is to
 * [MapMotion.GO_PEAK_SCALE] in [MapMotion.GO_PEAK_MS] ([goGrowSpec]) and then settles back to full size by
 * [MapMotion.GO_ENTER_MS] ([goSettleSpec]), both on the arrival curve. They are run one after the other on one animation, so a
 * circle that was part way out turns round and grows from there.
 */
internal fun goGrowSpec(): AnimationSpec<Float> = tween(MapMotion.GO_PEAK_MS, easing = MapMotion.ENTER_EASING)

/** The second of the go circle's two eases: from the peak back to full size, 120 ms. */
internal fun goSettleSpec(): AnimationSpec<Float> =
    tween(MapMotion.GO_ENTER_MS - MapMotion.GO_PEAK_MS, easing = MapMotion.ENTER_EASING)

/** The go circle's fade in is quick, so it is solid long before it has grown. */
internal fun goAppearFadeSpec(): AnimationSpec<Float> = tween(MapMotion.GO_FADE_IN_MS, easing = LinearEasing)

/** On the way out the scale falls as 1 minus p squared, so it stays near full size at first and is gone by the end. */
internal fun goLeaveScaleSpec(): AnimationSpec<Float> = tween(MapMotion.GO_LEAVE_MS, easing = GO_LEAVE_EASING)

/** And the alpha falls straight, 1 minus p. */
internal fun goLeaveFadeSpec(): AnimationSpec<Float> = tween(MapMotion.GO_LEAVE_MS, easing = LinearEasing)

private val GO_LEAVE_EASING = Easing { fraction -> fraction * fraction }

/**
 * The card spec: how the box and the lone pill fade into one another, and how the hairline of the box and the notice fades in and
 * out when the ground changes between the map state and the list state, the same [MapMotion.ENTER_MS] on the arrival curve. The
 * fill, text and glyphs do not change between the two states, so they are not on it. A snap with reduced motion, so the hairline
 * jumps in one frame.
 */
internal fun <T> mapCardFadeSpec(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) snap() else tween(MapMotion.ENTER_MS, easing = MapMotion.ENTER_EASING)

/** The swap's glide between the divider level and the From row's centre line, on the arrival curve. */
internal fun swapGlideSpec(): AnimationSpec<Float> = tween(MapMotion.SWAP_GLIDE_MS, easing = MapMotion.ENTER_EASING)

/** The swap's fade in and out: linear, no bounce and no move. */
internal fun swapFadeSpec(): AnimationSpec<Float> = tween(MapMotion.SWAP_FADE_MS, easing = LinearEasing)
