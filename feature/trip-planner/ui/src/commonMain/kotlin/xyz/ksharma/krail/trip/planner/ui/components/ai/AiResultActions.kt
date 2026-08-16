package xyz.ksharma.krail.trip.planner.ui.components.ai

/**
 * The three things the result card can do, bundled so the card, the surface that hosts it and
 * the screen that supplies them all pass one value rather than three lambdas each.
 */
data class AiResultActions(
    val onEditFrom: () -> Unit = {},
    val onEditTo: () -> Unit = {},
    val onSeeTimes: () -> Unit = {},
)
