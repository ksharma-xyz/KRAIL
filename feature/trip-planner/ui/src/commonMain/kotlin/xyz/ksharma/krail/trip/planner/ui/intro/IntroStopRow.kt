package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopLabelAssignRow
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop

/**
 * One stop of the destination or origin list, drawn by the row Search Stop draws for a stop (`StopLabelAssignRow`):
 * the name in the same type, the mode icons under it, the same padding and the same tap feedback, so the Intro and
 * Search Stop show the same list. It is not outlined and has no look of its own.
 *
 * The row has no label chevron, because nothing is labelled here. The highlighted row takes the app's activated row
 * treatment, the flat theme tint with rounded corners that `ManageStopLabelRow` uses for the row that is open, and
 * is announced as selected, so the state is shape and tint and also something a screen reader says, never colour
 * alone. A tap on the row is a choice, and it moves on.
 */
@Composable
internal fun IntroStopRow(
    stop: IntroStop,
    modes: List<TransportMode>,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(KrailTheme.dimensions.radiusM)
    StopLabelAssignRow(
        stopName = stop.name,
        transportModeSet = modes,
        stopLabels = persistentListOf(),
        assignedLabel = null,
        expanded = false,
        // No label is assigned from the Intro, so these never fire: the chevron that would call them is not drawn.
        onExpandToggle = {},
        onLabelPillClick = {},
        onNewLabelClick = {},
        showLabelAssign = false,
        onRowClick = onClick,
        // The highlight is the flat theme tint edge to edge with rounded corners, as ManageStopLabelRow's open row has.
        // The row has room above its name and none below, so the bottom is given the same room inside the tint.
        modifier = modifier
            .then(if (highlighted) Modifier.background(themeBackgroundColor(), shape) else Modifier)
            .padding(bottom = KrailTheme.dimensions.spacingM),
        // On the node that has the click and the name, so a screen reader reads one button that says it is selected.
        tapAreaModifier = Modifier
            .testTag(TripPlannerTestTags.introStop(stop.stopId))
            .semantics { selected = highlighted },
    )
}
