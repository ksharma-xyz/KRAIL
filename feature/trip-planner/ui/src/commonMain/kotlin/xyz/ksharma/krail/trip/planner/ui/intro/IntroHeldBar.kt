package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/**
 * A decision bar as it was last shown to the rider: the [step] it belonged to, its content ([bar], the words, the events and the
 * label pairs its row or stack was measured for) and the [fill] its primary button wore at that moment, which is null on the colour
 * screen. It is what a bar that is going away keeps drawing, so it leaves in its own clothes and not the next screen's. Each one
 * is its own object, so a bar that is shown again later is a new one.
 */
internal class IntroHeldBar(val step: IntroStep, val bar: IntroBar, val fill: Color?)

/**
 * The bar that is leaving, or null when none is. A bar leaves when the Intro has moved to a different step than the one [held] was
 * shown on and the bar of the new step is not shown yet: the colour screen and the two stop screens open with the button hidden
 * until the rider picks something, so without this the old bar faded out wearing the new screen's words, layout and fill, before the
 * new headline had arrived. When the new bar is shown it simply takes over, and on the same step nothing is held, because the bar of
 * a step fades in and out in its own clothes (`IntroDecisionBar`).
 */
internal fun introBarLeaving(held: IntroHeldBar?, step: IntroStep, currentShown: Boolean): IntroHeldBar? =
    held?.takeIf { !currentShown && it.step != step }

// The bar the rider last saw. It is written from what the composition reads and nothing else reads it back before it is written,
// so it needs no snapshot state and costs no recomposition.
private class HeldBarHolder {
    var held: IntroHeldBar? = null
}

/**
 * Keeps [bar] while it is shown, as the bar of [step] drawn in [fill], and returns the bar that is leaving ([introBarLeaving]) for
 * [IntroLeavingBar] to fade out. [shown] is whether the bar of [step] is shown to the rider now.
 */
@Composable
internal fun rememberLeavingIntroBar(step: IntroStep, bar: IntroBar?, shown: Boolean, fill: Color?): IntroHeldBar? {
    val holder = remember { HeldBarHolder() }
    if (bar != null && shown) holder.held = IntroHeldBar(step, bar, fill)
    return introBarLeaving(holder.held, step, currentShown = shown)
}

/**
 * A bar that is fading out, drawn over the bottom of the screen as [held] was: in its own words, layout (row or stacked) and fill, over
 * [BAR_REVEAL_MS], which is the fade the bar has always had. It takes no press, has nothing for a screen reader or the keyboard, and
 * is not part of the layout, so the room the screen reserves for its bar is the new step's from the first frame and nothing moves
 * while this fades. When it is gone it draws and takes nothing. With reduced motion it is not drawn at all: the old bar cuts out in
 * the frame the step changes, with its own content, and the new bar is not seen before it is shown.
 */
@Composable
internal fun IntroLeavingBar(held: IntroHeldBar, modifier: Modifier = Modifier) {
    val reveal = introBarRevealMs(isReduceMotionEnabled())
    var gone by remember(held) { mutableStateOf(reveal == 0) }
    val alpha = remember(held) { Animatable(1f) }
    LaunchedEffect(held, reveal) {
        if (reveal > 0) alpha.animateTo(0f, tween(durationMillis = reveal))
        gone = true
    }
    if (gone) return
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha.value }
            .clearAndSetSemantics {}
            .focusProperties { canFocus = false }
            .consumeTouches(true),
    ) {
        IntroDecisionBar(
            left = held.bar.left,
            right = held.bar.right,
            onEvent = {},
            rightShown = held.bar.rightShown,
            acceptsPresses = false,
            labelPairs = held.bar.labelPairs,
            primaryFill = held.fill,
        )
    }
}
