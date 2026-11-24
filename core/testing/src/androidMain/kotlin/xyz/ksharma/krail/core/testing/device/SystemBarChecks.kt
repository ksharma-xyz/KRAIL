package xyz.ksharma.krail.core.testing.device

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import kotlin.math.roundToInt

/** One dp in px: how far past a system region a node may reach before it counts as standing in it. */
private fun SemanticsNode.tolerancePx(): Float = layoutInfo.density.density

/**
 * Every control under [root] that stands, in whole or in part, in a region the system owns, each as one line for a failure
 * message. A control is a node the rider can press, toggle, select or type into, measured by the bounds the rider can see.
 *
 * A node inside a scroller may be partly under an edge the scroller can still move it away from: the bottom of a vertical list
 * that has more below, the top of one that has been scrolled down (the right and left of a horizontal one). That is what
 * `assertScrollEndClearsBottomBar` asks, and a scroller at the end of its range is checked like anything else. Across the scroll
 * axis a node has nowhere to go and is always checked. Scrollers are read as left to right and not reversed.
 */
internal fun controlsUnderSystemBars(root: SemanticsNode, profile: DeviceProfile): List<String> {
    val area = safeAreaOf(root, profile)
    val density = root.layoutInfo.density.density
    return root.controls()
        .mapNotNull { node ->
            val regions = node.regionsTaken(area)
            if (regions.isEmpty()) {
                null
            } else {
                "${node.describe()} at ${node.boundsInRoot.inDp(density)} stands in ${regions.joinToString(" and ")}"
            }
        }
        .toList()
}

/** How many controls [controlsUnderSystemBars] looked at, so a screen with none cannot pass by having nothing to check. */
internal fun controlCount(root: SemanticsNode): Int = root.controls().count()

private fun SemanticsNode.controls(): Sequence<SemanticsNode> =
    withDescendants().filter { it.isInteractive() && it.isDrawn() }

private fun SemanticsNode.scrollRanges(axis: SemanticsPropertyKey<ScrollAxisRange>): List<ScrollAxisRange> =
    ancestors().mapNotNull { it.config.getOrNull(axis) }.toList()

private fun SemanticsNode.regionsTaken(area: SafeArea): List<String> {
    val bounds = boundsInRoot
    val tolerance = tolerancePx()
    val vertical = scrollRanges(SemanticsProperties.VerticalScrollAxisRange)
    val horizontal = scrollRanges(SemanticsProperties.HorizontalScrollAxisRange)
    return buildList {
        if (bounds.left < area.left - tolerance && !horizontal.any { it.scrolledFromStart() }) add(SystemRegion.LEFT)
        if (bounds.top < area.top - tolerance && !vertical.any { it.scrolledFromStart() }) add(SystemRegion.TOP)
        if (bounds.right > area.right + tolerance && !horizontal.any { it.hasMoreAhead() }) add(SystemRegion.RIGHT)
        if (bounds.bottom > area.bottom + tolerance && !vertical.any { it.hasMoreAhead() }) add(SystemRegion.BOTTOM)
    }
}

/** The scroller has moved off its start, so content may have gone under the start edge on its way. */
private fun ScrollAxisRange.scrolledFromStart(): Boolean = value() > 0f

/** The scroller can still move toward its end, so content under the end edge can be scrolled clear of it. */
private fun ScrollAxisRange.hasMoreAhead(): Boolean = value() < maxValue()

/** The bottom edge of the lowest thing a scroller shows, in px, or null when it shows nothing. */
internal fun lowestContentBottom(scroller: SemanticsNode): Float? =
    scroller.withDescendants().drop(1).filter { it.isDrawn() }.maxOfOrNull { it.boundsInRoot.bottom }

internal fun Rect.inDp(density: Float): String =
    "(${(left / density).roundToInt()}, ${(top / density).roundToInt()}) to " +
        "(${(right / density).roundToInt()}, ${(bottom / density).roundToInt()}) dp"
