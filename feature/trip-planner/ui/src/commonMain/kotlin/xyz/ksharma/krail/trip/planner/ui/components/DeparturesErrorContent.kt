package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared error state composable for departure board surfaces.
 *
 * Used by both [DepartureBoardStopCard] (bottom sheet / uncontrolled card) and the
 * [AccordionStopSection] inside the SavedTrips accordion so both surfaces show a
 * consistent error message and retry affordance.
 *
 * @param onRetry Called when the user taps "Retry". Pass `null` to hide the button
 *                (e.g. when the caller cannot support inline retry).
 */
@Composable
internal fun DeparturesErrorContent(
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ErrorMessage(
        title = "Couldn't load departures",
        message = "Check your connection and try again.",
        actionData = onRetry?.let {
            ActionData(actionText = "Retry", onActionClick = it)
        },
        modifier = modifier,
    )
}
