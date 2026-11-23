package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopLabelAssignRow
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop

/**
 * One stop of the destination or origin list, drawn by the row Search Stop draws for a stop (`StopLabelAssignRow`):
 * the name in the same type, the mode icons under it, the same padding, so the Intro and Search Stop show the same list. It
 * is not outlined and has no look of its own.
 *
 * The row has no label chevron, because nothing is labelled here. The selected row takes the app's activated row
 * treatment, the flat theme tint with rounded corners that `ManageStopLabelRow` uses for the row that is open, and
 * is announced as selected, so the state is shape and tint and also something a screen reader says, never colour
 * alone. No row is selected when the list opens. A tap on the row selects it and does not move on, and a tap on
 * another row moves the selection.
 *
 * The tap area, the ripple and the tint are one shape: the whole row, edge to edge, from the room above its name to the
 * room under its icons, with the rounded corners. They are one node, clipped to the shape, which draws the tint, takes the
 * click and is the one button a screen reader finds, with the stop's name and whether it is selected. The row inside has no
 * click of its own. Rows follow one another with nothing between them, so a finger between two rows is on one of them and
 * there is no strip that is tinted and does nothing, or that does something and is not tinted. This is the Intro's own
 * rule: Search Stop's row is not changed and keeps its own tap area.
 */
@Composable
internal fun IntroStopRow(
    stop: IntroStop,
    modes: List<TransportMode>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(KrailTheme.dimensions.radiusM)
    // The outer box carries only what is passed in, such as the tag of the first row. The row is the node with the tap.
    Box(modifier = modifier) {
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
            // The row has no click of its own: the node around it takes it, over the tint and the room between the rows.
            onRowClick = null,
            // The shape, the tint, the click and its ripple, the tag and the selected state are on this one node, which is as
            // wide and as tall as the row. The room under the icons, which the row has, is inside it, as is the room above the name.
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(if (selected) Modifier.background(themeBackgroundColor(), shape) else Modifier)
                .klickable(onClick = onClick)
                .testTag(TripPlannerTestTags.introStop(stop.stopId))
                .semantics { this.selected = selected }
                .padding(bottom = KrailTheme.dimensions.spacingM),
        )
    }
}
