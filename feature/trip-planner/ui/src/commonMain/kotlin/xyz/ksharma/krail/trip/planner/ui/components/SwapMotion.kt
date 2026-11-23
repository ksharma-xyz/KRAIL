package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled

/**
 * The swap's motion, held by the card. [lift] is how far the swap has glided from the divider level (0) up to the From row's
 * centre line (1), and [fade] is its alpha. Both are animations read in the draw phase, so a frame of either redraws and
 * does not recompose. [present] is whether the swap is in the tree at all: it stays while it fades out and then leaves.
 */
@Stable
internal class SwapMotion(
    val lift: Animatable<Float, *>,
    val fade: Animatable<Float, *>,
    present: Boolean,
) {
    var present by mutableStateOf(present)

    /** A tap is answered by what is under it at that instant: from [MapMotion.SWAP_TAP_ALPHA] while it fades in, never while it leaves. */
    fun takesTaps(shown: Boolean): Boolean = shown && fade.value >= MapMotion.SWAP_TAP_ALPHA
}

/**
 * The swap's motion for [shown] (at least one stop chosen and no X on the card) and [lifted] (both stops chosen, so the go
 * circle is in the To row and the swap has made room). It fades in or out in [MapMotion.SWAP_FADE_MS] with no move, and glides
 * between its two heights in [MapMotion.SWAP_GLIDE_MS], each from wherever it is. Reduced motion jumps in one frame.
 */
@Composable
internal fun rememberSwapMotion(shown: Boolean, lifted: Boolean): SwapMotion {
    val reduceMotion = isReduceMotionEnabled()
    val motion = remember { SwapMotion(Animatable(if (lifted) 1f else 0f), Animatable(if (shown) 1f else 0f), shown) }
    LaunchedEffect(lifted, reduceMotion) {
        val target = if (lifted) 1f else 0f
        when {
            reduceMotion -> motion.lift.snapTo(target)
            motion.lift.value != target -> motion.lift.animateTo(target, swapGlideSpec())
        }
    }
    LaunchedEffect(shown, reduceMotion) {
        val target = if (shown) 1f else 0f
        if (shown) motion.present = true
        when {
            reduceMotion -> motion.fade.snapTo(target)
            motion.fade.value != target -> motion.fade.animateTo(target, swapFadeSpec())
        }
        if (!shown) motion.present = false
    }
    return motion
}
