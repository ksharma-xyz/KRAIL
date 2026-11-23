package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * The one motion spec of the map screens (`docs/MAP_HOME_UX.md`, Motion). Every animated thing on Search Stop reads its
 * numbers from here, so the screen moves as one piece and a change is made in one place.
 *
 * What moves is finite and short, never a loop. Something arriving takes [ENTER_MS] on a decelerating curve
 * ([ENTER_EASING]) while it fades in over [FADE_IN_MS]. Something leaving is quicker, [EXIT_MS], on an accelerating
 * curve ([EXIT_EASING]), because a rider who has already moved on should not wait for it. A card or a control that
 * grows out of where it came from (the Options card, the Options button) scales from [POP_START_SCALE], 80 percent, and
 * one that is not a card (the location control) scales from nothing. Text that changes in place cross fades in
 * [CROSSFADE_MS]. With reduced motion every one of them is plain: the thing is there or it is not, with no frame between.
 */
internal object MapMotion {
    /** How long something takes to arrive, in milliseconds. */
    const val ENTER_MS = 200

    /** How long the fade of an arrival takes, a touch shorter than the movement so it is solid before it stops. */
    const val FADE_IN_MS = 150

    /** How long something takes to leave. */
    const val EXIT_MS = 150

    /** Where a card or a button grows from, 80 percent of its size. */
    const val POP_START_SCALE = 0.8f

    /** Where the location control grows from and shrinks to: nothing. */
    const val VANISH_SCALE = 0f

    /** A label that changes in place cross fades in this long. */
    const val CROSSFADE_MS = 150

    /** How long the card's height, the chevron and the rows of the Departure Board take to move together. */
    const val EXPAND_MS = 300

    /** The rows of the board wait this long after the height starts to move, and are gone this long before it ends. */
    const val ROWS_DELAY_MS = 60

    /** The top box slides away and back in this long. */
    const val SLIDE_MS = 250

    /** The go circle arrives in this long: it grows past full size and settles back, see [goGrowSpec]. */
    const val GO_ENTER_MS = 300

    /** When the go circle is at its peak, [GO_PEAK_SCALE], on the way in. */
    const val GO_PEAK_MS = 180

    /** How big the go circle grows before it settles to its own size: 48 dp from 40 dp, still inside its 48 dp target. */
    const val GO_PEAK_SCALE = 1.2f

    /** The go circle's fade in is quick, so it is solid long before it has grown. */
    const val GO_FADE_IN_MS = 80

    /** The go circle leaves in this long: its scale is 1 minus p squared and its alpha 1 minus p, see [goLeaveScaleSpec]. */
    const val GO_LEAVE_MS = EXIT_MS

    /** The go circle takes taps from this scale on its way in, and none from the first frame of its way out. */
    const val GO_TAP_SCALE = 0.9f

    /** The half turn of the swap glyph, the Timetable's own reverse button turn. */
    const val SWAP_TURN_MS = 300

    /** The swap glides between the divider level and the From row's centre line in this long, together with the go circle. */
    const val SWAP_GLIDE_MS = 200

    /** The swap fades in, and out, in this long: no bounce and no move. */
    const val SWAP_FADE_MS = 150

    /** The swap takes taps from this alpha while it fades in, and none while it fades out. */
    const val SWAP_TAP_ALPHA = 0.5f

    val ENTER_EASING = FastOutSlowInEasing
    val FADE_IN_EASING = LinearOutSlowInEasing
    val EXIT_EASING = FastOutLinearInEasing

    /** A control that is not a card stands on its own centre. */
    val CENTRE = TransformOrigin.Center

    /** The Options card and the Options button grow out of the bottom start corner. */
    val BOTTOM_START = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 1f)
}

/** Fades in while scaling up from [MapMotion.POP_START_SCALE] about [origin]; plain, with no frame between, if [reduceMotion]. */
internal fun mapPopIn(reduceMotion: Boolean, origin: TransformOrigin = MapMotion.BOTTOM_START): EnterTransition =
    if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(MapMotion.FADE_IN_MS, easing = MapMotion.FADE_IN_EASING)) +
            scaleIn(
                initialScale = MapMotion.POP_START_SCALE,
                transformOrigin = origin,
                animationSpec = tween(MapMotion.ENTER_MS, easing = MapMotion.ENTER_EASING),
            )
    }

