@file:Suppress("StringLiteralDuplication")

package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.AssignToLabelIcon
import xyz.ksharma.krail.trip.planner.ui.components.LabelPillSize
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIcon
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIconSize
import xyz.ksharma.krail.trip.planner.ui.components.UnsetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * The stop row expands in place (same `animateContentSize` pattern as
 * `JourneyCard`/`TripSearchListItem`) to assign a label — no picker sheet.
 *
 * A stop row has exactly two states, and no path between them from this row:
 * - **Unassigned**: shows a plain "+" (`AssignToLabelIcon`). Tap → expands to show
 *   every label that isn't set anywhere else, as outlined pills — solid/set-elsewhere
 *   labels are never shown here, so there is no reassign-by-tapping-a-pill flow on this
 *   row. Tap an outlined pill → assigns instantly, no confirm, row collapses.
 * - **Assigned**: shows the label as a small, solid, non-interactive pill inline next
 *   to the transport-mode icons. No icon, no expand, no way to change or remove the
 *   label from here — that only happens in Manage.
 *
 * Pills never show a tick — solid means "set here", outline means "not set". A stop
 * can only ever be assigned to ONE label (the same 1:1 invariant as today),
 * automatically satisfied here since the wall excludes labels that are already set.
 */
@Composable
internal fun StopLabelAssignRow(
    stopName: String,
    transportModeSet: List<TransportMode>,
    stopLabels: ImmutableList<StopLabel>,
    assignedLabel: StopLabel?,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onLabelPillClick: (StopLabel) -> Unit,
    onNewLabelClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRowClick: (() -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions

    Column(
        modifier = modifier
            .padding(horizontal = dim.pageHorizontalPadding)
            .animateContentSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (onRowClick != null) Modifier.klickable(onClick = onRowClick) else Modifier)
                    // Vertical padding lives INSIDE the klickable chain (after it, not
                    // on the outer Column) so the ripple/touch target covers the row's
                    // full visual height — previously the padding sat outside the
                    // click modifier, so tapping that top margin did nothing and the
                    // ripple looked clipped to just the text+icons content.
                    .padding(top = dim.spacingM),
            ) {
                xyz.ksharma.krail.taj.components.Text(
                    text = stopName,
                    style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = KrailTheme.colors.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                    verticalAlignment = Alignment.CenterVertically,
                    // A touch more breathing room between the stop name and the
                    // mode-icon/pill row underneath it.
                    modifier = Modifier.padding(top = dim.spacingXXS),
                ) {
                    transportModeSet.forEach { mode ->
                        TransportModeIcon(transportMode = mode, size = TransportModeIconSize.Small)
                    }
                    if (assignedLabel != null) {
                        SetLabelPill(label = assignedLabel, size = LabelPillSize.Small, showIcon = false)
                    }
                }
            }
            if (assignedLabel == null) {
                AssignToLabelIcon(expanded = expanded, onClick = onExpandToggle)
            }
        }

        if (expanded && assignedLabel == null) {
            val unsetLabels = remember(stopLabels) { stopLabels.filterNot { it.isSet } }

            LazyRow(
                contentPadding = PaddingValues(top = dim.spacingM),
                horizontalArrangement = Arrangement.spacedBy(dim.spacingS),
            ) {
                items(items = unsetLabels, key = { it.label }) { label ->
                    UnsetLabelPill(
                        label = label,
                        showIcon = false,
                        modifier = Modifier
                            .clip(RoundedCornerShape(dim.radiusFull))
                            .klickable { onLabelPillClick(label) },
                    )
                }
                item(key = "new-label") {
                    NewLabelAssignChip(onClick = onNewLabelClick)
                }
            }
        }

        // Trailing margin before the next row — deliberately outside the klickable
        // chain above (it's the gap between rows, not part of this row's own tap
        // target).
        Spacer(modifier = Modifier.height(dim.spacingM))
    }
}

@Composable
private fun NewLabelAssignChip(onClick: () -> Unit) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    Row(
        modifier = Modifier
            .clip(shape)
            // onSurface, not outlineSubtle — matches UnsetLabelPill's border weight
            // so this chip doesn't read as fainter than the label pills beside it.
            .border(dim.strokeThin, KrailTheme.colors.onSurface, shape)
            .klickable(onClick = onClick)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
    ) {
        xyz.ksharma.krail.taj.components.Text(
            text = "+ New label",
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.label,
        )
    }
}

// region Previews

private val previewUnsetLabels = persistentListOf(
    StopLabel(emoji = "🏠", label = "Home"),
    StopLabel(emoji = "💼", label = "Work"),
)

private val previewMixedLabels = persistentListOf(
    StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Central Station"),
    StopLabel(emoji = "💼", label = "Work"),
    StopLabel(emoji = "🏋", label = "Gym", stopId = "2", stopName = "Bondi Junction"),
)

@PreviewComponent
@Composable
private fun PreviewStopLabelAssignRow_Unassigned() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        StopLabelAssignRow(
            stopName = "Wynyard Station",
            transportModeSet = listOf(TransportMode.Train),
            stopLabels = previewMixedLabels,
            assignedLabel = null,
            expanded = false,
            onExpandToggle = {},
            onLabelPillClick = {},
            onNewLabelClick = {},
        )
    }
}

/** Tap "+" → wall shows only unset labels (Home/Gym here are already set elsewhere). */
@PreviewComponent
@Composable
private fun PreviewStopLabelAssignRow_ExpandedUnsetOnly() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        StopLabelAssignRow(
            stopName = "Wynyard Station",
            transportModeSet = listOf(TransportMode.Train),
            stopLabels = previewUnsetLabels,
            assignedLabel = null,
            expanded = true,
            onExpandToggle = {},
            onLabelPillClick = {},
            onNewLabelClick = {},
        )
    }
}

/** Assigned + locked: small solid pill next to the mode icon, no "+"/tick, no expand. */
@PreviewComponent
@Composable
private fun PreviewStopLabelAssignRow_Assigned() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        StopLabelAssignRow(
            stopName = "Central Station",
            transportModeSet = listOf(TransportMode.Train, TransportMode.LightRail),
            stopLabels = previewMixedLabels,
            assignedLabel = previewMixedLabels[0],
            expanded = false,
            onExpandToggle = {},
            onLabelPillClick = {},
            onNewLabelClick = {},
        )
    }
}

// endregion
