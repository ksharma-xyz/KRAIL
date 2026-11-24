package xyz.ksharma.krail.trip.planner.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.DpRect
import xyz.ksharma.krail.core.testing.device.visibleBoundsInRoot

/**
 * Where the rider can see this node, in dp from the top left of the root: the node must be on screen and its bounds are the part
 * no ancestor clips. The one implementation, and the reasoning for it, is `visibleBoundsInRoot` in `:core:testing`, which the
 * device profile matrix uses too, so a layout probe and a matrix test measure the same thing.
 */
internal fun SemanticsNodeInteraction.getVisibleBoundsInRoot(): DpRect = visibleBoundsInRoot()
