package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import kotlin.math.roundToInt
import xyz.ksharma.krail.trip.planner.ui.components.MapMotion

/** The angle of the Departure Board's chevron, as the screen draws it right now, for a screen reader's tree and a test. */
internal val ChevronDegrees = SemanticsPropertyKey<Float>("ChevronDegrees")
internal var SemanticsPropertyReceiver.chevronDegreesValue by ChevronDegrees

internal const val CHEVRON_OPEN_DEGREES = 180f

/**
 * How the Departure Board opens and closes, from one transition so the parts move together
 * (`docs/MAP_HOME_UX.md`, Motion): the card's height ([reveal], 0 to 1), the chevron ([chevronDegrees], 0 to 180) and the
 * rows' fade ([rowsAlpha]). The height and the chevron take [MapMotion.EXPAND_MS] on the same curve. The rows begin to
 * fade in [MapMotion.ROWS_DELAY_MS] after the height starts and are gone [MapMotion.EXIT_MS] after a close begins, well
 * before the height ends, so the rows are never cut half way through a fade. With reduced motion all of it is plain.
 *
 * [composed] is whether the rows are still in the tree: while the board is open, and until the close has finished, so they
 * are there to be revealed and hidden and never removed under the shrinking height.
 */
@Stable
internal class BoardMotion(
    val reveal: () -> Float,
    val rowsAlpha: () -> Float,
    val chevronDegrees: () -> Float,
    val composed: Boolean,
)

@Composable
internal fun rememberBoardMotion(open: Boolean, reduceMotion: Boolean): BoardMotion {
    val transition = updateTransition(targetState = open, label = "stop-card-board")
    val reveal = transition.animateFloat(
        transitionSpec = {
            if (reduceMotion) snap() else tween(MapMotion.EXPAND_MS, easing = MapMotion.ENTER_EASING)
        },
        label = "board-height",
    ) { if (it) 1f else 0f }
    val chevron = transition.animateFloat(
        transitionSpec = {
            if (reduceMotion) snap() else tween(MapMotion.EXPAND_MS, easing = MapMotion.ENTER_EASING)
        },
        label = "board-chevron",
    ) { if (it) CHEVRON_OPEN_DEGREES else 0f }
    val rows = transition.animateFloat(
        transitionSpec = {
            when {
                reduceMotion -> snap()
                targetState -> tween(
                    durationMillis = MapMotion.EXPAND_MS - MapMotion.ROWS_DELAY_MS,
                    delayMillis = MapMotion.ROWS_DELAY_MS,
                    easing = MapMotion.FADE_IN_EASING,
                )
                else -> tween(MapMotion.EXIT_MS, easing = MapMotion.EXIT_EASING)
            }
        },
        label = "board-rows",
    ) { if (it) 1f else 0f }
    return BoardMotion(
        reveal = { reveal.value },
        rowsAlpha = { rows.value },
        chevronDegrees = { chevron.value },
        composed = open || transition.currentState,
    )
}

/**
 * Takes [progress] of its content's height, from none to all of it, with the content laid out at its full height from the
 * first frame, so nothing inside reflows or is cut and then pops as the height moves. The width is whatever the parent gives it.
 */
internal fun Modifier.revealHeight(progress: () -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = (placeable.height * progress().coerceIn(0f, 1f)).roundToInt()
    layout(placeable.width, height) { placeable.place(0, 0) }
}
