package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.trip.planner.ui.components.MapMotion

/**
 * How far the top box has slid away, 0 in place to 1 gone, moving to where [hidden] says on the one motion spec's curve and
 * duration ([MapMotion.SLIDE_MS]), in both directions, and plainly with reduced motion. The stop card's room is read from
 * the same value, so the card grows as the box leaves.
 */
@Composable
internal fun rememberTopBoxSlide(hidden: Boolean, reduceMotion: Boolean = isReduceMotionEnabled()): State<Float> =
    animateFloatAsState(
        targetValue = if (hidden) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(MapMotion.SLIDE_MS, easing = MapMotion.ENTER_EASING),
        label = "top-box-slide",
    )

/**
 * Slides the top box up out of the screen and fades it, by [progress] (0 in place, 1 gone), while the Departure Board is
 * expanded. The box is not removed: it stays in the tree, so its text, its focus and its selection are exactly as they were
 * and it is the same box that slides back, never a rebuilt one. It travels its own height plus [extraDistancePx] (the status
 * bar and the edge above it), so it leaves the screen entirely. While it is [hidden] it takes no touch and a screen reader
 * does not reach it. Only the box moves: the map's camera and the other controls are not part of it.
 */
internal fun Modifier.slideAwayUp(progress: () -> Float, extraDistancePx: Float, hidden: Boolean): Modifier =
    graphicsLayer {
        val p = progress().coerceIn(0f, 1f)
        translationY = -p * (size.height + extraDistancePx)
        alpha = 1f - p
    }.then(
        if (hidden) {
            Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                }
                .clearAndSetSemantics {}
        } else {
            Modifier
        },
    )
