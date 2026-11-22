package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/** The Set Home and Set Work pills for [stopLabels], in order, and none until the labels have loaded. */
internal fun labelPillsFor(stopLabels: List<StopLabel>): ImmutableList<HomeChip.Label> =
    homeChipsFor(stopLabels).filterIsInstance<HomeChip.Label>().toImmutableList()

/**
 * The first row of the Saved Trips list, directly under the title bar. It scrolls away with the list, and it is
 * gone while the cards are being reordered, so a tap cannot leave that mode.
 */
internal fun LazyListScope.labelPillsItem(
    stopLabels: List<StopLabel>,
    visible: Boolean,
    onLabelClick: (labelKey: String) -> Unit,
) {
    val pills = labelPillsFor(stopLabels)
    if (pills.isEmpty()) return
    item(key = "label_pills") {
        AnimatedVisibility(
            visible = visible,
            enter = expandVertically(animationSpec = tween(durationMillis = 250)) +
                fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 200)) +
                fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            HomeLabelPills(pills = pills, onLabelClick = onLabelClick)
        }
    }
}

/**
 * Set Home and Set Work as one row of whole pills. Each is the chip taj's [xyz.ksharma.krail.taj.components.Button]
 * at its small size draws ([LabelChip]), in taj's own default colours, with a 48 dp touch target. The row is one
 * line at every width and text size and never wraps: when the pills do not both fit it scrolls sideways, whole
 * pills only, and the next one peeks in at the edge. There is no fade on either edge. A set label plans a trip to
 * it and an unset one starts setting it, as the chips always did.
 */
@Composable
internal fun HomeLabelPills(
    pills: ImmutableList<HomeChip.Label>,
    onLabelClick: (labelKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    LazyRow(
        modifier = modifier.testTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS),
        contentPadding = PaddingValues(horizontal = dim.pageHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
        // No glow or stretch at the ends either: nothing fades on any edge.
        overscrollEffect = null,
    ) {
        items(items = pills, key = { pill -> pill.label.label }) { pill ->
            LabelChip(
                label = pill.label,
                text = pill.text,
                onClick = { onLabelClick(pill.label.label) },
                colors = ButtonDefaults.buttonColors(),
            )
        }
    }
}
