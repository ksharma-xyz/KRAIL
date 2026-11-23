package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
