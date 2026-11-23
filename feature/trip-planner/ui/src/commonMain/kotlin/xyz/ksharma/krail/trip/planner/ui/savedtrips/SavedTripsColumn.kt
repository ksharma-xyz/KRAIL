package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The widest the saved trips column grows: `docs/MAP_HOME_UX.md` rule 45.
 *
 * Phones are narrower than this, so they keep every dp they have. A window between this and the
 * dual pane gate (840 dp) gets the column centred with a gutter either side. The dual pane's
 * 480 dp list pane is narrower still, so inside it the column is the whole pane.
 */
internal val SAVED_TRIPS_COLUMN_MAX_WIDTH: Dp = 560.dp

/**
 * Width of the one saved trips column in a pane [availableWidth] wide.
 *
 * Pure, so the rule can be driven at every boundary without a layout. A pane with no room, or a
 * negative width a measure pass can briefly hand over, gets a column with no width rather than a
 * negative one.
 */
internal fun columnWidthFor(availableWidth: Dp): Dp =
    availableWidth.coerceIn(0.dp, SAVED_TRIPS_COLUMN_MAX_WIDTH)

/** Empty space on each side of the column in a pane [availableWidth] wide, so it sits centred. */
internal fun columnGutterFor(availableWidth: Dp): Dp =
    (availableWidth - columnWidthFor(availableWidth)).coerceAtLeast(0.dp) / 2

/**
 * Puts a full-width child (the bottom search row) on the same left and right edges as the trip
 * cards and the Park & Ride section, which sit in the list's column.
 *
 * The list pads its content by [columnGutterFor] and this gives its child [columnWidthFor] and
 * centres it, so the two agree by construction. Both measure inside the horizontal safe area (the
 * list's own column is already inset by it, for a camera notch or iPhone Duo's outer display), or
 * uneven insets would centre the row on a different line than the list.
 *
 * Only the child is narrower. This node still reports the full width it was offered, so the
 * list behind it keeps every touch in the gutter.
 */
@Composable
internal fun Modifier.savedTripsColumn(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .layout { measurable, constraints ->
            val columnWidth = columnWidthFor(constraints.maxWidth.toDp()).roundToPx()
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = constraints.minWidth.coerceAtMost(columnWidth),
                    maxWidth = columnWidth,
                ),
            )
            val outerWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
            layout(outerWidth, placeable.height) {
                placeable.place(x = (outerWidth - placeable.width) / 2, y = 0)
            }
        }
