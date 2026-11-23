package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Takes every touch on this part of the screen before anything under it can see it, while [consume] is true, and does
 * nothing when it is false. It is how a part of the Intro that is drawn but not ready for a press, a screen still sliding
 * in or out and a button still fading in, ignores the press without being drawn as a disabled button: the touch is
 * consumed on the way down, so the button under it never gets a click. The touch is not passed on, so the rider's
 * second tap on a bar that is changing in place cannot land on the next screen's button.
 */
internal fun Modifier.consumeTouches(consume: Boolean): Modifier = if (consume) {
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }
} else {
    this
}
