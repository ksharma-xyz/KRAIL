package xyz.ksharma.krail.trip.planner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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

/**
 * Resolves a text style based on whether an item represents a past/previous event.
 *
 * - **Past**: returns [KrailTheme.typography.titleMediumRegular] — identical metrics to
 *   `titleMedium` (16sp, lineHeight 20sp, letterSpacing 0.15sp) but [FontWeight.Normal],
 *   giving past items a visually lighter appearance that pairs with [pastDepartureColor].
 * - **Upcoming / active**: returns [activeStyle] unchanged.
 *
 * Use this everywhere `titleMedium` (bold) is used for text that should de-emphasise when past.
 */
@Composable
fun pastDepartureTextStyle(isPast: Boolean, activeStyle: TextStyle): TextStyle =
    if (isPast) KrailTheme.typography.titleMediumRegular else activeStyle
