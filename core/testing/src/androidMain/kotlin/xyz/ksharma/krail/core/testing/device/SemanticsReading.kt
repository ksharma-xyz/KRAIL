package xyz.ksharma.krail.core.testing.device

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.DpRect

private val INTERACTIVE_ROLES = setOf(
    Role.Button,
    Role.Checkbox,
    Role.Switch,
    Role.RadioButton,
    Role.Tab,
    Role.DropdownList,
)

/**
 * Where the rider can see this node, in dp from the top left of the root.
 *
 * A layout probe measures what the rider sees. The node's bounds are read from the semantics node's `boundsInRoot`, which Compose
 * clips to every ancestor, so a node a parent cuts off reports the part that is left of it and not the room it was laid out in.
 * A node clipped away entirely would report an empty rectangle at the origin, and a comparison against that can pass by
 * accident, so the node must also be on screen, which `assertIsDisplayed()` proves by walking the clip chain. Both checks run on
 * every call, so a test that measures through this helper cannot go green against a node that is not there to be seen.
 *
 * Where nothing is clipped the numbers are the ones the node was laid out with. The density is read from the node itself, so the
 * test needs none of its own.
 */
fun SemanticsNodeInteraction.visibleBoundsInRoot(): DpRect {
    assertIsDisplayed()
    val node = fetchSemanticsNode("Failed to retrieve the visible bounds of the node.")
    return with(node.layoutInfo.density) {
        val bounds = node.boundsInRoot
        DpRect(bounds.left.toDp(), bounds.top.toDp(), bounds.right.toDp(), bounds.bottom.toDp())
    }
}

/** The node and everything under it in the tree it came from, depth first. */
internal fun SemanticsNode.withDescendants(): Sequence<SemanticsNode> =
    sequenceOf(this) + children.asSequence().flatMap { it.withDescendants() }

/** The ancestors of this node, nearest first. */
internal fun SemanticsNode.ancestors(): Sequence<SemanticsNode> = generateSequence(parent) { it.parent }

/**
 * Whether the rider can press, toggle, select or type into this node: it has a click or long click action, text to set, a
 * toggle or selected state, or a role that is a control.
 */
internal fun SemanticsNode.isInteractive(): Boolean {
    val c = config
    return c.contains(SemanticsActions.OnClick) ||
        c.contains(SemanticsActions.OnLongClick) ||
        c.contains(SemanticsActions.SetText) ||
        c.contains(SemanticsProperties.ToggleableState) ||
        c.contains(SemanticsProperties.Selected) ||
        c.getOrNull(SemanticsProperties.Role) in INTERACTIVE_ROLES
}

/** Whether the node is placed and has an area left after every ancestor has clipped it. */
internal fun SemanticsNode.isDrawn(): Boolean =
    layoutInfo.isPlaced && boundsInRoot.width > 0f && boundsInRoot.height > 0f

/** A node named the way a person would find it: its test tag, its text, its description, and failing those its id. */
internal fun SemanticsNode.describe(): String {
    val tag = config.getOrNull(SemanticsProperties.TestTag)?.let { "tag '$it'" }
    val text = config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }?.let { "text '$it'" }
    val description = config.getOrNull(SemanticsProperties.ContentDescription)
        ?.joinToString(" ")
        ?.let { "description '$it'" }
    return listOfNotNull(tag, text, description).ifEmpty { listOf("node #$id") }.joinToString(", ")
}
