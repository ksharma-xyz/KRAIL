package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Where the search row's one action button (Reverse or Ask KRAIL) sits relative to the To and From bars. */
internal enum class ActionPlacement {
    /** Beside the bars, centred on the two of them. */
    Beside,

    /** In its own row under the From bar, at its end, so the bars take the whole width. */
    Under,
}

/**
 * The width of "Town Hall Station" at 100 percent text in a To or From bar, measured in the real
 * layout (145 dp, against 125 for "Central Station" and 160 for "Parramatta Station"). It stands
 * for a typical stop name. It is scaled linearly with the font scale, which is the safe side: on
 * Android 14 and later large text grows more slowly than that, so the button goes under a little
 * earlier than strictly needed and never later.
 */
internal val TypicalNameWidth: Dp = 145.dp

/**
 * Whether the action button fits beside the bars or goes under the From bar. When width is short
 * the layout goes vertical rather than squeezing the stop names (rule 13).
 *
 * The name room beside the bars is [contentWidth] less the [besideCost] of the button column and
 * [barReserved], the end columns the bar keeps for its own controls. The button stays beside while
 * that room still holds a typical name at the current [fontScale], and goes under otherwise.
 * Pure, so the boundary is driven directly.
 */
internal fun actionPlacementFor(
    contentWidth: Dp,
    fontScale: Float,
    besideCost: Dp,
    barReserved: Dp,
): ActionPlacement {
    val nameRoom = contentWidth - besideCost - barReserved
    return if (nameRoom >= TypicalNameWidth * fontScale) ActionPlacement.Beside else ActionPlacement.Under
}
