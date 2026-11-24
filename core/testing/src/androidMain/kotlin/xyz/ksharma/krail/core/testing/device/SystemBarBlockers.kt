package xyz.ksharma.krail.core.testing.device

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** The names of the four regions, in the words a failure message uses. */
internal object SystemRegion {
    const val TOP = "the top system region"
    const val BOTTOM = "the bottom system region"
    const val LEFT = "the left system region"
    const val RIGHT = "the right system region"
}

/** What the system regions took: every press that landed on one, so a test can tell a touch reached the app or did not. */
internal class BlockerLog {
    var presses: Int = 0
        private set
    var lastRegion: String? = null
        private set

    fun record(region: String) {
        presses++
        lastRegion = region
    }

    fun reset() {
        presses = 0
        lastRegion = null
    }
}

/**
 * A node that takes every touch over each edge the system owns, drawn over the whole screen as the system draws its bars: a
 * press on one never reaches what is underneath, so a control standing there cannot be tapped, as on a device. Each is the full
 * strip. The real status pill is smaller than the iPhone Duo's 84 pt side strip, so a strip is the stricter rule: the strip is
 * the system's, whatever it draws in it.
 *
 * Declared after the content in the same box so it is above it in z order, and outside the app's theme so a theme that pads for
 * the system bars cannot move it.
 */
@Composable
internal fun BoxScope.SystemBarBlockers(insets: SystemInsets, log: BlockerLog) {
    if (insets.top > 0) {
        Blocker(SystemRegion.TOP, log, Modifier.align(Alignment.TopStart).fillMaxWidth().height(insets.top.dp))
    }
    if (insets.bottom > 0) {
        Blocker(SystemRegion.BOTTOM, log, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(insets.bottom.dp))
    }
    if (insets.left > 0) {
        Blocker(SystemRegion.LEFT, log, Modifier.align(Alignment.TopStart).fillMaxHeight().width(insets.left.dp))
    }
    if (insets.right > 0) {
        Blocker(SystemRegion.RIGHT, log, Modifier.align(Alignment.TopEnd).fillMaxHeight().width(insets.right.dp))
    }
}

@Composable
private fun Blocker(region: String, log: BlockerLog, modifier: Modifier) {
    Box(
        modifier = modifier.pointerInput(log) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                    if (event.type == PointerEventType.Press) {
                        log.record(region)
                    }
                }
            }
        },
    )
}