/** Fades out while scaling back down to [MapMotion.POP_START_SCALE] about [origin], a little quicker than it came. */
internal fun mapPopOut(reduceMotion: Boolean, origin: TransformOrigin = MapMotion.BOTTOM_START): ExitTransition =
    if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING)) +
            scaleOut(
                targetScale = MapMotion.POP_START_SCALE,
                transformOrigin = origin,
                animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.ENTER_EASING),
            )
    }

/** Grows from nothing to full size about its own centre while it fades in. */
internal fun mapVanishIn(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(MapMotion.FADE_IN_MS, easing = MapMotion.FADE_IN_EASING)) +
            scaleIn(
                initialScale = MapMotion.VANISH_SCALE,
                transformOrigin = MapMotion.CENTRE,
                animationSpec = tween(MapMotion.ENTER_MS, easing = MapMotion.ENTER_EASING),
            )
    }

/** Shrinks to nothing about its own centre while it fades out. */
internal fun mapVanishOut(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING)) +
            scaleOut(
                targetScale = MapMotion.VANISH_SCALE,
                transformOrigin = MapMotion.CENTRE,
                animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.ENTER_EASING),
            )
    }

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

/** The swap's glide between the divider level and the From row's centre line, on the arrival curve. */
internal fun swapGlideSpec(): AnimationSpec<Float> = tween(MapMotion.SWAP_GLIDE_MS, easing = MapMotion.ENTER_EASING)

/** The swap's fade in and out: linear, no bounce and no move. */
internal fun swapFadeSpec(): AnimationSpec<Float> = tween(MapMotion.SWAP_FADE_MS, easing = LinearEasing)

/** A notice that comes and goes in place: it fades while its height grows or shrinks, on the arrival and leaving curves; plain with reduced motion. */
internal fun mapSizeFadeIn(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(MapMotion.FADE_IN_MS, easing = MapMotion.FADE_IN_EASING)) +
            expandVertically(animationSpec = tween(MapMotion.ENTER_MS, easing = MapMotion.ENTER_EASING))
    }

/** And leaves in place: it fades while its height shrinks to nothing, a little quicker than it came. */
internal fun mapSizeFadeOut(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING)) +
            shrinkVertically(animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING))
    }

/** A card that arrives from below: it slides up by its own height while it fades in, on the arrival curve; plain with reduced motion. */
internal fun mapSlideUpIn(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(MapMotion.FADE_IN_MS, easing = MapMotion.FADE_IN_EASING)) +
            slideInVertically(
                animationSpec = tween(MapMotion.ENTER_MS, easing = MapMotion.ENTER_EASING),
                initialOffsetY = { it },
            )
    }

/** And leaves downward the way it came, a little quicker: it slides down by its own height while it fades out. */
internal fun mapSlideDownOut(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING)) +
            slideOutVertically(
                animationSpec = tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING),
                targetOffsetY = { it },
            )
    }

/**
 * A control that comes and goes with [visible] by [enter] and [exit], and takes no tap while it is on its way out or gone:
 * once [visible] is false the control is still drawn for the length of its exit, and the touches over it are consumed so
 * the control does not act while it fades. It leaves the tree when it has gone, so a hidden control has no node at all
 * and takes up no room. The control's own layout is final from its first frame, so nothing around it moves while it scales.
 */
@Composable
internal fun MapControlVisibility(
    visible: Boolean,
    enter: EnterTransition,
    exit: ExitTransition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = enter, exit = exit) {
        val live = transition.targetState == EnterExitState.Visible
        Box(modifier = if (live) Modifier else Modifier.consumeTouches()) { content() }
    }
}

/** Consumes every touch over it before its children see one, on the initial pass. */
private fun Modifier.consumeTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}
