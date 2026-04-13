package xyz.ksharma.krail.trip.planner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Resolves a colour based on whether an item represents a past/previous event.
 *
 * - **Past**: all colours normalise to `KrailTheme.colors.onSurface`, providing uniform
 *   neutral text that pairs with the `pastDepartureRowSurface` background.
 * - **Upcoming / active**: returns [activeColor] unchanged.
 *
 * Use this everywhere a colour would otherwise be written as
 * `if (isPast) KrailTheme.colors.onSurface else someColor` — in `DepartureRow`,
 * `JourneyCard`, and `ScheduledTimeRow` — to keep past-item colour logic in one place.
 */
@Composable
fun pastDepartureColor(isPast: Boolean, activeColor: Color): Color =
    if (isPast) KrailTheme.colors.onSurface else activeColor
