package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import xyz.ksharma.krail.taj.theme.KrailTheme
import kotlin.math.max

/** How the card's two buttons sit. */
internal enum class SuggestionActionsLayout {
    /** Not now at the left and Add to home at the right, on one line. */
    Beside,

    /** Add to home on top and Not now under it, each the full width. Used whenever two whole buttons do not fit. */
    Stacked,
}

/**
 * Beside while both buttons fit on one line at the width they would each take on their own, with the gap
 * between them, and stacked otherwise. A button is one whole item: it is never made narrower, and its label
 * never breaks across lines to share a row with its neighbour.
 */
internal fun suggestionActionsLayout(
    notNowWidth: Int,
    addToHomeWidth: Int,
    gap: Int,
    available: Int,
): SuggestionActionsLayout =
    if (notNowWidth + gap + addToHomeWidth <= available) {
        SuggestionActionsLayout.Beside
    } else {
        SuggestionActionsLayout.Stacked
    }

/**
 * The card's two buttons, `docs/MAP_HOME_UX.md` rule 13: beside each other when there is room for both
 * whole, with the outlined [notNow] at the left and the primary [addToHome] at the right, and one under the
 * other when there is not, with the primary on top. The room is judged on what each button asks for on its own,
 * so it follows the text size and the window with no width written down.
 *
 * Each button is the caller's own taj button, so this places controls and draws none.
 */
@Composable
internal fun SuggestionActions(
    notNow: @Composable () -> Unit,
    addToHome: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = KrailTheme.dimensions.spacingM
    Layout(
        content = {
            notNow()
            addToHome()
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val notNowItem = measurables[0]
        val addToHomeItem = measurables[1]
        val gapPx = gap.roundToPx()
        val width = constraints.maxWidth
        val notNowWidth = notNowItem.maxIntrinsicWidth(constraints.maxHeight)
        val addToHomeWidth = addToHomeItem.maxIntrinsicWidth(constraints.maxHeight)

        when (suggestionActionsLayout(notNowWidth, addToHomeWidth, gapPx, width)) {
            SuggestionActionsLayout.Beside -> {
                val notNowPlaceable = notNowItem.measure(Constraints.fixedWidth(notNowWidth))
                val addToHomePlaceable = addToHomeItem.measure(Constraints.fixedWidth(addToHomeWidth))
                val height = max(notNowPlaceable.height, addToHomePlaceable.height)
                layout(width, height) {
                    notNowPlaceable.placeRelative(0, (height - notNowPlaceable.height) / 2)
                    addToHomePlaceable.placeRelative(
                        x = width - addToHomeWidth,
                        y = (height - addToHomePlaceable.height) / 2,
                    )
                }
            }
            SuggestionActionsLayout.Stacked -> {
                val addToHomePlaceable = addToHomeItem.measure(Constraints.fixedWidth(width))
                val notNowPlaceable = notNowItem.measure(Constraints.fixedWidth(width))
                layout(width, addToHomePlaceable.height + gapPx + notNowPlaceable.height) {
                    addToHomePlaceable.placeRelative(0, 0)
                    notNowPlaceable.placeRelative(0, addToHomePlaceable.height + gapPx)
                }
            }
        }
    }
}
