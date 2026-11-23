package xyz.ksharma.krail.trip.planner.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.DpRect

/**
 * Where the rider can see this node, in dp from the top left of the root.
 *
 * A layout probe measures what the rider sees. The node's bounds are read from the semantics node's `boundsInRoot`, which Compose
 * clips to every ancestor, so a node a parent cuts off reports the part that is left of it and not the room it was laid out in.
 * A node clipped away entirely would report an empty rectangle at the origin, and a comparison against that can pass by accident,
 * so the node must also be on screen, which `assertIsDisplayed()` proves by walking the clip chain. Both checks run on every call,
 * so a test that measures through this helper cannot go green against a node that is not there to be seen.
 *
 * Where nothing is clipped the numbers are the ones the node was laid out with. The density is read from the node itself, so the
 * test needs none of its own.
 */
internal fun SemanticsNodeInteraction.getVisibleBoundsInRoot(): DpRect {
    assertIsDisplayed()
    val node = fetchSemanticsNode("Failed to retrieve the visible bounds of the node.")
    return with(node.layoutInfo.density) {
        val bounds = node.boundsInRoot
        DpRect(bounds.left.toDp(), bounds.top.toDp(), bounds.right.toDp(), bounds.bottom.toDp())
    }
}
